# Supported profiles

A combination is available for cloud compilation only after its generator template passes a clean GitHub Actions build.

| Minecraft | Loader | Project kind | Java | Status |
|---|---|---|---:|---|
| 1.20.1 | Forge | Mod | 17 | CI verification pending |

The Java Studio selectors contain additional roadmap combinations. They must not be compiled until a matching profile is verified and marked \`supported\` in \`builder/profiles.json\`.

Each profile must declare the Minecraft version, loader, project kind, Java toolchain, Gradle version and loader dependency coordinates. Never mark an unverified profile as supported.
