# Supported profiles

Support is explicit: a profile is available only when its template and dependency versions have been verified by CI.

| Minecraft | Loader | Project kind | Status |
|---|---|---|---|
| 1.20.1 | Forge | mod | Starter template; CI verification required |

The UI should not offer unverified combinations. A version range in the Java Studio selector is not a guarantee that every loader supports every game version.

## Profile definition

Profiles are stored in `builder/profiles.json`. Each profile defines:
- Minecraft version
- loader
- project kind
- Java toolchain
- Gradle and loader plugin coordinates

Add a profile only after a clean CI build has verified it.
