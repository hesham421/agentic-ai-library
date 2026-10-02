"""Reads the uniqueness invariants the database enforces on a module's tables from the entities' @Table(uniqueConstraints) and from the Flyway migrations, including partial unique indexes whose WHERE clause JPA cannot express."""

import re
from pathlib import Path
from typing import Optional

from models.api_doc_model import UniqueConstraint

SQL_COMMENT_RE = re.compile(r"--[^\n]*|/\*.*?\*/", re.DOTALL)
JAVA_COMMENT_RE = re.compile(r"/\*.*?\*/|//[^\n]*", re.DOTALL)
MIGRATION_VERSION_RE = re.compile(r"^V(\d+)(?:_(\d+))*__", re.IGNORECASE)
CLASS_RE = re.compile(r"\bclass\s+(\w+)")
TABLE_ANNOTATION_RE = re.compile(r"@Table\s*\(")
TABLE_NAME_RE = re.compile(r'\bname\s*=\s*"(\w+)"')
UNIQUE_CONSTRAINT_RE = re.compile(r"@UniqueConstraint\s*\(([^)]*)\)")
COLUMN_NAMES_RE = re.compile(r'columnNames\s*=\s*(?:\{([^}]*)\}|"(\w+)")')
QUOTED_RE = re.compile(r'"(\w+)"')
COLUMN_ANNOTATION_RE = re.compile(r"@(?:Join)?Column\s*\(")
ANNOTATION_RE = re.compile(r"@[\w.]+(?:\([^()]*(?:\([^()]*\)[^()]*)*\))?")
FIELD_DECL_RE = re.compile(r"\s*(?:(?:private|protected|public|final|transient|volatile|static)\s+)*"
                           r"[\w.]+(?:<[^=]*>)?(?:\[\])*\s+(\w+)\s*(?:=|$)")

ADD_UNIQUE_RE = re.compile(r"ALTER\s+TABLE\s+(?:ONLY\s+)?(\w+)\s+ADD\s+CONSTRAINT\s+(\w+)\s+UNIQUE\s*\(([^)]*)\)", re.IGNORECASE)
CREATE_UNIQUE_INDEX_RE = re.compile(
    r"CREATE\s+UNIQUE\s+INDEX\s+(?:CONCURRENTLY\s+)?(?:IF\s+NOT\s+EXISTS\s+)?(\w+)\s+ON\s+(?:ONLY\s+)?(\w+)"
    r"(?:\s+USING\s+\w+)?\s*\(([^)]*)\)(?:\s+INCLUDE\s*\([^)]*\))?(?:\s+WHERE\s+(.+))?$", re.IGNORECASE)
CREATE_TABLE_RE = re.compile(r"CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?(\w+)\s*\((.*)\)", re.IGNORECASE | re.DOTALL)
INLINE_UNIQUE_RE = re.compile(r"(?:CONSTRAINT\s+(\w+)\s+)?UNIQUE\s*\(([^)]*)\)", re.IGNORECASE)
DROP_CONSTRAINT_RE = re.compile(r"ALTER\s+TABLE\s+(?:ONLY\s+)?(\w+)\s+DROP\s+CONSTRAINT\s+(?:IF\s+EXISTS\s+)?(\w+)", re.IGNORECASE)
DROP_INDEX_RE = re.compile(r"DROP\s+INDEX\s+(?:CONCURRENTLY\s+)?(?:IF\s+EXISTS\s+)?(\w+)", re.IGNORECASE)
DROP_TABLE_RE = re.compile(r"DROP\s+TABLE\s+(?:IF\s+EXISTS\s+)?(\w+)", re.IGNORECASE)


def _columns(raw: str) -> list[str]:
    return [c.strip().strip('"') for c in raw.split(",") if c.strip()]


def _paren_end(text: str, open_index: int) -> int:
    depth = 0
    for i in range(open_index, len(text)):
        if text[i] == "(":
            depth += 1
        elif text[i] == ")":
            depth -= 1
            if depth == 0:
                return i + 1
    return len(text)


def _column_fields(text: str) -> list[tuple[str, str]]:
    """(COLUMN, javaField) for every @Column/@JoinColumn(name=...) followed, past
    any other annotations, by the field it decorates."""
    pairs: list[tuple[str, str]] = []
    for m in COLUMN_ANNOTATION_RE.finditer(text):
        end = _paren_end(text, m.end() - 1)
        name_m = TABLE_NAME_RE.search(text[m.end():end - 1])
        semi = text.find(";", end)
        if not name_m or semi < 0:
            continue
        region = ANNOTATION_RE.sub("", text[end:semi])
        decl = FIELD_DECL_RE.match(region.replace("\n", " ").strip() + " ")
        if decl:
            pairs.append((name_m.group(1), decl.group(1)))
    return pairs


