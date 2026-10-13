package de.yuuki.mobarmy;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RitualRecoveryTest {
    @Test void bothOldSinglePaymentAndNewMultiIngredientJournalAreRecoveredOnce(){
        MobArmy p=mock(MobArmy.class);p.state=new YamlConfiguration();when(p.getConfig()).thenReturn(new YamlConfiguration());
        UUID owner=UUID.randomUUID();ItemStack old=mock(ItemStack.class),shards=mock(ItemStack.class),iron=mock(ItemStack.class);
        p.state.set("pending-rituals.old.owner",owner.toString());p.state.set("pending-rituals.old.item",old);
        p.state.set("pending-rituals.new.owner",owner.toString());p.state.set("pending-rituals.new.items",List.of(shards,iron));
        // First recovery reads no existing mail; cloning recovered mocks on a second pass is stubbed.
        when(old.clone()).thenReturn(old);when(shards.clone()).thenReturn(shards);when(iron.clone()).thenReturn(iron);
        new Items(p);assertNull(p.state.getConfigurationSection("pending-rituals"));assertEquals(3,p.state.getList("mail."+owner).size());
        new Items(p);assertEquals(3,p.state.getList("mail."+owner).size());
    }
}
