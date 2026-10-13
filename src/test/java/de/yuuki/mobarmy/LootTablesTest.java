package de.yuuki.mobarmy;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.function.DoubleSupplier;
import static org.junit.jupiter.api.Assertions.*;

class LootTablesTest {
    YamlConfiguration config(double chance){var c=new YamlConfiguration();c.set("pool.source","boss");c.set("pool.minimum-stage",5);c.set("pool.chance",chance);c.set("pool.entries",List.of(Map.of("item","a","weight",1),Map.of("item","b","weight",3)));return c;}
    @Test void probabilityIsCheckedOnceAndSuccessfulPoolChoosesExactlyOne(){
        var c=config(.02);var draws=new ArrayDeque<>(List.of(.019,.9));DoubleSupplier random=draws::removeFirst;
        assertEquals(List.of("b"),LootTables.roll(c,"boss",5,"w1",random));assertTrue(draws.isEmpty());
        assertEquals(List.of(),LootTables.roll(c,"boss",5,"w1",()->.02));
        assertEquals(List.of(),LootTables.roll(config(0),"boss",5,"w1",()->0));
    }
    @Test void weightedBoundariesAndSourcesStagesWeeksAreRespected(){
        var c=config(1);var pool=c.getConfigurationSection("pool");assertEquals("a",LootTables.choose(pool,0));assertEquals("a",LootTables.choose(pool,.24999));assertEquals("b",LootTables.choose(pool,.25));assertEquals("b",LootTables.choose(pool,1));
        assertEquals(List.of(),LootTables.roll(c,"miniboss",5,"w1",()->0));assertEquals(List.of(),LootTables.roll(c,"boss",4,"w1",()->0));
        c.set("pool.week","w3");assertEquals(List.of(),LootTables.roll(c,"boss",5,"w1",()->0));assertEquals(List.of("a"),LootTables.roll(c,"boss",5,"w3",()->0));
    }
    @Test void invalidWeightsAndChancesCannotGrantAnItem(){var c=config(2);assertThrows(IllegalArgumentException.class,()->LootTables.roll(c,"boss",5,"w1",()->0));c.set("pool.entries",List.of(Map.of("item","a","weight",0)));assertThrows(IllegalArgumentException.class,()->LootTables.choose(c.getConfigurationSection("pool"),0));}
}
