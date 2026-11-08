#!/usr/bin/env python3
"""Extract a PERWORK source ZIP with path-traversal and size checks."""
from __future__ import annotations
import argparse, shutil, stat, zipfile
from pathlib import Path, PurePosixPath

MAX_FILES = 1000
MAX_UNCOMPRESSED = 100 * 1024 * 1024
MAX_FILE_SIZE = 20 * 1024 * 1024

def safe_extract(archive, destination):
    with zipfile.ZipFile(archive) as zf:
        items = zf.infolist()
        if not items or len(items) > MAX_FILES:
            raise SystemExit(f"Archive must contain between 1 and {MAX_FILES} entries.")
        total, validated = 0, []
        for info in items:
            raw = info.filename.replace("\\", "/")
            path = PurePosixPath(raw)
            if path.is_absolute() or not path.parts or any(p in ("", ".", "..") for p in path.parts):
                raise SystemExit(f"Unsafe archive path: {info.filename}")
            if path.parts[0] != "main":
                raise SystemExit("The archive must contain only the selected main/ folder.")
            if info.flag_bits & 0x1:
                raise SystemExit("Encrypted ZIP entries are not supported.")
            if stat.S_ISLNK(info.external_attr >> 16):
                raise SystemExit(f"Symbolic links are not allowed: {info.filename}")
            if info.file_size > MAX_FILE_SIZE:
                raise SystemExit(f"File is too large: {info.filename}")
            total += info.file_size
            if total > MAX_UNCOMPRESSED:
                raise SystemExit("Uncompressed project size exceeds 100 MB.")
            target = (destination / Path(*path.parts)).resolve()
            if destination.resolve() not in target.parents and target != destination.resolve():
                raise SystemExit(f"Unsafe archive path: {info.filename}")
            validated.append((info, target))
        destination.mkdir(parents=True, exist_ok=True)
        for info, target in validated:
            if info.is_dir():
                target.mkdir(parents=True, exist_ok=True)
                continue
            target.parent.mkdir(parents=True, exist_ok=True)
            with zf.open(info) as source, target.open("wb") as output:
                shutil.copyfileobj(source, output)
        if not any((destination / "main").rglob("*.java")):
            raise SystemExit("The uploaded main/ folder does not contain any .java files.")

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--archive", required=True, type=Path)
    p.add_argument("--destination", required=True, type=Path)
    args = p.parse_args()
    safe_extract(args.archive, args.destination)
    print("Source archive validated and extracted safely.")

if __name__ == "__main__":
    main()
