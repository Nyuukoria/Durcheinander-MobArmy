# In-game checks for 1.2.0

Automated tests verify rules, command grammar and a mocked party-entry/teleport flow. The following checks exercise Minecraft's native movement, rendering and multiplayer behavior.

1. Update over an existing stopped demo. Start and confirm plugin version 1.2.0, new separate lobby and full arena ceiling. Old generated layouts migrate once; custom worlds with generate-demo false should remain hand-built.
2. Join a portal. A private `/raid` hint appears; no inventory GUI opens automatically. The waiting room has no walkable connection to the combat space. `/raid` opens voting. Leader Ready teleports everyone after the countdown; combat begins only after all successful arrivals.
3. Complete a stage. Collect miniboss loot before return. After up to 12 seconds, leftover custom loot is stored for the leader and everyone is teleported back to the lobby. Invite a new player between stages, elect a different leader, and start again.
4. With test mode OFF and week 1 minimum 2, each of two people can deal 50% boss/miniboss HP. With week 2 minimum 3 each can deal 33⅓%; with week 3 minimum 5 each can deal 20%. Extra members do not change the cap. Test mode ON uses 100%.
5. Test high mace smashes, bows and strong hits; normal hit cap remains 20%, mace 5%. At the final tiny fractional HP, the boss should finish rather than become stuck by rounding.
6. Use wind charges against the ground and nearby players in the raid. They work with visibly reduced, capped boost; they do not destroy blocks or cause explosion damage. Elytra/fireworks remain blocked. Check wall/outside pearl destinations and cooldown.
7. Watch ordinary spiders and spider jockeys at original walls and pillars: no upward climbing. Place a two-high player wall away from an original wall: climbing is permitted there. Mounted skeletons remain alive if the spider dies; wave waits for the rider too.
8. Enable the new resource pack. Test all seven custom textures in inventory and hand. Each item has lore. Authentic Chaosfragment on a default-world cauldron produces a floating core, runic ring, spiral, phase text, sounds and a final guardian after six seconds. Break the cauldron halfway: the catalyst remains in inventory with no extra mailbox copy. Restart halfway: the catalyst still remains once.
9. Ritualkern right-click keeps the item, grants resistance/regeneration and applies 60-second cooldown. Crown right-click applies speed/haste to the nearby party without consuming it, with 75-second cooldown. Relog does not reset item cooldowns. Renamed vanilla items cannot trigger abilities or a ritual.
10. Test Raidsplitter count/take on real PDC shards and renamed vanilla prismarine shards. Only real shards count; insufficient requested quantity removes nothing. NPC integration must pay only after API removal succeeds.
11. Check portal hologram readability, animated outline, locked red state, exact reward countdown, raid BossBar/ActionBar and per-player contribution display. Stopping/leaving removes that party's HUD. Reward expiry and first boss clear stop drops.
12. Test tab completion for every root/argument: portal create/remove/info, timer actions/durations/IDs, item actions/IDs/player names/amounts, cooldown clear/player/all, demo reset/rebuild and pack status/reload. Non-admins do not receive admin completions.
13. Wipe in solo demo: no 30-minute lock. `/raid cooldown clear` clears self, a named player or all. `/raid demo reset demo_w1` ends the group, resets stage/boss state and clears locks; choose a new reward timer afterward.
14. `/raid portal remove demo_w1` during an active party evacuates members and removes the trigger, hologram, HUD, mobs and portal progress. `remove all` removes all plugin portals; no particle animation should remain. Manually built frames are ordinary world blocks.
15. Confirm PvP off in demo default and raid worlds. Keep-inventory/XP, individual death elimination, full wipe progress reset, restrictions on original/player blocks, filled-shulker cleanup and offline mailbox retrieval still work.

For a public event, use the production config and manually balanced equipment/player counts. Do not deploy the localhost offline-authentication settings to a public network.

## Zusätzliche Prüfung für 1.2

- Beim Verbinden Pack annehmen, `/raid pack status` muss „geladen“ zeigen. Frische Items und bereits vorhandene Relikte kontrollieren. Test ohne Internet bei laufendem localhost-Server wiederholen.
- Bei abgelehntem Pack erscheinen verständliche Hinweise. Server-Packs aktivieren und `/raid pack reload` testen. Alte manuell installierte MobArmy-Packs deaktivieren, falls sie Modelle überschreiben.
- Portal schließen: nur ein Schild, roter Ring/Kreuz, Eintritt gesperrt. Öffnen: violett/cyan und Eintritt möglich. Ready: Sperre während Teleport/Kampf/Loot; nach Rückkehr offen. Zweites Portal derselben Woche zeigt bei belegter Arena ebenfalls gesperrt. Nach Neustart entfernte/alte Textdisplays dürfen nicht wieder auftauchen.
- Vorraum innen 9×9, Arena innen 100×100, mit Decke. Spawn-Pads und Wege frei; alle Hügel zu Fuß begehbar. Zwei Spielerblöcke über flachem Boden und auf Hügeln erlaubt, dritter gesperrt.
- `/raid item give frost_talisman`, `healing_bloom`, `gale_sigil`, `ritual_core` und `chaos_fragment` testen. Relikte behalten ihre Anzahl. Cooldown gilt über alle Kopien und nach Relog. Frost wirkt nur auf Raid-Gegner, verursacht keinen Schaden und überschreitet keine Boss-Anteile.
- Ritual beginnen und Kessel entfernen: Katalysator bleibt genau einmal im Inventar, keine zusätzliche Kopie im Postfach. Wiederverwendung nach 120 Sekunden. Alte ausstehende, bereits bezahlte Rituale aus 1.1 werden weiterhin einmal erstattet.

Automatisch getestet werden Gelände-Steigungen/Spawn-Pads, Config-Migration, lokal erreichbarer ZIP-Download, SHA-1 und HTTP-Methoden. Minecraft-Rendering und echtes Multiplayer-Spiel bleiben manuelle Prüfungen.
