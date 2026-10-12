# Durcheinander MobArmy 1.1.0

Spigot **Minecraft Java 26.3 / Java 25**. Konfigurierbare Raids mit drei Wochen, fünf Stages, Gruppen, Leader-Voting, Bossen, eigenen Items und Kesselritualen. Verwendet nur Bukkit/Spigot-APIs.

## Auf deinem Windows-PC testen

1. ZIP vollständig entpacken. Java und Git werden bei Bedarf automatisch als portable Tools heruntergeladen.
2. Bei einer neuen Installation `local-test/setup.bat` mit Internet starten. Setup baut Spigot und fragt dich selbst nach der Minecraft-EULA.
3. `local-test/start-server.bat` starten. Auf `Done` warten und Minecraft Java **26.3** mit `localhost:25565` verbinden.
4. In der Serverkonsole `op DEIN_NAME` eingeben.
5. Im Spiel `/raid kit` und `/raid demo w1` benutzen. Die Reward-Zeit im Admin-Menü auswählen.
6. In das animierte Portal gehen oder `/raid join demo_w1` benutzen. Du landest im **getrennten Vorraum**. Das Menü öffnet sich nur durch `/raid`.
7. In `/raid` den Leader wählen und als Leader **Ready** drücken. Nach drei Sekunden wird die gesamte Gruppe in die überdachte Arena teleportiert. Die Wave beginnt erst, wenn alle angekommen sind.
8. Nach einer Stage kurz Beute einsammeln, dann geht es automatisch zurück in den Vorraum. Mit `/raid` und Ready die nächste Stage starten.

**Die lokale Demo startet solo, ohne Wipe-Sperren und mit PvP aus in allen Welten.** Java muss nicht manuell installiert werden. Nach der ersten vollständigen Einrichtung und dem ersten erfolgreichen Serverstart geht der Server auch offline; Minecraft muss bereits offline startbar sein.

Installiere `resource-pack/MobArmy-resource-pack-26.3.zip` im Minecraft-Ordner `resourcepacks` und aktiviere das Pack. Bei Updates das alte Pack ersetzen und Minecrafts Ressourcen neu laden (F3+T), damit die vier neuen Item-Texturen sichtbar werden.

### Eine vorhandene Demo aktualisieren

Server mit `stop` beenden, neue ZIP über denselben Ordner entpacken, dann `local-test/start-server.bat` starten. Der Launcher ersetzt das Plugin automatisch und setzt die Demo-Flags. Java/Git/Spigot werden dabei wiederverwendet.

Die alten **generierten Demo-Arenen** werden beim ersten Start auf Layout 2 umgebaut: separater geschlossener Vorraum, Decke, Beleuchtung und Vorbereitungseinrichtung. Eigene Welten mit `generate-demo: false` werden nicht gebaut. Andere Konfigurationen, Loot-Postfach und Fortschritt bleiben erhalten.

## Was sich in 1.1 geändert hat

- Private Nachricht beim Betreten: „Mach /raid, um den Leader zu wählen.“ Kein automatisch geöffnetes Gruppen-Menü.
- Vorraum räumlich getrennt: Ready teleportiert alle hinein, Stage-Abschluss teleportiert zurück. Keine begehbare Verbindung zwischen Vorraum und Kampf.
- Überdachter Dungeon und eigener Vorraum mit Quarz, violetten Fenstern, Sitzbank, Werkbank, Amboss und Endertruhe.
- **Boss-/Miniboss-Anteil = 100 % / erforderliche Mindestspielerzahl.** Woche 1: 50 % pro Spieler, Woche 2: 33⅓ %, Woche 3: 20 %. Größere Gruppen machen diese Obergrenze nicht kleiner. In der Solo-Demo sind es 100 %.
- Normale Treffer bleiben auf 20 %, Mace-Treffer auf 5 % des Mob-Maximallebens pro Treffer begrenzt.
- Wind Charges funktionieren mit **65 % Schub**, maximal 0,9 horizontal / 0,75 aufwärts (Blöcke pro Tick), über Config änderbar. Explosionsschaden und Blockzerstörung bleiben gesperrt.
- Spinnen dürfen an Spielerblöcken klettern, an ursprünglichen Arena-Wänden und Pfeilern wird das Hochklettern tickweise verhindert. Reiter bleiben dabei erhalten. Keine NMS-Abhängigkeit.
- Animierte Portalringe, wandernde Partikel, farbiger Status und lesbares Textdisplay mit Restzeit bis auf Sekunden.
- Raid-Bossbar und Actionbar mit Stage, Wave, Gegnern, Leader, Spielerzahl, Reward-Zeit und deinem Boss-Anteil.
- Vier eigene transparente 128×128-Item-Texturen, neue Lore und nutzbare Relikte. Siehe `ITEMS.md`.
- Kesselritual in drei Phasen mit schwebendem Kern, rotierenden Runen, Energie-Spirale, Sounds und finalem Ausbruch. Abbruch gibt das Fragment ins Loot-Postfach zurück; angefangene Rituale werden beim Neustart erstattet.
- Kontextabhängige Tab-Vervollständigung für Wochen, Portale, Aktionen, Zeitangaben, Item-IDs, Spieler und Mengen; respektiert Permissions.
- Demo-Sperren aus; `/raid cooldown clear` und `/raid demo reset` zum manuellen Zurücksetzen.
- `/raid portal remove <id>` beendet auch laufende Gruppen und entfernt Portaltext, Partikel-Trigger und Portal-Fortschritt. `remove all` entfernt sämtliche Plugin-Portale. Selbst gebaute dekorative Rahmen sind normale Weltblöcke.

