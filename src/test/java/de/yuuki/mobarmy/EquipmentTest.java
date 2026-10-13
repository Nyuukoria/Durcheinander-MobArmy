package de.yuuki.mobarmy;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EquipmentTest {
    @Test void heldWeaponAndToolCountWithArmorButDuplicateCopiesAndInventoryDoNot(){
        var cfg=new YamlConfiguration();for(String id:List.of("blade","tool")){cfg.set("items."+id+".gear",true);cfg.set("items."+id+".set","warden");}cfg.set("items.boot.set","warden");cfg.set("items.boot.slot","boots");
        MobArmy p=mock(MobArmy.class);when(p.getConfig()).thenReturn(cfg);p.items=mock(Items.class);ItemStack blade=mock(ItemStack.class),tool=mock(ItemStack.class),boot=mock(ItemStack.class);when(p.items.id(blade)).thenReturn("blade");when(p.items.id(tool)).thenReturn("tool");when(p.items.id(boot)).thenReturn("boot");
        Player pl=mock(Player.class);PlayerInventory inv=mock(PlayerInventory.class);when(pl.getInventory()).thenReturn(inv);when(inv.getArmorContents()).thenReturn(new ItemStack[]{boot,null,null,null});when(inv.getItemInMainHand()).thenReturn(blade);when(inv.getItemInOffHand()).thenReturn(tool);
        Equipment equipment=new Equipment(p);assertEquals(Map.of("warden",3),equipment.sets(pl));when(inv.getItemInOffHand()).thenReturn(blade);assertEquals(Map.of("warden",2),equipment.sets(pl));
    }
    @Test void setCountingAcceptsOnlyAuthenticItemsInTheirCorrectArmorSlots(){
        var cfg=new YamlConfiguration();cfg.set("items.boot.set","gale");cfg.set("items.boot.slot","boots");cfg.set("items.helm.set","gale");cfg.set("items.helm.slot","helmet");cfg.set("items.wrong.set","gale");cfg.set("items.wrong.slot","helmet");
        MobArmy p=mock(MobArmy.class);when(p.getConfig()).thenReturn(cfg);p.items=mock(Items.class);
        ItemStack boots=mock(ItemStack.class),legs=mock(ItemStack.class),chest=mock(ItemStack.class),helmet=mock(ItemStack.class);
        when(p.items.id(boots)).thenReturn("boot");when(p.items.id(legs)).thenReturn("wrong");when(p.items.id(helmet)).thenReturn("helm");
        Player player=mock(Player.class);PlayerInventory inv=mock(PlayerInventory.class);when(player.getInventory()).thenReturn(inv);when(inv.getArmorContents()).thenReturn(new ItemStack[]{boots,legs,chest,helmet});
        assertEquals(Map.of("gale",2),new Equipment(p).sets(player));
    }
}
