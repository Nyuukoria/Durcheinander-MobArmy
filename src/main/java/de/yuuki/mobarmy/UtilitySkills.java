package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.*;
import java.util.*;

final class UtilitySkills implements Listener {
    final MobArmy p;
    final Map<UUID,Map<String,Long>> armed=new HashMap<>();
    final Set<UUID> mining=new HashSet<>();
    final NamespacedKey marker=new NamespacedKey("mobarmy","ranger_mark");
    UtilitySkills(MobArmy p){this.p=p;}
    boolean activate(Player player,ItemStack item){
        String id=p.items.id(item);var cfg=id==null?null:p.getConfig().getConfigurationSection("items."+id+".skill");
        if(cfg==null||!player.isSneaking())return false;
        long left=p.itemCooldowns.remaining(player.getUniqueId(),id,System.currentTimeMillis());if(left>0){p.say(player,"relic.cooldown","time",Rules.clock((left+999)/1000));return true;}
        String type=cfg.getString("type","");
        if(type.equals("SOIL")){
            Block b=player.getTargetBlockExact(5);if(b==null){p.say(player,"skill.soil-target");return true;}
            Farmland soil=b.getBlockData() instanceof Farmland f?f:b.getRelative(0,-1,0).getBlockData() instanceof Farmland f?f:null;
            if(soil==null){p.say(player,"skill.soil-target");return true;}
            String growth=b.getBlockData() instanceof Ageable crop?Math.round(100.0*crop.getAge()/crop.getMaximumAge())+"%":p.tr(player,"skill.no-crop");
            p.say(player,"skill.soil","moisture",soil.getMoisture(),"maximum",soil.getMaximumMoisture(),"growth",growth);
        }
        for(var row:cfg.getMapList("effects")){PotionEffectType effect=PotionEffectType.getByName(row.get("type").toString());if(effect!=null)player.addPotionEffect(new PotionEffect(effect,((Number)row.get("seconds")).intValue()*20,((Number)row.get("amplifier")).intValue(),true,false,true));}
        if(Set.of("SHEARS","FISHING","MARK").contains(type))armed.computeIfAbsent(player.getUniqueId(),ignored->new HashMap<>()).put(type,System.currentTimeMillis()+cfg.getInt("duration-seconds",15)*1000L);
        p.itemCooldowns.start(player,item,id,cfg.getLong("cooldown-seconds",60));
        player.getWorld().spawnParticle(Particle.ENCHANT,player.getLocation().add(0,1,0),14,.3,.4,.3,.02);player.playSound(player.getLocation(),Sound.BLOCK_AMETHYST_BLOCK_CHIME,.45f,1.5f);
        p.say(player,"skill.activated","name",cfg.getString(p.language.code(player).equals("en")?"name-en":"name",id));return true;
    }
    boolean active(UUID player,String type){return armed.getOrDefault(player,Map.of()).getOrDefault(type,0L)>System.currentTimeMillis();}
    void consume(UUID player,String type){Map<String,Long> skills=armed.get(player);if(skills!=null)skills.remove(type);}
    static double durabilityChance(boolean sharpened){return sharpened?.35:.10;}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void digging(BlockDamageEvent e){String id=p.items.id(e.getItemInHand());if(id!=null&&p.getConfig().getBoolean("items."+id+".utility")&&!e.getInstaBreak())mining.add(e.getPlayer().getUniqueId());}
    @EventHandler(priority=EventPriority.MONITOR) public void diggingStopped(BlockDamageAbortEvent e){mining.remove(e.getPlayer().getUniqueId());}
    @EventHandler(priority=EventPriority.MONITOR) public void dug(BlockBreakEvent e){mining.remove(e.getPlayer().getUniqueId());}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void selected(PlayerItemHeldEvent e){mining.remove(e.getPlayer().getUniqueId());}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void teleported(PlayerTeleportEvent e){mining.remove(e.getPlayer().getUniqueId());}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void durability(PlayerItemDamageEvent e){
        String id=p.items.id(e.getItem());if(id==null||!p.getConfig().getBoolean("items."+id+".utility"))return;
        if(Math.random()<durabilityChance(id.equals("harvest_shears")&&active(e.getPlayer().getUniqueId(),"SHEARS")))e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void fish(PlayerFishEvent e){
        if(e.getState()!=PlayerFishEvent.State.FISHING||!active(e.getPlayer().getUniqueId(),"FISHING"))return;
        ItemStack rod=e.getPlayer().getInventory().getItemInMainHand();if(!"angler_rod".equals(p.items.id(rod)))return;
        // Change one cast's wait range; vanilla weather, lure, loot and manual reeling stay active.
        FishHook hook=e.getHook();hook.setWaitTime(Math.max(1,(int)(hook.getMinWaitTime()*.8)),Math.max(1,(int)(hook.getMaxWaitTime()*.8)));consume(e.getPlayer().getUniqueId(),"FISHING");
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void shoot(EntityShootBowEvent e){
        if(!(e.getEntity() instanceof Player player)||!"ranger_crossbow".equals(p.items.id(e.getBow()))||!active(player.getUniqueId(),"MARK"))return;
        if(!(e.getProjectile() instanceof AbstractArrow))return;
        e.getProjectile().getPersistentDataContainer().set(marker,PersistentDataType.BYTE,(byte)1);consume(player.getUniqueId(),"MARK");
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void hit(ProjectileHitEvent e){
        if(e.getEntity().getPersistentDataContainer().has(marker)&&e.getHitEntity() instanceof LivingEntity mob&&!(mob instanceof Player)&&(mob instanceof Enemy||p.engine!=null&&p.engine.find(mob.getUniqueId())!=null||p.ritualSystem!=null&&p.ritualSystem.guardians.containsKey(mob.getUniqueId())))mob.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,120,0,true,false,true));
    }
    @EventHandler public void quit(PlayerQuitEvent e){armed.remove(e.getPlayer().getUniqueId());mining.remove(e.getPlayer().getUniqueId());}
    @EventHandler public void death(PlayerDeathEvent e){armed.remove(e.getEntity().getUniqueId());mining.remove(e.getEntity().getUniqueId());}
}
