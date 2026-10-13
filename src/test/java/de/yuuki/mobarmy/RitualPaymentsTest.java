package de.yuuki.mobarmy;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RitualPaymentsTest {
    ItemStack stack(Material material,int amount){ItemStack item=mock(ItemStack.class);when(item.getType()).thenReturn(material);when(item.getAmount()).thenReturn(amount);return item;}
    @Test void renamedVanillaItemsCannotPayForCustomCurrencyAndMissingInputsChangeNothing(){
        ItemStack fake=stack(Material.PRISMARINE_SHARD,64),real=stack(Material.PRISMARINE_SHARD,20),iron=stack(Material.IRON_INGOT,8);
        var recipe=List.of(new RitualPayments.Requirement("raid_shard",null,24),new RitualPayments.Requirement(null,"IRON_INGOT",8));
        assertNull(RitualPayments.plan(new ItemStack[]{fake,real,iron},recipe,i->i==real?"raid_shard":null));verify(real,never()).setAmount(anyInt());verify(iron,never()).setAmount(anyInt());
    }
    @Test void deductionsAreExactAcrossStacksAndCannotSpendAStackTwice(){
        ItemStack one=stack(Material.PRISMARINE_SHARD,20),two=stack(Material.PRISMARINE_SHARD,10),iron=stack(Material.IRON_INGOT,12);
        var requirements=List.of(new RitualPayments.Requirement("raid_shard",null,24),new RitualPayments.Requirement(null,"IRON_INGOT",8));
        assertEquals(Map.of(0,20,1,4,2,8),RitualPayments.plan(new ItemStack[]{one,two,iron},requirements,i->i==iron?null:"raid_shard"));
        var duplicate=List.of(new RitualPayments.Requirement("raid_shard",null,20),new RitualPayments.Requirement("raid_shard",null,20));assertNull(RitualPayments.plan(new ItemStack[]{one,two},duplicate,i->"raid_shard"));
    }
    @Test void customMaterialsAreNotAcceptedAsVanillaInputs(){ItemStack custom=stack(Material.IRON_INGOT,8);assertNull(RitualPayments.plan(new ItemStack[]{custom},List.of(new RitualPayments.Requirement(null,"IRON_INGOT",8)),i->"custom_iron"));assertThrows(IllegalArgumentException.class,()->new RitualPayments.Requirement(null,null,1));}
}