## Gruppen, Stages und Rewards

Eine Gruppe pro Portal und gleichzeitig nur eine Gruppe pro physischer Arena. Ein erster Leader wird automatisch bestimmt. Jede Person hat eine Stimme; eine strikte Mehrheit wählt den Leader. Nur der Leader startet. Beim Verlassen/Disconnect wird ein neuer Leader zufällig bestimmt und der Fortschritt weitergeführt.

Stages 1–4: drei Waves und Miniboss. Stage 5: vier Waves, dann Custom-Boss als Wave 5. Reiter frühestens ab Wave 3. Werte, Ausrüstung und Spawnpunkte sind konfigurierbar. Die nächste Wave wartet auf alle Mob-Tode; fehlende/entfernte Entities werden nicht als Sieg gewertet.

Neue Spieler dürfen im Vorraum zwischen Stages beitreten. Während Teleport, Kampf und Beutesammlung ist das Portal gesperrt. Tote behalten Items/XP, scheiden aber aus dem Durchlauf aus. Ein vollständiger Wipe setzt die Gruppe auf Stage 1 zurück. Produktions-Sperre: 30 Minuten am Event-Tag; in der Demo deaktiviert.

Rewards gelten für alle bis zum gewählten Timer, spätestens bis Mitternacht in `Europe/Berlin`, und enden nach dem ersten Boss-Abschluss. Danach ist Wiederholung ohne neue Drops möglich. Nur Gruppen-Eintritt und Stage-Abschluss werden öffentlich angekündigt und nur bei aktiven Rewards. Hinweise, Voting und HUD sind privat.

Nach Minibossen bleiben bis zu 12 Sekunden zum Einsammeln. Verbliebene **Custom-Raid-Items** werden vor dem Rückteleport ins Leader-Inventar/Postfach gerettet. Nach dem Endboss gilt die gemeinsame Beutesammlung bis maximal 120 Sekunden. Boss-Loot geht an den Leader; volle Inventare werden durch `/raid loot` abgefangen. Gespeicherte Gewinner-Gruppen erlauben das Verteilen auch nach dem Verlassen an offline Mitglieder.

PvP in Raid-Welten, Elytra-Boosts, Feuerwerke und schädliche Explosionen sind gesperrt. Golden Apples, Tränke und Totems haben Stage-Limits. Enderperlen brauchen ein freies, gültiges Ziel innerhalb der Arena und haben Cooldown. Ursprüngliche Blöcke sind geschützt. Temporäre Spielerblöcke dürfen maximal zwei Schichten über dem Boden stehen; nur der Platzierende darf sie abbauen. Bei Cleanup werden sie einschließlich gefüllter Shulker-Boxen zurückgegeben.

## Befehle

| Befehl | Bedeutung / Permission |
|---|---|
| `/raid` | Gruppen-Menü mit Voting, Ready und Loot — `mobarmy.play` |
| `/raid join <portal>`, `/raid start`, `/raid ready`, `/raid loot` | Beitreten, als Leader starten, Postfach — `mobarmy.play` |
| `/raid leave`, `/raid status`, `/raid help` | Verlassen, Status und Hilfe |
| `/raid portal pos1`, `/raid portal pos2` | Eigene Blockpositionen auswählen — `mobarmy.admin` |
| `/raid portal create <week> [id]` | Trigger erstellen, danach Reward-Zeit wählen — `mobarmy.admin` |
| `/raid portal info [id]` | Details anzeigen — `mobarmy.admin` |
| `/raid portal remove <id\|all>` | Komplett entfernen, laufende Gruppe wird evakuiert — `mobarmy.admin` |
| `/raid open [id]`, `/raid close [id]`, `/raid stop [id]` | Zugang oder Gruppe steuern — `mobarmy.admin` |
| `/raid timer set\|add <dauer> [id]`, `/raid timer clear [id]` | Rewards setzen/verlängern/ausschalten — `mobarmy.timer` |
| `/raid demo <week>` | Testportal erstellen — `mobarmy.admin` |
| `/raid demo reset [id\|all]` | Demo auf Stage 1 und Rewards-Status zurücksetzen, Sperren löschen; Timer anschließend neu setzen — `mobarmy.admin` |
| `/raid demo rebuild <week>` | Eine generierte Demo-Arena neu bauen; vorher Gruppen beenden — `mobarmy.admin` |
| `/raid cooldown clear [spieler\|all]` | Wipe-Sperre und Ausscheiden zurücksetzen; ohne Spieler: du selbst — `mobarmy.admin` |
| `/raid test on\|off` | Solo-/Wipe-Regeln während der laufenden Demo ändern — `mobarmy.admin` |
| `/raid kit`, `/raid reload` | Ausrüstung / Config laden — `mobarmy.admin` |
| `/raid build`, `/raid visit <week>` | Expliziter Bau-Modus; nur ohne aktive Gruppen — `mobarmy.build` |
| `/raid item give\|take <id> [spieler] [anzahl]` | Echte Custom-Items geben/entfernen — `mobarmy.item` |
| `/raid item count <id> [spieler]` | Echte Custom-Items zählen — `mobarmy.item` |

