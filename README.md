# Durcheinander MobArmy

A Spigot **Minecraft Java 26.3 / Java 25** raid plugin made for Yuuki's concept. Uses Bukkit/Spigot APIs only. No Paper-only methods, NMS, WorldEdit, economy plugin, ModelEngine, or client mod is required.

## Test on your Windows PC

1. Extract the complete `DurcheinanderMobArmy-1.0.0-local-test.zip` to a normal folder (not inside the ZIP).
2. No Java or Git installation is needed. Setup downloads its own **portable Java 25** and **portable Git with Git Bash**, verifies their checksums, and keeps them in `local-test/runtime`. It requires 64-bit Windows and changes no system PATH or registry settings.
3. Double-click `local-test/setup.bat`. It downloads the portable tools and official Spigot BuildTools, then builds Spigot 26.3. First setup requires internet and can take several minutes. Read the Minecraft EULA and answer the acceptance prompt yourself.
4. Double-click `local-test/start-server.bat`. Wait for `Done`.
5. Start Minecraft **Java 26.3**, choose Multiplayer → Direct Connection → `localhost:25565`.
6. In the **server console**, type `op YOUR_MINECRAFT_NAME` (without `/`).
7. In-game, on safe ground, run:

   ```text
   /raid kit
   /raid demo w1
   ```

8. Choose a reward duration in the GUI. A purple particle portal appears four blocks to your right. Walk into it, or use `/raid join demo_w1`.
9. Open `/raid`, click **Ready**, then walk south through the five-block opening from the waiting room into the arena. Waves begin when every party member is inside. Between stages, use `/raid` → **Ready** again.
10. To try other weeks, finish or `/raid leave`, then `/raid demo w2` or `/raid demo w3`.

The Windows localhost launcher automatically enables solo testing and disables player PvP in **all worlds**, including the default world. No `/raid test on` command is needed. These demo flags are also applied when updating an existing test server; other raid config entries and saved progress are preserved.

To update an existing localhost demo, stop the server, extract the new ZIP over the same folder, then run `local-test/start-server.bat`. The launcher copies the updated plugin JAR and applies the demo flags; you do not need to download Java or rebuild Spigot again.

`/raid test off` restores player minimums (2 / 3 / 5) and teamwork caps until the next localhost launch. Change test mode only with no active party. Solo mode and global PvP overrides are OFF in the production JAR's default config; raid-world PvP is always disabled.

Install `resource-pack/MobArmy-resource-pack-26.3.zip` in your Minecraft client's resource-pack folder and enable it. The custom boss uses an original static purple voxel golem model with vanilla texture references. Without the pack, its item display will use a missing-model appearance. A client resource pack is required for custom visuals; the plugin alone cannot supply them offline.

**After setup and the first successful server start**, the server can run without internet on your PC. Minecraft must also be available and launchable offline already. The test server binds to `127.0.0.1` and uses `online-mode=false`, so it is a PC-only test. Use `online-mode=true` and your usual network settings when deploying a real server.

Use `stop` in the server console to shut down cleanly. Later launches use the portable Java directly, without downloading anything. Keep `local-test/runtime/java` when moving the server folder. The setup downloads dependencies on demand; the small test ZIP itself does not contain the Java/Git binaries.

## Included gameplay

- Three generated 49×56 demo structures in independent void worlds (`raid_w1`, `raid_w2`, `raid_w3`); the actual combat region is 47×47.
- Five stages per week: stages 1–4 each have three waves plus a miniboss; stage 5 has four ordinary waves and the custom boss as wave 5.
- Configurable mob counts, health, attack damage, movement speed, armor, knockback resistance, equipment, and riders. Riders first appear in wave 3. Week strength: 1.0 / 1.75 / 2.5; later stages and waves are stronger.
- One party per portal and one active party per physical arena. No upper party-size limit; a configurable safety limit prevents excessive mob spawning.
- Automatic first leader, paginated party GUI, one vote per member, strict-majority leader election. Only the leader starts. The arena gate waits for all members.
- No joining during entry/combat/loot; new players may join between stages. Random leadership handover on leaving/disconnecting; saved stage follows the new leader.
- Death preserves inventory and XP and eliminates the player from that run. Full wipes reset progress and apply a configurable event-day cooldown.
- Shared reward deadline chosen in the admin GUI, a timer hologram, midnight cutoff in `Europe/Berlin`, and no drops after the first boss completion. Portals remain playable without rewards unless manually closed.
- Only two public announcements: party entry and stage completion, only while rewards are available. Command feedback and instructions remain private.
- First boss loot goes directly to the leader; overflow persists in `/raid loot`. Right-click stored loot to distribute it to a party member. The winning party roster is saved so the leader can distribute stored loot after leaving, including to offline members' mailboxes.
- Boss exit waits for remaining item entities or the configured 120-second timeout.
- Final-damage caps: normal attacks ≤20% mob max HP; direct mace attacks ≤5%. Boss/miniboss contribution defaults to 40%, with a 50% minimum for a two-person stage to make week 1 possible. Solo test mode permits 100%. The stage's starting party size determines this exception, so leaving does not lift the teamwork requirement.
- Golden apples, potions (drinking/splash/lingering), and totems have per-stage limits; apples and pearls also have cooldowns. Raid PvP, gliding, fireworks, wind charges, explosions, and mob friendly fire are blocked.
- Arena structures are protected. Each member can place at most two layers above the floor and break only their own placements. Temporary blocks and filled shulker boxes are returned to their owner at cleanup, with inventory overflow going to the mailbox.
- PDC-authenticated items and cauldron rituals in the configured default world. Rituals remain usable after the raid timer expires.

