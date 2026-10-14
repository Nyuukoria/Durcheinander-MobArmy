# Ausrüstung, Setboni und Seltenheiten

Die ursprünglichen 21 Raid-Ausrüstungsteile funktionieren als normale Minecraft-Rüstung/Waffen/Werkzeuge mit Haltbarkeit. Sie werden nicht beim Benutzen verbraucht. Verzauberungen oberhalb der üblichen Grenze sind echte Enchantments. Mending repariert mit normalen XP; Raid-Gegner selbst geben weiterhin keine XP. Das Wiederherstellungsritual repariert getragene Custom-Rüstung sowie Custom-Werkzeuge in Hotbar/Nebenhand.

## Drei vollständige Sets

Jedes Set besteht aus Helm, Brustplatte, Beinschutz und Stiefeln. Zwei-/Vier-Teil-Boni gelten in allen Welten, solange die passenden Teile getragen oder in Haupt-/Nebenhand gehalten werden. Rüstung zählt nur im vorgesehenen Rüstungsslot; unterschiedliche Waffen/Werkzeuge zählen als weitere Teile. Zwei Kopien derselben Item-ID zählen einmal; Inventarvorräte zählen nicht. Gemischte Sets zählen separat. Nach Ablegen oder Wechseln klingen Bonus-Effekte innerhalb von höchstens drei Sekunden ab; stärkere andere Potion-Effekte werden nicht gezielt entfernt.

| Set / IDs | Material und Trim | Verzauberungen | Zwei Teile | Vier Teile |
|---|---|---|---|---|
| Wächter · `warden_helmet/chestplate/leggings/boots` | Eisen, Gold/Sentry | Schutz IV, Haltbarkeit IV, Mending I | Resistenz I | zusätzlich Regeneration I |
| Sturm · `gale_helmet/chestplate/leggings/boots` | Diamant, Quarz/Spire | Schutz V, Haltbarkeit IV, Mending I | Tempo I | zusätzlich langsamer Fall |
| Abgrund · `abyss_helmet/chestplate/leggings/boots` | Netherit, Amethyst/Silence | Schutz VI, Haltbarkeit V, Mending I | Feuerresistenz | zusätzlich Resistenz I und Stärke I |

Alle Helme: Atmung III. Wächter-/Sturmstiefel: Federfall IV; Abgrundstiefel: Federfall VI. Die Inventar-Icons sind eigene 16×16-Pixelgrafiken. Am Körper erscheinen Minecrafts Material-Rüstungsmodelle mit unterschiedlichen Vanilla-Trims; es ist kein eigenes 3D-Rüstungsmodell enthalten.

## Waffen und Werkzeuge

| Item-ID | Ausrüstung | Verzauberungen |
|---|---|---|
| `warden_blade` | Wächterklinge, Eisen | Schärfe VI, Haltbarkeit IV, Mending I |
| `gale_axe` | Sturmaxt, Diamant | Schärfe VI, Effizienz VI, Haltbarkeit IV, Mending I |
| `tempest_bow` | Sturmbogen | Stärke V, Schlag II, Haltbarkeit IV, Mending I; benötigt Pfeile |
| `abyss_sword` | Abgrundklinge, Netherit | Schärfe VIII, Plünderung IV, Haltbarkeit V, Mending I |
| `abyss_mace` | Abgrundstreitkolben | Dichte VI, Windstoß III, Haltbarkeit V, Mending I |
| `star_bow` | Sternenbogen | Stärke VII, Schlag III, Haltbarkeit V, Unendlichkeit I; ein normaler Pfeil benötigt |
| `echo_pickaxe` | Echospitzhacke, Diamant | Effizienz VII, Glück IV, Haltbarkeit IV, Mending I |
| `runic_shovel` | Runenschaufel, Eisen | Effizienz VI, Haltbarkeit IV, Mending I |
| `harvest_hoe` | Erntehacke, Diamant | Effizienz VI, Glück IV, Haltbarkeit IV, Mending I |

