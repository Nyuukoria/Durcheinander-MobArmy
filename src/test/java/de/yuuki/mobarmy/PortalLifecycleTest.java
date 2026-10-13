package de.yuuki.mobarmy;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.persistence.*;
import org.bukkit.util.Transformation;
import org.joml.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class PortalLifecycleTest {
    void field(Object target,String name,Object value)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);}
    MobArmy plugin()throws Exception{MobArmy p=mock(MobArmy.class);p.state=new YamlConfiguration();p.zone=ZoneId.of("UTC");p.entityKey=NamespacedKey.fromString("mobarmy:raid_entity");when(p.today()).thenReturn(LocalDate.of(2026,10,13));field(p,"portals",new LinkedHashMap<String,Portal>());return p;}
    TextDisplay label(){TextDisplay text=mock(TextDisplay.class);when(text.isValid()).thenReturn(true);when(text.getUniqueId()).thenReturn(UUID.randomUUID());when(text.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));when(text.getTransformation()).thenReturn(new Transformation(new Vector3f(),new Quaternionf(),new Vector3f(1),new Quaternionf()));return text;}
    @Test void labelsStayAbovePortalAndRepeatedTicksDoNotCreateDuplicates()throws Exception{
        MobArmy p=plugin();World world=mock(World.class);when(world.getName()).thenReturn("world");Player player=mock(Player.class);when(player.getUniqueId()).thenReturn(UUID.randomUUID());when(world.getPlayers()).thenReturn(List.of(player));when(world.getNearbyEntities(any(Location.class),anyDouble(),anyDouble(),anyDouble())).thenReturn(List.of());
        TextDisplay de=label(),en=label();Deque<TextDisplay> labels=new ArrayDeque<>(List.of(de,en));when(world.spawn(any(Location.class),eq(TextDisplay.class),any(Consumer.class))).thenAnswer(i->{TextDisplay text=labels.remove();((Consumer<TextDisplay>)i.getArgument(2)).accept(text);return text;});
        Portal q=new Portal(p,"demo_w1","w1",new Region("world",0,64,0,2,67,0),new Location(world,-2,64,0));p.portals.put(q.id,q);
        try(var bukkit=mockStatic(Bukkit.class)){bukkit.when(()->Bukkit.getWorld("world")).thenReturn(world);bukkit.when(()->Bukkit.getPlayer(player.getUniqueId())).thenReturn(player);q.tick();q.tick();}
        verify(world,times(2)).spawn(argThat(at->at.getY()==69),eq(TextDisplay.class),any(Consumer.class));verify(player,times(1)).showEntity(p,de);verify(player,times(1)).hideEntity(p,en);assertEquals(2,q.labels.size());
    }
    @Test void permanentDeleteRemovesStateAndAllLanguageLabelsWithoutTouchingOtherEntities()throws Exception{
        MobArmy p=plugin();World world=mock(World.class);TextDisplay de=label(),en=label();ArmorStand stale=mock(ArmorStand.class),unrelated=mock(ArmorStand.class);for(var e:List.of(stale,unrelated))when(e.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
        when(stale.getPersistentDataContainer().get(p.entityKey,PersistentDataType.STRING)).thenReturn("portal:demo_w1");when(unrelated.getPersistentDataContainer().get(p.entityKey,PersistentDataType.STRING)).thenReturn("other");when(world.getEntities()).thenReturn(List.of(stale,unrelated));
        Portal q=new Portal(p,"demo_w1","w1",new Region("world",0,64,0,2,67,0),new Location(world,-2,64,0));q.labels.put("de",de);q.labels.put("en",en);q.label=de;p.portals.put(q.id,q);q.save(p.state.createSection("portals."+q.id));p.state.set("progress."+q.id+".player",5);
        try(var bukkit=mockStatic(Bukkit.class)){bukkit.when(()->Bukkit.getWorld("world")).thenReturn(world);bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of());new RaidCommand(p).remove(q);}
        assertFalse(p.portals.containsKey(q.id));assertFalse(p.state.contains("portals."+q.id));assertFalse(p.state.contains("progress."+q.id));verify(de,atLeastOnce()).remove();verify(en).remove();verify(stale).remove();verify(unrelated,never()).remove();assertTrue(q.labels.isEmpty());
    }
}