Dauer: `90s`, `30m`, `2h`; nur eine Zahl bedeutet Minuten. OP hat Admin-Rechte. Normale Spieler haben `mobarmy.play`. Ohne Portal-ID wird das Portal deiner Gruppe oder das einzige vorhandene Portal verwendet.

Die Portalregion ist ein Quader mit Partikeln und Betretens-Erkennung. Dekorative Formen/Rahmen kannst du darum bauen. Ein neuer Event-Durchlauf benötigt einen neuen Portalnamen; ein verlängerter Timer hebt einen normalen abgeschlossenen Boss nicht auf. Demo-Reset ist die ausdrückliche Test-Ausnahme.

## Konfiguration und Erweiterung

`plugins/DurcheinanderMobArmy/config.yml` enthält `weeks`, `items`, `balance`, `presentation`, `ritual-animation`, `messages` und `testing`. `state.yml` speichert Fortschritt, Deadline, Boss-Status, Cooldowns, Rückkehrpositionen, Loot und vorübergehende Block-/Ritual-Recovery.

Neue Woche: vorhandenen `weeks.<id>`-Eintrag kopieren, eigene Welt und fünf Stages eintragen. Eine eigene Arena mit `generate-demo: false` bauen/importieren. `arena` und `lobby` sind unabhängige, inklusive Blockregionen; `min.y` steht direkt über dem Boden. Arena höchstens 100×100. Spawnpunkte benötigen einen soliden Boden und zwei freie Blöcke; bevorzugt wird der am weitesten entfernte Punkt mit Mindestabstand.

Neue Items: Material, `name`, `lore`, `item-model`, Drop-Quellen und optional `ritual` oder `use` hinzufügen. Drop-Quellen: `mob`, `miniboss`, `boss`, optional Stage/Woche, Menge und Chance. Relikt-`use`: Verbrauch, Radius auf aktuelle Gruppenmitglieder, Cooldown, Nachricht und Liste von Potion-Effekten mit Sekunden/Amplifier. Items werden über PDC-ID erkannt, Namen lassen sich daher nicht als Ersatz fälschen.

Vor `/raid reload` sämtliche Gruppen und laufenden Rituale beenden. Für globale Bukkit-Reloads ist der normale Server-Neustart vorzuziehen. Regelmäßige Server-Backups sind sinnvoll, da Minecraft-Spielerdaten und Plugin-YAML getrennt gespeichert werden.

## Bauen und prüfen

Mit Java 25 und Maven 3.9+:

```sh
mvn clean verify
python3 tools/build_pack.py
python3 tools/build_release.py
```

JAR: `target/DurcheinanderMobArmy-1.1.0.jar`. Resource Pack: `target/MobArmy-resource-pack-26.3.zip`. Das Pack verwendet Format 97.1. GitHub Actions baut JAR, Pack und ZIP und prüft die Windows-Setup-Skripte.

Automatische Tests prüfen Timer/Mitternacht/Boss-Abschluss, Schaden und Mindestspieler-Anteile, Waves/Reiter, komplette Tab-Grammatik, Spinnen-Regelausnahme, GUI-freien Eintritt und den Gruppen-Teleport. Sichtbarkeit, native Wind-Charge-Physik, Spinnen-Bewegung und Multiplayer-Balance bitte mit `TESTING.md` im Spiel prüfen.

Versionsquellen: [Spigot 26.3](https://www.spigotmc.org/threads/spigot-bungeecord-26-3.724398/), [Spigot API](https://hub.spigotmc.org/javadocs/spigot/), [Minecraft 26.3](https://feedback.minecraft.net/hc/en-us/articles/48913133328013-Minecraft-Java-Edition-26-3).
