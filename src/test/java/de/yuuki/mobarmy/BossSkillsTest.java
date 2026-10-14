package de.yuuki.mobarmy;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class BossSkillsTest {
    @Test void ringHasSafeCenterAndOutsideAndChargeHasSideAndRearEscape(){assertFalse(BossSkills.area("SHOCKWAVE",0,0,0,0,1));assertTrue(BossSkills.area("SHOCKWAVE",3,0,0,0,1));assertFalse(BossSkills.area("SHOCKWAVE",7.1,0,0,0,1));assertTrue(BossSkills.area("CHARGE",0,0,6,0,1));assertFalse(BossSkills.area("CHARGE",0,0,-1,0,1));assertFalse(BossSkills.area("CHARGE",1.3,0,5,0,1));assertFalse(BossSkills.area("CHARGE",0,4,5,0,1));}
    @Test void configuredBossesHaveDistinctSupportedRotations()throws Exception{var cfg=YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(getClass().getResourceAsStream("/config.yml")));Set<List<String>> rotations=new HashSet<>();for(String id:cfg.getConfigurationSection("boss-types").getKeys(false)){var skills=cfg.getStringList("boss-types."+id+".skills");assertEquals(3,skills.size(),id);assertTrue(BossSkills.TYPES.containsAll(skills),id);rotations.add(skills);}assertTrue(rotations.size()>=7);}
    @Test void summonedAddsDoNotDropVanillaOrCustomLootButOrdinaryMobsStillDo(){MobArmy p=mock(MobArmy.class);when(p.getConfig()).thenReturn(new YamlConfiguration());p.items=mock(Items.class);p.bossSkills=new BossSkills(p);Portal portal=mock(Portal.class);when(portal.rewards()).thenReturn(true);Arena a=mock(Arena.class);Session s=new Session(portal,a,UUID.randomUUID());portal.session=s;s.phase=Session.Phase.FIGHT;Engine engine=new Engine(p);try{var field=MobArmy.class.getDeclaredField("portals");field.setAccessible(true);field.set(p,new LinkedHashMap<>(Map.of("custom",portal)));}catch(Exception e){throw new RuntimeException(e);}LivingEntity add=mock(LivingEntity.class),ordinary=mock(LivingEntity.class);UUID id=UUID.randomUUID(),other=UUID.randomUUID();when(add.getUniqueId()).thenReturn(id);when(ordinary.getUniqueId()).thenReturn(other);s.mobs.addAll(List.of(id,other,UUID.randomUUID()));p.bossSkills.adds.add(id);var drops=new ArrayList<ItemStack>(List.of(mock(ItemStack.class)));engine.killed(add,drops);assertTrue(drops.isEmpty());verify(p.items,never()).drops(any(),anyInt(),any());assertFalse(s.mobs.contains(id));assertFalse(p.bossSkills.adds.contains(id));when(p.items.drops(any(),anyInt(),any())).thenReturn(List.of(mock(ItemStack.class)));engine.killed(ordinary,drops);assertEquals(1,drops.size());}
}
