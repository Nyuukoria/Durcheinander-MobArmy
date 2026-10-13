# In-game checks for 1.5.0

Automated tests verify rules, command grammar and a mocked party-entry/teleport flow. The following checks exercise Minecraft's native movement, rendering and multiplayer behavior.

1. Update over an existing stopped demo. Start and confirm plugin version 1.5.0, new separate lobby and full arena ceiling. Old generated layouts migrate once; custom worlds with generate-demo false should remain hand-built.
2. Join a portal. A private `/raid` hint appears; no inventory GUI opens automatically. The waiting room has no walkable connection to the combat space. `/raid` opens voting. Leader Ready teleports everyone after the countdown; combat begins only after all successful arrivals.
3. Complete a stage. Collect miniboss loot before return. After up to 12 seconds, leftover custom loot is stored for the leader and everyone is teleported back to the lobby. Invite a new player between stages, elect a different leader, and start again.
4. With test mode OFF and week 1 minimum 2, each of two people can deal 50% boss/miniboss HP. With week 2 minimum 3 each can deal 33⅓%; with week 3 minimum 5 each can deal 20%. Extra members do not change the cap. Test mode ON uses 100%.
5. Test high mace smashes, bows and strong hits; normal hit cap remains 20%, mace 5%. At the final tiny fractional HP, the boss should finish rather than become stuck by rounding.
6. Use wind charges against the ground and nearby players in the raid. They work with visibly reduced, capped boost; they do not destroy blocks or cause explosion damage. Elytra/fireworks remain blocked. Check wall/outside pearl destinations and cooldown.
7. Watch ordinary spiders and spider jockeys at original walls and pillars: no upward climbing. Place a two-high player wall away from an original wall: climbing is permitted there. Mounted skeletons remain alive if the spider dies; wave waits for the rider too.
8. Enable the new resource pack. Test all 34 custom textures in inventory and hand. Each item has lore. Chaosfragment on a default-world cauldron opens the ritual selection; choose Awakening to summon a guardian after eight seconds. Paid recipes refund their ingredients into `/raid loot` if interrupted before completion. Katalyst stays in inventory. Restart halfway: ingredients recover once.
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

## Zusätzliche Prüfung für 1.3

- Alle elf Icons im Inventar und in der Hand prüfen: echte 16×16-Pixeloptik, keine verschwommenen Kanten. `/raid pack reload` lädt das neue Pack; beim Verbinden annehmen.
- `/raid kit` enthält alle zehn wiederverwendbaren Relikte. Glutamulett gewährt 15s Feuerresistenz, Wächterrune vier zusätzliche Herzen für 8s.
- Jägerlinse markiert nur Raid-Gegner in 16 Blöcken für 12s und gewährt 30s Nachtsicht. Schwächungszahn wirkt 6s auf normale Raid-Gegner, 3s auf Bosse. Kein Effekt darf Spieler oder andere Welt-Mobs treffen. Ohne Gegner wird kein Cooldown verbraucht.
- Alle Items bleiben nach Rechtsklick erhalten; Cooldowns gelten auch nach Relog und für weitere Kopien derselben Item-ID.

## Zusätzliche Prüfung für 1.4

- `/raid kit gear`, `/rezepte`, `/raid kit ritual` ausprobieren. Das Basis-Kit darf bereits getragene Custom-Rüstung nicht überschreiben. Inventarüberlauf muss im Postfach ankommen; kein Rezept-Icon darf durch Shift-Klick, Drag, Zahlentasten oder Doppelklick entnommen werden.
- Wächter-/Sturm-/Abgrundteile anziehen. `/raid gear` zählt nur echte PDC-Items im richtigen Slot. Zwei-/Vier-Teil-Boni wirken in allen Welten; Ablegen/Wechseln beendet sie nach höchstens 60 Ticks. Andere stärkere Potion-Effekte bleiben bestehen.
- Hohe Enchantments im Tooltip und Spiel prüfen: Schärfe VIII, Power VII, Dichte VI, Effizienz VII, Fortune IV. Haltbarkeit sinkt normal; Mending mit normalen Welt-XP. Bogen benötigt passenden Pfeil und schießt; Custom-Bow-Icon hat keine separate Pull-Sprite-Animation.
- Trefferschaden mit seltenen Waffen bleibt gedeckelt. Tools verändern geschützte Arena-Blöcke nicht. Enchanting/Amboss/Schleifstein/Schmiedetisch/Vanilla-Crafting akzeptieren keine Custom-Items; normale Vanilla-Items weiterhin nutzbar.
- Alle sieben Rezepte gemäß `RITUALS.md` ausführen. Vor Start unzureichende Zutaten: kein Verbrauch. Custom-Splitter und umbenannte Vanilla-Splitter unterscheiden. Kesselbruch, Wechsel der Hand, Logout oder Weglaufen erstattet bezahlte Zutaten; Cooldown bleibt.
- Wiederherstellung repariert Custom-Gear in Rüstung, Hotbar und Nebenhand. Wenn nichts beschädigt ist: kein Start/Verbrauch. Schmiedergebnis und Wächterbeute kommen an die ausführende Person, auch bei vollem Inventar/offline Besitzer.
- Wächter benötigen einen sicheren Spawn, bleiben höchstens 10 Minuten und geben bei Entfernen/Umwelttod keine Sieg-Beute. Eigene Rituale mit `/raid ritual cancel`, alle nur ausdrücklich per `cancel all` als Admin beenden.
- Produktive Drop-Chancen im Rezepte-Menü gegen aktuelle YAML prüfen. Legendaries stammen aus 1-%-/2-%-Boss-Pools; mehrere Items desselben Pools dürfen nicht beim selben Wurf entstehen. Trainingsdurchläufe ohne Rewards geben keine Pool-Drops.
- Bestehende 1.3-Konfiguration upgraden: neue Gear-/Set-/Loot-/Ritual-Sektionen vorhanden, Arenen und bestehende individuelle Einstellungen erhalten. Alte einzelne und neue mehrteilige offene Ritualzahlungen werden einmal ins Postfach übernommen.

