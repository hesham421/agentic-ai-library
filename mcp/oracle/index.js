import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { CallToolRequestSchema, ListToolsRequestSchema } from "@modelcontextprotocol/sdk/types.js";
import { spawn } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";
import dotenv from "dotenv";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
dotenv.config({ path: path.join(__dirname, ".env") });

// This DB issues pre-11g ("0x939") password verifiers, which node-oracledb's Thin mode
// cannot authenticate and Thick mode would need Oracle Instant Client 19+ for. SQLcl's
// JDBC Thin driver handles this verifier natively, so it stays the execution engine here.
const SQLCL_PATH = process.env.SQLCL_PATH || "C:\\sources\\sqlcl-latest\\sqlcl\\bin\\sql.exe";

const ALLOW_WRITE = process.env.ALLOW_WRITE === "true";
const MAX_ROWS = parseInt(process.env.MAX_ROWS || "1000", 10);
const QUERY_TIMEOUT_MS = parseInt(process.env.QUERY_TIMEOUT_MS || "30000", 10);

function buildConnectString(envPrefix) {
  const host = process.env[`${envPrefix}_DB_HOST`];
  const port = process.env[`${envPrefix}_DB_PORT`];
  const service = process.env[`${envPrefix}_DB_SERVICE`];
  const user = process.env[`${envPrefix}_DB_USER`];
  const password = process.env[`${envPrefix}_DB_PASSWORD`];
  if (!host || !port || !service || !user || !password) return null;
  return `${user}/${password}@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=${host})(PORT=${port}))(CONNECT_DATA=(SERVICE_NAME=${service})))`;
}

// TEST is the only connection this server knows. There is no PROD connection by design.
const CONNECTIONS = {
  TEST: buildConnectString("TEST"),
};

// Runs one SQLcl session. Returns SQLcl's raw stdout and exit code.
function runSqlcl(script, env) {
  const connection = CONNECTIONS[env];
  if (!connection) {
    return Promise.reject(new Error(`Missing ${env} DB connection settings. Copy .env.example to .env and fill it in.`));
  }

  return new Promise((resolve, reject) => {
    const proc = spawn(SQLCL_PATH, ["-L", connection], {
      stdio: ["pipe", "pipe", "pipe"],
    });

    let out = "";
    const timer = setTimeout(() => {
      proc.kill();
      reject(new Error(`Query timed out after ${QUERY_TIMEOUT_MS}ms`));
    }, QUERY_TIMEOUT_MS);

    proc.stdout.on("data", (chunk) => {
      out += chunk.toString();
    });

    proc.stdin.write(script);
    proc.stdin.end();

    proc.on("close", (code) => {
      clearTimeout(timer);
      resolve({ out, code });
    });

    proc.on("error", (e) => {
      clearTimeout(timer);
      reject(e);
    });
  });
}

// SELECT / WITH: rows as JSON.
async function runQuery(sql, env) {
  const { out } = await runSqlcl(
    "SET SQLFORMAT JSON\nSET SQLBLANKLINES ON\n" + sql + ";\nEXIT;\n",
    env
  );

  const start = out.indexOf('{"results"');
  if (start === -1) {
    throw new Error("No JSON found in SQLcl output:\n" + out);
  }
  const end = out.lastIndexOf("}") + 1;
  const parsed = JSON.parse(out.slice(start, end));

  const block = parsed.results[0];
  if (!block) {
    return { status: "success", rowCount: 0, truncated: false, data: [] };
  }

  const cols = block.columns.map((c) => c.name.toUpperCase());
  const allRows = block.items.map((item) => {
    const row = {};
    cols.forEach((col) => {
      row[col] = item[col] !== undefined ? item[col] : item[col.toLowerCase()];
    });
    return row;
  });

  const truncated = allRows.length > MAX_ROWS;
  const data = truncated ? allRows.slice(0, MAX_ROWS) : allRows;
  return { status: "success", rowCount: data.length, truncated, data };
}

// DML / DDL / PL/SQL: committed on success, rolled back and reported on any error.
async function runWrite(sql, plsql, env) {
  const body = plsql ? sql + "\n/\n" : sql + ";\n";
  const { out, code } = await runSqlcl(
    "SET SQLBLANKLINES ON\nSET FEEDBACK ON\nWHENEVER SQLERROR EXIT FAILURE ROLLBACK\n" + body + "COMMIT;\nEXIT;\n",
    env
  );

  if (code !== 0 || /\b(ORA|PLS|SP2)-\d{4,5}\b|Error report/.test(out)) {
    throw new Error("Statement failed and was rolled back:\n" + out.slice(out.indexOf("Connected to")));
  }
  const feedback = out
    .split(/\r?\n/)
    .map((l) => l.trim())
    .filter((l) => /\b(created|altered|dropped|truncated|inserted|updated|deleted|merged|succeeded|completed|Commit complete|Grant|Revoke|renamed)\b/i.test(l));
  return { status: "success", committed: true, feedback };
}

// ── Remote-database guard ────────────────────────────────────────────────
// TEST holds database links (some named *PROD*) and public synonyms over them, so a plain
// "SELECT * FROM <synonym>" can reach another database. Every statement is refused if it
// contains "@" (a link, CONNECT, or a SQLcl script — even inside a string, to cover dynamic
// SQL), or names an object that resolves through a link. That list is read from TEST itself.

let remoteNamesPromise = null;

