package de.yuuki.mobarmy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DemoTerrainTest {
    @Test void terrainIsWalkableAndSpawnPadsAreFlat(){
        int highest=0;
        for(int x=-50;x<=49;x++)for(int z=-50;z<=49;z++){
            int y=DemoTerrain.height(x,z);highest=Math.max(highest,y);
            assertTrue(y>=0&&y<=4);
            assertTrue(Math.abs(y-DemoTerrain.height(x+1,z))<=1,"Unwalkable X cliff at "+x+","+z);
            assertTrue(Math.abs(y-DemoTerrain.height(x,z+1))<=1,"Unwalkable Z cliff at "+x+","+z);
        }
        assertEquals(4,highest);
        for(int sx:new int[]{-32,32})for(int sz:new int[]{-32,32})for(int dx=-5;dx<=5;dx++)for(int dz=-5;dz<=5;dz++)assertEquals(0,DemoTerrain.height(sx+dx,sz+dz));
        for(int x=-2;x<=2;x++)for(int z=-38;z<=-34;z++)assertEquals(0,DemoTerrain.height(x,z));
    }
}
