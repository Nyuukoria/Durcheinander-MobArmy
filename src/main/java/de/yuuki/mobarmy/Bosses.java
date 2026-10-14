package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import java.util.*;

final class Bosses implements Listener {
    final Map<UUID,GolemRig> rigs=new HashMap<>();long animations;
    final MobArmy p;final Map<UUID,UUID> previews=new HashMap<>();final Map<UUID,Long> pulse=new HashMap<>();final NamespacedKey abilityKey;
    record Warning(Session session,Mob mob,String ability,Location center,long impact){}
    final Map<UUID,Warning> warnings=new HashMap<>();
    Bosses(MobArmy p){this.p=p;abilityKey=new NamespacedKey(p,"boss_ability");}
    Set<String> ids(){return p.getConfig().getConfigurationSection("boss-types").getKeys(false);}
    ConfigurationSection definition(String id){var c=p.getConfig().getConfigurationSection("boss-types."+id);if(c==null)throw new IllegalArgumentException("Unbekannter Boss: "+id);return c;}
    ConfigurationSection select(ConfigurationSection stage,boolean finalBoss,ConfigurationSection fallback){
        List<String> pool=stage.getStringList(finalBoss?"boss-pool":"miniboss-pool");if(pool.isEmpty())return fallback;
        var cfg=new YamlConfiguration();String selected=pool.get(new Random().nextInt(pool.size()));ConfigUpgrade.copy(definition(selected),cfg);cfg.set("profile-id",selected);return cfg;
    }
    static boolean permitted(String type){return !Set.of("PHANTOM","VEX","EVOKER").contains(type.toUpperCase(Locale.ROOT));}
    void model(LivingEntity mob,ConfigurationSection cfg,String tag){
        String item=cfg.getString("model-item");if(item==null)return;
        String variant=cfg.getString("model-variant",item.equals("boss_model_frost")?"frost":item.equals("boss_model_ember")?"ember":"abyss");
        if(!Set.of("abyss","frost","ember").contains(variant))throw new IllegalArgumentException("Unknown golem variant: "+variant);
        unmodel(mob.getUniqueId());rigs.put(mob.getUniqueId(),new GolemRig(p,mob,variant,tag));
    }
    void attach(Mob mob,ConfigurationSection cfg){if(p.bossSkills!=null&&cfg.contains("profile-id"))mob.getPersistentDataContainer().set(p.bossSkills.profile,PersistentDataType.STRING,cfg.getString("profile-id"));if(cfg.contains("ability"))mob.getPersistentDataContainer().set(abilityKey,PersistentDataType.STRING,cfg.getString("ability"));}
    void summon(Player player,String id){
        var cfg=definition(id);if(!permitted(cfg.getString("type")))throw new IllegalArgumentException("Phantoms, Vex and Evokers are disabled");Location at=player.getLocation().clone().add(player.getLocation().getDirection().setY(0).normalize().multiply(3));
        at.setY(player.getLocation().getY());at.setYaw(player.getLocation().getYaw()+180);if(!at.getBlock().isPassable()||!at.clone().add(0,1,0).getBlock().isPassable()||cfg.contains("model-item")&&!at.clone().add(0,2,0).getBlock().isPassable())throw new IllegalArgumentException("Vor dir ist kein freier Platz.");
        if(previews.size()>=12)throw new IllegalArgumentException("Zuerst Boss-Vorschauen entfernen: /raid boss clear");
        Entity entity=at.getWorld().spawnEntity(at,EntityType.valueOf(cfg.getString("type")));if(!(entity instanceof Mob mob)){entity.remove();throw new IllegalArgumentException("Boss must be a mob");}
        try{mob.setAI(false);mob.setInvulnerable(true);mob.setSilent(true);mob.setGravity(false);mob.setPersistent(false);mob.setCanPickupItems(false);mob.setRemoveWhenFarAway(false);mob.setCustomName(MobArmy.color(p.text(player,cfg.getString("name"))+" &8[DEMO]"));mob.setCustomNameVisible(true);
            if(mob instanceof Slime slime)slime.setSize(3);
            var eq=mob.getEquipment();var gear=cfg.getConfigurationSection("equipment");if(eq!=null&&gear!=null)for(String slot:gear.getKeys(false)){ItemStack item=new ItemStack(Material.valueOf(gear.getString(slot)));switch(slot){case "helmet"->eq.setHelmet(item);case "chestplate"->eq.setChestplate(item);case "leggings"->eq.setLeggings(item);case "boots"->eq.setBoots(item);case "hand"->eq.setItemInMainHand(item);default->{}}}
            Engine.set(mob,org.bukkit.attribute.Attribute.MAX_HEALTH,cfg.getDouble("health",100));mob.setHealth(cfg.getDouble("health",100));
            mob.getPersistentDataContainer().set(p.entityKey,PersistentDataType.STRING,"boss-preview");model(mob,cfg,"boss-preview");previews.put(mob.getUniqueId(),player.getUniqueId());
        }catch(RuntimeException e){remove(mob);throw e;}
        p.say(player,"admin.boss-preview");
    }
    void clear(Player player,boolean all){for(var e:new ArrayList<>(previews.entrySet()))if(all||e.getValue().equals(player.getUniqueId())){Entity mob=Bukkit.getEntity(e.getKey());if(mob!=null)remove(mob);previews.remove(e.getKey());}}
    void remove(Entity mob){unmodel(mob.getUniqueId());for(Entity child:mob.getPassengers())child.remove();mob.remove();}
    void tick(){
        long now=System.currentTimeMillis();Set<UUID> alive=new HashSet<>();
        previews.keySet().removeIf(id->Bukkit.getEntity(id)==null);if(p.bossSkills!=null){p.bossSkills.tick();return;}
        for(var entry:new ArrayList<>(warnings.entrySet())){
            Warning warning=entry.getValue();if(!warning.mob().isValid()||warning.session().phase!=Session.Phase.FIGHT||!warning.session().mobs.contains(entry.getKey())){warnings.remove(entry.getKey());continue;}
            if(now<warning.impact())continue;warnings.remove(entry.getKey());
            Mob mob=warning.mob();mob.getWorld().spawnParticle(warning.ability().equals("frost")?Particle.SNOWFLAKE:Particle.ENCHANT,warning.center(),40,1,.4,1,.05);GolemRig rig=rigs.get(entry.getKey());if(rig!=null)rig.attack();
            if(warning.ability().equals("rally"))mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,60,0,true,true,true));
            for(Player player:p.engine.players(warning.session()))if(Rituals.inStrike(player.getLocation(),warning.center())&&mob.hasLineOfSight(player)){
                if(warning.ability().equals("frost"))player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,40,0,true,true,true));
                if(warning.ability().equals("reveal"))player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,60,0,true,true,true));
            }
        }
        for(Portal portal:p.portals.values()){
            Session s=portal.session;if(s==null||s.phase!=Session.Phase.FIGHT)continue;
            for(UUID id:s.contribution.keySet()){
                Entity entity=Bukkit.getEntity(id);if(!(entity instanceof Mob mob))continue;alive.add(id);String ability=mob.getPersistentDataContainer().get(abilityKey,PersistentDataType.STRING);
                if(ability==null||pulse.getOrDefault(id,0L)>now)continue;pulse.put(id,now+8000);
                Player target=p.engine.players(s).stream().filter(pl->pl.getWorld().equals(mob.getWorld())&&!pl.isDead()).min(Comparator.comparingDouble(pl->pl.getLocation().distanceSquared(mob.getLocation()))).orElse(null);
                if(target!=null){warnings.put(id,new Warning(s,mob,ability,target.getLocation().clone(),now+2000));p.say(target,"combat.warning");}
            }
        }
        pulse.keySet().retainAll(alive);warnings.keySet().retainAll(alive);
    }
    void unmodel(UUID mob){GolemRig rig=rigs.remove(mob);if(rig!=null)rig.remove();}
    void hurt(UUID mob){GolemRig rig=rigs.get(mob);if(rig!=null)rig.hurt();}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void attack(EntityDamageByEntityEvent e){GolemRig rig=rigs.get(e.getDamager().getUniqueId());if(rig!=null)rig.attack();}
    void animate(){if(p.bossSkills!=null)p.bossSkills.animate();if(++animations%2!=0)return;for(var entry:new ArrayList<>(rigs.entrySet())){if(!entry.getValue().valid())unmodel(entry.getKey());else entry.getValue().animate();}if(animations%6==0)for(Warning warning:warnings.values())for(int i=0;i<16;i++){double angle=i*Math.PI/8;warning.mob().getWorld().spawnParticle(Particle.DUST,warning.center().clone().add(Math.cos(angle)*2.4,.15,Math.sin(angle)*2.4),1,0,0,0,0,new Particle.DustOptions(warning.ability().equals("frost")?Color.AQUA:Color.ORANGE,1));}}
    void shutdown(){if(p.bossSkills!=null)p.bossSkills.clear();for(UUID id:previews.keySet()){Entity e=Bukkit.getEntity(id);if(e!=null)remove(e);}previews.clear();pulse.clear();warnings.clear();for(UUID id:new ArrayList<>(rigs.keySet()))unmodel(id);}
}
