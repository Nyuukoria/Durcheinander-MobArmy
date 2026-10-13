package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.util.Transformation;
import org.joml.*;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import java.lang.reflect.Field;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EngineFlowTest {
    void field(Object target,String name,Object value)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);}
    @Test void joinDoesNotOpenGuiAndReadyTeleportsWholePartyFromSeparateLobby()throws Exception{
        MobArmy p=mock(MobArmy.class);Arena a=mock(Arena.class);World source=mock(World.class),dungeon=mock(World.class);
        when(source.getName()).thenReturn("world");when(dungeon.getName()).thenReturn("raid_w1");
        YamlConfiguration cfg=new YamlConfiguration();cfg.set("presentation.entry-countdown-seconds",0);when(p.getConfig()).thenReturn(cfg);when(p.today()).thenReturn(LocalDate.of(2026,10,13));p.zone=ZoneId.of("Europe/Berlin");p.state=new YamlConfiguration();
        field(p,"arenas",new LinkedHashMap<>(Map.of("w1",a)));field(p,"portals",new LinkedHashMap<String,Portal>());field(p,"returns",new HashMap<UUID,Location>());
        field(a,"world",dungeon);field(a,"lobbySpawn",new Location(dungeon,88.5,64,.5));field(a,"bounds",new Region("raid_w1",-23,64,-23,23,75,23));when(a.minimum()).thenReturn(2);when(a.entryPoint(anyInt())).thenReturn(new Location(dungeon,.5,64,-20.5));
        p.menus=mock(Menus.class);p.presentation=mock(Presentation.class);Engine engine=new Engine(p);p.engine=engine;
        Portal portal=new Portal(p,"demo_w1","w1",new Region("world",0,64,0,2,67,0),new Location(source,-2,64,0));portal.open=true;p.portals.put(portal.id,portal);
        TextDisplay text=mock(TextDisplay.class);when(text.isValid()).thenReturn(true);when(text.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));when(text.getTransformation()).thenReturn(new Transformation(new Vector3f(),new Quaternionf(),new Vector3f(1),new Quaternionf()));when(source.spawn(any(Location.class),eq(TextDisplay.class))).thenReturn(text);when(source.spawn(any(Location.class),eq(TextDisplay.class),any(java.util.function.Consumer.class))).thenReturn(text);when(source.getNearbyEntities(any(Location.class),anyDouble(),anyDouble(),anyDouble())).thenReturn(List.of());
        Player first=mock(Player.class),second=mock(Player.class);AtomicReference<Location> one=new AtomicReference<>(new Location(source,-2,64,0)),two=new AtomicReference<>(new Location(source,-2,64,0));
        Map<UUID,Player> players=new HashMap<>();for(var entry:List.of(Map.entry(first,one),Map.entry(second,two))){Player player=entry.getKey();UUID id=UUID.randomUUID();players.put(id,player);when(player.getUniqueId()).thenReturn(id);when(player.hasPermission("mobarmy.play")).thenReturn(true);when(player.getLocation()).thenAnswer(i->entry.getValue().get());when(player.getWorld()).thenAnswer(i->entry.getValue().get().getWorld());when(player.teleport(any(Location.class))).thenAnswer(i->{entry.getValue().set(i.getArgument(0));return true;});}
        try(MockedStatic<Bukkit> bukkit=mockStatic(Bukkit.class)){
            bukkit.when(()->Bukkit.getWorld("world")).thenReturn(source);bukkit.when(()->Bukkit.getPlayer(any(UUID.class))).thenAnswer(i->players.get(i.getArgument(0)));
            assertTrue(engine.join(first,portal));assertTrue(engine.join(second,portal));
            verify(p.menus,never()).party(any(Player.class),anyInt());verify(p).say(first,"raid.welcome");assertEquals(88.5,one.get().getX());
            Session s=portal.session;when(p.session(first.getUniqueId())).thenReturn(s);engine.ready(first);assertEquals(Session.Phase.ENTERING,s.phase);verify(a,never()).gate(true);
            engine.tick();assertEquals(Session.Phase.FIGHT,s.phase);assertTrue(s.arena.bounds.contains(one.get()));assertTrue(s.arena.bounds.contains(two.get()));verify(a).gate(true);
        }
    }
}