The supplied loot list is sample content because the concept does not define final items. There is no automatic Discord posting or fixed weekly scheduler; admins open the appropriate week on the announced event day. Demo balancing is a starting point, not a guarantee of fight difficulty.

## Commands and permissions

| Command | Permission | Purpose |
|---|---|---|
| `/raid`, `/raid start`, `/raid join <portal>`, `/raid loot` | `mobarmy.play` (everyone) | Party GUI, leader start, portal entry, mailbox |
| `/raid leave`, `/raid status`, `/raid help` | Everyone | Leave and inspect |
| `/raid portal pos1`, `pos2` | `mobarmy.admin` (OP) | Select your current block coordinates |
| `/raid portal create <week> [id]` | `mobarmy.admin` | Create the selection as a particle portal and choose its timer |
| `/raid portal remove [id]` | `mobarmy.admin` | Remove an idle portal |
| `/raid open [id]`, `/raid close [id]`, `/raid stop [id]` | `mobarmy.admin` | Toggle admission or end a party |
| `/raid timer set <duration> [id]` | `mobarmy.timer` (OP) | Set deadline from now |
| `/raid timer add <duration> [id]`, `/raid timer clear [id]` | `mobarmy.timer` | Extend deadline / disable drops |
| `/raid item give <item> [player] [amount]` | `mobarmy.item` (OP) | Give authenticated items |
| `/raid demo <week>`, `/raid test on\|off`, `/raid kit`, `/raid reload` | `mobarmy.admin` | Testing and configuration |
| `/raid build`, `/raid visit <week>` | `mobarmy.build` (OP) | Explicit building bypass while no parties are active |

Durations: `90s`, `30m`, `2h`; plain numbers mean minutes. Where an ID is omitted, the current party's portal or sole configured portal is used. Remove/recreate with a **new portal ID** for a new rewarded event. Extending the timer does not reset a completed boss.

Portal selections are axis-aligned cuboids with movement detection, particles, portal sound, and a floating label. Build your own decorative portal frame around the region. A single region is rectangular; nonrectangular visual frames work as long as the chosen trigger covers their entrance.

## Configuration / custom arenas

First start writes `plugins/DurcheinanderMobArmy/config.yml` and `state.yml`.

- `weeks.<id>` supplies world, minimum players, strength, lobby and arena bounds, gate region, spawn points, and `stages.1` through `stages.5`. Duplicate a week entry and change its ID/world to add further versions.
- `generate-demo: true` builds each default demo once. Set it to `false` before introducing a hand-built world; `/raid build` + `/raid visit <week>` lets you edit, or import your own schematic with a separate tool. Schematic loading is not built into this plugin.
- `arena.min/max`, `lobby.min/max`, `door.min/max` use inclusive block coordinates. The arena is limited to 100×100. Its `min.y` is the first standing layer directly above the floor.
- `spawn-points` are `x,y,z` strings; each must have a solid floor and two passable standing blocks. Spawns prefer the farthest eligible point and wait if the minimum distance cannot be met. After 30 failed attempts the current stage returns to the lobby without awarding completion.
- `items.<id>` defines material, display name, lore, `item-model` (preferred), optional legacy `custom-model-data`, `drops`, and optional `ritual`. Drop fields: `source` (`mob`, `miniboss`, `boss`), optional `stage`, optional `week`, `amount`, `chance` (0–1).
- Ritual fields: `type`, `health`, `damage`, `name`, `result` (another item ID). Recognized by PDC, not by renamed display names.
- `balance` holds all limits. Minecraft/Spigot's maximum mob-health attribute is clamped to 1024.
- `state.yml` holds portal deadlines/event days/first-boss status, leader progress, cooldowns, return positions and loot mailboxes. Do not edit it while running. Stop all parties before `/raid reload`; avoid Bukkit's global `/reload`.

On a clean server shutdown, parties are evacuated and the current stage is saved; restarting a party begins that stage at wave 1. Return positions and temporary gate/placement snapshots are persisted for disconnect/crash recovery. A crash during inventory mutations cannot guarantee atomicity with Minecraft's separate player-data saves; use regular world/player/plugin backups.

## Build and validation

With Java 25 and Maven 3.9+:

```sh
mvn clean verify
python3 tools/build_pack.py
python3 tools/build_release.py
```

The JAR is `target/DurcheinanderMobArmy-1.0.0.jar`. Put it in your server's `plugins` folder, restart, and enable the client resource pack. The resource-pack builder uses Python's standard library only. GitHub Actions builds and uploads the JAR and resource pack automatically.

Tests cover reward deadline/midnight/first-boss cutoff, hit/share limits, repeated boss hits, player scaling, duration validation, leader majorities, and the complete shipped wave/rider/minimum configuration. Follow `TESTING.md` for in-game acceptance checks before opening a public event.

Version sources: [Spigot 26.3 announcement](https://www.spigotmc.org/threads/spigot-bungeecord-26-3.724398/), [Spigot API documentation](https://hub.spigotmc.org/javadocs/spigot/), [official Minecraft 26.3 changes](https://feedback.minecraft.net/hc/en-us/articles/48913133328013-Minecraft-Java-Edition-26-3). Resource pack targets format **97.1**.
