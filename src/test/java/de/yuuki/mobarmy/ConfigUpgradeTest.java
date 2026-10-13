package de.yuuki.mobarmy;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigUpgradeTest {
    @Test void v13AddsGearAndRecipesWhileKeepingArenasAndCustomEnchantSettings()throws Exception{
        String resource=new String(Objects.requireNonNull(getClass().getResourceAsStream("/config.yml")).readAllBytes(),StandardCharsets.UTF_8);
        var cfg=new YamlConfiguration();cfg.loadFromString(resource);cfg.set("configuration-version",4);cfg.set("loot-pools",null);cfg.set("equipment-sets",null);cfg.set("rituals",null);cfg.set("items.abyss_sword",null);
        cfg.set("items.warden_blade.enchantments.sharpness",7);cfg.set("weeks.w1.arena.max",List.of(40,85,42));
        MobArmy p=mock(MobArmy.class);when(p.getConfig()).thenReturn(cfg);when(p.getResource("config.yml")).thenAnswer(i->new ByteArrayInputStream(resource.getBytes(StandardCharsets.UTF_8)));
        ConfigUpgrade.run(p);assertEquals(5,cfg.getInt("configuration-version"));assertEquals(List.of(40,85,42),cfg.getIntegerList("weeks.w1.arena.max"));assertEquals(7,cfg.getInt("items.warden_blade.enchantments.sharpness"));
        assertEquals(8,cfg.getInt("items.abyss_sword.enchantments.sharpness"));assertEquals(7,cfg.getConfigurationSection("rituals").getKeys(false).size());assertEquals(.01,cfg.getDouble("loot-pools.abyss.chance"));
    }
    @Test void v12UpdateAddsRelicsWithoutRebuildingOrOverwritingExistingSettings()throws Exception{
        String resource=new String(Objects.requireNonNull(getClass().getResourceAsStream("/config.yml")).readAllBytes(),StandardCharsets.UTF_8);
        var cfg=new YamlConfiguration();cfg.loadFromString(resource);cfg.set("configuration-version",3);
        cfg.set("weeks.w1.arena.max",List.of(40,85,42));cfg.set("items.ritual_core.name","My core");cfg.set("items.ritual_core.use.cooldown-seconds",99);
        for(String id:List.of("ember_charm","guardian_rune","hunter_lens","weakening_fang"))cfg.set("items."+id,null);
        MobArmy p=mock(MobArmy.class);when(p.getConfig()).thenReturn(cfg);when(p.getResource("config.yml")).thenAnswer(i->new ByteArrayInputStream(resource.getBytes(StandardCharsets.UTF_8)));
        ConfigUpgrade.run(p);
        assertEquals(5,cfg.getInt("configuration-version"));assertEquals(List.of(40,85,42),cfg.getIntegerList("weeks.w1.arena.max"));
        assertEquals("My core",cfg.getString("items.ritual_core.name"));assertEquals(99,cfg.getInt("items.ritual_core.use.cooldown-seconds"));
        for(String id:List.of("ember_charm","guardian_rune","hunter_lens","weakening_fang")){assertTrue(cfg.contains("items."+id,true));assertEquals(0,cfg.getInt("items."+id+".use.consume"));}
    }
    @Test void v11DemoUpgradesExistingGeometryAndReusableItems()throws Exception{
        String resource=new String(Objects.requireNonNull(getClass().getResourceAsStream("/config.yml")).readAllBytes(),StandardCharsets.UTF_8);
        var cfg=new YamlConfiguration();cfg.loadFromString(resource);cfg.set("configuration-version",2);
        cfg.set("weeks.w1.lobby.min",List.of(80,64,-8));cfg.set("weeks.w1.lobby.max",List.of(96,70,8));
        cfg.set("weeks.w2.generate-demo",false);cfg.set("weeks.w2.arena.max",List.of(12,78,15));
        cfg.set("items.ritual_core.use.consume",1);cfg.set("items.frost_talisman",null);
        MobArmy p=mock(MobArmy.class);when(p.getConfig()).thenReturn(cfg);when(p.getResource("config.yml")).thenAnswer(i->new ByteArrayInputStream(resource.getBytes(StandardCharsets.UTF_8)));
        ConfigUpgrade.run(p);
        assertEquals(List.of(88,68,4),cfg.getIntegerList("weeks.w1.lobby.max"));assertEquals(List.of(12,78,15),cfg.getIntegerList("weeks.w2.arena.max"));
        assertEquals(0,cfg.getInt("items.ritual_core.use.consume"));assertEquals(120,cfg.getInt("items.chaos_fragment.ritual.cooldown-seconds"));assertTrue(cfg.contains("items.frost_talisman"));
    }
    @Test void oldDemoMovesLobbyButKeepsCustomWorldsDropRatesAndPlayerSettings()throws Exception{
        String resource=new String(Objects.requireNonNull(getClass().getResourceAsStream("/config.yml")).readAllBytes(),StandardCharsets.UTF_8);
        var defaults=new YamlConfiguration();defaults.loadFromString(resource);var cfg=new YamlConfiguration();cfg.setDefaults(defaults);
        cfg.set("testing.solo",true);cfg.set("balance.hit-cap",.13);
        for(String id:List.of("old","custom")){String base="weeks."+id;cfg.set(base+".generate-demo",id.equals("old"));cfg.set(base+".lobby.min",List.of(-23,64,-30));cfg.set(base+".lobby.max",List.of(23,69,-25));cfg.set(base+".arena.max",List.of(23,84,23));}
        cfg.set("items.raid_shard.name","Old shard");cfg.set("items.raid_shard.drops",List.of(Map.of("source","mob","chance",.42)));
        MobArmy p=mock(MobArmy.class);when(p.getConfig()).thenReturn(cfg);when(p.getResource("config.yml")).thenAnswer(i->new ByteArrayInputStream(resource.getBytes(StandardCharsets.UTF_8)));
        ConfigUpgrade.run(p);
        assertEquals(5,cfg.getInt("configuration-version"));assertEquals(List.of(80,64,-4),cfg.getIntegerList("weeks.old.lobby.min"));assertEquals(List.of(-23,64,-30),cfg.getIntegerList("weeks.custom.lobby.min"));
        assertEquals(.13,cfg.getDouble("balance.hit-cap"));assertTrue(cfg.getBoolean("testing.solo"));assertEquals(.42,cfg.getMapList("items.raid_shard.drops").getFirst().get("chance"));
        assertEquals("mobarmy:raid_shard",cfg.getString("items.raid_shard.item-model"));
        assertEquals(List.of(49,85,49),cfg.getIntegerList("weeks.old.arena.max"));assertEquals(0,cfg.getInt("items.ritual_core.use.consume"));assertTrue(cfg.contains("items.frost_talisman",true));
        cfg.set("items.raid_shard.name","My own name");ConfigUpgrade.run(p);assertEquals("My own name",cfg.getString("items.raid_shard.name"));verify(p,times(1)).saveConfig();
    }
}
