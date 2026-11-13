# Perfectify — Changelog

## 0.1.0-alpha — November 13, 2026

### Architecture
- Created a standalone Perfectify project for NeoForge 1.21.1 and Java 21.
- Renamed the mod ID, Java packages, and resource paths.
- Removed explanatory comments from Java source files.
- Prepared automated verification and GitHub Release publishing.

### Gameplay
- Boat crafting with wooden shovels.
- Armor-stand arms, offhand equipment, and redstone-driven poses.
- Potion cauldrons, bottle extraction, tipped arrows, dyed water, and dyed leather armor.
- Weighted swamp-hut cauldron potion selection.
- Weaker potion effects from cauldron contact.
- Snow accumulation, capped snow layers, and plant coverage.
- Tactical skeleton, creeper, and zombie movement; iron golems offering poppies.
- Softer map colors, shield animation changes, and sheared sheep wool coloring.
- Chat formatting, highlighted player names, and skin portraits.

### Fixes
- Prevented mixing incompatible potion bottle variants without clearing the cauldron.
- Corrected tipped-arrow fluid usage when only 1–3 units remain.
- Prevented extra empty bottles when pouring potions in Creative mode.

### Pose accuracy and validation
- Transcribed all 13 armor stand pose rotations from Mojang's official animation samples.
- Redstone signal strength now selects a specific pose instead of a random preset.
- Sneak-interact with an empty hand cycles poses; a held item swaps with the stand's offhand.
- Dedicated-server checks require an actual startup-complete message rather than Gradle's exit code alone.
- Added a standalone source archive for transferring the project into a new repository.

### Status
- Ported gameplay is still experimental.
- In-game validation is required for Mixins, cauldron synchronization, and Bedrock behavior parity.
- Verification builds are not public releases.
