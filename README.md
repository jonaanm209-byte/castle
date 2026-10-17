# Paradox Castle (NeoForge · Minecraft 1.21.1 · Cobblemon 1.8.1)

A rare castle generates in the overworld. Inside it:

- **Two key chests** (one in the NW tower, one in the SE tower): a **Koraidon Key** and a **Miraidon Key**. Keys spawn with **0 charge**.
- **Two pedestals** on a dais in the keep's great hall (red = Koraidon, purple = Miraidon).
- Filler chests (bread, ingots, a little treasure) in the other towers and the keep's upper floors.

## Gameplay

1. Find the castle (rare: roughly one candidate per 1,024 blocks, land biomes only) and loot a key.
2. Carry the key and **KO wild paradox Pokémon** in Cobblemon battles. Each KO adds 1 charge to every uncharged key in your inventory (10 needed; tune `KeyCharge.REQUIRED`). The key's bar fills up and it glows when full.
3. Right-click the **matching** pedestal with a **fully charged** key. The key is consumed and the legendary (level 70) appears 3–5 blocks in front of you.
   - Partially charged key → pedestal refuses and shows the charge.
   - Wrong legend's key → pedestal refuses.

Only **wild** paradox Pokémon count (not trainers' or other players'). The mod adds **no** paradox spawns and **no** Pokémon models; Koraidon and Miraidon come from Cobblemon itself.

## Build

Requires JDK 21.

```
./gradlew build          # jar in build/libs
./gradlew runClient      # dev client with Cobblemon
```

Runtime needs: NeoForge 21.1.x, Cobblemon 1.8.1 (NeoForge), Kotlin for Forge 5.x.

Pinned versions live in `gradle.properties` (`neo_version`, `kff_version`, `kotlin_version`, `moddev_version`). Bump them if Gradle can't find one on first sync.

## Testing quickly

```
/place structure paradoxcastle:paradox_castle ~ ~ ~       # not needed if you can't find one; see below
/locate structure paradoxcastle:paradox_castle
/give @s paradoxcastle:koraidon_key[paradoxcastle:charge=10]
/spawnpokemon ironvaliant                                  # or any Cobblemon spawn command, to get a paradox target
```

(Creative tab "Paradox Castle" also has pre-charged keys.)

## Compatibility notes

- Everything is namespaced `paradoxcastle:` and no vanilla or other-mod registry entries, tags or loot tables are modified, so it can't clash with other content mods.
- **Wild Battle API**: only swaps the AI on wild actors; the faint event and actor types this mod relies on are unchanged.
- **Other structure mods** (When Dungeons Arise, YUNG's, etc.): the castle uses its own structure set and salt, and keeps 6 chunks away from vanilla villages. Overlap with another mod's structures can't be fully excluded without naming their structure sets.
- **Sophisticated Backpacks and similar**: only keys in the player's own inventory (including hotbar/offhand) charge. Keys stored in a backpack, chest or ender chest do not.
- **You're in Grave Danger**: keys keep their charge through death/grave retrieval (charge is stored on the item).
- Client-only mods in the pack (Sodium, Xaero's, etc.) don't touch anything this mod adds.

## Files

- `src/main/kotlin/...` – keys, pedestals, KO tracking, summoning
- `src/main/resources/data/paradoxcastle/` – structure, jigsaw pool, structure set, biome tag, loot tables
- `tools/generate_assets.py` – regenerates the castle `.nbt` and key textures (edit the layout there)
