# Eigene Arenen bauen – ab 1.8

Du brauchst OP oder `mobarmy.build`. Eine Arena darf während der Bearbeitung keinen laufenden Raid haben. Der Befehl wartet einen Server-Tick, damit die Welt für Backup/Export sicher gespeichert und kurz entladen werden kann. Spieler in dieser Welt werden dafür vorübergehend in die normale Welt und anschließend zurück teleportiert. Große Exporte können den Server kurz beanspruchen; maximal 256 MiB Weltdateien pro ZIP.

## Schnellstart

```
/raid arena edit w1
```

Du bekommst Creative und Flugmodus mit einem Auswahlstab in Slot 9. Dein vorheriges Inventar inklusive Rüstung, Nebenhand und Cursor, dein Spielmodus, Flugzustand und Hotbar-Slot werden separat gespeichert. Beim Beenden werden sie wiederhergestellt; das Creative-Bauinventar wird verworfen. Vor Beginn entsteht ein Weltbackup in `plugins/DurcheinanderMobArmy/arena-backups/`. Baue in der Arena-Welt: die normale Welt ist kein unterstütztes Arena-Backupziel.

Die vorhandene 100×100-Arena und der getrennte kleine Vorraum bleiben als Bauvorlage erhalten. Ab der ersten Bearbeitung wird die automatische Demo-Erzeugung für diese Karte deaktiviert, damit deine Bauten beim Neustart erhalten bleiben. Während des Baumodus bleibt der Raid-Eintritt gesperrt.

```
/raid arena save
/raid arena export
/raid arena done
```

`save` speichert Welt und Konfiguration und prüft die Karte. Es beendet den Baumodus nicht. `export` erzeugt eine ZIP und nennt ihren Dateipfad im Chat. `done` speichert und verlässt den Baumodus. Eine fehlerhafte Karte bleibt als gespeicherter Entwurf gesperrt. Auch ungültige Entwürfe können exportiert werden, damit ich sie bearbeiten kann.

Die Exportdatei findest du im Serverordner unter:

```
plugins/DurcheinanderMobArmy/arena-exports/w1-<Zeitstempel>.zip
```

**Schick mir diese komplette ZIP**, um deine Arena in die nächste Plugin-Version einzubauen. Sie enthält die echten Minecraft-Weltdateien mit Blöcken, Blockzuständen, Schildern, Block-Entities, Biomen und gespeicherten Entities sowie die Raid-Konfiguration. Spielerdateien, Session-Locks und Server-Konfigurationen werden ausgeschlossen. `validation.txt` nennt den Prüfstatus; `sha256.txt` enthält Prüfsummen. Es gibt keinen Importbefehl in dieser Version; manuelle Wiederherstellung erfordert einen gestoppten Server und ein Backup, siehe `README.txt` im Export.

## Neue Karte mit eigener ID

```
/raid arena create forest w1
```

Kopiert die tatsächliche gespeicherte Welt und Raid-Einstellungen von `w1` in `mobarmy_arena_forest` und öffnet den Baumodus für `forest`. Die ID muss mit einem Kleinbuchstaben beginnen, maximal 32 Zeichen lang sein und darf nur Kleinbuchstaben, Ziffern, `_` oder `-` enthalten. Eine vorhandene Welt wird nicht überschrieben. Beende eine laufende Bearbeitung vor dem Klonen. Das Portal für die neue Karte legst du danach mit `/raid portal pos1`, `/raid portal pos2` und `/raid portal create forest <portal-id>` an. Die beiden Portalpunkte wählst du in der normalen Welt.

## Grenzen ändern

Wähle mit dem Stab zwei Blöcke: Linksklick = Ecke 1, Rechtsklick = Ecke 2. Alternativ wähle deine aktuellen Positionen mit `/raid arena pos1` und `/raid arena pos2`.

```
/raid arena region forest arena
/raid arena region forest lobby
/raid arena region forest door
```

Die Auswahl umfasst den **Innenraum**, inklusive begehbarer Höhen, nicht die äußere Hülle. Arena: maximal 100×100 Blöcke und 128 Blöcke Höhe. Vorraum und Arena dürfen sich nicht überschneiden. Boden liegt eine Ebene unter dem Innenraum, Decke eine Ebene darüber, Wände direkt außerhalb. Pflanzen und Landschaft sind möglich; Spieler- und Mobspawnpunkte brauchen Boden und drei freie Blöcke Höhe. Der Türbereich darf maximal 10.000 Blöcke enthalten; er wird während des Kampfes vorübergehend zur Barriere und danach wiederhergestellt. Für einen vollständig getrennten Vorraum kann er im vorhandenen kleinen ungenutzten Bereich bleiben: Spieler betreten/verlassen die Arena per Teleport.

