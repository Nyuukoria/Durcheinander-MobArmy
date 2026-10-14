package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.scheduler.*;
import org.bukkit.util.Transformation;
import org.joml.*;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RitualLifecycleTest {
    static class Fixture implements AutoCloseable {
        final MockedStatic<Bukkit> bukkit=mockStatic(Bukkit.class);
        final MobArmy p=mock(MobArmy.class);final World world=mock(World.class);final Player player=mock(Player.class);final UUID owner=UUID.randomUUID();
        final PlayerInventory inv=mock(PlayerInventory.class);final ItemStack fragment=mock(ItemStack.class),bound=mock(ItemStack.class),reward=mock(ItemStack.class);
        final ItemStack[] storage=new ItemStack[36];final Rituals rituals;final Location at;
        Fixture(){
            p.state=new YamlConfiguration();p.itemKey=NamespacedKey.fromString("mobarmy:custom_item");p.entityKey=NamespacedKey.fromString("mobarmy:raid_entity");p.items=mock(Items.class);p.language=mock(Language.class);p.equipment=mock(Equipment.class);p.presentation=mock(Presentation.class);
            var cfg=new YamlConfiguration();cfg.set("ritual-world","world");cfg.set("rituals.awakening.catalyst","chaos_fragment");cfg.set("rituals.awakening.mode","SUMMON");cfg.set("rituals.awakening.result","ritual_core");cfg.set("rituals.awakening.display-item","ritual_core");when(p.getConfig()).thenReturn(cfg);
            when(world.getName()).thenReturn("world");when(world.getPlayers()).thenReturn(List.of(player));when(player.getWorld()).thenReturn(world);when(player.getLocation()).thenReturn(new Location(world,.5,65,.5));when(player.getUniqueId()).thenReturn(owner);when(player.isOnline()).thenReturn(true);when(player.hasPermission("mobarmy.play")).thenReturn(true);when(player.getInventory()).thenReturn(inv);
            storage[0]=fragment;when(inv.getStorageContents()).thenAnswer(i->storage.clone());when(inv.getItemInMainHand()).thenAnswer(i->storage[0]);when(inv.getItem(anyInt())).thenAnswer(i->storage[i.getArgument(0)]);doAnswer(i->{storage[i.getArgument(0)]=i.getArgument(1);return null;}).when(inv).setItem(anyInt(),any());
            when(fragment.getAmount()).thenReturn(1);when(fragment.getType()).thenReturn(Material.AMETHYST_SHARD);when(fragment.clone()).thenReturn(bound);when(bound.getAmount()).thenReturn(1);when(p.items.id(fragment)).thenReturn("chaos_fragment");when(p.items.make(anyString(),anyInt())).thenReturn(reward);
            when(p.language.recipeName(eq(player),any())).thenReturn("Awakening");when(p.tr(eq(player),anyString(),any(Object[].class))).thenReturn("message");
            Block air=mock(Block.class),stone=mock(Block.class),cauldron=mock(Block.class);when(air.isPassable()).thenReturn(true);when(air.getType()).thenReturn(Material.AIR);Material solid=mock(Material.class);when(solid.isSolid()).thenReturn(true);when(stone.getType()).thenReturn(solid);when(cauldron.getType()).thenReturn(Material.CAULDRON);
            when(world.getBlockAt(any(Location.class))).thenAnswer(i->{Location l=i.getArgument(0);return l.getBlockX()==0&&l.getBlockZ()==0&&l.getBlockY()==64?cauldron:l.getBlockY()<65?stone:air;});
            when(world.spawn(any(Location.class),eq(ItemDisplay.class))).thenAnswer(i->{ItemDisplay d=mock(ItemDisplay.class);when(d.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));when(d.getTransformation()).thenReturn(new Transformation(new Vector3f(),new AxisAngle4f(),new Vector3f(1),new AxisAngle4f()));return d;});
            TextDisplay label=mock(TextDisplay.class);when(label.getTransformation()).thenReturn(new Transformation(new Vector3f(),new AxisAngle4f(),new Vector3f(1),new AxisAngle4f()));when(label.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));when(world.spawn(any(Location.class),eq(TextDisplay.class))).thenReturn(label);
            bukkit.when(()->Bukkit.getPlayer(owner)).thenReturn(player);bukkit.when(()->Bukkit.createBossBar(anyString(),any(BarColor.class),any(BarStyle.class))).thenReturn(mock(BossBar.class));
            BukkitScheduler scheduler=mock(BukkitScheduler.class);BukkitTask task=mock(BukkitTask.class);when(task.getTaskId()).thenReturn(42);when(scheduler.runTaskTimer(eq(p),any(Runnable.class),anyLong(),anyLong())).thenReturn(task);bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            rituals=new Rituals(p);at=new Location(world,0,64,0);
        }
        Rituals.Cast start(){rituals.start(player,at,"awakening");return rituals.casts.values().iterator().next();}
        Mob fight(Rituals.Cast cast,boolean participated){Mob mob=mock(Mob.class);UUID id=UUID.randomUUID();when(mob.getUniqueId()).thenReturn(id);when(mob.getKiller()).thenReturn(player);cast.guardian=mob;cast.ownerHit=participated;rituals.guardians.put(id,new Rituals.Trial(owner,cast.cfg,System.currentTimeMillis()+180000,cast));return mob;}
        public void close(){bukkit.close();}
    }
    @Test void catalystIsEscrowedChannelSlotIsLockedAndAbortReturnsItOnce(){try(Fixture f=new Fixture()){
        Rituals.Cast c=f.start();assertNull(f.storage[0]);assertTrue(f.p.state.getList("pending-rituals."+c.key+".items").contains(f.bound));
        PlayerItemHeldEvent held=new PlayerItemHeldEvent(f.player,0,1);f.rituals.held(held);assertTrue(held.isCancelled());verify(f.p.items,never()).deliver(any(),any());
        c.abort();c.abort();verify(f.p.items,times(1)).store(f.owner,f.bound);assertTrue(f.rituals.casts.isEmpty());assertNull(f.p.state.get("pending-rituals."+c.key));
    }}
    @Test void awakeningWaitsForItsTrialAndWinningReturnsTheBoundFragment(){try(Fixture f=new Fixture()){
        Rituals.Cast c=f.start();Mob guardian=f.fight(c,true);assertNull(f.rituals.channel(f.owner));verify(f.p.items,never()).deliver(any(),any());
        assertTrue(f.rituals.killed(guardian,new ArrayList<>()));verify(f.p.items).deliver(f.owner,f.reward);verify(f.p.items).deliver(f.owner,f.bound);verify(f.p.items,never()).store(any(),any());assertTrue(c.committed);assertTrue(c.done);assertTrue(f.rituals.guardians.isEmpty());
    }}
    @Test void killingSomeoneElsesTrialWithoutOwnerParticipationCannotGrantFreeLoot(){try(Fixture f=new Fixture()){
        Rituals.Cast c=f.start();Mob guardian=f.fight(c,false);f.rituals.killed(guardian,new ArrayList<>());verify(f.p.items,never()).deliver(any(),any());verify(f.p.items).store(f.owner,f.bound);assertFalse(c.committed);
    }}
    @Test void logoutDuringTrialRemovesGuardianAndRefundsEscrow(){try(Fixture f=new Fixture()){
        Rituals.Cast c=f.start();Mob guardian=f.fight(c,true);var event=mock(org.bukkit.event.player.PlayerQuitEvent.class);when(event.getPlayer()).thenReturn(f.player);f.rituals.quit(event);verify(guardian).remove();verify(f.p.items).store(f.owner,f.bound);assertTrue(f.rituals.guardians.isEmpty());
    }}
    @Test void forgeRepairAndBlessingAlsoRequireVictoryBeforeApplyingTheirResults(){
        for(String mode:List.of("FORGE","REPAIR","BUFF"))try(Fixture f=new Fixture()){
            var cfg=f.p.getConfig();cfg.set("rituals.awakening.mode",mode);cfg.set("rituals.awakening.pool","tool");cfg.set("loot-pools.tool.entries",List.of(Map.of("item","silk_pickaxe","weight",1)));when(f.p.equipment.repairSlots(f.player)).thenReturn(List.of(0));when(f.p.equipment.repair(f.player)).thenReturn(1);
            Rituals.Cast cast=f.start();verify(f.p.equipment,never()).repair(any());verify(f.p.items,never()).deliver(any(),any());assertFalse(cast.committed);
            Mob mob=f.fight(cast,true);f.rituals.killed(mob,new ArrayList<>());assertTrue(cast.committed,mode);verify(f.p.items).deliver(f.owner,f.bound);
            if(mode.equals("FORGE"))verify(f.p.items).deliver(f.owner,f.reward);if(mode.equals("REPAIR"))verify(f.p.equipment).repair(f.player);
        }
    }
}
