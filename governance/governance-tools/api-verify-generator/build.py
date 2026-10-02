"""Builds each module's api-verify script (api-verify skill, .claude/skills/api-verify/SKILL.md).

    python3 governance/governance-tools/api-verify-generator/build.py REG DOC CHK RPT INT

Concatenates _common.py + _helpers.py + body_<MOD>.py into
governance/shared/backend/modules/<MOD>/test-api/test_<mod>_apis.py (the skill's output path).
The generated scripts are never hand-edited: change a fragment here and rebuild.
"""
import sys, pathlib
G = pathlib.Path(__file__).resolve().parent
PART = G.parents[1] / "shared" / "backend" / "modules"
for mod in sys.argv[1:]:
    src = (G/"_common.py").read_text() + (G/"_helpers.py").read_text() + (G/f"body_{mod}.py").read_text() + \
          '\n\nif __name__ == "__main__":\n    sys.exit(run(EXTRA_ARGS, preflight, SUITES))\n'
    src = src.replace("__MOD__", mod).replace("__mod__", mod.lower())
    out = PART/mod/"test-api"/f"test_{mod.lower()}_apis.py"
    out.parent.mkdir(exist_ok=True)
    out.write_text(src); out.chmod(0o755)
    print("wrote", out)
