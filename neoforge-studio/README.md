# Bedrock Mechanics Studio · NeoForge 1.21.1 · v0.3.0

Repository: `Ketsuneban/perwork-builder`; sources at `neoforge-studio/mod/`.
A separate workflow builds this project with Java 21 and NeoForge 21.1.265.
The existing PERWORK builder is not changed.

## Features implemented in v0.3.0

1. All wooden boat recipes (including bamboo raft) again require a wooden shovel.
2. Armor stands show both arms when loaded and accept sneak+right-click to swap offhand items; nearby redstone rising edges rotate between pose presets.
3. Vanilla cauldrons track potion content per dimension in persistent SavedData, support potion bottles, tipped arrows and fluid collisions.
4. Uninitialized, identifiable witch hut cauldrons receive a random potion with 1-3 bottle levels; weights: 25% healing, 25% poison, 15% speed, 10% slowness, 10% weakness, 10% water breathing, 5% fire resistance.
5. Brief potion effects apply to players immersed inside a potion-filled cauldron.
6. Colored water and dyeing dyeable equipment (including leather armor).
7. First-person shield equip progress adjusted to reduce blocking animation delay.
8. Lightweight combat movement adjustments for skeletons, zombies, and creepers (6 nearby mobs/player/40 ticks).
9. Sheared-sheep base textures are selectively tinted on the client to preserve wool-color remnants; original vanilla texture is read at runtime, not redistributed.
10. Slow, irregular snow buildup in snowing biomes, max 5 layers, near active players only.
11. Unsupported snow layers become a falling block, rather than immediately disappearing.
12. Snowfall may cover short grass, ferns and certain flowers.
13. Iron golems again offer poppies to nearby baby villagers in peaceful moments.
14. A gentler, less saturated client-side map palette.
15. Chat recognizes legacy formatting codes such as `&b`, `&l`, `&o`, `&r`.
16. Online player names are highlighted aqua inside formatted chat text.
17. Skin-head icons beside recognizable player chat messages; vanilla TAB continues displaying available skin heads. TAB labels are cyan.

The existing key sequence NumPad 9 -> 9 -> 3 shows the technical placeholder
SYSTEM EVENT / NUMPAD 993 - EVENT RECEIVED.

## Scope, caveats and testing status

This is a **first playable implementation / experimental build**, not a verified
pixel-perfect Bedrock conversion. The GitHub Actions Java/Gradle compile and
artifact upload pass; these changes have **not been interactively tested in a
Minecraft client or dedicated server**. Some injected rendering hooks may be
incompatible with other rendering mods. Back up worlds before testing.

- **Cauldrons:** The code stores potion and dye values server-side but currently
  displays vanilla water color in the basin, not the actual potion/dye tint.
  The storage is SavedData, not a custom per-block block entity. Fluid
  interaction synchronization and all special potion edge cases are not yet exact.
- **Witch huts:** A loaded-hut fingerprint (crafting table plus potted red
  mushroom) is used instead of a worldgen structure processor; probabilities
  match the documented Bedrock table, but initialization behavior is approximate.
- **Snow:** The falling layer landing behavior and flower coverage need
  in-world tests; some tall plants are not handled.
- **Mobs:** Movement is more tactical but not a complete Bedrock AI rewrite.
- **Armor stands:** Pose presets are inspired by Bedrock, not all Bedrock
  poses and timing replicated.
- **Sheep:** The texture is tinted heuristically and will depend on the
  player's installed resource pack.
- **Chat avatars:** Sender detection is heuristic for non-standard chat formats;
  only vanilla skin-head rendering is provided in TAB where supported.
- **Shield:** Only the visual interpolation is affected; server blocking
  validation has not been modified.

## Build

`.github/workflows/neoforge-studio.yml` runs on changes to
`neoforge-studio/mod/**`. Download the artifact `neoforge-1.21.1-mod`
containing `studioexample-0.3.0.jar`. Install on the Minecraft 1.21.1 NeoForge
client. The more accurate armor-stand, cauldron, weather, AI and chat logic is
server-authoritative; in multiplayer, install the same mod on the server.
Never commit secrets into the public repository.
