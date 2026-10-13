package de.yuuki.mobarmy;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class PearlsTest {
    void field(Object target,String name,Object value)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);}
    @Test void collisionAtArenaWallCanMoveSlightlyInwardButOutsideTravelStaysBlocked()throws Exception{
        World world=mock(World.class);when(world.getName()).thenReturn("raid_w1");Block air=mock(Block.class);when(air.isPassable()).thenReturn(true);when(world.getBlockAt(anyInt(),anyInt(),anyInt())).thenReturn(air);when(world.getBlockAt(any(Location.class))).thenReturn(air);
        Arena arena=mock(Arena.class);field(arena,"world",world);field(arena,"bounds",new Region("raid_w1",-50,64,-50,49,85,49));Player player=mock(Player.class);when(player.getBoundingBox()).thenReturn(new BoundingBox(0,64,0,.6,65.8,.6));
        try(var pearls=mockStatic(Pearls.class,CALLS_REAL_METHODS)){
            pearls.when(()->Pearls.clear(any(Location.class))).thenReturn(true);
            Location safe=Pearls.safe(player,arena,new Location(world,50.1,64,0));assertNotNull(safe);assertTrue(safe.getX()<50);assertTrue(arena.bounds.contains(safe));
            assertNull(Pearls.safe(player,arena,new Location(world,55,64,0)));
        }
    }
    @Test void candidateSearchPrefersNearbyLandingsAndCanSearchDownFromAnEntityHit(){
        World world=mock(World.class);Location at=new Location(world,1.2,71,2.8);var list=Pearls.candidates(at);assertEquals(at,list.getFirst());
        double previous=-1;for(Location l:list){assertTrue(l.distanceSquared(at)>=previous);previous=l.distanceSquared(at);}
        assertTrue(list.stream().anyMatch(l->l.getBlockY()==64&&Math.abs(l.getX()-at.getX())<1&&Math.abs(l.getZ()-at.getZ())<1));
    }
}