function loadRemoteNames() {
  remoteNamesPromise ??= runQuery(
    "SELECT synonym_name AS n FROM all_synonyms WHERE db_link IS NOT NULL " +
      "UNION SELECT name FROM all_dependencies WHERE referenced_link_name IS NOT NULL",
    "TEST"
  )
    .then((r) => new Set(r.data.map((row) => String(row.N).toUpperCase())))
    .catch((e) => {
      remoteNamesPromise = null; // retry on the next call; refuse this one
      throw new Error("Could not load the remote-object list, statement refused: " + e.message);
    });
  return remoteNamesPromise;
}

function stripComments(sql) {
  return sql.replace(/\/\*[\s\S]*?\*\//g, " ").replace(/--[^\n]*/g, " ");
}

async function assertLocalOnly(sql) {
  if (sql.includes("@")) {
    throw new Error('Refused: "@" is not allowed (database links, CONNECT and scripts can reach other databases).');
  }
  if (/\bdatabase\s+link\b/i.test(sql)) {
    throw new Error("Refused: database links cannot be created, changed or used.");
  }
  const remote = await loadRemoteNames();
  const words = stripComments(sql).replace(/'(?:[^']|'')*'/g, " ").toUpperCase().match(/[A-Z_][A-Z0-9_$#]*/g) ?? [];
  const hit = words.find((w) => remote.has(w));
  if (hit) {
    throw new Error(`Refused: ${hit} resolves through a database link to another database.`);
  }
}

// ── Statement classification ─────────────────────────────────────────────

const WRITE_KEYWORDS = /^(insert|update|delete|merge|create|alter|drop|truncate|comment|rename|grant|revoke|begin|declare|call)\b/i;
const PLSQL_START = /^(begin|declare|create\s+(or\s+replace\s+)?(editionable\s+|noneditionable\s+)?(procedure|function|package|trigger|type))\b/i;

function classify(sql) {
  const trimmed = sql.trim();
  const head = stripComments(trimmed).trim();
  if (/^\/\s*$/m.test(trimmed)) {
    throw new Error('A line holding only "/" is not allowed — send one statement per call.');
  }

  if (/^(select|with)\b/i.test(head)) {
    const stmt = trimmed.replace(/;\s*$/, "");
    if (stmt.includes(";")) throw new Error("Multiple statements are not allowed");
    return { kind: "query", stmt };
  }

  if (!WRITE_KEYWORDS.test(head)) {
    throw new Error("Unsupported statement. Allowed: SELECT/WITH, DML, DDL, PL/SQL blocks.");
  }
  if (!ALLOW_WRITE) {
    throw new Error("Writes are disabled. Set ALLOW_WRITE=true in .env to enable them on TEST.");
  }

  const plsql = PLSQL_START.test(head);
  const stmt = plsql ? trimmed : trimmed.replace(/;\s*$/, "");
  if (!plsql && stmt.includes(";")) throw new Error("Multiple statements are not allowed");
  return { kind: "write", stmt, plsql };
}

// ── MCP Server ─────────────────────────────────────────────────────────────

const server = new Server(
  { name: "oracle-mcp", version: "2.1.0" },
  { capabilities: { tools: {} } }
);

server.setRequestHandler(ListToolsRequestSchema, async () => ({
  tools: [
    {
      name: "executeQuery",
      description:
        "Execute one SQL statement against the Oracle TEST database via SQLcl (the only database this server can reach). " +
        "SELECT/WITH return rows as JSON. " +
        (ALLOW_WRITE
          ? "DML, DDL and PL/SQL blocks are executed and committed on success, rolled back on error. "
          : "Writes are disabled. ") +
        "Database links and objects that resolve through them are refused.",
      inputSchema: {
        type: "object",
        properties: {
          sql: { type: "string", description: "One SQL statement or one PL/SQL block" },
          env: { type: "string", enum: ["TEST"], description: "Target environment (default: TEST)" },
        },
        required: ["sql"],
      },
    },
  ],
}));

server.setRequestHandler(CallToolRequestSchema, async (req) => {
  if (req.params.name !== "executeQuery") {
    throw new Error(`Unknown tool: ${req.params.name}`);
  }

  const { sql, env: rawEnv } = req.params.arguments ?? {};
  if (!sql) throw new Error("Missing required argument: sql");

  const env = (rawEnv || "TEST").toUpperCase();
  if (!(env in CONNECTIONS)) {
    throw new Error(`Unknown environment: ${env}. Only TEST is available.`);
  }

  // Mandatory logging (no credentials, no query results)
  console.error(`[MCP] ENV=${env}`);
  console.error(`[MCP] SQL=${sql}`);

  const { kind, stmt, plsql } = classify(sql);
  await assertLocalOnly(stmt);
  const result = kind === "query" ? await runQuery(stmt, env) : await runWrite(stmt, plsql, env);
  return {
    content: [{ type: "text", text: JSON.stringify(result, null, 2) }],
  };
});

// ── Startup ──────────────────────────────────────────────────────────────

async function main() {
  if (process.env.SKIP_STARTUP_TEST === "true") {
    console.error("[STARTUP] Skipping startup tests (SKIP_STARTUP_TEST=true).");
  } else {
    console.error("[STARTUP] Running startup test on TEST...");
    try {
      const result = await runQuery("SELECT 1 AS OK FROM DUAL", "TEST");
      console.error("[STARTUP] TEST Result:", JSON.stringify(result));
    } catch (err) {
      console.error("[STARTUP] TEST Error:", err.message);
    }
  }
  console.error(`[STARTUP] Writes ${ALLOW_WRITE ? "ENABLED" : "disabled"} on TEST.`);

  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error("[MCP] Oracle server ready on stdio");
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
