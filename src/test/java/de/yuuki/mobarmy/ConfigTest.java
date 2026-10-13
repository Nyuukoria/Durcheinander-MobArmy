package de.yuuki.mobarmy;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {
    @Test void bundledWeeksHaveExactlyTheRequiredWaveStructureAndRiderRestrictions() {
        var input=getClass().getResourceAsStream("/config.yml");assertNotNull(input);
        var cfg=YamlConfiguration.loadConfiguration(new InputStreamReader(input,StandardCharsets.UTF_8));
        assertEquals(3,cfg.getConfigurationSection("weeks").getKeys(false).size());
        for(String week:cfg.getConfigurationSection("weeks").getKeys(false))for(int stage=1;stage<=5;stage++){
            var s=cfg.getConfigurationSection("weeks."+week+".stages."+stage);assertNotNull(s);
            assertEquals(stage==5?4:3,s.getConfigurationSection("waves").getKeys(false).size());
            assertNotNull(s.getConfigurationSection(stage==5?"boss":"miniboss"));
            for(int wave=1;wave<=(stage==5?4:3);wave++){
                var mobs=s.getMapList("waves."+wave+".mobs");assertFalse(mobs.isEmpty());
                if(wave<3)assertTrue(mobs.stream().noneMatch(m->m.containsKey("rider")));
            }
        }
        assertFalse(cfg.getBoolean("testing.solo"));
        assertEquals(2,cfg.getInt("weeks.w1.minimum-players"));assertEquals(3,cfg.getInt("weeks.w2.minimum-players"));assertEquals(5,cfg.getInt("weeks.w3.minimum-players"));
        assertNotNull(cfg.getConfigurationSection("items.chaos_fragment.ritual"));
        assertFalse(cfg.getBoolean("testing.disable-wipe-cooldown"));
        for(String week:cfg.getConfigurationSection("weeks").getKeys(false)){
            var a=cfg.getConfigurationSection("weeks."+week);assertTrue(a.getIntegerList("lobby.min").getFirst()>a.getIntegerList("arena.max").getFirst());
            assertEquals(85,a.getIntegerList("arena.max").get(1));
        }
        assertEquals("mobarmy:raid_shard",cfg.getString("items.raid_shard.item-model"));assertTrue(cfg.getBoolean("items.raid_shard.shop-token"));
        assertFalse(cfg.getMapList("items.ritual_core.use.effects").isEmpty());assertEquals(0,cfg.getInt("items.ritual_core.use.consume"));
        for(String id:cfg.getConfigurationSection("items").getKeys(false)){
            assertEquals(0,cfg.getInt("items."+id+".use.consume"),id);assertEquals(0,cfg.getInt("items."+id+".ritual.consume"),id);
        }
        assertFalse(cfg.getBoolean("testing.local-resource-pack"));
        assertEquals(100,cfg.getIntegerList("weeks.w1.arena.max").getFirst()-cfg.getIntegerList("weeks.w1.arena.min").getFirst()+1);
        assertEquals(9,cfg.getIntegerList("weeks.w1.lobby.max").getFirst()-cfg.getIntegerList("weeks.w1.lobby.min").getFirst()+1);
    }
}
