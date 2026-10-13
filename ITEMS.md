# Items, Funktionen und spätere NPC-Shops

| Item-ID | Funktion |
|---|---|
| `chaos_fragment` | Rechtsklick auf einen Kessel in der konfigurierten Default-Welt: animiertes 6-Sekunden-Ritual. Bleibt erhalten; 120 Sekunden Cooldown. Beschwört einen Wächter, dessen Beute ein Ritualkern ist. |
| `ritual_core` | Rechtsklick: Resistenz I + Regeneration I für 8 Sekunden. Kein Verbrauch, Cooldown: 60 Sekunden. |
| `chaos_crown` | Rechtsklick: Tempo I + Eile I für 10 Sekunden für dich und aktuelle Gruppenmitglieder innerhalb 8 Blöcken. Kein Verbrauch, Cooldown: 75 Sekunden. Außerhalb einer Gruppe wirkt die Krone auf dich selbst. |
| `frost_talisman` | Rechtsklick im Kampf: Langsamkeit II (6s) auf Raid-Gegner innerhalb 8 Blöcken, Bosse nur I (3s). Kein Schaden, kein Verbrauch, 40s Cooldown; ohne Gegner kein Cooldown. Drop: Stage-2-Miniboss. |
| `healing_bloom` | Regeneration II (6s) für dich/deine Raid-Gruppe in 8 Blöcken. Kein Verbrauch, 90s Cooldown. Drop: Stage-3-Miniboss. |
| `gale_sigil` | Tempo II und langsamer Fall (8s) für dich. Kein Verbrauch, 35s Cooldown. Drop: Stage-4-Miniboss. |
| `ember_charm` | Glutamulett: Feuerresistenz (15s), 45s Cooldown. Kein Verbrauch. Stage-1-Miniboss: 50 %. |
| `guardian_rune` | Wächterrune: Absorption II (8s), vier zusätzliche Herzen; 75s Cooldown. Kein Verbrauch. Stage-2-Miniboss: 50 %. |
| `hunter_lens` | Jägerlinse: markiert Raid-Gegner in 16 Blöcken durch Leuchten (12s), dazu Nachtsicht für dich (30s); 35s Cooldown. Funktioniert während einer Stage mit Gegnern in Reichweite. Kein Verbrauch. Stage-3-Miniboss: 50 %. |
| `weakening_fang` | Schwächungszahn: Schwäche I auf Raid-Gegner in 8 Blöcken (6s, Bosse 3s); 50s Cooldown, kein direkter Schaden. Kein Verbrauch. Stage-4-Miniboss: 50 %. |
| `raid_shard` | Raidsplitter aus Gegnern/Minibossen. Handelsware für den späteren NPC-Shop; Material `PRISMARINE_SHARD`, feste PDC-Item-ID `raid_shard`, eigenes Modell und Lore. |
| `boss_model` | Interne Darstellung des Endbosses, kein Belohnungs-Relikt. |

Relikt-Cooldowns werden gespeichert und gelten auch nach einem Relog. Andere Relikte können über `items.<id>.use` konfiguriert werden. Alle benutzbaren Items benötigen ihre echte PDC-ID, ein umbenanntes Vanilla-Item reicht nicht.

## Anbindung an ein anderes Shop-Plugin

Ein NPC-Shop ist noch nicht enthalten. Die Verkaufsbeträge und der NPC werden später in deinem Shop-Plugin eingestellt. Es muss Custom-Item-Metadaten/PDC exakt vergleichen oder die folgende API benutzen; ein reiner Materialvergleich würde auch gewöhnliche Prismarinscherben akzeptieren.

`DurcheinanderMobArmy` als Plugin-Abhängigkeit eintragen. Die API auf dem Bukkit-Hauptthread aufrufen. Der öffentliche Plugin-Einstieg bietet:

```java
MobArmy raid = (MobArmy) Bukkit.getPluginManager().getPlugin("DurcheinanderMobArmy");
String id = raid.getCustomItemId(itemStack);
int available = raid.countCustomItems(player, "raid_shard");
if (raid.removeCustomItems(player, "raid_shard", amount)) {
    // Erst jetzt mit DEINEM Economy-/Shop-Plugin bezahlen.
}
ItemStack template = raid.createCustomItem("raid_shard", 1);
```

`removeCustomItems` prüft vor dem Entfernen die gesamte Menge und entfernt bei zu wenigen echten Items nichts. Das Plugin nimmt selbst keine automatische Zahlung vor. Für commandfähige Shops/Admins: `/raid item count raid_shard Yuuki` und `/raid item take raid_shard Yuuki 16`. Für sichere Käufe/Verkäufe das Erfolgsresultat der API vor der Auszahlung prüfen.

## Texturen

Alle elf Icons liegen als **echte 16×16-RGBA-PNGs** unter `resource-pack/assets/mobarmy/textures/item/`. Je Datei: höchstens sechs deckende Farben plus vollständig transparente Pixel, harte Kanten ohne Antialiasing oder weiche Glows. Die Modelle überschreiben keine Vanilla-Items. Die integrierten Asset-Prüfungen verlangen exakt 16×16 Pixel.

Die Motive werden mit der eingebauten Imagegen-Funktion als stark vereinfachte Minecraft-Pixelgrafiken erstellt, dann mit Nearest-Neighbor auf genau 16×16 formatiert und auf maximal sechs deckende Farben quantisiert. Der transparente Hintergrund bleibt erhalten. Die vollständigen Prompts stehen in `resource-pack/texture-prompts.json`; als Motive dienen schlichte Splitter, Kugel, Krone, Blume, Windspirale, Flamme, Schildrune, Lupe und Zahn. Es werden keine Vanilla-Dateien kopiert.

Das fertig gebaute Pack liegt als `resource-pack/MobArmy-resource-pack-26.3.zip` in der Test-ZIP und ist zusätzlich im Plugin gebündelt. Die localhost-Demo fordert es automatisch vom eigenen PC an; bitte im Minecraft-Dialog annehmen. `/raid pack status` und `/raid pack reload` helfen bei der Prüfung. Alte manuell installierte MobArmy-Packs deaktivieren, wenn sie Modelle überschreiben.
