package de.yuuki.mobarmy;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ArenaChecksTest {
    YamlConfiguration layout(){var c=new YamlConfiguration();c.set("world","custom");c.set("arena.min",List.of(-50,64,-50));c.set("arena.max",List.of(49,85,49));c.set("lobby.min",List.of(80,64,-4));c.set("lobby.max",List.of(88,68,4));c.set("door.min",List.of(80,64,-4));c.set("door.max",List.of(80,65,-3));c.set("arena-spawn",List.of(.5,64.,.5));c.set("lobby-spawn",List.of(84.5,64.,.5));c.set("spawn-points",List.of("-40,64,-40","40,64,-40","-40,64,40","40,64,40"));return c;}
    @Test void acceptsSeparatedHundredBlockArena(){assertTrue(ArenaChecks.layout(layout()).isEmpty());}
    @Test void rejectsOversizeAndOverlappingPreparationRoom(){var c=layout();c.set("arena.max",List.of(50,85,49));assertTrue(ArenaChecks.layout(c).contains("size"));c=layout();c.set("lobby.min",List.of(-3,64,-3));c.set("lobby.max",List.of(3,68,3));assertTrue(ArenaChecks.layout(c).contains("overlap"));}
    @Test void fewerUniqueSpawnsOrInvalidPlayerPositionKeepDraftLocked(){var c=layout();c.set("spawn-points",Collections.nCopies(4,"40,64,40"));assertTrue(ArenaChecks.layout(c).contains("spawns"));c=layout();c.set("arena-spawn",List.of(Double.NaN,64.,.5));assertTrue(ArenaChecks.layout(c).contains("point"));}
    @Test void invalidCornersAndButtonsAreRejected(){var c=layout();c.set("door.max",List.of(79,65,-3));assertTrue(ArenaChecks.layout(c).contains("regions"));c=layout();c.set("controls.start.location",List.of(85,68,0));assertTrue(ArenaChecks.layout(c).contains("controls"));assertTrue(ArenaChecks.oversized(new Region("custom",Integer.MIN_VALUE,0,0,Integer.MAX_VALUE,1,1),100000));}
}
