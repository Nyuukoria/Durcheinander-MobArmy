package de.yuuki.mobarmy;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.inventory.meta.components.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ItemCooldownsTest {
    @Test void cooldownsRoundUpExpireAndAreBoundToThePlayerAndItemId(){
        MobArmy p=mock(MobArmy.class);p.state=new YamlConfiguration();ItemCooldowns timers=new ItemCooldowns(p);UUID a=UUID.randomUUID(),b=UUID.randomUUID();p.state.set("item-cooldowns."+a+".core",1100);
        assertEquals(100,timers.remaining(a,"core",1000));assertEquals(0,timers.remaining(b,"core",1000));assertEquals(0,timers.remaining(a,"crown",1000));assertEquals(0,timers.remaining(a,"core",1101));
        assertEquals(1,ItemCooldowns.ticks(1));assertEquals(2,ItemCooldowns.ticks(51));assertEquals(0,ItemCooldowns.ticks(0));assertEquals(.5f,ItemCooldowns.progress(500,1000));assertEquals(1,ItemCooldowns.progress(0,1000));
    }
    @Test void relicsUseDistinctNativeGroupsAndUtilitiesDoNotBlockFishingOrShooting(){
        var cfg=new YamlConfiguration();cfg.set("use.cooldown-seconds",60);ItemMeta meta=mock(ItemMeta.class);UseCooldownComponent group=mock(UseCooldownComponent.class);when(meta.getUseCooldown()).thenReturn(group);
        ItemCooldowns.decorate("ritual_core",cfg,meta);verify(group).setCooldownGroup(NamespacedKey.fromString("mobarmy:ability/ritual_core"));verify(group).setCooldownSeconds(60f);verify(meta).setUseCooldown(group);
        cfg.set("utility",true);cfg.set("skill.cooldown-seconds",60);ItemCooldowns.decorate("angler_rod",cfg,meta);verify(meta).setUseCooldown(null);
    }
    @Test void utilityCountdownDoesNotSetANativeCooldownAndUpdatesItsGuiMeter(){
        MobArmy p=mock(MobArmy.class);p.state=new YamlConfiguration();var cfg=new YamlConfiguration();cfg.set("items.rod.utility",true);cfg.set("items.rod.skill.cooldown-seconds",60);cfg.set("items.rod.lore",List.of("Rod",""));when(p.getConfig()).thenReturn(cfg);when(p.tr(any(),eq("skill.remaining"),any(Object[].class))).thenReturn("Ready soon");p.items=mock(Items.class);
        Player player=mock(Player.class);UUID owner=UUID.randomUUID();when(player.getUniqueId()).thenReturn(owner);PlayerInventory inv=mock(PlayerInventory.class);when(player.getInventory()).thenReturn(inv);when(inv.getSize()).thenReturn(1);
        ItemStack item=mock(ItemStack.class);ItemMeta meta=mock(ItemMeta.class);CustomModelDataComponent model=mock(CustomModelDataComponent.class);when(inv.getItem(0)).thenReturn(item);when(p.items.id(item)).thenReturn("rod");when(item.getItemMeta()).thenReturn(meta);when(meta.getCustomModelDataComponent()).thenReturn(model);
        p.state.set("item-cooldowns."+owner+".rod",System.currentTimeMillis()+30000);new ItemCooldowns(p).sync(player);verify(model).setFlags(List.of(true));verify(model).setFloats(argThat(f->f.getFirst()>.49f&&f.getFirst()<.51f));verify(player,never()).setCooldown(any(ItemStack.class),anyInt());
        p.state.set("item-cooldowns."+owner+".rod",0);new ItemCooldowns(p).sync(player);verify(model).setFlags(List.of(false));verify(model).setFloats(List.of(1f));
    }
    @Test void normalVanillaItemsAreNeverDecoratedOrPutOnSkillCooldown(){
        ItemMeta meta=mock(ItemMeta.class);ItemCooldowns.decorate("plain",new YamlConfiguration(),meta);verifyNoInteractions(meta);assertEquals(.1,UtilitySkills.durabilityChance(false));assertEquals(.35,UtilitySkills.durabilityChance(true));
    }
    @Test void meterUpdatesNeverInterruptAnActiveVanillaCharge(){
        MobArmy p=mock(MobArmy.class);p.state=new YamlConfiguration();var cfg=new YamlConfiguration();cfg.set("items.bow.utility",true);cfg.set("items.bow.skill.cooldown-seconds",45);when(p.getConfig()).thenReturn(cfg);p.items=mock(Items.class);
        Player pl=mock(Player.class);PlayerInventory inv=mock(PlayerInventory.class);when(pl.getInventory()).thenReturn(inv);when(inv.getSize()).thenReturn(1);ItemStack bow=mock(ItemStack.class);when(inv.getItem(0)).thenReturn(bow);when(p.items.id(bow)).thenReturn("bow");when(pl.isHandRaised()).thenReturn(true);
        new ItemCooldowns(p).sync(pl);verify(bow,never()).setItemMeta(any());verify(bow,never()).getItemMeta();verify(pl,never()).setCooldown(any(ItemStack.class),anyInt());
        when(pl.isHandRaised()).thenReturn(false);p.utilitySkills=new UtilitySkills(p);p.utilitySkills.mining.add(pl.getUniqueId());new ItemCooldowns(p).sync(pl);verify(bow,never()).getItemMeta();
    }
}
