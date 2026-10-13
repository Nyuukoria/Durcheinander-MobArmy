package de.yuuki.mobarmy;

import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BossDamageTest {
    @Test void reportedHitWithNoNativeHealthLossCannotSpendThePlayersShare(){
        MobArmy p=mock(MobArmy.class);LivingEntity mob=mock(LivingEntity.class);UUID id=UUID.randomUUID(),playerId=UUID.randomUUID();when(mob.getUniqueId()).thenReturn(id);when(mob.getHealth()).thenReturn(100.0);Player player=mock(Player.class);when(player.getUniqueId()).thenReturn(playerId);Session s=new Session(null,null,playerId);s.contribution.put(id,new HashMap<>());BossDamage tracker=new BossDamage(p);
        try(var bukkit=mockStatic(Bukkit.class)){bukkit.when(Bukkit::getScheduler).thenReturn(mock(BukkitScheduler.class));for(int i=0;i<10;i++){tracker.accepted(s,mob,player,20,100);tracker.flush(mob);}}
        assertTrue(s.contribution.get(id).isEmpty());verify(mob,never()).setHealth(0);assertEquals(20,Rules.damage(20,100,0,false,true,.2,.05,1));
    }
    @Test void twoSameTickHitsAreCreditedOnceAndStaleTasksCannotCreditTheWrongHit(){
        MobArmy p=mock(MobArmy.class);LivingEntity mob=mock(LivingEntity.class);UUID id=UUID.randomUUID(),playerId=UUID.randomUUID();when(mob.getUniqueId()).thenReturn(id);AtomicReference<Double> health=new AtomicReference<>(100.0);when(mob.getHealth()).thenAnswer(i->health.get());Player player=mock(Player.class);when(player.getUniqueId()).thenReturn(playerId);Session s=new Session(null,null,playerId);s.contribution.put(id,new HashMap<>());
        List<Runnable> tasks=new ArrayList<>();BukkitScheduler scheduler=mock(BukkitScheduler.class);when(scheduler.runTask(eq(p),any(Runnable.class))).thenAnswer(i->{tasks.add(i.getArgument(1));return null;});BossDamage tracker=new BossDamage(p);
        try(var bukkit=mockStatic(Bukkit.class)){bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);tracker.accepted(s,mob,player,20,100);health.set(80.0);tracker.accepted(s,mob,player,20,100);health.set(60.0);tasks.forEach(Runnable::run);}
        assertEquals(40,s.contribution.get(id).get(playerId));assertTrue(tracker.pending.isEmpty());
    }
    @Test void actualFloatHealthAndIgnoredHitsCannotLeaveAnImmortalSoloBoss(){
        MobArmy p=mock(MobArmy.class);LivingEntity mob=mock(LivingEntity.class);UUID id=UUID.randomUUID(),playerId=UUID.randomUUID();when(mob.getUniqueId()).thenReturn(id);double maximum=483.6;AtomicReference<Double> health=new AtomicReference<>((double)(float)maximum);when(mob.getHealth()).thenAnswer(i->health.get());doAnswer(i->{health.set(i.getArgument(0));return null;}).when(mob).setHealth(anyDouble());Player player=mock(Player.class);when(player.getUniqueId()).thenReturn(playerId);Session s=new Session(null,null,playerId);s.contribution.put(id,new HashMap<>());BossDamage tracker=new BossDamage(p);
        try(var bukkit=mockStatic(Bukkit.class)){bukkit.when(Bukkit::getScheduler).thenReturn(mock(BukkitScheduler.class));for(int i=0;i<100&&health.get()>0;i++){
            double dealt=s.contribution.get(id).getOrDefault(playerId,0.0),damage=Rules.damage(999,maximum,dealt,false,true,.2,.05,1);assertTrue(damage>0,"share was spent before the boss died");tracker.accepted(s,mob,player,damage,maximum);
            if(i%3!=0)health.set((double)Math.max(0,(float)(health.get()-(float)damage)));tracker.flush(mob);
        }}
        assertEquals(0,health.get());assertEquals(maximum,s.contribution.get(id).get(playerId),Rules.healthEpsilon(maximum));
    }
    @Test void aFullHalfShareDoesNotKillAHalfHealthyProductionBoss(){
        MobArmy p=mock(MobArmy.class);LivingEntity mob=mock(LivingEntity.class);UUID id=UUID.randomUUID(),playerId=UUID.randomUUID();when(mob.getUniqueId()).thenReturn(id);AtomicReference<Double> health=new AtomicReference<>(100.0);when(mob.getHealth()).thenAnswer(i->health.get());Player player=mock(Player.class);when(player.getUniqueId()).thenReturn(playerId);Session s=new Session(null,null,playerId);s.contribution.put(id,new HashMap<>());BossDamage tracker=new BossDamage(p);
        try(var bukkit=mockStatic(Bukkit.class)){bukkit.when(Bukkit::getScheduler).thenReturn(mock(BukkitScheduler.class));tracker.accepted(s,mob,player,50,100);health.set(50.0);tracker.flush(mob);}
        assertEquals(50,s.contribution.get(id).get(playerId));verify(mob,never()).setHealth(0);assertEquals(0,Rules.damage(20,100,50,false,true,.2,.05,.5));assertEquals(99.9,Rules.percentUsed(99.999,100));
    }
}
