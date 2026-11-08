#!/usr/bin/env python3
"""Generate a Minecraft Forge Gradle project from an uploaded main/ directory."""
from __future__ import annotations
import argparse, json, re, shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PROFILE_FILE = ROOT / "builder" / "profiles.json"
SAFE_ID = re.compile(r"^[a-z][a-z0-9_]{1,63}$")
SAFE_VERSION = re.compile(r"^[0-9A-Za-z][0-9A-Za-z._+-]{0,63}$")

def parse_args():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--source", required=True, type=Path)
    p.add_argument("--output", required=True, type=Path)
    p.add_argument("--minecraft", required=True)
    p.add_argument("--loader", required=True)
    p.add_argument("--kind", required=True)
    p.add_argument("--mod-id", required=True)
    p.add_argument("--mod-name", required=True)
    p.add_argument("--mod-version", default="1.0.0")
    p.add_argument("--resources", type=Path)
    return p.parse_args()

def load_profile(mc, loader, kind):
    data = json.loads(PROFILE_FILE.read_text(encoding="utf-8"))
    for profile in data.get("profiles", []):
        if (profile["minecraft"], profile["loader"], profile["kind"]) == (mc, loader, kind):
            if profile.get("status") != "supported":
                raise SystemExit("This profile is not yet verified by CI.")
            return profile
    raise SystemExit(f"Unsupported profile: Minecraft {mc}, {loader}, {kind}. See SUPPORTED_PROFILES.md.")

def validate_args(args):
    if not SAFE_ID.fullmatch(args.mod_id):
        raise SystemExit("mod-id must start with a lowercase letter and contain only lowercase letters, digits, or underscores")
    if not SAFE_VERSION.fullmatch(args.mod_version):
        raise SystemExit("Invalid mod-version")
    if not args.mod_name.strip() or len(args.mod_name) > 128 or any(ord(char) < 32 or 127 <= ord(char) <= 159 for char in args.mod_name):
        raise SystemExit("mod-name must be 1..128 characters and contain no control characters")
    if not args.source.is_dir():
        raise SystemExit(f"Source directory does not exist: {args.source}")
    if args.output.exists() and any(args.output.iterdir()):
        raise SystemExit(f"Output directory is not empty: {args.output}")

def find_java_root(source):
    preferred = source / "java"
    if preferred.is_dir():
        return preferred
    if any(source.rglob("*.java")):
        return source
    raise SystemExit("No .java files found in uploaded main/ directory")

def copy_tree_files(source, destination):
    if not source.is_dir():
        return 0
    copied = 0
    for file in source.rglob("*"):
        if file.is_symlink():
            raise SystemExit(f"Symbolic links are not allowed: {file.relative_to(source)}")
        if not file.is_file():
            continue
        target = destination / file.relative_to(source)
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(file, target)
        copied += 1
    return copied

def copy_java_files(source, destination):
    copied = 0
    for file in source.rglob("*"):
        if file.is_symlink():
            raise SystemExit(f"Symbolic links are not allowed: {file.relative_to(source)}")
        if not file.is_file() or file.suffix.lower() != ".java":
            continue
        target = destination / file.relative_to(source)
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(file, target)
        copied += 1
    return copied

def toml_string(value):
    clean = value.replace("\\", "\\\\").replace('"', '\\"').replace("\r", " ").replace("\n", " ")
    return '"' + clean + '"'

def main():
    args = parse_args()
    profile = load_profile(args.minecraft, args.loader, args.kind)
    validate_args(args)
    source, output = args.source.resolve(), args.output.resolve()
    java_root = find_java_root(source)
    output.mkdir(parents=True, exist_ok=True)
    java_count = copy_java_files(java_root, output / "src" / "main" / "java")
    if not java_count:
        raise SystemExit("No .java files found in uploaded main/ directory")
    resources = args.resources.resolve() if args.resources else source / "resources"
    resource_count = copy_tree_files(resources, output / "src" / "main" / "resources")
    settings_lines = [
        "pluginManagement {",
        "    repositories {",
        "        gradlePluginPortal()",
        "        maven { url = 'https://maven.minecraftforge.net/' }",
        "    }",
        "}",
        "rootProject.name = '" + args.mod_id + "'",
    ]
    (output / "settings.gradle").write_text(chr(10).join(settings_lines) + chr(10), encoding="utf-8")
    (output / "gradle.properties").write_text("org.gradle.jvmargs=-Xmx3G\norg.gradle.daemon=false\norg.gradle.parallel=false\n", encoding="utf-8")
    (output / "build.gradle").write_text(build_gradle(profile, args), encoding="utf-8")
    meta_inf = output / "src" / "main" / "resources" / "META-INF"
    meta_inf.mkdir(parents=True, exist_ok=True)
    (meta_inf / "mods.toml").write_text(mods_toml(profile, args), encoding="utf-8")
    print(f"Generated Gradle project: {java_count} Java file(s), {resource_count} resource file(s).")
    print(f"Profile: Minecraft {profile['minecraft']} / {profile['loader']} / {profile['kind']}")

def build_gradle(profile, args):
    safe_name = args.mod_name.replace("\\", "\\\\").replace("'", "\\'")
    return f"""plugins {{
    id 'java'
    id 'net.minecraftforge.gradle' version '{profile['forge_gradle_plugin']}'
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
            'Specification-Title': '{safe_name}',
            'Specification-Vendor': 'PERWORK Builder',
            'Specification-Version': '{args.mod_version}'
        ])
    }}
}}
"""

def mods_toml(profile, args):
    return f"""modLoader="javafml"
loaderVersion="{profile['loader_version']}"
license="All Rights Reserved"
[[mods]]
modId="{args.mod_id}"
version="{args.mod_version}"
displayName={toml_string(args.mod_name)}
description={toml_string(f"Generated by PERWORK Builder for Minecraft {profile['minecraft']} / Forge.")}
"""

if __name__ == "__main__":
    main()
