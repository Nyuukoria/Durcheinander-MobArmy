package de.yuuki.mobarmy;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class ContentConfigTest {
    @Test void bossPoolsAndExpandedWavesAreValidAndExcludePhantomsVexAndVexSummoners(){
        var cfg=YamlConfiguration.loadConfiguration(new InputStreamReader(getClass().getResourceAsStream("/config.yml"),StandardCharsets.UTF_8));var bosses=cfg.getConfigurationSection("boss-types");assertEquals(9,bosses.getKeys(false).size());
        for(String id:bosses.getKeys(false)){String type=bosses.getString(id+".type");org.bukkit.entity.EntityType.valueOf(type);assertTrue(Bosses.permitted(type));String item=bosses.getString(id+".model-item");if(item!=null)assertTrue(cfg.getBoolean("items."+item+".internal"));}
        assertFalse(Bosses.permitted("VEX"));assertFalse(Bosses.permitted("PHANTOM"));assertFalse(Bosses.permitted("EVOKER"));
        var types=new java.util.HashSet<String>();for(String week:cfg.getConfigurationSection("weeks").getKeys(false))for(int stage=1;stage<=5;stage++){
            var s=cfg.getConfigurationSection("weeks."+week+".stages."+stage);for(String pool:java.util.List.of("miniboss-pool","boss-pool"))for(String id:s.getStringList(pool))assertTrue(bosses.contains(id));
            for(String wave:s.getConfigurationSection("waves").getKeys(false)){int total=0;for(var spec:s.getMapList("waves."+wave+".mobs")){String type=spec.get("type").toString();assertTrue(Bosses.permitted(type));types.add(type);total+=((Number)spec.get("count")).intValue();}assertTrue(total>=10);}
        }
        assertTrue(types.size()>=14);
    }
    @Test void allEquipmentPoolsAndRitualIngredientsPointToRealItemsAndValidRecipes(){
        var cfg=YamlConfiguration.loadConfiguration(new InputStreamReader(getClass().getResourceAsStream("/config.yml"),StandardCharsets.UTF_8));
        var items=cfg.getConfigurationSection("items");long gear=0;
        for(String id:items.getKeys(false)){
            var item=items.getConfigurationSection(id);Material.valueOf(item.getString("material"));if(!item.getBoolean("gear"))continue;gear++;
            var enchantments=item.getConfigurationSection("enchantments");assertNotNull(enchantments,id);for(String enchant:enchantments.getKeys(false))assertTrue(enchantments.getInt(enchant)>=1&&enchantments.getInt(enchant)<=10);
            assertFalse(enchantments.contains("infinity")&&enchantments.contains("mending"),id+" has conflicting bow enchants");
            if(item.contains("set"))assertTrue(cfg.contains("equipment-sets."+item.getString("set")),id);
        }
        assertEquals(29,gear);
        var pools=cfg.getConfigurationSection("loot-pools");for(String id:pools.getKeys(false)){
            var pool=pools.getConfigurationSection(id);assertTrue(pool.getDouble("chance")>0&&pool.getDouble("chance")<=.1);
            for(var row:pool.getMapList("entries"))assertTrue(items.contains(row.get("item").toString()),id);assertNotNull(LootTables.choose(pool,0));
        }
        var rituals=cfg.getConfigurationSection("rituals");assertEquals(11,rituals.getKeys(false).size());
        for(String id:rituals.getKeys(false)){
            var ritual=rituals.getConfigurationSection(id);assertTrue(items.contains(ritual.getString("catalyst")));assertTrue(items.contains(ritual.getString("display-item")));
            assertTrue(ritual.isConfigurationSection("trial"),id);assertTrue(Bosses.permitted(ritual.getString("trial.type")));assertTrue(ritual.getDouble("trial.health")>=45);assertTrue(ritual.getDouble("trial.damage")<=9);
            org.bukkit.entity.EntityType.valueOf(ritual.getString("trial.type"));if(ritual.contains("trial.model-item"))assertTrue(items.getBoolean(ritual.getString("trial.model-item")+".internal"));
            if(ritual.contains("pool"))assertTrue(pools.contains(ritual.getString("pool")));if(ritual.contains("legendary-pool"))assertTrue(pools.contains(ritual.getString("legendary-pool")));
            for(var row:ritual.getMapList("ingredients")){assertTrue(((Number)row.get("amount")).intValue()>0);if(row.containsKey("item"))assertTrue(items.contains(row.get("item").toString()));else Material.valueOf(row.get("material").toString());}
        }
    }
}
