# Durcheinander MobArmy 1.8.0

## Neu in 1.8

- `/raid arena edit <arena>`: eigener Creative-Baumodus mit separatem Inventar, Backup, Entwurf, Prüfung und Welt-ZIP-Export. `/raid arena create <id> [vorlage]` kopiert eine bestehende Karte. Anleitung: [ARENA-BUILDING.md](ARENA-BUILDING.md).
- 17 Waffen-/Werkzeugfähigkeiten: Wächterhaltung, Sturmhieb, Abgrundstoß, Streitkolben-Stoßwelle, Wind-/Sternenpfeile, Eile, Fortbewegung, Nachsaat, Angeln und mehr. Schleichen + Rechtsklick aktiviert; normale Nutzung bleibt möglich. Fundorte und Ritualwege stehen in `/rezepte`, Details in [EQUIPMENT.md](EQUIPMENT.md).
- Neun Raid-Bossprofile mit zehn Angriffstypen, zwei Sekunden Vorwarnung und Bodenpartikeln. Ringstoß hat eine sichere Mitte; Ansturm lässt seitlich Platz. Unter halbem Leben verkürzen sich die Pausen. Beschworene Verstärkung gibt weder XP noch Beute.


Spigot **Minecraft Java 26.3 / Java 25**. Konfigurierbare Raids mit drei Wochen, fünf Stages, Gruppen, Leader-Voting, Bossen, eigenen Items und Kesselritualen. Verwendet nur Bukkit/Spigot-APIs.

## Auf deinem Windows-PC testen

1. ZIP vollständig entpacken. Java und Git werden bei Bedarf automatisch als portable Tools heruntergeladen.
2. Bei einer neuen Installation `local-test/setup.bat` mit Internet starten. Setup baut Spigot und fragt dich selbst nach der Minecraft-EULA.
3. `local-test/start-server.bat` starten. Auf `Done` warten und Minecraft Java **26.3** mit `localhost:25565` verbinden.
4. In der Serverkonsole `op DEIN_NAME` eingeben.
5. Im Spiel `/raid kit` (inklusive aller zehn wiederverwendbaren Relikte) und `/raid demo w1` benutzen. Die Reward-Zeit im Admin-Menü auswählen.
6. In das animierte Portal gehen oder `/raid join demo_w1` benutzen. Du landest im **getrennten Vorraum**. Das Gruppenmenü öffnet sich durch `/raid`. An der Wand befinden sich Rezepte und Start; Start bedient nur der Leader.
7. In `/raid` den Leader wählen und als Leader **Ready** drücken. Nach drei Sekunden wird die gesamte Gruppe in die überdachte Arena teleportiert. Die Wave beginnt erst, wenn alle angekommen sind.
8. Nach einer Stage kurz Beute einsammeln, dann geht es automatisch zurück in den Vorraum. Mit `/raid` und Ready die nächste Stage starten.

**Die lokale Demo startet solo, ohne Wipe-Sperren und mit PvP aus in allen Welten.** Java muss nicht manuell installiert werden. Nach der ersten vollständigen Einrichtung und dem ersten erfolgreichen Serverstart geht der Server auch offline; Minecraft muss bereits offline startbar sein.

Die Demo liefert die Texturen automatisch von deinem PC an Minecraft: den Ressourcenpaket-Dialog beim Verbinden **annehmen**. Das funktioniert nach dem Setup auch ohne Internet. `/raid pack status` zeigt den Ladezustand; `/raid pack reload` fordert das Pack erneut an. Falls du Server-Packs deaktiviert hast, bei diesem Server **Server-Ressourcenpakete: Aktiviert** einstellen und erneut verbinden. Alternativ `resource-pack/MobArmy-resource-pack-26.3.zip` im Client-Ordner `resourcepacks` aktivieren. Andere Launcher benötigen dabei ihren eigenen Minecraft-Ordner.

Die 34 eigenen Item-Texturen sind echte **16×16-PNGs** mit kleinen Farbpaletten, harten Pixelkanten und transparentem Hintergrund. Das Pack wird im **Client** geladen.

