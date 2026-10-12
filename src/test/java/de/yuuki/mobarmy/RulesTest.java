package de.yuuki.mobarmy;

import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class RulesTest {
    final ZoneId zone=ZoneId.of("Europe/Berlin");
    @Test void rewardsStopAtDeadlineAndMidnightAndFirstBoss(){
        LocalDate day=LocalDate.of(2026,10,12);long now=day.atTime(23,59).atZone(zone).toInstant().toEpochMilli();
        assertTrue(Rules.rewards(false,now+3600000,day,now,zone));
        assertFalse(Rules.rewards(false,now,day,now,zone));
        assertFalse(Rules.rewards(true,now+3600000,day,now,zone));
        assertFalse(Rules.rewards(false,now+3600000,day,now+60000,zone));
    }
    @Test void capsOverpoweredMaceAndBossParticipation(){
        assertEquals(20,Rules.damage(10000,100,0,false,false,.2,.05,.4));
        assertEquals(5,Rules.damage(10000,100,0,true,false,.2,.05,.4));
        assertEquals(3,Rules.damage(10000,100,37,false,true,.2,.05,.4));
        assertEquals(0,Rules.damage(10,100,40,false,true,.2,.05,.4));
        assertEquals(2,Rules.damage(2,100,0,false,true,.2,.05,.4));
    }
    @Test void damageNeverExceedsShareOverRepeatedHits(){
        double dealt=0;for(int i=0;i<100;i++)dealt+=Rules.damage(9999,250,dealt,i%2==0,true,.2,.05,.4);
        assertEquals(100,dealt,1e-8);
    }
    @Test void scalingDoesNotReduceBelowBaseForSmallGroups(){assertEquals(8,Rules.units(8,2,3,.25));assertEquals(12,Rules.units(8,5,3,.25));}
    @Test void durationsAreBoundedAndRejectInvalidInput(){
        assertEquals(1800000,Rules.duration("30m"));assertEquals(7200000,Rules.duration("2h"));assertEquals(90000,Rules.duration("90s"));assertEquals(1800000,Rules.duration("30"));
        assertThrows(IllegalArgumentException.class,()->Rules.duration("0m"));assertThrows(IllegalArgumentException.class,()->Rules.duration("8d"));assertThrows(RuntimeException.class,()->Rules.duration(""));
    }
    @Test void votingNeedsStrictMajority(){assertFalse(Rules.majority(2,4));assertTrue(Rules.majority(3,4));assertTrue(Rules.majority(2,3));}
}
