package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.*;
import java.util.*;

final class Bosses {
    final MobArmy p;final Map<UUID,UUID> previews=new HashMap<>();final Map<UUID,Long> pulse=new HashMap<>();final NamespacedKey abilityKey;
    Bosses(MobArmy p){this.p=p;abilityKey=new NamespacedKey(p,"boss_ability");}
    Set<String> ids(){return p.getConfig().getConfigurationSection("boss-types").getKeys(false);}
    ConfigurationSection definition(String id){var c=p.getConfig().getConfigurationSection("boss-types."+id);if(c==null)throw new IllegalArgumentException("Unbekannter Boss: "+id);return c;}
    ConfigurationSection select(ConfigurationSection stage,boolean finalBoss,ConfigurationSection fallback){
        List<String> pool=stage.getStringList(finalBoss?"boss-pool":"miniboss-pool");if(pool.isEmpty())return fallback;
        var cfg=new YamlConfiguration();ConfigUpgrade.copy(definition(pool.get(new Random().nextInt(pool.size()))),cfg);return cfg;
    }
    static boolean permitted(String type){return !Set.of("PHANTOM","VEX","EVOKER").contains(type.toUpperCase(Locale.ROOT));}
    void model(LivingEntity mob,ConfigurationSection cfg,String tag){
        String item=cfg.getString("model-item");if(item==null)return;
        ItemDisplay display=mob.getWorld().spawn(mob.getLocation(),ItemDisplay.class);display.setItemStack(p.items.make(item,1));display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        display.setPersistent(false);display.getPersistentDataContainer().set(p.entityKey,PersistentDataType.STRING,tag);var t=display.getTransformation();t.getTranslation().set(0,-.45f,0);display.setTransformation(t);mob.addPassenger(display);mob.setInvisible(true);
    }
    void attach(Mob mob,ConfigurationSection cfg){if(cfg.contains("ability"))mob.getPersistentDataContainer().set(abilityKey,PersistentDataType.STRING,cfg.getString("ability"));}
    void summon(Player player,String id){
        var cfg=definition(id);if(!permitted(cfg.getString("type")))throw new IllegalArgumentException("Phantoms, Vex and Evokers are disabled");Location at=player.getLocation().clone().add(player.getLocation().getDirection().setY(0).normalize().multiply(3));
        at.setY(player.getLocation().getY());if(!at.getBlock().isPassable()||!at.clone().add(0,1,0).getBlock().isPassable())throw new IllegalArgumentException("Vor dir ist kein freier Platz.");
        if(previews.size()>=12)throw new IllegalArgumentException("Zuerst Boss-Vorschauen entfernen: /raid boss clear");
        Entity entity=at.getWorld().spawnEntity(at,EntityType.valueOf(cfg.getString("type")));if(!(entity instanceof Mob mob)){entity.remove();throw new IllegalArgumentException("Boss must be a mob");}
        try{mob.setAI(false);mob.setInvulnerable(true);mob.setSilent(true);mob.setGravity(false);mob.setPersistent(false);mob.setCanPickupItems(false);mob.setRemoveWhenFarAway(false);mob.setCustomName(MobArmy.color(p.text(player,cfg.getString("name"))+" &8[DEMO]"));mob.setCustomNameVisible(true);
            if(mob instanceof Slime slime)slime.setSize(3);
            var eq=mob.getEquipment();var gear=cfg.getConfigurationSection("equipment");if(eq!=null&&gear!=null)for(String slot:gear.getKeys(false)){ItemStack item=new ItemStack(Material.valueOf(gear.getString(slot)));switch(slot){case "helmet"->eq.setHelmet(item);case "chestplate"->eq.setChestplate(item);case "leggings"->eq.setLeggings(item);case "boots"->eq.setBoots(item);case "hand"->eq.setItemInMainHand(item);default->{}}}
            Engine.set(mob,org.bukkit.attribute.Attribute.MAX_HEALTH,cfg.getDouble("health",100));mob.setHealth(cfg.getDouble("health",100));
            mob.getPersistentDataContainer().set(p.entityKey,PersistentDataType.STRING,"boss-preview");model(mob,cfg,"boss-preview");previews.put(mob.getUniqueId(),player.getUniqueId());
        }catch(RuntimeException e){remove(mob);throw e;}
        p.tell(player,"Boss-Vorschau beschworen. Keine Angriffe, Drops oder Raid-Fortschritte.");
    }
    void clear(Player player,boolean all){for(var e:new ArrayList<>(previews.entrySet()))if(all||e.getValue().equals(player.getUniqueId())){Entity mob=Bukkit.getEntity(e.getKey());if(mob!=null)remove(mob);previews.remove(e.getKey());}}
    static void remove(Entity mob){for(Entity child:mob.getPassengers())child.remove();mob.remove();}
    void tick(){
        long now=System.currentTimeMillis();Set<UUID> alive=new HashSet<>();
        previews.keySet().removeIf(id->Bukkit.getEntity(id)==null);
        for(Portal portal:p.portals.values()){
            Session s=portal.session;if(s==null||s.phase!=Session.Phase.FIGHT)continue;
            for(UUID id:s.contribution.keySet()){
                Entity entity=Bukkit.getEntity(id);if(!(entity instanceof Mob mob))continue;alive.add(id);String ability=mob.getPersistentDataContainer().get(abilityKey,PersistentDataType.STRING);
                if(ability==null||pulse.getOrDefault(id,0L)>now)continue;pulse.put(id,now+8000);
                mob.getWorld().spawnParticle(ability.equals("frost")?Particle.SNOWFLAKE:Particle.ENCHANT,mob.getLocation().add(0,1,0),45,2,1,2,.08);
                if(ability.equals("rally"))mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,60,0,true,true,true));
                for(Player player:p.engine.players(s))if(player.getLocation().distanceSquared(mob.getLocation())<=64){
                    if(ability.equals("frost"))player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,40,0,true,true,true));
                    if(ability.equals("reveal"))player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,60,0,true,true,true));
                }
            }
        }
        pulse.keySet().retainAll(alive);
    }
    void shutdown(){for(UUID id:previews.keySet()){Entity e=Bukkit.getEntity(id);if(e!=null)remove(e);}previews.clear();pulse.clear();}
}