def find_entity_constraints(source_root: Path) -> tuple[list[UniqueConstraint], dict[str, dict[str, str]], dict[str, str]]:
    """(constraints declared on entities, {TABLE: {COLUMN: entityField}}, {TABLE: EntityClass})."""
    constraints: list[UniqueConstraint] = []
    column_fields: dict[str, dict[str, str]] = {}
    entity_of: dict[str, str] = {}
    for path in sorted(source_root.rglob("*.java")):
        text = JAVA_COMMENT_RE.sub("", path.read_text(encoding="utf-8", errors="ignore"))
        table_m = TABLE_ANNOTATION_RE.search(text)
        if not table_m or "@Entity" not in text:
            continue
        table_body = text[table_m.end() - 1:_paren_end(text, table_m.end() - 1)]
        name_m = TABLE_NAME_RE.search(table_body)
        class_m = CLASS_RE.search(text)
        if not name_m or not class_m:
            continue
        table = name_m.group(1).upper()
        entity = class_m.group(1)
        entity_of[table] = entity
        fields = column_fields.setdefault(table, {})
        for col, field_name in _column_fields(text):
            fields.setdefault(col.upper(), field_name)
        for uc in UNIQUE_CONSTRAINT_RE.finditer(table_body):
            inner = uc.group(1)
            uc_name = TABLE_NAME_RE.search(inner)
            cols_m = COLUMN_NAMES_RE.search(inner)
            if not cols_m:
                continue
            cols = QUOTED_RE.findall(cols_m.group(1)) if cols_m.group(1) is not None else [cols_m.group(2)]
            constraints.append(UniqueConstraint(
                table=table, name=(uc_name.group(1) if uc_name else "(unnamed)").upper(), columns=cols,
                entity=entity, sources=[path.name]))
    return constraints, column_fields, entity_of


def _migration_files(migration_roots: list[Path]) -> list[Path]:
    files = []
    for root in migration_roots:
        if root.is_dir():
            files += [p for p in root.rglob("*.sql") if MIGRATION_VERSION_RE.match(p.name)]
    def version(p: Path) -> tuple:
        m = MIGRATION_VERSION_RE.match(p.name)
        return tuple(int(x) for x in re.findall(r"\d+", p.name.split("__")[0]))
    return sorted(files, key=lambda p: (version(p), p.name))


def _statements(text: str) -> list[str]:
    text = SQL_COMMENT_RE.sub(" ", text)
    return [re.sub(r"\s+", " ", s).strip() for s in text.split(";") if s.strip()]


def find_migration_constraints(migration_roots: list[Path]) -> list[UniqueConstraint]:
    """Replays every migration in version order, so a constraint dropped -- or
    a table dropped and recreated -- later in the history never surfaces as live."""
    live: dict[tuple[str, str], UniqueConstraint] = {}
    for path in _migration_files(migration_roots):
        for stmt in _statements(path.read_text(encoding="utf-8", errors="ignore")):
            m = DROP_TABLE_RE.match(stmt)
            if m:
                table = m.group(1).upper()
                for key in [k for k in live if k[0] == table]:
                    del live[key]
                continue
            m = DROP_CONSTRAINT_RE.match(stmt)
            if m:
                live.pop((m.group(1).upper(), m.group(2).upper()), None)
                continue
            m = DROP_INDEX_RE.match(stmt)
            if m:
                for key in [k for k in live if k[1] == m.group(1).upper()]:
                    del live[key]
                continue
            m = ADD_UNIQUE_RE.match(stmt)
            if m:
                table, name, cols = m.group(1).upper(), m.group(2).upper(), _columns(m.group(3))
                live[(table, name)] = UniqueConstraint(table=table, name=name, columns=cols, sources=[path.name])
                continue
            m = CREATE_UNIQUE_INDEX_RE.match(stmt)
            if m:
                name, table, cols, where = m.group(1).upper(), m.group(2).upper(), _columns(m.group(3)), m.group(4)
                scope = f"WHERE {where.strip()}" if where else "all rows"
                live[(table, name)] = UniqueConstraint(table=table, name=name, columns=cols, scope=scope, sources=[path.name])
                continue
            m = CREATE_TABLE_RE.match(stmt)
            if m:
                table = m.group(1).upper()
                for inline in INLINE_UNIQUE_RE.finditer(m.group(2)):
                    name = (inline.group(1) or "(unnamed)").upper()
                    live[(table, name)] = UniqueConstraint(table=table, name=name, columns=_columns(inline.group(2)), sources=[path.name])
    return sorted(live.values(), key=lambda c: (c.table, c.name))


def find_unique_constraints(source_root: Optional[Path], migration_roots: list[Path]) -> list[UniqueConstraint]:
    """Only tables this module's own entities declare are reported. Where both
    the entity and a migration declare the same constraint the row cites both;
    where they disagree on columns the row says so instead of choosing."""
    if source_root is None:
        return []
    entity_constraints, column_fields, entity_of = find_entity_constraints(source_root)
    if not entity_of:
        return []
    merged: dict[tuple[str, str], UniqueConstraint] = {}
    for c in entity_constraints:
        merged[(c.table, c.name)] = c
    for c in find_migration_constraints(migration_roots):
        if c.table not in entity_of:
            continue
        key = (c.table, c.name)
        existing = merged.get(key)
        if existing is None:
            merged[key] = c
            continue
        existing.sources = existing.sources + c.sources
        existing.scope = c.scope
        if [x.upper() for x in existing.columns] != [x.upper() for x in c.columns]:
            existing.note = (f"entity declares ({', '.join(existing.columns)}) but "
                             f"{c.sources[0]} declares ({', '.join(c.columns)})")
    rows = []
    for c in merged.values():
        c.entity = c.entity or entity_of.get(c.table)
        fields = column_fields.get(c.table, {})
        c.entity_fields = [fields.get(col.upper(), "") for col in c.columns]
        rows.append(c)
    return sorted(rows, key=lambda c: (c.table, c.name))