### Eine vorhandene Demo aktualisieren

Server mit `stop` beenden, neue ZIP über denselben Ordner entpacken, dann `local-test/start-server.bat` starten. Der Launcher ersetzt das Plugin automatisch und setzt die Demo-Flags. Java/Git/Spigot werden dabei wiederverwendet.

Alte Layouts vor Version 1.2 werden vollständig neu gebaut; neuere **generierte Demo-Arenen** erhalten Layout 4 als Reparatur: Wand-/Boden-/Deckenlöcher werden geschlossen, die Endertruhe ausgerichtet und Wandtasten ergänzt. Das Layout enthält einen **9×9-Vorraum** und eine geschlossene **100×100-Landschaftsarena** mit sanften Hügeln, Wegen, Ruinen und kleinen Bäumen/Kristallgebilden. Die drei Welten behalten ihre eigenen Themen. Der erste Start benötigt durch den Umbau länger. Alte versionierte Plugin-JARs sichert der Launcher außerhalb des Plugin-Suchpfads, um doppelte Plugin-Versionen zu vermeiden. Eigene Welten mit `generate-demo: false` werden nicht gebaut. Andere Konfigurationen, Loot-Postfach und Fortschritt bleiben erhalten.

## Neu in 1.7

- Sichtbare Cooldowns: native Icon-Anzeige auf Relikten, eigener Hotbar-/Inventarbalken auf Alltagswerkzeugen sowie Restzeit in der Lore. Pro Spieler und Item-ID gespeichert; normale Werkzeugnutzung bleibt verfügbar.
- Acht Survival-Fähigkeiten per **Schleichen + Rechtsklick** und 10 % zusätzlicher Verschleißschutz. Bergbau-/Laufboni, Bodenprobe, schonende Schere, schnelleres Anbeißen, Jägermarke und Unterwasserhilfe. Einzelne Blöcke, normale Drops und manuelles Spielen bleiben erhalten; eigene 16×16-Texturen und Beschaffungswege bleiben bestehen. Details: `EQUIPMENT.md`.
- Alle **elf Rituale** führen durch eine mehrphasige Beschwörung mit Runenringen, kreisenden Fragmenten, Lichtsäule, Sounds und Bossbar in einen Wächterkampf. Belohnungen erfolgen erst nach einem Sieg mit eigener Beteiligung. Unterschiedliche Wächter, angekündigte Bodenangriffe und eine stärkere zweite Kampfphase.
- Chaosfragment wird bis zum Abschluss gebunden. Während der Beschwörung sind Hotbar-Wechsel und Inventarverschiebungen gesperrt; im Kampf darf die Waffe gewechselt werden. Abbruch, Tod, Logout oder verlorener Wächter erstatten Fragment und Zutaten ins Postfach. Sieg verbraucht die Zutaten und gibt das Fragment zurück.
- Neue Survival-Rituale: Tauchersegen, Wanderersegen und Bergmannssegen. Raid-Bosse kündigen Fähigkeiten jetzt mit Bodenmarkierungen und zwei Sekunden Ausweichzeit an.

**Update:** Server stoppen, ZIP darüber entpacken und `local-test/start-server.bat` starten. `/raid pack reload` lädt den neuen Werkzeug-Cooldownbalken. `/raid kit gear`, `/raid kit ritual` und `/rezepte rituals` helfen beim Testen. Neue Konfiguration wird ergänzt; eigene Skill-Cooldowns, Ritualwerte und Raid-Fortschritte bleiben erhalten.

## Neu in 1.6

