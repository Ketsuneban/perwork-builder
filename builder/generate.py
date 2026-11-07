#!/usr/bin/env python3
"""Generate a starter Gradle project from a Java source folder.

This generator copies source/resources and writes build metadata. It never executes
uploaded code. Build the resulting project only in an isolated disposable worker.
"""
from __future__ import annotations

import argparse
import json
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PROFILE_FILE = ROOT / "builder" / "profiles.json"
SAFE_ID = re.compile(r"^[a-z][a-z0-9_]{1,63}$")
SAFE_VERSION = re.compile(r"^[0-9A-Za-z][0-9A-Za-z._+-]{0,63}$")


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--source", required=True, type=Path, help="Directory containing Java sources (e.g. main)")
    p.add_argument("--output", required=True, type=Path, help="Output project directory")
    p.add_argument("--minecraft", required=True)
    p.add_argument("--loader", required=True, choices=["forge"])
    p.add_argument("--kind", required=True, choices=["mod"])
    p.add_argument("--mod-id", required=True)
    p.add_argument("--mod-name", required=True)
    p.add_argument("--mod-version", default="1.0.0")
    p.add_argument("--resources", type=Path)
    return p.parse_args()


def load_profile(mc: str, loader: str, kind: str) -> dict:
    data = json.loads(PROFILE_FILE.read_text(encoding="utf-8"))
    for profile in data["profiles"]:
        if (profile["minecraft"], profile["loader"], profile["kind"]) == (mc, loader, kind):
            return profile
    raise SystemExit(f"Unsupported profile: Minecraft {mc}, {loader}, {kind}. See SUPPORTED_PROFILES.md")


def validate_args(args: argparse.Namespace) -> None:
    if not SAFE_ID.fullmatch(args.mod_id):
        raise SystemExit("mod-id must start with a lowercase letter and contain only lowercase letters, digits, or underscores")
    if not SAFE_VERSION.fullmatch(args.mod_version):
        raise SystemExit("Invalid mod-version")
    if not args.mod_name.strip() or len(args.mod_name) > 128:
        raise SystemExit("mod-name must be 1..128 characters")
    if not args.source.is_dir():
        raise SystemExit(f"Source directory does not exist: {args.source}")
    if args.output.exists() and any(args.output.iterdir()):
        raise SystemExit(f"Output directory is not empty: {args.output}")


def find_java_root(source: Path) -> Path:
    preferred = source / "java"
    if preferred.is_dir():
        return preferred
    if any(source.rglob("*.java")):
        return source
    raise SystemExit("No .java files found in source directory")


def main() -> None:
    args = parse_args()
    profile = load_profile(args.minecraft, args.loader, args.kind)
    validate_args(args)

    source = args.source.resolve()
    output = args.output.resolve()
    java_root = find_java_root(source)
    output.mkdir(parents=True, exist_ok=True)
    (output / "src" / "main" / "java").mkdir(parents=True)
    (output / "src" / "main" / "resources").mkdir(parents=True)

    java_files = list(java_root.rglob("*.java"))
    if not java_files:
        raise SystemExit("No .java files found")
    for file in java_files:
        relative = file.relative_to(java_root)
        destination = output / "src" / "main" / "java" / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(file, destination)

    if args.resources:
        resources = args.resources.resolve()
        if not resources.is_dir():
        raise SystemExit(f"Resources directory does not exist: {resources}")
        for file in resources.rglob("*"):
            if file.is_file():
                relative = file.relative_to(resources)
                destination = output / "src" / "main" / "resources" / relative
                destination.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(file, destination)

    (output / "settings.gradle").write_text("rootProject.name = '" + args.mod_id + "'\n", encoding="utf-8")
    (output / "gradle.properties").write_text(
        "org.gradle.jvmargs=-Xmx3G\norg.gradle.daemon=false\n",
        encoding="utf-8",
    )
    (output / "build.gradle").write_text(build_gradle(profile, args), encoding="utf-8")
    (output / "src" / "main" / "resources" / "META-INF").mkdir(parents=True, exist_ok=True)
    (output / "src" / "main" / "resources" / "META-INF" / "mods.toml").write_text(
        mods_toml(profile, args), encoding="utf-8"
    )
    print(f"Generated {output} from {len(java_files)} Java source file(s).")
    print("This profile is a starter template; compile it in an isolated worker and verify API compatibility.")


def build_gradle(profile: dict, args: argparse.Namespace) -> str:
    return f"""plugins {{
    id 'java'
    id 'net.minecraftforge.gradle' version '[6.0,6.2)'
}}

group = 'com.perwork.generated'
version = '{args.mod_version}'

java.toolchain.languageVersion = JavaLanguageVersion.of({profile['java']})

minecraft {{
    mappings channel: 'official', version: '{profile['minecraft']}'
    runs {{
        client {{ workingDirectory project.file('run') }}
        server {{ workingDirectory project.file('run') }}
    }}
}}

dependencies {{
    minecraft 'net.minecraftforge:forge:{profile['forge']}'
}}

tasks.named('jar', Jar).configure {{
    manifest {{
        attributes([
            'Specification-Title': '{args.mod_name}',
            'Specification-Vendor': 'PERWORK Builder',
            'Specification-Version': '{args.mod_version}'
        ])
    }}
}}
"""


def mods_toml(profile: dict, args: argparse.Namespace) -> str:
    return f"""modLoader="javafml"
loaderVersion="[47,)"
license="All Rights Reserved"

[[mods]]
modId="{args.mod_id}"
version="{args.mod_version}"
displayName="{args.mod_name}"
description='Generated by PERWORK Builder for Minecraft {profile['minecraft']} / Forge.'
"""


if __name__ == "__main__":
    main()
