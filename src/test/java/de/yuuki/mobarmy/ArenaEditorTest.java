package de.yuuki.mobarmy;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ArenaEditorTest {
    MobArmy plugin(){MobArmy p=mock(MobArmy.class);p.state=new YamlConfiguration();p.commands=new RaidCommand(p);p.items=mock(Items.class);return p;}
    @Test void restoreReturnsOriginalInventoryModeAndSlotExactlyOnce(){MobArmy p=plugin();ArenaEditor editor=new ArenaEditor(p);Player pl=mock(Player.class);UUID id=UUID.randomUUID();when(pl.getUniqueId()).thenReturn(id);PlayerInventory inv=mock(PlayerInventory.class);when(pl.getInventory()).thenReturn(inv);when(inv.getSize()).thenReturn(41);ItemStack saved=mock(ItemStack.class),copy=mock(ItemStack.class);when(saved.clone()).thenReturn(copy);String path="editor-returns."+id;p.state.set(path+".items",Arrays.asList(saved,null));p.state.set(path+".mode","SURVIVAL");p.state.set(path+".slot",4);p.commands.builders.add(id);assertTrue(editor.recover(pl));assertTrue(editor.recover(pl));verify(inv,times(1)).setContents(argThat(items->items.length==41&&items[0]==copy&&items[1]==null));verify(inv).setHeldItemSlot(4);verify(pl).setGameMode(GameMode.SURVIVAL);verify(pl,times(1)).saveData();assertFalse(p.commands.builders.contains(id));assertFalse(p.state.contains(path));}
    @Test void failedTeleportPreservesReturnSnapshotAndNeverOverwritesInventory(){MobArmy p=plugin();ArenaEditor editor=new ArenaEditor(p);Player pl=mock(Player.class);UUID id=UUID.randomUUID();when(pl.getUniqueId()).thenReturn(id);World world=mock(World.class);String path="editor-returns."+id;p.state.set(path+".location",new Location(world,1,64,1));assertFalse(editor.recover(pl));verify(pl,never()).getInventory();assertTrue(p.state.contains(path));assertTrue(editor.transfers.isEmpty());}
    @Test void newArenaNamesCannotEscapeDataOrWorldDirectories(){assertTrue(ArenaEditor.validId("forest_ruins"));assertFalse(ArenaEditor.validId("../world"));assertFalse(ArenaEditor.validId("x/y"));assertFalse(ArenaEditor.validId(""));assertFalse(ArenaEditor.validId("a".repeat(33)));}
}
