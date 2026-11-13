# NeoForge Studio

Isolated Minecraft 1.21.1 NeoForge mod development workspace.

- Sources: `neoforge-studio/mod/`
- Build: `.github/workflows/neoforge-studio.yml` (Java 21, Gradle 8.14.3, NeoForge 21.1.265)
- Web interface: `neoforge-studio/site/`

The public GitHub repository already hosted PERWORK Builder. All its existing functionality is preserved: the new mod is isolated in its own folder and workflow.

A push to neoforge-studio/mod starts a new GitHub Actions build. Download the JAR from the workflow run (artifact neoforge-1.21.1-mod). Never commit tokens or secrets into a public repository.

## Current mod: Secret Title 993 (v0.2.0)

While playing Minecraft **1.21.1** with **NeoForge 21.1.x**, press **NumPad 9**, release, press **NumPad 9** again, then **NumPad 3**. Keep each step within two seconds.

A gold title reads **✦ ТАЙНЫЙ КОД ✦** and the cyan/purple subtitle reads **993 • СЕКРЕТ РАСКРЫТ**. The message fades in/out over five seconds.

- Only physical numeric-keypad keys count, not the numbers above the letters.
- Works while playing in the world; won't type into the chat, inventory, or menus.
- Holding a key will **not** count as multiple presses.
- This is client-side functionality; the server does not have to send a Title packet.
- Install the compiled JAR in the client's `mods/` folder.

GitHub Actions compiles with Java 21. The compiled JAR has not been tested interactively in a Minecraft client.
