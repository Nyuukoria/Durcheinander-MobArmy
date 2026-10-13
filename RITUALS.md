# Kesselrituale

Ein wiederverwendbares **Chaosfragment** in die Haupthand nehmen und einen Kessel in der konfigurierten Default-Welt rechtsklicken. Das Menü zeigt Zutaten, Ergebnis und Cooldown. Alternativ den Kessel innerhalb sechs Blöcken anschauen und `/raid ritual <id>` ausführen. `/rezepte rituals` zeigt Rezepte, ohne etwas zu starten.

Während der acht Sekunden langen Zeremonie am Kessel bleiben und den Katalysator halten. Zwei gegenläufige Runenringe, ein schwebendes Ergebnis-Icon, eine Energiespirale, vier Phasen und Sounds zeigen den Fortschritt. Jedes Ritual hat seine eigene Farbe. Der Katalysator wird niemals verbraucht. Angegebene Zutaten werden bezahlt; umbenannte Vanilla-Splitter ersetzen keine echten Raidsplitter.

| ID / Ritual | Verbrauchte Zutaten | Ergebnis | Cooldown |
|---|---|---|---:|
| `awakening` · Erwachen | keine | Fragmentwächter: 80 HP, 6 Angriff; Spieler-Sieg gibt einen Ritualkern | 120s |
| `warden_forge` · Wächterschmiede | 24 Raidsplitter, 8 Eisenbarren, 4 Lapislazuli | ein zufälliges Wächter-Rüstungsteil | 180s |
| `gale_forge` · Sturmschmiede | 48 Raidsplitter, 2 Sturmessenzen, 6 Diamanten | ein zufälliges Sturm-Rüstungsteil | 240s |
| `echo_workshop` · Echowerkstatt | 32 Raidsplitter, 1 Sturmessenz, 4 Diamanten | eine Echospitzhacke, Runenschaufel oder Erntehacke | 180s |
| `abyss_trial` · Prüfung des Abgrunds | 32 Raidsplitter, 1 Leerenessenz | Abgrundwächter: 180 HP, 9 Angriff; bei Spieler-Sieg 90 % ein Werkzeug, 10 % stattdessen eine legendäre Waffe | 300s |
| `restoration` · Wiederherstellung | 8 Raidsplitter, 2 Goldbarren | vollständige Reparatur von beschädigter Custom-Rüstung und Custom-Ausrüstung in Hotbar/Nebenhand | 60s |
| `blessing` · Segen der Vorhut | 4 Raidsplitter, 2 Smaragde | Resistenz I und Tempo I, 30s, auf Personen im Umkreis von sechs Blöcken am Kessel | 180s |

Schmiede- und Reparatur-Ergebnisse kommen direkt ins Inventar oder ins `/raid loot` Postfach. Prüfung-Loot gehört der ausführenden Person, auch wenn andere beim Kampf helfen. Ein Spieler muss den Wächter besiegen; reine Umwelttode geben keine Belohnung. Der Wächter sucht einen freien Spawn neben dem Kessel und verschwindet nach zehn Minuten ohne Loot. Ein vollendetes Beschwörungsritual erstattet beim späteren Entfernen/Verlieren des Wächters keine Zutaten.

Fehlende Zutaten verhindern den Start vollständig. Kessel entfernt, ausgeloggt, zu weit weg oder Katalysator weggelegt: die unfertige Zeremonie bricht ab und bezahlte Zutaten landen im Postfach. Auch angefangene Zeremonien aus dem vorherigen Serverprozess werden einmal aus dem Journal ins Postfach übernommen. Der Cooldown läuft bei Abbruch weiter. Plugin-YAML und Minecraft-Spielerdaten werden getrennt gespeichert; eine harte Prozess-/Datenträgerunterbrechung ist keine gemeinsame Datenbanktransaktion.

`/raid ritual cancel` beendet eigene laufende Zeremonien und eigene Wächter. `/raid ritual cancel all` braucht Admin-Rechte. Entfernte Wächter geben keine Sieg-Beute. Eine Person und ein Kessel können jeweils nur eine Zeremonie gleichzeitig ausführen; insgesamt gilt `maximum-ritual-mobs` für Zeremonien/Wächter.

## Lokal testen

1. `/raid kit ritual`: Katalysator, echte Raidsplitter, Essenzen, Vanilla-Zutaten und Kessel bekommen. Überlauf mit `/raid loot` abholen.
2. Kessel außerhalb der Dungeon-Welten platzieren. Chaosfragment halten, rechtsklicken, `warden_forge` wählen.
3. Ergebnis nach acht Sekunden prüfen; Anzahl der bezahlten Materialien und erhaltenen Katalysatoren kontrollieren.
4. `/raid kit gear`, beschädigte Ausrüstung in Hotbar/Rüstung/Nebenhand legen, dann `restoration` testen.
5. `awakening` beziehungsweise `abyss_trial` starten und den Wächter besiegen.
6. Für weitere Versuche: `/raid cooldown clear`. **Nur im Solo-Testmodus** löscht dieser Befehl auch Item-/Ritual-Cooldowns. Produktionsregeln bleiben bestehen. Aktive Zeremonien vorher abbrechen.

## Wandererschmiede und übersichtliche Rezepte (1.6)

`wanderer_forge`: 32 echte Raidsplitter, 6 Diamanten und 8 Eisenbarren. Chaosfragment in der Haupthand bleibt erhalten. Acht Sekunden Zeremonie, 60 Sekunden Abklingzeit. Ein zufälliges Alltagswerkzeug ist bei erfolgreichem Abschluss garantiert; alle acht Werkzeuge sind gleich wahrscheinlich.

`/rezepte rituals`: kurze Ritual-Karten. Anklicken zeigt Ergebnis, Ablauf und Zutaten als eigene Karten. Im Kesselmenü: Linksklick startet, Rechtsklick öffnet die Beschreibung. Item-Karten enthalten keine langen Zutaten-/Drop-Listen mehr; sie stehen in der jeweiligen Detailansicht. Die angezeigte Sprache folgt `/raid language de|en`.
