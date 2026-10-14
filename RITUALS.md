# Kesselrituale 1.7

Chaosfragment in der Haupthand halten und einen Kessel in der konfigurierten Ritualwelt rechtsklicken. Alternativ den Kessel innerhalb sechs Blöcken anschauen und `/raid ritual <id>` ausführen. `/rezepte rituals` zeigt die Zutaten und den Ablauf. Alle elf Rituale enthalten eine verpflichtende Wächterprüfung.

Beim Start werden ein Chaosfragment und die Zutaten bis zum Ausgang des Rituals aufbewahrt. Während der 8–10 Sekunden langen Beschwörung am Kessel bleiben; Hotbarwechsel, Tauschen, Wegwerfen und Inventarverschiebungen sind gesperrt. Runenringe, drei kreisende Fragmente, Energieeffekte, eine Lichtsäule, vier Phasen, Sounds und eine Bossbar zeigen den Ablauf. Danach darfst du zur Waffe wechseln. Das Fragment bleibt bis zum Ende gebunden.

Der Wächter erscheint 6–9 Blöcke vom Kessel und mindestens 5 Blöcke von Spielern entfernt. Der Platz muss frei sein. Nach einer kurzen Aufwachphase greift er an. Gelb/rote Bodenringe kündigen Spezialangriffe 1,5 Sekunden vorher an. Verlasse den Kreis; Wände blockieren den Treffer. Bei halbem Leben wird der Wächter schneller und benutzt Fähigkeiten häufiger. Frost verlangsamt, Glut markiert und Sturm stößt leicht zurück. Es werden keine Blöcke zerstört oder Feuer gelegt. Die Spezialangriffe und KI richten sich an die ausführende Person, nicht an unbeteiligte Spieler.

Die Person muss am Kampf teilnehmen. Freunde dürfen helfen; ein fremder letzter Treffer stiehlt keine Belohnung. Umwelt-/Nichtspieler-Schaden kann den Wächter nicht ersetzen. Im Kampf höchstens 20 Blöcke vom Kessel entfernen. Tod, Logout, entfernter Kessel, verlorener Wächter, Abbruch oder das Drei-Minuten-Limit beenden den Versuch. Fragment und Zutaten landen dann einmal in `/raid loot`; der Ritualcooldown läuft weiter. Auch nach einem Neustart werden offene Zutaten samt Fragment aus dem Journal zurückgegeben. Minecraft-Spielerdaten und Plugin-YAML bleiben getrennte Speicher; harte Prozessabbrüche sind keine gemeinsame Datenbanktransaktion.

Nach dem Sieg werden die Zutaten verbraucht, das Chaosfragment zurückgegeben und die Belohnung ausgeführt. Volle Inventare nutzen `/raid loot`.

| ID | Zutaten | Belohnung nach Sieg | Cooldown |
|---|---|---|---:|
| `awakening` | keine | Ritualkern | 120 s |
| `warden_forge` | 24 Raidsplitter, 8 Eisenbarren, 4 Lapislazuli | zufällige Wächterausrüstung; Obsidian-Golem als Gegner | 180 s |
| `gale_forge` | 48 Raidsplitter, 2 Sturmessenzen, 6 Diamanten | zufällige Sturmausrüstung | 240 s |
| `echo_workshop` | 32 Raidsplitter, 1 Sturmessenz, 4 Diamanten | zufälliges Bergbauwerkzeug | 180 s |
| `abyss_trial` | 32 Raidsplitter, 1 Leerenessenz | Werkzeug; 10 % Chance auf legendäre Waffe | 300 s |
| `restoration` | 8 Raidsplitter, 2 Goldbarren | Reparatur getragener Custom-Ausrüstung und Hotbar/Nebenhand | 60 s |
| `blessing` | 4 Raidsplitter, 2 Smaragde | Resistenz I und Tempo I für 30 s | 180 s |
| `wanderer_forge` | 32 Raidsplitter, 6 Diamanten, 8 Eisenbarren | eines der acht Alltagswerkzeuge, gleich wahrscheinlich | 60 s |
| `diver_blessing` | 12 Raidsplitter, 4 Lapislazuli | Wasseratmung und Nachtsicht für 3 Minuten | 180 s |
| `traveler_blessing` | 12 Raidsplitter, 4 Lapislazuli | Tempo I für 90 s, langsamer Fall für 20 s | 180 s |
| `miner_blessing` | 16 Raidsplitter, 4 Lapislazuli | Eile I für 90 s, Nachtsicht für 3 Minuten | 180 s |

Segnungen gelten immer für die ausführende Person sowie für nahe Spieler im Sechs-Blöcke-Radius am Kessel. Katalysator und umbenannte Vanilla-Zutaten können keine echten Custom-Zutaten ersetzen.

Zum Testen: `/raid kit ritual`, Kessel aufstellen, Chaosfragment auswählen. `/raid ritual cancel` beendet eigene Rituale; `cancel all` benötigt Admin-Rechte. Im Solo-Demomodus leert `/raid cooldown clear` auch Item-/Ritualcooldowns. Eine Person und ein Kessel können jeweils nur ein Ritual durchführen. `maximum-ritual-mobs` begrenzt die Zahl gleichzeitig laufender Rituale.
