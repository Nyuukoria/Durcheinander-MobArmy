package de.yuuki.mobarmy;
import org.bukkit.*;
import org.bukkit.entity.Player;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class RitualStrikeTest {
    @Test void announcedStrikeHitsOnlyItsSmallAreaAndMatchingFloorAndWorld(){World w=mock(World.class),other=mock(World.class);Location mark=new Location(w,0,64,0);
        assertTrue(Rituals.inStrike(new Location(w,2,64,0),mark));assertFalse(Rituals.inStrike(new Location(w,2,64,2),mark));assertFalse(Rituals.inStrike(new Location(w,0,68,0),mark));assertFalse(Rituals.inStrike(new Location(other,0,64,0),mark));
    }
    @Test void blessingsReachTheirOwnerAfterAnOutlyingFightWhileNearbyHelpersKeepTheConfiguredRadius(){
        World w=mock(World.class);UUID id=UUID.randomUUID();Player owner=mock(Player.class),near=mock(Player.class),far=mock(Player.class);when(owner.getUniqueId()).thenReturn(id);when(owner.getLocation()).thenReturn(new Location(w,10,64,0));when(near.getUniqueId()).thenReturn(UUID.randomUUID());when(far.getUniqueId()).thenReturn(UUID.randomUUID());when(near.getLocation()).thenReturn(new Location(w,2,64,0));when(far.getLocation()).thenReturn(new Location(w,7,64,0));
        assertEquals(List.of(owner,near),Rituals.buffTargets(List.of(owner,near,far),id,new Location(w,0,64,0),6));
    }
}
