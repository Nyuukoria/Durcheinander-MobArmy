# Items, Funktionen und spätere NPC-Shops

| Item-ID | Funktion |
|---|---|
| `chaos_fragment` | Rechtsklick auf einen Kessel in der konfigurierten Default-Welt: animiertes 6-Sekunden-Ritual. Verbraucht 1 Fragment, beschwört einen Wächter; dessen Beute ist ein Ritualkern. |
| `ritual_core` | Rechtsklick: Resistenz I + Regeneration I für 8 Sekunden. Verbrauch: 1 Kern, Cooldown: 60 Sekunden. |
| `chaos_crown` | Rechtsklick: Tempo I + Eile I für 10 Sekunden für dich und aktuelle Gruppenmitglieder innerhalb 8 Blöcken. Kein Verbrauch, Cooldown: 75 Sekunden. Außerhalb einer Gruppe wirkt die Krone auf dich selbst. |
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

Vier eigene transparente 128×128-PNGs liegen unter `resource-pack/assets/mobarmy/textures/item/`. Sie wurden mit der eingebauten Imagegen-Funktion erstellt und zur Minecraft-Asset-Größe mit unveränderten Pixeln/Alpha verkleinert. Es sind keine globalen Ersetzungen von Vanilla-Items.

Prompts: ein einzelnes zentriertes Minecraft-Pixelart-Inventar-Icon mit transparentem Hintergrund, dunklen Randpixeln, violett/rosa/cyan-Palette, ohne Text/Labels/Umgebung; je Asset:

- Chaosfragment: gezackter violetter Amethystsplitter mit leuchtender pinker Rune.
- Ritualkern: türkis/violette Energiekugel mit dunklem Metall-Runenring und cyanfarbenem Kern.
- Chaoskrone: violett/obsidianfarbene dreizackige Krone, pink-violetter Edelstein und goldene Kanten.
- Raidsplitter: kantiger türkisfarbener Prismarinsplitter mit violetter Rune und cyanfarbenen Facetten, als Dungeon-Handelsmarke.

Das fertig gebaute Pack liegt als `resource-pack/MobArmy-resource-pack-26.3.zip` in der Test-ZIP. Aktiviere es im Minecraft-Client.