- **Boss-Schaden korrigiert:** Der persönliche Anteil zählt den tatsächlichen Lebensverlust nach einem Treffer. Treffer ohne Lebensverlust verbrauchen keinen Anteil. Kleine Float-Rundungsreste nach einem berechtigten letzten Treffer werden beendet. Die Anzeige nutzt Zehntelprozent und rundet nicht vorzeitig auf 100 %. Produktionslimits bleiben erhalten.
- **Spawns weiter entfernt:** mindestens 18 Blöcke horizontal von jedem Spieler. Eine größere Höhendifferenz zählt nicht als Abstand. Die Gegner verteilen sich weiterhin auf unterschiedliche Spawnflächen.
- **Animierte Obsidian-Golems:** sechs bewegliche Teile, Schritte nach tatsächlicher Bewegung, Kopfbewegung, Armschläge und Trefferreaktion. Alle drei Wächtervarianten behalten Obsidian als Hauptmaterial. Ein normaler Eisengolem übernimmt KI und Trefferfläche; die sichtbare Größe passt zu seiner Höhe. Keine versetzte, starre Mitfahrer-Figur. Boss-Vorschauen schauen die beschwörende Person an.
- **Acht Alltagswerkzeuge:** Seidenspitzhacke, Forstaxt, Pfadfinderschaufel, Feldhacke, Gartenschere, Anglerglück, Waldläuferarmbrust und Heimkehrdreizack. Eigene 16×16-Pixeltexturen und normale Minecraft-Verzauberungen. Angel und Armbrust haben eigene Modellzustände; der geworfene Dreizack verwendet die normale Minecraft-Entität. Keine automatische Baumfällung, Erzadern, Ernte oder Teleports. Einzelne Blöcke und normales Angeln bleiben die übliche Minecraft-Spielweise.
- **Erhalten:** 4 % Chance auf eines der acht Alltagswerkzeuge pro belohntem Miniboss ab Stufe 2; je Werkzeug 0,5 %. Das neue Ritual **Wandererschmiede** garantiert eines von acht zufälligen Werkzeugen gegen 32 Raidsplitter, 6 Diamanten und 8 Eisenbarren. Der Chaosfragment-Katalysator bleibt erhalten. `/raid kit gear` enthält nun alle 29 Ausrüstungsteile.
- **Vanilla-Werkstätten für Alltagswerkzeuge:** normale Amboss-Reparatur, Verzauberungsbücher und Umbenennen. Eigene Namen bleiben beim Sprachwechsel bestehen. Schleifstein und Netherit-Aufwertung machen daraus ein gewöhnliches Vanilla-Werkzeug; normale Enchantments beim Aufwerten bleiben erhalten. Alte Raid-Sets mit erhöhten Enchantments bleiben geschützt.
- **Klarere Oberfläche:** kurze Lore mit Leerzeilen, maximal 36 sichtbare Zeichen je Zeile, 28 Karten mit Abstand pro Rezeptseite. Item anklicken: Fundorte/Drops getrennt von Ritualwegen. Ritual anklicken: Zutaten als eigene Gegenstände und klare Schritte. Im Kesselmenü startet Linksklick das Ritual, Rechtsklick zeigt das Rezept.
- **Feste deutsche/englische Texte:** Gruppenmenü, Rezeptansichten, HUD, Portalstatus, Perlen-, Relikt-, Ritual- und Packmeldungen sowie häufige Admin-Rückmeldungen verwenden vollständige Vorlagen. IDs, Commands und Namen werden als Werte eingesetzt, statt Wort für Wort übersetzt. Persönliche Sprache bleibt unter `/raid language de|en` erhalten.

**Upgrade:** Server stoppen, ZIP darüber entpacken, `local-test/start-server.bat` starten. Ressourcenpaket annehmen oder `/raid pack reload`. Alte Wellen/Portalfortschritte und angepasste Boss-Lebenspunkte bleiben erhalten; Standard-Golemträger, neue Ausrüstung, kurze Lore und 18-Blöcke-Spawnabstand werden migriert.

## Neu in 1.5

