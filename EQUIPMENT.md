# Ausrüstung, Setboni und Seltenheiten

Alle 21 Ausrüstungsteile funktionieren als normale Minecraft-Rüstung/Waffen/Werkzeuge mit Haltbarkeit. Sie werden nicht beim Benutzen verbraucht. Verzauberungen oberhalb der üblichen Grenze sind echte Enchantments. Mending repariert mit normalen XP; Raid-Gegner selbst geben weiterhin keine XP. Das Wiederherstellungsritual repariert getragene Custom-Rüstung sowie Custom-Werkzeuge in Hotbar/Nebenhand.

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

- `/raid kit gear`: OP-Testkit mit allen 21 Teilen; selbst ausrüsten. Überlauf landet in `/raid loot`.
- `/raid gear`: getragene Set-Teile prüfen.
- `/rezepte gear` und `/rezepte sets`: Werte, Boni und Drop-Chancen ansehen.
- Normal spielen: nur bei aktiven Rewards droppen Raid-Pools. Schmiederituale bieten gezielte Fortschrittswege gegen Zutaten.

Werkbank, Amboss, Schleifstein, Enchanting und Schmiedetisch akzeptieren die Custom-Items nicht als normale Zutaten. Das verhindert, dass hohe Enchantments, Set-IDs oder Modelle bei Vanilla-Kombinationen verloren gehen. Repariere mit Mending oder Ritual. Weitergeben, Lagern und die vorhandene Shop-API bleiben möglich.
