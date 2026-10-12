package de.yuuki.mobarmy;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CompletionTest {
    final List<String> allowed=List.of("portal","timer","item","demo","cooldown","test","join","help");
    List<String> suggest(String... args){return Completion.suggest(args,allowed,List.of("w1","w2","w3"),List.of("demo_w1","event_two"),List.of("chaos_fragment","raid_shard"),List.of("Yuuki","Ashy"));}
    @Test void completesOnlyTheExpectedArgumentType(){
        assertEquals(List.of("w1","w2","w3"),suggest("portal","create",""));
        assertEquals(List.of("Yuuki"),suggest("item","give","raid_shard","y"));
        assertEquals(List.of("1","16","32","64","8"),suggest("item","give","raid_shard","Yuuki",""));
        assertEquals(List.of("15m","1h","2h","30m"),suggest("timer","set",""));
        assertEquals(List.of("demo_w1","event_two"),suggest("timer","set","1h",""));
        assertEquals(List.of("all","demo_w1","event_two"),suggest("portal","remove",""));
        assertTrue(suggest("help","").isEmpty());assertTrue(suggest("item","count","raid_shard","Yuuki","").isEmpty());
    }
    @Test void resetAndCooldownCommandsAreDiscoverable(){assertEquals(List.of("reset"),suggest("demo","res"));assertEquals(List.of("all","demo_w1","event_two"),suggest("demo","reset",""));assertEquals(List.of("w1","w2","w3"),suggest("demo","rebuild",""));assertEquals(List.of("all","Ashy","Yuuki"),suggest("cooldown","clear",""));}
    @Test void permissionFilteredRootsDoNotLeakAdminArguments(){assertTrue(Completion.suggest(new String[]{"portal",""},List.of("help","join"),List.of("w1"),List.of("secret"),List.of(),List.of()).isEmpty());}
}