Die Namen der Bogen-Verzauberungen „Stärke“ und „Schlag“ entsprechen Power/Punch. Keine Waffe umgeht den 20-%-Treffer-Cap, den 5-%-Mace-Cap oder den persönlichen Boss-Anteil. Glück wirkt nach den normalen Block-Loot-Regeln; Plünderung verändert die festen Plugin-Drop-Chancen nicht. Werkzeuge dürfen keine geschützten Arena-Blöcke abbauen.

Wächterteile: Wächterklinge und Runenschaufel. Sturmteile: Sturmaxt, Sturmbogen, Echospitzhacke und Erntehacke. Abgrundteile: Abgrundklinge, Abgrundstreitkolben und Sternenbogen. Beispiel: drei Sturm-Rüstungsteile plus Sturmaxt in der Hand aktivieren den Vier-Teil-Bonus.

## Genaue Drop-Chancen

Ein Pool würfelt einmal pro berechtigtem Gegner-Tod. Bei Erfolg kommt genau **ein** gleich gewichtetes Item aus diesem Pool. Verschiedene Pools würfeln unabhängig. Alle bestehenden Reward-Timer-/Boss-Abschluss-Regeln gelten weiterhin.

| Pool | Quelle | Chance auf irgendein Item | Anzahl Auswahlmöglichkeiten | Chance auf ein bestimmtes Item |
|---|---|---:|---:|---:|
| Wächterrüstung | Miniboss, Stage 1–4 | 10 % | 4 | 2,5 % |
| Sturmrüstung | Miniboss, Stage 3–4 | 5 % | 4 | 1,25 % |
| Abgrundrüstung | Endboss | 1 % | 4 | 0,25 % |
| Seltene Waffen | Miniboss, Stage 2–4 | 8 % | 3 | etwa 2,667 % |
| Legendäre Waffen | Endboss | 2 % | 3 | etwa 0,667 % |
| Werkzeuge | Miniboss, Stage 3–4 | 6 % | 3 | 2 % |

Sturmessenz: Stage-3-Miniboss, 20 %. Leerenessenz: Endboss, 25 %. Beide sind Ritual-Zutaten. Die Gruppenchancen sind keine Garantie nach einer bestimmten Anzahl Durchläufe; es gibt keinen versteckten Pity-Zähler. Der Rezepte-Menü zeigt die konkrete Item-Chance aus den aktuellen Config-Werten.

## Ausprobieren und Erhalten

- `/raid kit gear`: OP-Testkit mit allen 29 Teilen; selbst ausrüsten. Überlauf landet in `/raid loot`.
- `/raid gear`: getragene Set-Teile prüfen.
- `/rezepte gear` und `/rezepte sets`: Werte, Boni und Drop-Chancen ansehen.
- Normal spielen: nur bei aktiven Rewards droppen Raid-Pools. Schmiederituale bieten gezielte Fortschrittswege gegen Zutaten.

Werkbank, Amboss, Schleifstein, Enchanting und Schmiedetisch akzeptieren die Custom-Items nicht als normale Zutaten. Das verhindert, dass hohe Enchantments, Set-IDs oder Modelle bei Vanilla-Kombinationen verloren gehen. Repariere mit Mending oder Ritual. Weitergeben, Lagern und die vorhandene Shop-API bleiben möglich.

## Acht Alltagswerkzeuge ab 1.6

Alle acht besitzen eigene 16×16-Pixeltexturen. Angel- und Armbrustzustände bleiben über eigene Modellzuordnungen erhalten.

Diese Werkzeuge besitzen eigene Pixeltexturen und verwenden normale Minecraft-Mechaniken und Verzauberungsgrenzen. Die neuen Fähigkeiten werden mit Schleichen + Rechtsklick aktiviert. Normales Holzfällen und Abbauen bleiben einzeln, Angeln und Schießen bleiben manuell.