```
/raid arena seal forest
```

Füllt passierbare Lücken der äußeren Hülle von Arena und Vorraum mit poliertem Tiefenschiefer. Vorhandene feste Blöcke bleiben erhalten. **Auch beabsichtigte Durchgänge werden geschlossen**; nutze den Befehl zum Abdichten einer getrennten Karte, nicht für offene Architektur. Regionen und Spawnangaben müssen vorher gültig sein.

## Spawnpunkte und Wandbedienung

Stelle dich an die jeweilige gewünschte Position:

```
/raid arena set forest arena-spawn
/raid arena set forest lobby-spawn
/raid arena spawn forest clear
/raid arena spawn forest add
```

Füge mindestens vier verschiedene, weit verteilte Gegnerpunkte hinzu. Der Spawnplaner prüft Kandidaten um diese Punkte und weiterhin mindestens 18 Blöcke Abstand zu Spielern. Sind zu wenige freie entfernte Stellen vorhanden, kann eine Welle nicht vollständig starten: plane breite Wege und ausreichend freie Fläche. Maximal 64 Punkte. Weitere Befehle:

```
/raid arena spawn forest list
/raid arena spawn forest remove 2
```

Für Bedienelemente schaue auf eine feste Wand im Vorraum, höchstens fünf Blöcke entfernt. Vor dieser Wand und darüber müssen zwei Plätze für Knopf und Schild frei sein; auch der Wandblock darüber muss fest sein.

```
/raid arena set forest recipes
/raid arena set forest start
```

Der Rezepteknopf öffnet das Menü. Den Startknopf darf weiterhin nur der Gruppenleiter verwenden. Alte Bedienelemente kannst du im Baumodus abbauen. Es wird beim Betreten des Vorraums kein Menü automatisch geöffnet.

## Speichern, Schutz und Neustart

- Alle 60 Sekunden werden Welt und aktueller Entwurf gespeichert. `save` nach größeren Bauabschnitten ist trotzdem sinnvoll.
- `/raid arena validate forest` zeigt Probleme. `/raid arena list` zeigt vorhandene IDs; `/raid arena help` zeigt die Kurzbefehle.
- Logout und Plugin-Shutdown speichern den Entwurf und stellen das ursprüngliche Spielerinventar wieder her. Nach Tod erfolgt die Wiederherstellung beim Respawn. Nach einem Serverabbruch wird ein gespeicherter Rückkehrdatensatz beim nächsten Login abgearbeitet. Änderungen seit dem letzten Speichern können bei einem harten Abbruch verloren gehen.
- Nach Unterbrechungen bleibt die Karte gesperrt: erneut `edit`, dann `save` und `done`, um sie nach erfolgreicher Prüfung freizugeben.
- Spielende Raid-Teilnehmer dürfen deine Weltblöcke nicht abbauen. Kampfplatzierungen werden zurückgesetzt; Explosionen, Feuer, Wasserfluss, natürliche Blockänderungen und Mob-Griefing dürfen die Arena nicht umbauen. Beim Bauen kannst du Blöcke, Rahmen, Rüstungsständer und Container bewusst ändern.
- Setboni und normale Raidregeln gelten auch auf selbst gebauten Karten. Demo-Skip und Cooldown-Clear bleiben verfügbar.

# Building arenas (English)

Use `/raid arena edit <id>` as OP. Your normal inventory/mode are saved; you build with a separate Creative inventory. Use `save` to persist and validate, `export` to create a complete world ZIP, and `done` to leave and publish a valid map. Invalid or interrupted builds stay locked drafts. Send the ZIP from `plugins/DurcheinanderMobArmy/arena-exports/` for integration.

`/raid arena create forest w1` clones an existing map into a new world. Select corners using the wand or `pos1`/`pos2`; assign `region <id> arena|lobby|door`. Arena bounds describe the interior; the floor, roof and walls are immediately outside. Keep the lobby separate. Set player positions with `set <id> arena-spawn|lobby-spawn`. Add at least four spread-out enemy positions with `spawn <id> add`; use `clear`, `list`, or `remove <number>` to manage them. Look at a solid lobby wall and use `set <id> recipes|start` to place controls. `seal` fills passable shell holes, including intentional openings. Backups are made before editing; autosave runs every 60 seconds. Exports preserve native world data and omit playerdata/server files. Restoration instructions are included in each export; no import command is provided.