- Demo-Steuerung: `/raid skip wave [portal]` entfernt die aktuelle Welle und beginnt die nächste. Bei Mini-/Endbossen wird die Stufe übersprungen. `/raid skip stage [portal]` kehrt zur nächsten Stufe im Vorraum zurück; Stufe 5 beendet den Durchlauf. Keine neue Skip-Beute und kein Boss-Abschluss-Flag. Admin-Rechte und Demo-Portal oder Solo-Testmodus erforderlich.
- 14 Gegnerarten, 10–30 Gegner pro normaler Welle plus spätere Reiter. Zufällige sichere Spawns über die Arena mit Spielerabstand und Abstand zu vorhandenen Gegnern. Keine Phantome, Vex oder Evoker. Jeder frisch gespawnte Raid-Gegner leuchtet 10 Sekunden.
- Neun Bosstypen mit unterschiedlichen Aussehen, Leben und Frost-/Tempo-/Markierungsfähigkeiten. Drei kubische Wächtermodelle verwenden Minecraft-Blocktexturen. `/raid boss list`, `/raid boss summon frost_guardian` und `/raid boss clear [all]` zeigen unbewegliche, unverwundbare Vorschauen. Vorschauen geben weder Beute noch Fortschritt und werden bei Logout/Serverstopp entfernt.
- Setboni zählen authentische Rüstung im richtigen Slot und unterschiedliche passende Waffen/Werkzeuge in Haupt-/Nebenhand. Gleiche Item-ID zählt einmal. Alle Welten sind erlaubt; Inventarvorräte zählen nicht. `/raid gear` zeigt die aktive Zählung. Items nennen ihre Beschaffung; `/rezepte gear` ergänzt genaue Drop-Chancen und mögliche Schmiederituale.
- `/raid language de|en` (auch `sprache` oder `lang`) speichert die persönliche Sprache. Chat, Menüs, HUD, Item-Namen/Lore und Portalstatus werden entsprechend angezeigt. Befehle und IDs bleiben gleich. Wandbeschriftungen sind zweisprachig.
- Der Katalog heißt **Rezepte**: `/rezepte [items|gear|sets|rituals]`, auch `/recipes` oder `/raid rezepte`. Der alte `codex`-Unterbefehl bleibt als Alias nutzbar.
- Enderperlen-Cooldown wird beim Wurf gesetzt, statt beim Landen zu blockieren. Standard jetzt 3 Sekunden. Bei Gegner-/Wandtreffern wird ein naher freier Landeplatz innerhalb der Arena gesucht; Ausbruch durch die Außenwand bleibt gesperrt.
- Portaltexte stehen fest über der Eintrittsfläche; genau eine Sprachversion wird pro Spieler angezeigt. `/raid portal delete <id|all>` entfernt Trigger, Anzeigen, Fortschritt und gespeicherten Portalstatus dauerhaft. Danach kann dieselbe ID an anderer Stelle neu erzeugt werden. Dekorative Rahmen sind gewöhnliche selbst gebaute Blöcke.
- Geschlossene Arena-Außenwand statt altem Durchgang, automatische Lochreparatur für generierte Arenen, nach innen gerichtete Endertruhe und echte klickbare Schilder/Tasten für Rezepte und Leader-Start im Vorraum.

## Neu in 1.4