| ID | Funktion | Verzauberungen |
|---|---|---|
| `silk_pickaxe` | Diamantspitzhacke, Blöcke vollständig erhalten | Effizienz V, Behutsamkeit I, Haltbarkeit III, Reparatur I |
| `forester_axe` | Diamantaxt, gewöhnliches Holzfällen | Effizienz V, Haltbarkeit III, Reparatur I |
| `pathfinder_shovel` | Diamantschaufel, Erde/Sand/Graswege | Effizienz V, Behutsamkeit I, Haltbarkeit III, Reparatur I |
| `cultivator_hoe` | Diamanthacke, Feldarbeit/Ernte | Effizienz V, Glück III, Haltbarkeit III, Reparatur I |
| `harvest_shears` | Schere, Blätter/Wolle/Ranken | Effizienz V, Haltbarkeit III, Reparatur I |
| `angler_rod` | Normales Angeln | Glück des Meeres III, Köder III, Haltbarkeit III, Reparatur I |
| `ranger_crossbow` | Armbrust, normale Pfeile/Feuerwerkskörper | Schnellladen III, Mehrfachschuss I, Haltbarkeit III, Reparatur I |
| `returning_trident` | Dreizack mit Rückkehr nach dem Wurf | Treue III, Harpune V, Haltbarkeit III, Reparatur I |

Die Raid-Sicherheitsregeln gelten weiter, zum Beispiel keine Feuerwerkskörper in der Arena. Diese acht Werkzeuge gehören keinem Rüstungsset an. Normale XP-Reparatur und Amboss funktionieren. Am Schleifstein bzw. beim Netherit-Upgrade wird das Item zu gewöhnlicher Vanilla-Ausrüstung; die spezielle Item-ID und Lore entfallen, damit spätere Inventar-Aktualisierungen entfernte Enchantments nicht wiederherstellen. Beim Upgrade bleiben normale Enchantments erhalten.

Miniboss ab Stufe 2: 4 % Pool-Chance, genau ein Werkzeug; 0,5 % je einzelner ID. Wandererschmiede: garantiert ein zufälliges Werkzeug, 12,5 % je ID. Zutaten: 32 Raidsplitter, 6 Diamanten, 8 Eisenbarren. Details im Rezeptmenü; `/raid kit gear` zum Testen.

## Survival-Fähigkeiten und Cooldowns (1.7)

Alle acht Alltagswerkzeuge sparen zusätzlich mit 10 % Wahrscheinlichkeit einen Haltbarkeitsverlust. Keine Extra-Drops, keine automatische Weltbearbeitung und keine Veränderung gewöhnlicher Vanilla-Items. Alle Fähigkeiten sind wiederverwendbar.

| Werkzeug | Schleichen + Rechtsklick | Cooldown |
|---|---|---:|
| Seidenspitzhacke | Eile II für 8 s, Nachtsicht für 20 s | 75 s |
| Forstaxt | Eile II und Tempo I für 10 s | 60 s |
| Pfadfinderschaufel | Tempo I für 12 s, langsamer Fall für 4 s | 60 s |
| Feldhacke | Bodenfeuchte und Pflanzenwachstum ablesen; Eile I für 8 s | 15 s |
| Gartenschere | Verschleißschutz steigt für 8 s auf 50 % | 45 s |
| Anglerglück | Nächster manueller Wurf innerhalb 15 s: Basiswartezeit vor Köder-Verzauberung −20 % | 60 s |
| Waldläuferarmbrust | Nächster normaler Pfeil innerhalb 15 s markiert ein Monster für 6 s; keine Markierung von Spielern | 45 s |
| Heimkehrdreizack | Wasseratmung für 15 s, Delfins Gunst für 4 s | 60 s |

Relikte zeigen den nativen Minecraft-Cooldown direkt auf ihrem Icon. Die acht Werkzeuge zeigen mit dem Ressourcenpaket einen eigenen Balken in Hotbar und Inventar; die verbleibende Zeit steht zusätzlich in der Lore. Die Spezialfähigkeit bleibt gesperrt, aber normale Nutzung läuft weiter. Während Aufladen oder längerem Abbauen pausiert nur die Bild-/Lore-Aktualisierung des gehaltenen Werkzeugs und holt danach auf; der Timer läuft weiter. Doppelte Exemplare teilen pro Spieler und Item-ID denselben Timer. Relog und Neustart setzen ihn nicht zurück; Demo-Clear entfernt auch die Anzeige. Schleifstein/Netherit-Upgrade entfernen die speziellen Fähigkeiten zusammen mit der Custom-ID.

## Waffenaktionen und stärkere Skills ab 1.8

