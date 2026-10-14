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
import org.bukkit.util.Vector;

final class UtilitySkills implements Listener {
    final MobArmy p;
    final Map<UUID,Map<String,Long>> armed=new HashMap<>();
    record Readiness(float value,long time){}
    final Map<UUID,Readiness> readiness=new HashMap<>();
    final Map<UUID,Integer> harvests=new HashMap<>();
    final NamespacedKey arrowSkill=new NamespacedKey("mobarmy","arrow_skill");
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
        if(type.equals("SOIL")||type.equals("HARVEST")){harvests.put(player.getUniqueId(),cfg.getInt("charges",3));armed.computeIfAbsent(player.getUniqueId(),ignored->new HashMap<>()).put("HARVEST",System.currentTimeMillis()+cfg.getInt("duration-seconds",15)*1000L);}
        if(type.equals("DIVE")&&player.isInWater()){Vector v=player.getEyeLocation().getDirection().multiply(.65);v.setY(Math.max(-.2,Math.min(.25,v.getY())));player.setVelocity(v);}
        if(Set.of("SHEARS","FISHING","MARK","PATH","GUARD","SWEEP","PIERCE","SHOCK","WIND","STAR").contains(type))armed.computeIfAbsent(player.getUniqueId(),ignored->new HashMap<>()).put(type,System.currentTimeMillis()+cfg.getInt("duration-seconds",15)*1000L);
        p.itemCooldowns.start(player,item,id,cfg.getLong("cooldown-seconds",60));
        player.getWorld().spawnParticle(Particle.ENCHANT,player.getLocation().add(0,1,0),14,.3,.4,.3,.02);player.playSound(player.getLocation(),Sound.BLOCK_AMETHYST_BLOCK_CHIME,.45f,1.5f);
        p.say(player,"skill.activated","name",cfg.getString(p.language.code(player).equals("en")?"name-en":"name",id));return true;
    }
    boolean active(UUID player,String type){return armed.getOrDefault(player,Map.of()).getOrDefault(type,0L)>System.currentTimeMillis();}
    void consume(UUID player,String type){Map<String,Long> skills=armed.get(player);if(skills!=null)skills.remove(type);}
    static double durabilityChance(boolean sharpened){return sharpened?.50:.10;}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void digging(BlockDamageEvent e){String id=p.items.id(e.getItemInHand());if(id!=null&&p.getConfig().isConfigurationSection("items."+id+".skill")&&!e.getInstaBreak())mining.add(e.getPlayer().getUniqueId());}
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
        FishHook hook=e.getHook();hook.setWaitTime(Math.max(1,(int)(hook.getMinWaitTime()*.65)),Math.max(1,(int)(hook.getMaxWaitTime()*.65)));consume(e.getPlayer().getUniqueId(),"FISHING");
    }
    boolean hostile(Player player,LivingEntity mob){
        if(mob instanceof Player||mob.isDead()||mob.isInvulnerable())return false;
        Session raid=p.engine==null?null:p.engine.find(mob.getUniqueId());if(raid!=null)return raid.phase==Session.Phase.FIGHT&&raid.members.contains(player.getUniqueId());
        return mob instanceof Enemy||p.ritualSystem!=null&&p.ritualSystem.guardians.containsKey(mob.getUniqueId());
    }
    void capture(){for(Player pl:Bukkit.getOnlinePlayers())if(strike(pl.getUniqueId())!=null)readiness.put(pl.getUniqueId(),new Readiness(pl.getAttackCooldown(),System.currentTimeMillis()));}
    boolean ready(Player pl){Readiness r=readiness.get(pl.getUniqueId());return pl.getAttackCooldown()>=.9f||r!=null&&System.currentTimeMillis()-r.time()<150&&r.value()>=.9f;}
    String strike(UUID id){for(String type:List.of("SWEEP","PIERCE","SHOCK"))if(active(id,type))return type;return null;}
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true) public void boost(EntityDamageByEntityEvent e){
        if(e.getDamager() instanceof Player pl&&e.getEntity() instanceof LivingEntity mob&&hostile(pl,mob)&&ready(pl)){String type=strike(pl.getUniqueId());String held=p.items.id(pl.getInventory().getItemInMainHand());if(type!=null&&type.equals(p.getConfig().getString("items."+held+".skill.type")))e.setDamage(e.getDamage()*1.2);}
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void combat(EntityDamageByEntityEvent e){
        if(e.getEntity() instanceof Player pl&&e.getDamager() instanceof LivingEntity attacker&&hostile(pl,attacker)&&active(pl.getUniqueId(),"GUARD")){push(attacker,pl.getLocation(),.45);attacker.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,60,0,true,false,true));}
        if(!(e.getDamager() instanceof Player pl)||!(e.getEntity() instanceof LivingEntity mob)||!hostile(pl,mob)||!ready(pl))return;
        String type=strike(pl.getUniqueId()),held=p.items.id(pl.getInventory().getItemInMainHand());if(type==null||!type.equals(p.getConfig().getString("items."+held+".skill.type")))return;consume(pl.getUniqueId(),type);readiness.remove(pl.getUniqueId());
        if(type.equals("PIERCE")){mob.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,80,0,true,false,true));mob.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,100,0,true,false,true));}
        else {int count=0;for(Entity entity:mob.getNearbyEntities(type.equals("SHOCK")?4:3,2,type.equals("SHOCK")?4:3).stream().filter(x->x instanceof LivingEntity).limit(20).toList())if(entity instanceof LivingEntity other&&hostile(pl,other)&&pl.hasLineOfSight(other)){other.damage(2,pl);push(other,pl.getLocation(),type.equals("SHOCK")?.6:.35);if(++count>=(type.equals("SHOCK")?5:3))break;}}
        pl.getWorld().spawnParticle(Particle.SWEEP_ATTACK,mob.getLocation().add(0,1,0),3,.6,.3,.6,0);
    }
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true) public void fall(EntityDamageEvent e){if(e.getEntity() instanceof Player pl&&e.getCause()==EntityDamageEvent.DamageCause.FALL&&active(pl.getUniqueId(),"PATH")){e.setDamage(e.getDamage()*.5);consume(pl.getUniqueId(),"PATH");}}
    static void push(LivingEntity mob,Location origin,double power){Vector v=mob.getLocation().toVector().subtract(origin.toVector()).setY(0);if(v.lengthSquared()>.01)mob.setVelocity(v.normalize().multiply(power).setY(.15));}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void shoot(EntityShootBowEvent e){
        if(!(e.getEntity() instanceof Player player)||!(e.getProjectile() instanceof AbstractArrow arrow))return;String id=p.items.id(e.getBow()),type=p.getConfig().getString("items."+id+".skill.type");
        if(type==null&&"ranger_crossbow".equals(id))type="MARK";
        if(type==null||!Set.of("MARK","WIND","STAR").contains(type)||!active(player.getUniqueId(),type))return;
        arrow.getPersistentDataContainer().set(marker,PersistentDataType.BYTE,(byte)1);arrow.getPersistentDataContainer().set(arrowSkill,PersistentDataType.STRING,type);consume(player.getUniqueId(),type);
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void hit(ProjectileHitEvent e){
        if(!e.getEntity().getPersistentDataContainer().has(marker)||!(e.getEntity().getShooter() instanceof Player player)||!(e.getHitEntity() instanceof LivingEntity mob)||!hostile(player,mob))return;
        String type=e.getEntity().getPersistentDataContainer().getOrDefault(arrowSkill,PersistentDataType.STRING,"MARK");
        if(type.equals("WIND")){push(mob,player.getLocation(),.55);mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,60,0,true,false,true));}
        else{mob.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,120,0,true,false,true));mob.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,80,0,true,false,true));}
        if(type.equals("STAR")){int count=0;for(Entity near:mob.getNearbyEntities(3,2,3))if(near instanceof LivingEntity other&&hostile(player,other)&&player.hasLineOfSight(other)){other.damage(2,player);if(++count>=2)break;}}
        mob.getWorld().spawnParticle(Particle.ENCHANT,mob.getLocation().add(0,1,0),20,.5,.5,.5,.08);
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void harvest(BlockBreakEvent e){
        Player pl=e.getPlayer();if(!active(pl.getUniqueId(),"HARVEST")||harvests.getOrDefault(pl.getUniqueId(),0)<=0)return;String held=p.items.id(pl.getInventory().getItemInMainHand());if(!Set.of("cultivator_hoe","harvest_hoe").contains(Objects.toString(held,"")))return;
        if(!(e.getBlock().getBlockData() instanceof Ageable crop)||crop.getAge()!=crop.getMaximumAge())return;Material cropType=e.getBlock().getType();Material seed=seed(cropType);if(seed==null)return;Location at=e.getBlock().getLocation();
        Bukkit.getScheduler().runTask(p,()->{
            if(!pl.isOnline()||!pl.getWorld().equals(at.getWorld())||pl.getLocation().distanceSquared(at)>36||!at.getBlock().getType().isAir()||at.clone().subtract(0,1,0).getBlock().getType()!=Material.FARMLAND||!active(pl.getUniqueId(),"HARVEST"))return;
            int slot=-1;for(int i=0;i<36;i++){ItemStack stack=pl.getInventory().getItem(i);if(stack!=null&&stack.getType()==seed&&p.items.id(stack)==null){slot=i;break;}}if(slot<0){p.say(pl,"skill.harvest-empty");return;}
            Block block=at.getBlock();var old=block.getState();block.setType(cropType,false);ItemStack input=pl.getInventory().getItem(slot);var placement=new BlockPlaceEvent(block,old,block.getRelative(0,-1,0),input,pl,true,org.bukkit.inventory.EquipmentSlot.HAND);Bukkit.getPluginManager().callEvent(placement);
            if(placement.isCancelled()||!placement.canBuild()){old.update(true,false);return;}input.setAmount(input.getAmount()-1);pl.getInventory().setItem(slot,input.getAmount()>0?input:null);int remaining=harvests.merge(pl.getUniqueId(),-1,Integer::sum);if(remaining<=0)consume(pl.getUniqueId(),"HARVEST");p.say(pl,"skill.replanted","count",Math.max(0,remaining));
        });
    }
    static Material seed(Material crop){return switch(crop){case WHEAT->Material.WHEAT_SEEDS;case BEETROOTS->Material.BEETROOT_SEEDS;case CARROTS->Material.CARROT;case POTATOES->Material.POTATO;default->null;};}
    @EventHandler public void quit(PlayerQuitEvent e){armed.remove(e.getPlayer().getUniqueId());harvests.remove(e.getPlayer().getUniqueId());mining.remove(e.getPlayer().getUniqueId());readiness.remove(e.getPlayer().getUniqueId());}
    @EventHandler public void death(PlayerDeathEvent e){armed.remove(e.getEntity().getUniqueId());harvests.remove(e.getEntity().getUniqueId());mining.remove(e.getEntity().getUniqueId());readiness.remove(e.getEntity().getUniqueId());}
}