- Drei komplette Rüstungssets (12 Teile): Wächter, Sturm und Abgrund. Zwei-/Vier-Teil-Boni gelten beim Tragen oder Halten in allen Welten. Getragene Rüstung benutzt normale Eisen-/Diamant-/Netherit-Modelle mit Gold-/Quarz-/Amethyst-Trims; eigene 16×16-Icons erscheinen im Inventar und in der Hand.
- Neun Waffen/Werkzeuge mit echter Haltbarkeit, hohen Verzauberungen und normalen Minecraft-Funktionen: unter anderem Schärfe VIII, Bogenstärke VII, Dichte VI, Effizienz VII und Glück IV. Raid-Damage-Caps gelten weiter. Looting erhöht nicht die konfigurierten Raid-Pool-Chancen.
- Gewichtete Beute-Gruppen mit genau einem Item bei erfolgreichem Wurf. Legendäre Rüstung hat 1 % Gruppen-Chance, legendäre Waffen 2 % beim belohnten Endboss. Konkrete Chancen stehen im Rezepte-Menü und in `EQUIPMENT.md`.
- Acht Kesselrituale einschließlich Wandererschmiede; zuvor: Erwachen, Wächterschmiede, Sturmschmiede, Echowerkstatt, Abgrundprüfung, Wiederherstellung und Segen. Zutaten werden geprüft und bezahlt, der Chaosfragment-Katalysator bleibt erhalten. Abbruch/Neustart einer nicht vollendeten Zeremonie erstattet Zutaten ins Postfach; Cooldowns bleiben bestehen.
- Abgrundprüfung beschwört einen stärkeren Wächter. Nach einem Spieler-Sieg bekommt die ausführende Person ein Werkzeug; 10 % Chance ersetzen es durch eine legendäre Waffe. Wächter verschwinden nach 10 Minuten ohne Sieg-Beute. Die bereits vollendete Beschwörung erstattet ihre Zutaten nicht.
- `/rezepte` ist ein lesbarer Item-/Set-/Ritual-Katalog. `/raid gear` zeigt deine getragenen Set-Teile. Die Test-Kits geben Ausrüstung und Ritualzutaten; Überlauf landet in `/raid loot`.
- Custom-Items sind für normale Werkbank-/Amboss-/Schmiedetisch-/Schleifstein-/Enchanting-Vorgänge gesperrt, damit Item-IDs und hohe Verzauberungen erhalten bleiben. Mending und das Ritual reparieren sie. Shop-API und direkte normale Werkzeugnutzung bleiben verfügbar.

## Neu in 1.3

- Alle sieben bisherigen Icons komplett vereinfacht und als 16×16-Pixeltexturen mit höchstens sechs deckenden Farben neu erstellt. Keine weichen Kanten, Verläufe oder äußeren Glows.
- Vier zusätzliche wiederverwendbare Relikte: Glutamulett, Wächterrune, Jägerlinse und Schwächungszahn. Zehn Relikte plus Raidsplitter-Währung haben eigene Texturen.
- Neue Gegner-Effekte betreffen nur aktive Raid-Gegner, verursachen keinen direkten Schaden und umgehen keine Boss-Anteile. Boss-Schwäche hält kürzer an.
- Bestehende 1.2-Arenen und angepasste Relikt-Einstellungen bleiben beim Upgrade erhalten; neue Items werden ergänzt. Minecraft bekommt durch den geänderten Pack-Hash die neuen Texturen.

## Neu in 1.2

- Ressourcenpaket im Plugin gebündelt, lokaler Download mit SHA-1-Cache, Ladezustand und Neu-Laden-Befehl. Der Pack-Server bindet nur an 127.0.0.1:8765 und beendet sich mit dem Minecraft-Server.
- Portal-Sperre als roter Ring mit Kreuz, eindeutiger Status und Zurückhalten an der Eintrittsfläche. Alte Plugin-Textdisplays werden bereinigt. Nach der Stage wird der Ring wieder offen.
- 100×100-Arena mit begehbaren Hügeln; die erlaubte Bauhöhe folgt dem Gelände. Kleiner separater Vorraum mit Werkbank, Amboss und Endertruhe.
- Drei zusätzliche Relikte: Frosttalisman, Lebensblüte und Windrune. Ritual-Katalysator und Ritualkern werden nicht mehr verbraucht. Alle Funktionen/Cooldowns und Drop-Stages stehen in `ITEMS.md`.

## Raid-Funktionen