Aktivierung: **Schleichen + Rechtsklick**. Jede Item-ID hat einen eigenen spielergebundenen Cooldown; ein zweites Exemplar umgeht ihn nicht. Die neun bisherigen Raid-Waffen/Werkzeuge bekommen jetzt ebenfalls einen GUI-Balken und eine kurze Restzeit in der Lore. Native Nutzung bleibt während der Abklingzeit möglich. Zusätzlicher Angriffsschaden läuft durch normale Schadenereignisse und die bestehenden Boss-Anteil-/Treffergrenzen. Effekte betreffen feindliche Mobs und zugehörige Raid-/Ritualgegner; Spieler, Haustiere und Dorfbewohner sind ausgeschlossen.

| Item | Aktivierte Fähigkeit | Cooldown |
|---|---|---|
| Wächterklinge `warden_blade` | 6 s Resistenz I; direkte Monsterangriffe lösen Zurückstoß und 3 s Schwäche I aus | 45 s |
| Sturmaxt `gale_axe` | Nächster vollständig aufgeladener Treffer: +20 % Grundschaden; bis zu 3 weitere sichtbare Feinde erleiden 2 Schadenspunkte und Zurückstoß | 45 s |
| Abgrundklinge `abyss_sword` | Nächster aufgeladener Treffer: +20 % Grundschaden, 4 s Schwäche I und 5 s Leuchten | 50 s |
| Abgrundstreitkolben `abyss_mace` | Nächster aufgeladener Treffer: +20 % Grundschaden, Stoßwelle gegen bis zu 5 weitere sichtbare Feinde in 4 Blöcken Entfernung | 60 s |
| Sturmbogen `tempest_bow` | Nächster normaler Pfeil: zusätzlicher Zurückstoß und 3 s Verlangsamung I | 45 s |
| Sternenbogen `star_bow` | Nächster Pfeil: 6 s Leuchten, 4 s Schwäche I; bis zu 2 weitere sichtbare Feinde in 3 Blöcken erhalten 2 Schadenspunkte | 60 s |
| Echospitzhacke `echo_pickaxe` | Eile II 10 s und Nachtsicht 30 s | 60 s |
| Runenschaufel `runic_shovel` | Tempo II 6 s, zusätzlich ein Falltreffer in den nächsten 12 s um 50 % reduziert | 60 s |
| Erntehacke `harvest_hoe` | Innerhalb 30 s bis zu 6 manuell geerntete reife Feldpflanzen nachsäen | 45 s |

Die sechs vorbereiteten Angriffs-/Pfeilaktionen verfallen nach 15 Sekunden, wenn sie nicht verbraucht wurden. Die Wächterhaltung läuft sechs Sekunden.

Die acht Alltagswerkzeuge behalten ihre normale Haltbarkeit, Fundorte und Vanilla-Verzauberungen. Verbesserungen: Forstaxt nun Eile II/Tempo I 10 s; Pfadfinderschaufel zusätzlich ein halber Falltreffer innerhalb 15 s (Tempo I 12 s, langsamer Fall 4 s bleiben); Feldhacke kann nach einer Bodenprobe drei manuell geerntete Pflanzen innerhalb 15 s nachsäen; Gartenschere 50 % Verschleißschutz statt 35 % im Skillfenster und Eile II 8 s; Angel reduziert die Basiswartezeit eines Wurfs um 35 %; Waldläuferarmbrust markiert und schwächt ein Monster; Rückkehrdreizack gibt Wasseratmung 30 s, Delfingunst 8 s und einen kurzen Schub nur im Wasser. Seidenspitzhacke behält Eile II 8 s und Nachtsicht 20 s.

Nachsaat unterstützt Weizen, Rote Bete, Karotten und Kartoffeln. Du erntest jede Pflanze selbst, brauchst passende normale Samen/Karotten/Kartoffeln im Inventar und bleibst in sechs Blöcken Nähe. Es wird genau ein Saatitem verbraucht und auf Alter 0 gepflanzt; normale Schutzplugins können das Platzieren verhindern. Keine zusätzliche Ernte, Vervielfältigung oder kostenlose Saat.

Die bisherigen Drop-Pools und Ritualwege bleiben erhalten; `/rezepte` zeigt sie pro Item. Die OP-Demo stellt alles über `/raid kit gear` bereit.
