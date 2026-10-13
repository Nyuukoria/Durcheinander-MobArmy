package de.yuuki.mobarmy;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EquipmentTest {
    @Test void setCountingAcceptsOnlyAuthenticItemsInTheirCorrectArmorSlots(){
        var cfg=new YamlConfiguration();cfg.set("items.boot.set","gale");cfg.set("items.boot.slot","boots");cfg.set("items.helm.set","gale");cfg.set("items.helm.slot","helmet");cfg.set("items.wrong.set","gale");cfg.set("items.wrong.slot","helmet");
        MobArmy p=mock(MobArmy.class);when(p.getConfig()).thenReturn(cfg);p.items=mock(Items.class);
        ItemStack boots=mock(ItemStack.class),legs=mock(ItemStack.class),chest=mock(ItemStack.class),helmet=mock(ItemStack.class);
        when(p.items.id(boots)).thenReturn("boot");when(p.items.id(legs)).thenReturn("wrong");when(p.items.id(helmet)).thenReturn("helm");
        Player player=mock(Player.class);PlayerInventory inv=mock(PlayerInventory.class);when(player.getInventory()).thenReturn(inv);when(inv.getArmorContents()).thenReturn(new ItemStack[]{boots,legs,chest,helmet});
        assertEquals(Map.of("gale",2),new Equipment(p).sets(player));
    }
}