- Private Nachricht beim Betreten: „Mach /raid, um den Leader zu wählen.“ Kein automatisch geöffnetes Gruppen-Menü.
- Vorraum räumlich getrennt: Ready teleportiert alle hinein, Stage-Abschluss teleportiert zurück. Keine begehbare Verbindung zwischen Vorraum und Kampf.
- Überdachter Dungeon und eigener Vorraum mit Quarz, violetten Fenstern, Sitzbank, Werkbank, Amboss und Endertruhe.
- **Boss-/Miniboss-Anteil = 100 % / erforderliche Mindestspielerzahl.** Woche 1: 50 % pro Spieler, Woche 2: 33⅓ %, Woche 3: 20 %. Größere Gruppen machen diese Obergrenze nicht kleiner. In der Solo-Demo sind es 100 %.
- Normale Treffer bleiben auf 20 %, Mace-Treffer auf 5 % des Mob-Maximallebens pro Treffer begrenzt.
- Wind Charges funktionieren mit **65 % Schub**, maximal 0,9 horizontal / 0,75 aufwärts (Blöcke pro Tick), über Config änderbar. Explosionsschaden und Blockzerstörung bleiben gesperrt.
- Spinnen dürfen an Spielerblöcken klettern, an ursprünglichen Arena-Wänden und Pfeilern wird das Hochklettern tickweise verhindert. Reiter bleiben dabei erhalten. Keine NMS-Abhängigkeit.
- Animierte Portalringe, wandernde Partikel, farbiger Status und lesbares Textdisplay mit Restzeit bis auf Sekunden.
- Raid-Bossbar und Actionbar mit Stage, Wave, Gegnern, Leader, Spielerzahl, Reward-Zeit und deinem Boss-Anteil.
- 34 eigene transparente 16×16-Item-Texturen, neue Lore und nutzbare Relikte. Siehe `ITEMS.md`.
- Kesselritual in drei Phasen mit schwebendem Kern, rotierenden Runen, Energie-Spirale, Sounds und finalem Ausbruch. Der Katalysator bleibt erhalten, auch bei Abbruch. Die 120-Sekunden-Abklingzeit läuft weiter. Verbrauch aus älteren angefangenen Ritualen wird beim Neustart erstattet.
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
| `/raid pack status\|reload` | Pack-Ladezustand / erneut anfordern — `mobarmy.play` |
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
| `/raid cooldown clear [spieler\|all]` | Wipe-Sperre und Ausscheiden zurücksetzen; in Solo-Demo auch Item-/Ritual-Cooldowns; ohne Spieler: du selbst — `mobarmy.admin` |
| `/raid test on\|off` | Solo-/Wipe-Regeln während der laufenden Demo ändern — `mobarmy.admin` |
| `/raid kit basic\|gear\|ritual` | Basis-Kit, sämtliche neue Ausrüstung oder Ritual-Testzutaten — `mobarmy.admin` |
| `/rezepte [items\|gear\|sets\|rituals]` | Item-/Set-/Rezept-Katalog — `mobarmy.play` |
| `/raid language de\|en` | Persönliche Sprache dauerhaft wechseln |
| `/raid skip wave\|stage [portal]` | Demo-Welle/-Stufe überspringen — `mobarmy.admin` |
| `/raid boss list\|summon <id>\|clear [all]` | Demo-Boss-Vorschau — `mobarmy.admin` |
| `/raid portal delete <id\|all>` | Portale dauerhaft entfernen und ID wieder freigeben — `mobarmy.admin` |
| `/raid gear` | Getragene/gehaltene Set-Teile und Hinweis zu Boni — `mobarmy.play` |
| `/raid rituals`, `/raid ritual <id>` | Kessel anschauen: Menü oder Ritual ausführen — `mobarmy.play` |
| `/raid ritual cancel [all]` | Eigene Rituale/Wächter beenden; `all` benötigt Admin-Rechte |
| `/raid reload` | Config laden, zuvor Gruppen/Rituale/Wächter beenden — `mobarmy.admin` |
| `/raid build`, `/raid visit <week>` | Expliziter Bau-Modus; nur ohne aktive Gruppen — `mobarmy.build` |
| `/raid item give\|take <id> [spieler] [anzahl]` | Echte Custom-Items geben/entfernen — `mobarmy.item` |
| `/raid item count <id> [spieler]` | Echte Custom-Items zählen — `mobarmy.item` |

Dauer: `90s`, `30m`, `2h`; nur eine Zahl bedeutet Minuten. OP hat Admin-Rechte. Normale Spieler haben `mobarmy.play`. Ohne Portal-ID wird das Portal deiner Gruppe oder das einzige vorhandene Portal verwendet.

