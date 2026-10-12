package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;

final class Items {
    final MobArmy p;
    Items(MobArmy p){this.p=p;}
    Set<String> ids(){var cfg=p.getConfig().getConfigurationSection("items");return cfg==null?Set.of():cfg.getKeys(false);}
    ItemStack make(String id,int count){
        var cfg=p.getConfig().getConfigurationSection("items."+id);if(cfg==null)throw new IllegalArgumentException("Unknown item: "+id);
        ItemStack item=new ItemStack(Material.valueOf(cfg.getString("material","AMETHYST_SHARD")),count);var meta=item.getItemMeta();
        meta.setDisplayName(MobArmy.color(cfg.getString("name",id)));meta.setLore(cfg.getStringList("lore").stream().map(MobArmy::color).toList());
        String model=cfg.getString("item-model");if(model!=null)meta.setItemModel(Objects.requireNonNull(NamespacedKey.fromString(model)));
        if(cfg.contains("custom-model-data"))meta.setCustomModelData(cfg.getInt("custom-model-data"));
        meta.getPersistentDataContainer().set(p.itemKey,PersistentDataType.STRING,id);item.setItemMeta(meta);return item;
    }
    String id(ItemStack item){return item==null||!item.hasItemMeta()?null:item.getItemMeta().getPersistentDataContainer().get(p.itemKey,PersistentDataType.STRING);}
    List<ItemStack> drops(String source,int stage,String week){
        List<ItemStack> out=new ArrayList<>();for(String id:ids()){
            var c=p.getConfig().getConfigurationSection("items."+id);
            for(var d:c.getMapList("drops")){
                if(!source.equals(String.valueOf(d.get("source"))))continue;
                if(d.containsKey("stage")&&((Number)d.get("stage")).intValue()!=stage)continue;
                if(d.containsKey("week")&&!week.equals(String.valueOf(d.get("week"))))continue;
                double chance=d.containsKey("chance")?((Number)d.get("chance")).doubleValue():1;
                if(Math.random()<=chance)out.add(make(id,d.containsKey("amount")?((Number)d.get("amount")).intValue():1));
            }
        }return out;
    }
    List<ItemStack> mailbox(UUID owner){List<ItemStack> result=new ArrayList<>();for(Object entry:p.state.getList("mail."+owner,List.of()))if(entry instanceof ItemStack item)result.add(item.clone());return result;}
    void mailbox(UUID owner,List<ItemStack> items){p.state.set("mail."+owner,items.isEmpty()?null:items);p.saveState();}
    void store(UUID owner,ItemStack item){List<ItemStack> mail=mailbox(owner);mail.add(item.clone());mailbox(owner,mail);}
    void deliver(UUID owner,ItemStack item){Player target=Bukkit.getPlayer(owner);if(target==null){store(owner,item);return;}for(ItemStack remainder:target.getInventory().addItem(item.clone()).values())store(owner,remainder);p.saveState();}
    boolean ritual(Player player,org.bukkit.block.Block cauldron,ItemStack held){
        String id=id(held);if(id==null)return false;var cfg=p.getConfig().getConfigurationSection("items."+id+".ritual");if(cfg==null)return false;
        if(p.arena(player.getWorld())!=null||!player.getWorld().getName().equals(p.getConfig().getString("ritual-world","world"))){p.tell(player,"Das Ritual funktioniert nur in der Default-Welt.");return true;}
        if(p.engine.rituals.size()>=p.getConfig().getInt("maximum-ritual-mobs",20)){p.tell(player,"Zu viele aktive Rituale.");return true;}
        Location spawn=cauldron.getLocation().add(0.5,1,0.5);
        Entity entity=spawn.getWorld().spawnEntity(spawn,EntityType.valueOf(cfg.getString("type","HUSK")));
        if(!(entity instanceof Mob mob)){entity.remove();return true;}
        mob.getPersistentDataContainer().set(p.entityKey,PersistentDataType.STRING,"ritual");mob.setPersistent(false);
        double health=Math.min(1024,cfg.getDouble("health",80));Engine.set(mob,org.bukkit.attribute.Attribute.MAX_HEALTH,health);mob.setHealth(health);
        Engine.set(mob,org.bukkit.attribute.Attribute.ATTACK_DAMAGE,cfg.getDouble("damage",6));mob.setCustomName(MobArmy.color(cfg.getString("name","&dRitualwächter")));mob.setCustomNameVisible(true);mob.setTarget(player);mob.setCanPickupItems(false);
        if(mob.getEquipment()!=null){mob.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));mob.getEquipment().setHelmetDropChance(0);}
        p.engine.rituals.put(mob.getUniqueId(),cfg.getString("result",""));held.setAmount(held.getAmount()-1);
        spawn.getWorld().spawnParticle(Particle.PORTAL,spawn,80,0.8,0.5,0.8,0.1);spawn.getWorld().playSound(spawn,Sound.ENTITY_EVOKER_PREPARE_SUMMON,1,0.6f);
        return true;
    }
}
