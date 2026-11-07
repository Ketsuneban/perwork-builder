# PERWORK Builder

Minecraft project generation and build tooling for Java Studio's **Create** section.

> This repository is the public build-tooling layer. It does not contain PERWORK's private site code, credentials, or user projects.

## Current scope

This starter implementation provides:
- A project generator for Minecraft Java projects.
- Explicit build profiles (Minecraft version, loader, and project kind).
- A GitHub Actions workflow that can build a generated project.
- Build artifacts as downloadable JARs.

The first supported profile is **Minecraft 1.20.1 + Forge + mod**. Other versions/loaders are intentionally not advertised as supported until their templates and dependency coordinates are verified.

## Repository layout

- `builder/generate.py` — generates a Gradle project from a Java source directory.
- `builder/profiles.json` — supported profiles and template metadata.
- `.github/workflows/build-generated-project.yml` — builds a generated project archive.
- `examples/` — minimal input example.

## Generate a project

Requires Python 3.11+.

```bash
python builder/generate.py \
  --source examples/main \
  --output generated-project \
  --minecraft 1.20.1 \
  --loader forge \
  --kind mod \
  --mod-id examplemod \
  --mod-name "Example Mod" \
  --mod-version 1.0.0
```

Then run the generated Gradle wrapper:

```bash
cd generated-project
./gradlew build
```

On Windows use `gradlew.bat build`.

## Input contract

The source directory may contain Java files under either:
- `main/java/**` (preferred), or
- Java files directly beneath `main/**` (generator places them under `src/main/java`).

Optional resources can be supplied with `--resources` pointing to a directory. The generator copies those files into `src/main/resources`.

The generator does **not** execute uploaded Java or Gradle code. Build execution must happen in a disposable isolated worker. Do not run untrusted projects on the PERWORK web server or expose GitHub credentials to the generated build.

## GitHub Actions

The workflow accepts a ZIP project artifact named `generated-project.zip` from a prior workflow/run, extracts it, runs Gradle, and uploads the resulting JARs. It is a starter workflow for trusted project artifacts; a production API must create isolated build jobs and pass artifacts securely.

## Security notes

- Never commit tokens, private keys, Firebase credentials, or user source projects.
- Uploaded archives must be size-limited and validated against path traversal.
- Build jobs must have CPU, memory, disk, and time limits and no access to PERWORK secrets.
- Treat Gradle scripts and annotation processors as executable untrusted code.
- Public Actions logs and artifacts may expose uploaded project contents; use private, short-lived artifacts for user builds.

## Roadmap

1. Add and verify loader/version profiles.
2. Add a server-side job API and isolated worker.
3. Add upload validation, job status, logs, and artifact expiration.
4. Connect Java Studio to this repository's versioned generator.
5. Add automated fixture builds for every declared profile.

See [SUPPORTED_PROFILES.md](SUPPORTED_PROFILES.md) for the currently declared profile.
