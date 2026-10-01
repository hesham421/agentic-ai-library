#!/usr/bin/env python3
"""
generate.py — stable CLI entrypoint for the API documentation generator.
════════════════════════════════════════════════════════════════════
    python3 generate.py --module ORG --function generate
    python3 generate.py --module ORG --function update
    python3 generate.py --module ORG --function review
    python3 generate.py --module ORG --function check     # review + assertions, exit 2 on any FAIL

Normal usage needs only --module and --function. Everything else --
which OpenAPI group is ORG's, where its source lives, which shared/common
modules it depends on, where its docs already live -- is auto-discovered
from the repository itself (see discovery.py). This is what keeps CI/CD
usage down to "module name + desired operation," and what lets a brand-new
module (HR, Inventory, ...) work without any change to this tool, as long
as it follows the same GroupedOpenApi / Maven-reactor conventions every
current module already follows.

Override flags exist only for the cases discovery genuinely can't resolve
on its own (server not running on the expected port, an unusual checkout
layout, wanting to point at a different saved OpenAPI file, etc.) — they
are never required for normal use.
"""

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import discovery
import generator


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--module", required=True, help="Module code, e.g. ORG, SECURITY, MASTERDATA (used to auto-discover everything else)")
    ap.add_argument("--function", required=True, choices=["generate", "update", "review", "check"], help="Execution mode")

    overrides = ap.add_argument_group("explicit overrides (optional — auto-discovered by default)")
    overrides.add_argument("--openapi", help="Override: OpenAPI JSON as a local file path or http(s) URL")
    overrides.add_argument("--source", type=Path, help="Override: path to the module's own src/main/java")
    overrides.add_argument("--common-source", type=Path, action="append", help="Override: path to a shared/common module's src/main/java (repeatable)")
    overrides.add_argument("--api-spec", type=Path, help="Override: path to the module's API document (api-spec-<mod>.yaml, OpenAPI 3.1 -> contract ids). Default: scripts/gov-module.py plan MOD --json -> api_spec")
    overrides.add_argument("--execution-plan", type=Path, help="Override: path to the module's backend execution plan (its prose API REGISTRY is read ONLY when no API document exists — a pre-v7 module)")
    overrides.add_argument("--output", type=Path, help="Override: output directory for generated documentation")
    overrides.add_argument("--backend-root", type=Path, help="Override: path to the backend repository root (default: the backend/ repo this governance/ checkout now lives inside)")
    args = ap.parse_args()

    try:
        context = discovery.resolve(
            args.module,
            backend_root=args.backend_root,
            openapi_override=args.openapi,
            source_override=args.source,
            common_source_overrides=args.common_source,
            output_override=args.output,
            execution_plan_override=args.execution_plan,
            api_spec_override=args.api_spec,
        )
    except discovery.DiscoveryError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1

    print(f"OpenAPI     : {context.openapi_source}")
    if context.source_root:
        print(f"Source      : {context.source_root}")
    if context.common_source_roots:
        print(f"Common src  : {', '.join(str(p) for p in context.common_source_roots)}")
    if context.api_spec:
        print(f"Contract    : {context.api_spec}")
    elif context.execution_plan:
        print(f"Contract    : {context.execution_plan}")
    if context.contract_note:
        print(f"Contract src: {context.contract_note}")

    try:
        if args.function == "check":
            report, exit_code = generator.check(context)
            print(report)
            return exit_code
        report = generator.run(context, mode=args.function)
    except Exception as exc:  # noqa: BLE001 - surface any load/parse failure to the user
        print(f"ERROR: {exc}", file=sys.stderr)
        if not args.openapi:
            print(f"Hint: the OpenAPI URL above was derived from this backend's own "
                  f"application.properties (server.port / springdoc.api-docs.path). Pass --openapi "
                  f"explicitly to point at a different server or a saved JSON file.", file=sys.stderr)
        return 1

    print(report)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