Für weitere PCs/LAN oder einen öffentlichen Server `testing.local-resource-pack: false` setzen und die Pack-ZIP öffentlich per HTTP(S) bereitstellen; `resource-pack.url` und optional `resource-pack.sha1` konfigurieren. Der localhost-Link ist nur für Minecraft auf demselben PC gedacht. Wenn Port 8765 belegt ist, `resource-pack.port` ändern und den Server neu starten.

Die Portalregion ist ein Quader mit Partikeln und Betretens-Erkennung. Dekorative Formen/Rahmen kannst du darum bauen. Ein neuer Event-Durchlauf benötigt einen neuen Portalnamen; ein verlängerter Timer hebt einen normalen abgeschlossenen Boss nicht auf. Demo-Reset ist die ausdrückliche Test-Ausnahme.

## Konfiguration und Erweiterung

`plugins/DurcheinanderMobArmy/config.yml` enthält `weeks`, `items`, `balance`, `presentation`, `ritual-animation`, `messages` und `testing`. `EQUIPMENT.md` enthält Set-/Enchantment-/Drop-Tabellen, `RITUALS.md` die Rezepte und Test-Schritte. `state.yml` speichert Fortschritt, Deadline, Boss-Status, Cooldowns, Rückkehrpositionen, Loot und vorübergehende Block-/Ritual-Recovery.

Neue Woche: vorhandenen `weeks.<id>`-Eintrag kopieren, eigene Welt und fünf Stages eintragen. Eine eigene Arena mit `generate-demo: false` bauen/importieren. `arena` und `lobby` sind unabhängige, inklusive Blockregionen; `min.y` steht direkt über dem Boden. Arena höchstens 100×100. Spawnpunkte benötigen einen soliden Boden und zwei freie Blöcke; bevorzugt wird der am weitesten entfernte Punkt mit Mindestabstand.

Neue Items: Material, `name`, `lore`, `item-model`, Drop-Quellen und optional `ritual` oder `use` hinzufügen. Drop-Quellen: `mob`, `miniboss`, `boss`, optional Stage/Woche, Menge und Chance. Relikt-`use`: Verbrauch, Radius auf aktuelle Gruppenmitglieder, Cooldown, Nachricht und Liste von Potion-Effekten mit Sekunden/Amplifier. Items werden über PDC-ID erkannt, Namen lassen sich daher nicht als Ersatz fälschen.

Vor `/raid reload` sämtliche Gruppen und laufenden Rituale beenden. Für globale Bukkit-Reloads ist der normale Server-Neustart vorzuziehen. Regelmäßige Server-Backups sind sinnvoll, da Minecraft-Spielerdaten und Plugin-YAML getrennt gespeichert werden.

## Bauen und prüfen

Mit Java 25 und Maven 3.9+:

```sh
python3 tools/build_pack.py
mvn clean verify
python3 tools/build_release.py
```

JAR: `target/DurcheinanderMobArmy-1.8.0.jar`. Resource Pack: `target/MobArmy-resource-pack-26.3.zip`. Das Pack verwendet Format 97.1. GitHub Actions baut JAR, Pack und ZIP und prüft die Windows-Setup-Skripte.

Automatische Tests prüfen Timer/Mitternacht/Boss-Abschluss, Schaden und Mindestspieler-Anteile, Waves/Reiter, komplette Tab-Grammatik, Spinnen-Regelausnahme, GUI-freien Eintritt und den Gruppen-Teleport. Sichtbarkeit, native Wind-Charge-Physik, Spinnen-Bewegung und Multiplayer-Balance bitte mit `TESTING.md` im Spiel prüfen.

Versionsquellen: [Spigot 26.3](https://www.spigotmc.org/threads/spigot-bungeecord-26-3.724398/), [Spigot API](https://hub.spigotmc.org/javadocs/spigot/), [Minecraft 26.3](https://feedback.minecraft.net/hc/en-us/articles/48913133328013-Minecraft-Java-Edition-26-3).
