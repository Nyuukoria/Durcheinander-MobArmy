package de.yuuki.mobarmy;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class DemoControlsTest {
    void field(Object target,String name,Object value)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);}
    @Test void skippingLastStageClearsEnemiesAndFinishesWithoutBossRewardsOrClearFlag()throws Exception{
        MobArmy p=mock(MobArmy.class);p.state=new YamlConfiguration();p.items=mock(Items.class);p.presentation=mock(Presentation.class);when(p.today()).thenReturn(LocalDate.of(2026,10,13));field(p,"returns",new HashMap<UUID,Location>());
        World world=mock(World.class);when(world.getName()).thenReturn("world");Arena a=mock(Arena.class);UUID leader=UUID.randomUUID();Portal q=new Portal(p,"demo_w1","w1",new Region("world",0,64,0,2,67,0),new Location(world,-2,64,0));Session s=new Session(q,a,leader);s.stage=5;s.participants.add(leader);q.session=s;s.phase(Session.Phase.FIGHT);s.wave=5;
        Mob enemy=mock(Mob.class);UUID enemyId=UUID.randomUUID();s.mobs.add(enemyId);s.contribution.put(enemyId,new HashMap<>());when(enemy.getPassengers()).thenReturn(List.of());
        try(var bukkit=mockStatic(Bukkit.class)){bukkit.when(()->Bukkit.getEntity(enemyId)).thenReturn(enemy);new Engine(p).skipWave(s);}
        verify(enemy).remove();assertNull(q.session);assertFalse(q.bossKilled);assertEquals(1,p.state.getInt("progress."+q.id+"."+leader));verify(p.items,never()).drops(anyString(),anyInt(),anyString());verify(p,never()).announcement(any(),anyString(),anyString());
    }
    @Test void stageSkipReturnsToLobbyAndNormalWaveSkipStartsTheNextWaveWithoutLoot()throws Exception{
        MobArmy p=mock(MobArmy.class);p.state=new YamlConfiguration();p.items=mock(Items.class);when(p.today()).thenReturn(LocalDate.of(2026,10,13));World world=mock(World.class);Arena a=mock(Arena.class);UUID leader=UUID.randomUUID();Portal q=new Portal(p,"demo_w1","w1",new Region("world",0,64,0,2,67,0),new Location(world,0,64,0));Session s=new Session(q,a,leader);s.stage=2;q.session=s;s.phase(Session.Phase.FIGHT);s.wave=1;
        Engine engine=spy(new Engine(p));doReturn(List.of()).when(engine).players(s);doAnswer(i->{s.wave++;return null;}).when(engine).spawnWave(s);
        engine.skipWave(s);assertEquals(2,s.wave);assertEquals(2,s.stage);engine.skipStage(s);assertEquals(3,s.stage);assertEquals(0,s.wave);assertEquals(Session.Phase.LOBBY,s.phase);assertFalse(q.bossKilled);verify(p.items,never()).drops(anyString(),anyInt(),anyString());
        assertThrows(IllegalArgumentException.class,()->engine.skipWave(s));
    }
}