Automatische Tests decken gewichtete Chancen/Grenzfälle, Zutatenprüfung ohne Teilzahlung, Fake-Item-Ablehnung, Journal-Recovery, Set-Zählung, Konfigurations-Verweise und 16×16-PNGs ab. Echte Enchantment-Effekte, getragene Trims, Rituale und Multiplayer-Balance bleiben im Minecraft-Client zu prüfen.

## Zusätzliche Prüfung für 1.5

- Als Leader und Nicht-Leader die Starttaste und das Schild anklicken: nur Leader kann starten. Rezepte-Taste öffnet den Katalog bei beiden. Endertruhe zeigt mit dem Verschluss zur Raummitte.
- `/raid skip wave` während einer Normalwelle: alte Gegner verschwinden, nächste Welle startet, keine Skip-Drops. Während des Minibosses: nächste Stufe im Vorraum. `/raid skip stage` auch im Vorraum prüfen. Stufe 5 beendet den Durchlauf ohne Boss-Abschluss-Flag. In Produktionsmodus an normalem Event-Portal sowie ohne Admin-Rechte werden Skips abgelehnt.
- Mehrere Wellen ansehen: Spawns über die Arena verteilt und mindestens acht Blöcke von Spielern entfernt. Unterschiedliche Zombietypen, Skelette, Spinnen und spätere Hexen/Blazes/Breezes/Pillager usw.; keine Phantome/Vex. Alle Raid-Mobs inklusive Reiter leuchten nach Spawn 200 Ticks.
- `/raid boss list`, alle neun IDs beschwören und mit `/raid boss clear all` entfernen. Keine KI, Angriffe, Drops oder Fortschritte in der Vorschau. Echte Bosswellen zeigen unterschiedliche Varianten; Frost verlangsamt kurz, Sturm beschleunigt den Boss, Markierung lässt nahe Spieler kurz leuchten.
- Zwei Rüstungsteile, drei plus passende Waffe, gemischte Sets sowie identische Waffen in beiden Händen prüfen. Werkzeug in Nebenhand zählt; Werkzeug im Inventar zählt nicht. Boni funktionieren auch vor dem Raid und außerhalb der Raid-Welten.
- Mit zwei Spielern `/raid language de` und `/raid language en` prüfen: deutsche/englische Menüs, HUD, Meldungen und Items, jeweils genau ein Portaltext. Sprache bleibt nach Relog/Neustart erhalten. Commands/IDs bleiben unverändert. Das Rezepte-Menü nennt Zutaten, Ritualwege und konkrete Drop-Chancen.
- Perlen mitten im Kampf, beim Treffer an Gegnern, an Außen-/Spielerwänden und über Hügeln werfen. Landung innerhalb der Arena, keine nachträgliche Cooldown-Ablehnung; nächste Perle nach drei Sekunden. Kein Teleport außerhalb der Arena oder während Vorraum/Loot.
- Vorhandene Normalarena mit altem hinterem Durchgang starten: Außenwand geschlossen. Stage starten, abschließen, stoppen und Server neu starten: kein Loch entsteht wieder. Generierte Boden-/Deckenlöcher werden ebenfalls repariert; individuelle Arenen werden nicht automatisch umgebaut.
- Portal mit langer ID, Sprachwechsel und Relog: Anzeige bleibt über dem Portal, keine doppelte Sprachversion. `/raid portal delete demo_w1`, Neustart und `/raid demo w1` von anderer Position: alter Trigger/Text/Partikel verschwunden, neue Position funktioniert. Selbst gebaute Rahmen bleiben Weltblöcke.

Automatische Tests prüfen Demo-Fortschritt ohne Beute, Spawnabstände, sicheren Perlen-Suchbereich, Set-Zählung mit Händen, Sprachübersetzung ohne veränderte Befehle, alle Gegner-/Bossreferenzen, Migration und Ressourcenpaket. Client-Rendering, Minecraft-Physik und echte Mehrspieler-Interaktionen sind weiterhin In-game-Prüfungen.
