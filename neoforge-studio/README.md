# NeoForge Studio

Isolated Minecraft 1.21.1 NeoForge mod development workspace.

- Sources: `neoforge-studio/mod/`
- Build: `.github/workflows/neoforge-studio.yml` (Java 21, Gradle 8.14.3, NeoForge 21.1.265)
- Web interface: `neoforge-studio/site/`

The public GitHub repository already hosted PERWORK Builder. All its existing functionality is preserved: the new mod is isolated in its own folder and workflow.

A push to neoforge-studio/mod starts a new GitHub Actions build. Download the JAR from the workflow run (artifact neoforge-1.21.1-mod). Never commit tokens or secrets into a public repository.
