package de.yuuki.mobarmy;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigUpgradeTest {
    @Test void oldDemoMovesLobbyButKeepsCustomWorldsDropRatesAndPlayerSettings()throws Exception{
        String resource=new String(Objects.requireNonNull(getClass().getResourceAsStream("/config.yml")).readAllBytes(),StandardCharsets.UTF_8);
        var defaults=new YamlConfiguration();defaults.loadFromString(resource);var cfg=new YamlConfiguration();cfg.setDefaults(defaults);
        cfg.set("testing.solo",true);cfg.set("balance.hit-cap",.13);
        for(String id:List.of("old","custom")){String base="weeks."+id;cfg.set(base+".generate-demo",id.equals("old"));cfg.set(base+".lobby.min",List.of(-23,64,-30));cfg.set(base+".lobby.max",List.of(23,69,-25));cfg.set(base+".arena.max",List.of(23,84,23));}
        cfg.set("items.raid_shard.name","Old shard");cfg.set("items.raid_shard.drops",List.of(Map.of("source","mob","chance",.42)));
        MobArmy p=mock(MobArmy.class);when(p.getConfig()).thenReturn(cfg);when(p.getResource("config.yml")).thenAnswer(i->new ByteArrayInputStream(resource.getBytes(StandardCharsets.UTF_8)));
        ConfigUpgrade.run(p);
        assertEquals(2,cfg.getInt("configuration-version"));assertEquals(List.of(80,64,-8),cfg.getIntegerList("weeks.old.lobby.min"));assertEquals(List.of(-23,64,-30),cfg.getIntegerList("weeks.custom.lobby.min"));
        assertEquals(.13,cfg.getDouble("balance.hit-cap"));assertTrue(cfg.getBoolean("testing.solo"));assertEquals(.42,cfg.getMapList("items.raid_shard.drops").getFirst().get("chance"));
        assertEquals("mobarmy:raid_shard",cfg.getString("items.raid_shard.item-model"));
        cfg.set("items.raid_shard.name","My own name");ConfigUpgrade.run(p);assertEquals("My own name",cfg.getString("items.raid_shard.name"));verify(p,times(1)).saveConfig();
    }
}
