# In-game acceptance checklist

Run on a separate localhost server with your test inventory. This checklist is manual; automated rule/config checks do not verify GUI rendering, pathfinding, resource-pack appearance, or multiplayer balance.

1. **Boot:** plugin enables, three demo worlds appear, no exceptions in console. Install and enable the resource pack in Minecraft 26.3.
2. **Solo:** OP yourself, `/raid test on`, `/raid kit`, `/raid demo w1`, choose 60 minutes. Enter, Ready, then walk into the arena. Gate remains open until entry and shuts before wave 1.
3. **Wave structure:** clear 3 waves and miniboss. Lobby return and stage increment happen once. Repeat through stage 5, whose fifth wave contains only the custom boss. Model tracks the invisible husk; bossbar follows HP.
4. **Rewards:** stage-1 miniboss produces Chaosfragment. First boss crown goes to leader. Repeat with full inventory: crown appears in `/raid loot` and cannot be duplicated with shift-click, number keys, dragging, or rapid clicking.
5. **Teamwork:** test with 2 people and solo mode OFF. Ready cannot start below minimum. Vote leader, disconnect elected leader, confirm random handover and preserved stage. With 3 people, one participant stops dealing boss damage at 40%; with 2, at 50%.
6. **Portal locking:** outsiders cannot join via command, walking, or pearls during entry/fight/loot. They can join between stages. A second portal targeting the same world cannot run concurrently.
7. **Deaths:** inventory and XP survive death; respawn is outside portal. Dead member cannot rejoin the running party. Wipe resets everyone to stage 1; event-day cooldown blocks re-entry. After the event date, no wipe cooldown is enforced.
8. **Timer:** set a short timer, kill mobs before and after expiry. Only pre-expiry kills yield loot. Adjust system/timezone on a disposable test server to test midnight cutoff. Boss first-clear permanently disables rewards even if timer is extended.
9. **Restrictions:** high mace smash, bow, mob arrows, splash potions, TNT, crystals, anchor, firework, wind charge, elytra, wall/outside pearl destinations, consecutive apples/totems/potions. Neither raid-mob friendly fire nor player PvP should deal damage.
10. **Blocks:** building on arena floor works up to two layers, a third layer fails. Original walls cannot be mined. Only the placer can break temporary blocks. Filled shulker boxes return intact after stage cleanup or `/raid stop`, with overflow in the mailbox.
11. **Ritual:** in default `world`, place a cauldron and right-click with the authentic fragment. A ritual husk appears and drops a Ritualkern when killed, including when raid rewards are expired. A renamed ordinary amethyst shard must not work.
12. **Restart:** with a party at stage 2, cleanly stop/restart. Portal deadlines, completed-boss flags, mailbox and leader stage survive. Offline/dead players recover outside the raid. Each resumed stage begins with wave 1.
13. **Custom content:** copy a week to a new ID/world, define five stages/spawns/items and reload with no active parties. Invalid/empty waves return to lobby and log an actionable error instead of awarding progress.

Use `/raid test off` after testing and remove test portals. Start a real event with a new portal ID and a selected reward timer.
