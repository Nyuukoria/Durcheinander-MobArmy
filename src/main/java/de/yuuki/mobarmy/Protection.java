package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import java.util.*;

final class Protection implements Listener {
    final MobArmy p;
    final Map<UUID,Location> deathReturns=new HashMap<>();
    Protection(MobArmy p){this.p=p;}
    Entity source(Entity e){if(e instanceof Projectile pr&&pr.getShooter() instanceof Entity owner)return owner;if(e instanceof AreaEffectCloud cloud&&cloud.getSource() instanceof Entity owner)return owner;if(e instanceof TNTPrimed tnt&&tnt.getSource()!=null)return tnt.getSource();return e;}
    @EventHandler public void worldLoaded(org.bukkit.event.world.WorldLoadEvent e){if(p.getConfig().getBoolean("testing.disable-pvp"))e.getWorld().setPVP(false);}
    boolean raid(World w){return p.arena(w)!=null;}
    @EventHandler(ignoreCancelled=true) public void move(PlayerMoveEvent e){
        if(e.getTo()==null)return;Player player=e.getPlayer();Session s=p.session(player.getUniqueId());
        if(s!=null&&s.phase==Session.Phase.FIGHT&&!s.arena.bounds.contains(e.getTo())){e.setTo(e.getFrom());return;}
        if(s==null&&!raid(player.getWorld())){Portal portal=p.portalAt(e.getTo());if(portal!=null)p.engine.join(player,portal);}
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void teleport(PlayerTeleportEvent e){
        if(e.getTo()==null)return;Player player=e.getPlayer();Session s=p.session(player.getUniqueId());Arena destination=p.arena(e.getTo().getWorld());
        if(e.getCause()==PlayerTeleportEvent.TeleportCause.ENDER_PEARL){
            Portal portal=p.portalAt(e.getTo());if(portal!=null){e.setCancelled(true);return;}
            if(s!=null){long now=System.currentTimeMillis();Location to=e.getTo();
                if(s.phase!=Session.Phase.FIGHT||!s.arena.bounds.contains(to)||!to.getBlock().isPassable()||!to.clone().add(0,1,0).getBlock().isPassable()||!to.clone().subtract(0,1,0).getBlock().getType().isSolid()||now-s.pearlTimes.getOrDefault(player.getUniqueId(),0L)<p.getConfig().getLong("balance.pearl-cooldown-seconds",10)*1000){e.setCancelled(true);return;}
                s.pearlTimes.put(player.getUniqueId(),now);
            }
        }
        if(destination!=null&&s==null&&!p.commands.builders.contains(player.getUniqueId())){e.setCancelled(true);return;}
        if(s!=null&&s.phase==Session.Phase.FIGHT&&destination!=null&&!s.arena.bounds.contains(e.getTo()))e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void teleported(PlayerTeleportEvent e){
        Session s=p.session(e.getPlayer().getUniqueId());if(s!=null&&e.getTo()!=null&&!e.getTo().getWorld().equals(s.arena.world))Bukkit.getScheduler().runTask(p,()->{if(p.session(e.getPlayer().getUniqueId())==s)p.engine.leave(e.getPlayer(),false);});
    }
    @EventHandler(ignoreCancelled=true) public void vanillaPortal(PlayerPortalEvent e){if(p.portalAt(e.getFrom())!=null||raid(e.getFrom().getWorld()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void pearlHit(ProjectileHitEvent e){if(e.getEntity() instanceof EnderPearl&&p.portalAt(e.getEntity().getLocation())!=null){e.setCancelled(true);e.getEntity().remove();}}
    @EventHandler public void login(PlayerJoinEvent e){Bukkit.getScheduler().runTask(p,()->{p.recover(e.getPlayer());if(raid(e.getPlayer().getWorld())&&p.session(e.getPlayer().getUniqueId())==null)e.getPlayer().teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());});}
    @EventHandler public void quit(PlayerQuitEvent e){p.commands.builders.remove(e.getPlayer().getUniqueId());Session s=p.session(e.getPlayer().getUniqueId());if(s!=null){Location exit=s.portal.exit.clone();p.engine.leave(e.getPlayer(),false);p.returns.put(e.getPlayer().getUniqueId(),exit);p.saveState();}}
    @EventHandler(priority=EventPriority.HIGHEST) public void death(PlayerDeathEvent e){
        if(!raid(e.getEntity().getWorld()))return;e.setKeepInventory(true);e.setKeepLevel(true);e.setDroppedExp(0);e.getDrops().clear();e.setDeathMessage(null);
        Session s=p.session(e.getEntity().getUniqueId());Location exit=s==null?Bukkit.getWorlds().getFirst().getSpawnLocation():s.portal.exit.clone();deathReturns.put(e.getEntity().getUniqueId(),exit);p.engine.leave(e.getEntity(),true);
    }
    @EventHandler public void respawn(PlayerRespawnEvent e){Location l=deathReturns.remove(e.getPlayer().getUniqueId());if(l==null)l=p.returns.get(e.getPlayer().getUniqueId());if(l!=null){e.setRespawnLocation(l);p.returns.remove(e.getPlayer().getUniqueId());p.saveState();}}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void damage(EntityDamageEvent e){
        Session victim=p.engine.find(e.getEntity().getUniqueId());
        if(raid(e.getEntity().getWorld())&&(e.getCause()==EntityDamageEvent.DamageCause.BLOCK_EXPLOSION||e.getCause()==EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)){e.setCancelled(true);return;}
        if(victim!=null&&!(e instanceof EntityDamageByEntityEvent)){e.setCancelled(true);return;}
        if(!(e instanceof EntityDamageByEntityEvent by))return;Entity attacker=source(by.getDamager());
        if((raid(e.getEntity().getWorld())||p.getConfig().getBoolean("testing.disable-pvp"))&&attacker instanceof Player&&e.getEntity() instanceof Player){e.setCancelled(true);return;}
        if(victim!=null){
            if(!(attacker instanceof Player player)||!victim.members.contains(player.getUniqueId())||victim.phase!=Session.Phase.FIGHT){e.setCancelled(true);return;}
            LivingEntity mob=(LivingEntity)e.getEntity();boolean boss=victim.contribution.containsKey(mob.getUniqueId());
            boolean mace=by.getDamager() instanceof Player&&player.getInventory().getItemInMainHand().getType()==Material.MACE;
            double dealt=boss?victim.contribution.get(mob.getUniqueId()).getOrDefault(player.getUniqueId(),0.0):0;
            double share=Math.max(p.getConfig().getDouble("balance.boss-player-share",0.4),1.0/Math.max(2,victim.stagePlayers));
            if(p.getConfig().getBoolean("testing.solo"))share=1;
            double cap=Rules.damage(e.getFinalDamage(),mob.getAttribute(Attribute.MAX_HEALTH).getValue(),dealt,mace,boss,p.getConfig().getDouble("balance.hit-cap",0.2),p.getConfig().getDouble("balance.mace-cap",0.05),share);
            if(cap<=0){e.setCancelled(true);return;}
            // Bukkit re-evaluates armor modifiers after setDamage. Binary search caps FINAL damage.
            if(e.getFinalDamage()>cap){double lo=0,hi=e.getDamage();for(int i=0;i<24;i++){double mid=(lo+hi)/2;e.setDamage(mid);if(e.getFinalDamage()>cap)hi=mid;else lo=mid;}e.setDamage(lo);}
        }
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void contributed(EntityDamageByEntityEvent e){Session s=p.engine.find(e.getEntity().getUniqueId());Entity attacker=source(e.getDamager());if(s!=null&&attacker instanceof Player player&&s.contribution.containsKey(e.getEntity().getUniqueId())){double actual=Math.min(e.getFinalDamage(),((LivingEntity)e.getEntity()).getHealth());s.contribution.get(e.getEntity().getUniqueId()).merge(player.getUniqueId(),actual,Double::sum);}}
    @EventHandler(priority=EventPriority.HIGHEST) public void mobDeath(EntityDeathEvent e){if(p.engine.find(e.getEntity().getUniqueId())!=null||p.engine.rituals.containsKey(e.getEntity().getUniqueId())){e.setDroppedExp(0);p.engine.killed(e.getEntity(),e.getDrops());}}
    @EventHandler(ignoreCancelled=true) public void itemSpawn(ItemSpawnEvent e){Arena a=p.arena(e.getEntity().getWorld());if(a==null)return;for(Portal portal:p.portals.values())if(portal.session!=null&&portal.session.arena==a)portal.session.drops.add(e.getEntity().getUniqueId());}
    @EventHandler(ignoreCancelled=true) public void target(EntityTargetLivingEntityEvent e){Session s=p.engine.find(e.getEntity().getUniqueId());if(s!=null&&(!(e.getTarget() instanceof Player player)||!s.members.contains(player.getUniqueId())))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void launch(ProjectileLaunchEvent e){
        if(!raid(e.getEntity().getWorld()))return;
        if(e.getEntity() instanceof WindCharge||e.getEntity() instanceof Firework){e.setCancelled(true);return;}
        if(e.getEntity() instanceof AbstractArrow arrow&&arrow.getShooter() instanceof Mob mob&&p.engine.find(mob.getUniqueId())!=null&&mob.getAttribute(Attribute.ATTACK_DAMAGE)!=null)arrow.setDamage(mob.getAttribute(Attribute.ATTACK_DAMAGE).getValue()/2);
        if(e.getEntity() instanceof ThrownPotion potion&&potion.getShooter() instanceof Player player&&!use(player,"potion",p.getConfig().getInt("balance.potions-per-stage",3)))e.setCancelled(true);
    }
    @EventHandler(ignoreCancelled=true) public void interact(PlayerInteractEvent e){
        Player player=e.getPlayer();ItemStack item=e.getItem();
        if(e.getHand()==EquipmentSlot.HAND&&e.getAction()==Action.RIGHT_CLICK_BLOCK&&e.getClickedBlock()!=null&&e.getClickedBlock().getType().name().endsWith("CAULDRON")&&p.items.ritual(player,e.getClickedBlock(),item)){e.setCancelled(true);return;}
        if(!raid(player.getWorld()))return;
        if(item!=null&&(item.getType()==Material.WIND_CHARGE||item.getType()==Material.FIREWORK_ROCKET||item.getType()==Material.END_CRYSTAL||item.getType()==Material.TNT||item.getType()==Material.TNT_MINECART))e.setCancelled(true);
        if(e.getClickedBlock()!=null&&e.getClickedBlock().getType()==Material.RESPAWN_ANCHOR)e.setCancelled(true);
    }
    @EventHandler(ignoreCancelled=true) public void glide(EntityToggleGlideEvent e){if(e.isGliding()&&raid(e.getEntity().getWorld()))e.setCancelled(true);}
    boolean use(Player player,String kind,int limit){Session s=p.session(player.getUniqueId());if(s==null||!s.active())return true;Map<String,Integer> uses=s.uses.computeIfAbsent(player.getUniqueId(),id->new HashMap<>());if(uses.getOrDefault(kind,0)>=limit){p.tell(player,"Limit pro Stage erreicht: "+kind);return false;}uses.merge(kind,1,Integer::sum);return true;}
    @EventHandler(ignoreCancelled=true,priority=EventPriority.HIGHEST) public void consume(PlayerItemConsumeEvent e){
        Session s=p.session(e.getPlayer().getUniqueId());if(s==null||!s.active())return;Material m=e.getItem().getType();
        if(m==Material.GOLDEN_APPLE||m==Material.ENCHANTED_GOLDEN_APPLE){long now=System.currentTimeMillis();UUID id=e.getPlayer().getUniqueId();if(now-s.appleTimes.getOrDefault(id,0L)<p.getConfig().getLong("balance.apple-cooldown-seconds",30)*1000||!use(e.getPlayer(),"apple",p.getConfig().getInt("balance.apples-per-stage",3))){e.setCancelled(true);return;}s.appleTimes.put(id,now);}
        if(m==Material.POTION&&!use(e.getPlayer(),"potion",p.getConfig().getInt("balance.potions-per-stage",3)))e.setCancelled(true);
    }
    @EventHandler(ignoreCancelled=true,priority=EventPriority.HIGHEST) public void resurrect(EntityResurrectEvent e){if(e.getEntity() instanceof Player player&&!use(player,"totem",p.getConfig().getInt("balance.totems-per-stage",1)))e.setCancelled(true);}
    boolean builder(Player player){return p.commands.builders.contains(player.getUniqueId())&&p.session(player.getUniqueId())==null;}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void place(BlockPlaceEvent e){
        Arena a=p.arena(e.getBlock().getWorld());if(a==null||builder(e.getPlayer()))return;Session s=p.session(e.getPlayer().getUniqueId());
        int height=e.getBlock().getY()-a.bounds.y1()+1;
        if(s==null||!a.bounds.contains(e.getBlock().getLocation())||a.door.contains(e.getBlock().getLocation())||height<1||height>p.getConfig().getInt("balance.maximum-build-height",2)||EnumSet.of(Material.TNT,Material.RESPAWN_ANCHOR,Material.FIRE,Material.LAVA,Material.WATER).contains(e.getBlock().getType())){e.setCancelled(true);return;}
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void placed(BlockPlaceEvent e){
        if(!raid(e.getBlock().getWorld())||builder(e.getPlayer()))return;Session s=p.session(e.getPlayer().getUniqueId());if(s==null)return;
        s.blocks.put(e.getBlock().getLocation(),e.getPlayer().getUniqueId());s.blockOriginal.putIfAbsent(e.getBlock().getLocation(),e.getBlockReplacedState().getBlockData());p.saveState();
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void breakBlock(BlockBreakEvent e){
        if(!raid(e.getBlock().getWorld())||builder(e.getPlayer()))return;Session s=p.session(e.getPlayer().getUniqueId());Location l=e.getBlock().getLocation();
        if(s==null||!e.getPlayer().getUniqueId().equals(s.blocks.get(l))){e.setCancelled(true);return;}
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void broken(BlockBreakEvent e){Session s=p.session(e.getPlayer().getUniqueId());if(s!=null){s.blocks.remove(e.getBlock().getLocation());s.blockOriginal.remove(e.getBlock().getLocation());p.saveState();}}
    @EventHandler(ignoreCancelled=true) public void explode(EntityExplodeEvent e){if(raid(e.getLocation().getWorld())){e.blockList().clear();e.setCancelled(true);}}
    @EventHandler(ignoreCancelled=true) public void explode(BlockExplodeEvent e){if(raid(e.getBlock().getWorld())){e.blockList().clear();e.setCancelled(true);}}
    @EventHandler(ignoreCancelled=true) public void burn(BlockBurnEvent e){if(raid(e.getBlock().getWorld()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void ignite(BlockIgniteEvent e){if(raid(e.getBlock().getWorld()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void flow(BlockFromToEvent e){if(raid(e.getBlock().getWorld()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void bucket(PlayerBucketEmptyEvent e){if(raid(e.getPlayer().getWorld())&&!builder(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void bucket(PlayerBucketFillEvent e){if(raid(e.getPlayer().getWorld())&&!builder(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void piston(BlockPistonExtendEvent e){if(raid(e.getBlock().getWorld()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void piston(BlockPistonRetractEvent e){if(raid(e.getBlock().getWorld()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void change(EntityChangeBlockEvent e){if(raid(e.getBlock().getWorld()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void spawn(CreatureSpawnEvent e){if(raid(e.getLocation().getWorld())&&e.getSpawnReason()!=CreatureSpawnEvent.SpawnReason.CUSTOM)e.setCancelled(true);}
}
