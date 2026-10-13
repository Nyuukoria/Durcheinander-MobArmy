package de.yuuki.mobarmy;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SpawnPlannerTest {
    @Test void spreadNeverReusesAnOccupiedPadWhileSpacedPadsRemain(){
        var pads=List.of(new SpawnPlanner.Point(-20,64,-20),new SpawnPlanner.Point(20,64,-20),new SpawnPlanner.Point(-20,64,20),new SpawnPlanner.Point(20,64,20));
        List<SpawnPlanner.Point> occupied=new ArrayList<>();Random random=new Random(9);
        for(int i=0;i<4;i++){int index=SpawnPlanner.choose(pads,List.of(new SpawnPlanner.Point(0,64,0)),occupied,8,4,random);assertTrue(index>=0);assertFalse(occupied.contains(pads.get(index)));occupied.add(pads.get(index));}
    }
    @Test void playerSafetyDistanceIsRequiredEvenWhenAllPadsAreCrowded(){
        var one=new SpawnPlanner.Point(0,64,0);assertEquals(-1,SpawnPlanner.choose(List.of(one),List.of(one),List.of(),8,4,new Random(1)));
        SpawnPlanner.Point near=new SpawnPlanner.Point(1,64,0),far=new SpawnPlanner.Point(20,64,0);
        assertEquals(1,SpawnPlanner.choose(List.of(near,far),List.of(one),List.of(far),8,4,new Random(1)));
    }
}
