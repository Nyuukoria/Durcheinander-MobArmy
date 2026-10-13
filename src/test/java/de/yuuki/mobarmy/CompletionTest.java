package de.yuuki.mobarmy;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CompletionTest {
    @Test void demoBossLanguageAndDeleteCompletionUseTheirOwnArgumentGrammar(){
        var allowed=List.of("skip","boss","language","rezepte","portal");var empty=List.<String>of();var portals=List.of("demo_w1");var bosses=List.of("frost_guardian","ember_guardian");
        assertEquals(List.of("stage","wave"),Completion.suggest(new String[]{"skip",""},allowed,empty,portals,empty,empty,empty,bosses));
        assertEquals(portals,Completion.suggest(new String[]{"skip","wave",""},allowed,empty,portals,empty,empty,empty,bosses));
        assertEquals(List.of("frost_guardian"),Completion.suggest(new String[]{"boss","summon","fr"},allowed,empty,portals,empty,empty,empty,bosses));
        assertEquals(List.of("de","en"),Completion.suggest(new String[]{"language",""},allowed,empty,portals,empty,empty,empty,bosses));
        assertEquals(List.of("all","demo_w1"),Completion.suggest(new String[]{"portal","delete",""},allowed,empty,portals,empty,empty,empty,bosses));
    }
    @Test void codexKitAndConfiguredRitualsHaveTheCorrectArgumentTypes(){
        var allowed=List.of("codex","kit","ritual");
        assertEquals(List.of("gear","items","rituals","sets"),Completion.suggest(new String[]{"codex",""},allowed,List.of(),List.of(),List.of(),List.of(),List.of("awakening")));
        assertEquals(List.of("basic","gear","ritual"),Completion.suggest(new String[]{"kit",""},allowed,List.of(),List.of(),List.of(),List.of(),List.of("awakening")));
        assertEquals(List.of("custom_recipe"),Completion.suggest(new String[]{"ritual","custom"},allowed,List.of(),List.of(),List.of(),List.of(),List.of("awakening","custom_recipe")));
    }
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
