package de.yuuki.mobarmy;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class UtilitySkillsTest {
    MobArmy plugin(){MobArmy p=mock(MobArmy.class);p.items=mock(Items.class);when(p.getConfig()).thenReturn(new YamlConfiguration());return p;}
    @Test void ordinaryRightClickIsNeverConvertedIntoASkill(){MobArmy p=plugin();p.getConfig().set("items.rod.skill.type","FISHING");Player pl=mock(Player.class);ItemStack rod=mock(ItemStack.class);when(p.items.id(rod)).thenReturn("rod");assertFalse(new UtilitySkills(p).activate(pl,rod));verify(pl,never()).addPotionEffect(any());}
    @Test void fishingBuffChangesOneAuthenticCastThenLeavesTheNextCastAlone(){
        MobArmy p=plugin();UtilitySkills skills=new UtilitySkills(p);Player pl=mock(Player.class);UUID id=UUID.randomUUID();when(pl.getUniqueId()).thenReturn(id);PlayerInventory inv=mock(PlayerInventory.class);when(pl.getInventory()).thenReturn(inv);ItemStack rod=mock(ItemStack.class);when(inv.getItemInMainHand()).thenReturn(rod);when(p.items.id(rod)).thenReturn("angler_rod");
        PlayerFishEvent e=mock(PlayerFishEvent.class);when(e.getPlayer()).thenReturn(pl);when(e.getState()).thenReturn(PlayerFishEvent.State.FISHING);FishHook hook=mock(FishHook.class);when(e.getHook()).thenReturn(hook);when(hook.getMinWaitTime()).thenReturn(100);when(hook.getMaxWaitTime()).thenReturn(600);
        skills.armed.put(id,new HashMap<>(Map.of("FISHING",System.currentTimeMillis()+15000)));skills.fish(e);skills.fish(e);verify(hook,times(1)).setWaitTime(80,480);assertFalse(skills.active(id,"FISHING"));
    }
    @Test void markAttachesToOneRealArrowAndCannotMakeFireworksAnExtraAttack(){
        MobArmy p=plugin();UtilitySkills skills=new UtilitySkills(p);Player pl=mock(Player.class);UUID id=UUID.randomUUID();when(pl.getUniqueId()).thenReturn(id);ItemStack bow=mock(ItemStack.class);when(p.items.id(bow)).thenReturn("ranger_crossbow");EntityShootBowEvent e=mock(EntityShootBowEvent.class);when(e.getEntity()).thenReturn(pl);when(e.getBow()).thenReturn(bow);
        Firework firework=mock(Firework.class);when(e.getProjectile()).thenReturn(firework);skills.armed.put(id,new HashMap<>(Map.of("MARK",System.currentTimeMillis()+15000)));skills.shoot(e);assertTrue(skills.active(id,"MARK"));verifyNoInteractions(firework);
        AbstractArrow arrow=mock(AbstractArrow.class);PersistentDataContainer data=mock(PersistentDataContainer.class);when(arrow.getPersistentDataContainer()).thenReturn(data);when(e.getProjectile()).thenReturn(arrow);skills.shoot(e);skills.shoot(e);verify(data,times(1)).set(skills.marker,PersistentDataType.BYTE,(byte)1);assertFalse(skills.active(id,"MARK"));
    }
    @Test void vanillaAndOldRaidEquipmentKeepTheirNormalDurabilityBehavior(){
        MobArmy p=plugin();UtilitySkills skills=new UtilitySkills(p);PlayerItemDamageEvent e=mock(PlayerItemDamageEvent.class);ItemStack item=mock(ItemStack.class);when(e.getItem()).thenReturn(item);skills.durability(e);when(p.items.id(item)).thenReturn("warden_blade");skills.durability(e);verify(e,never()).setCancelled(anyBoolean());
    }
}
