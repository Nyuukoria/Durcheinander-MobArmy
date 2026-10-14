package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

final class Items {
    final MobArmy p;
    Items(MobArmy p){
        this.p=p;
        var pending=p.state.getConfigurationSection("pending-rituals");
        if(pending!=null)for(String key:pending.getKeys(false)){
            UUID owner=UUID.fromString(pending.getString(key+".owner"));List<ItemStack> returned=mailbox(owner);ItemStack legacy=pending.getItemStack(key+".item");if(legacy!=null)returned.add(legacy);for(Object entry:pending.getList(key+".items",List.of()))if(entry instanceof ItemStack item)returned.add(item);p.state.set("mail."+owner,returned);
        }
        p.state.set("pending-rituals",null);
    }
    Set<String> ids(){var cfg=p.getConfig().getConfigurationSection("items");return cfg==null?Set.of():cfg.getKeys(false);}
    ItemStack make(String id,int count){
        var cfg=p.getConfig().getConfigurationSection("items."+id);if(cfg==null)throw new IllegalArgumentException("Unknown item: "+id);
        ItemStack item=new ItemStack(Material.valueOf(cfg.getString("material","AMETHYST_SHARD")),count);var meta=item.getItemMeta();
        if(count<1||count>item.getMaxStackSize())throw new IllegalArgumentException("Dieses Item erlaubt maximal "+item.getMaxStackSize()+" pro Stack.");
        meta.setDisplayName(MobArmy.color(cfg.getString("name",id)));meta.setLore(cfg.getStringList("lore").stream().map(MobArmy::color).toList());
        String model=cfg.getString("item-model");if(model!=null)meta.setItemModel(Objects.requireNonNull(NamespacedKey.fromString(model)));
        if(cfg.contains("custom-model-data"))meta.setCustomModelData(cfg.getInt("custom-model-data"));
        Equipment.decorate(cfg,meta);ItemCooldowns.decorate(id,cfg,meta);
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
        }for(String item:LootTables.roll(p.getConfig().getConfigurationSection("loot-pools"),source,stage,week,Math::random))out.add(make(item,1));return out;
    }
    ItemStack refreshed(ItemStack item){
        String id=id(item);if(id==null||!ids().contains(id))return item.clone();
        ItemStack result=item.clone();var meta=result.getItemMeta();var template=make(id,item.getAmount()).getItemMeta();
        String utilityName=meta.getPersistentDataContainer().get(new NamespacedKey(p,"utility_name"),org.bukkit.persistence.PersistentDataType.STRING);meta.setDisplayName(utilityName==null?template.getDisplayName():utilityName);meta.setLore(template.getLore());meta.setItemModel(template.getItemModel());
        ItemCooldowns.decorate(id,p.getConfig().getConfigurationSection("items."+id),meta);
        if(template.hasCustomModelData())meta.setCustomModelData(template.getCustomModelData());Equipment.decorate(p.getConfig().getConfigurationSection("items."+id),meta);result.setItemMeta(meta);return result;
    }
    void refresh(Player player){for(int slot=0;slot<player.getInventory().getSize();slot++){ItemStack item=player.getInventory().getItem(slot);if(item!=null&&id(item)!=null)player.getInventory().setItem(slot,p.language==null?refreshed(item):p.language.localized(player.getUniqueId(),refreshed(item)));}if(p.itemCooldowns!=null)p.itemCooldowns.sync(player);}
    List<ItemStack> mailbox(UUID owner){List<ItemStack> result=new ArrayList<>();for(Object entry:p.state.getList("mail."+owner,List.of()))if(entry instanceof ItemStack item)result.add(p.language==null?refreshed(item):p.language.localized(owner,refreshed(item)));return result;}
    void mailbox(UUID owner,List<ItemStack> items){p.state.set("mail."+owner,items.isEmpty()?null:items);p.saveState();}
    void store(UUID owner,ItemStack item){List<ItemStack> mail=mailbox(owner);mail.add(item.clone());mailbox(owner,mail);}
    void deliver(UUID owner,ItemStack item){Player target=Bukkit.getPlayer(owner);if(target==null){store(owner,item);return;}for(ItemStack remainder:target.getInventory().addItem(p.language==null?item.clone():p.language.localized(owner,item)).values())store(owner,remainder);p.saveState();}
    int count(Player player,String id){int total=0;for(ItemStack item:player.getInventory().getContents())if(id.equals(id(item)))total+=item.getAmount();return total;}
    boolean take(Player player,String id,int amount){
        if(amount<1||count(player,id)<amount)return false;int left=amount;
        for(int slot=0;slot<player.getInventory().getSize()&&left>0;slot++){
            ItemStack item=player.getInventory().getItem(slot);if(!id.equals(id(item)))continue;int used=Math.min(left,item.getAmount());left-=used;
            if(used==item.getAmount())player.getInventory().setItem(slot,null);else{item.setAmount(item.getAmount()-used);player.getInventory().setItem(slot,item);}
        }
        return true;
    }
    boolean activate(Player player,ItemStack held){
        String id=id(held);if(id==null)return false;var cfg=p.getConfig().getConfigurationSection("items."+id+".use");if(cfg==null)return false;
        long now=System.currentTimeMillis(),until=p.state.getLong("item-cooldowns."+player.getUniqueId()+"."+id);
        if(until>now){p.say(player,"relic.cooldown","time",Rules.clock((until-now+999)/1000));return true;}
        int consumed=cfg.getInt("consume",0);if(held.getAmount()<consumed)return true;
        List<Player> targets=new ArrayList<>();targets.add(player);double radius=cfg.getDouble("radius",0);Session session=p.session(player.getUniqueId());
        double enemyRadius=cfg.getDouble("enemy-radius",0);int affectedEnemies=0;
        if(enemyRadius>0){
            if(session==null||session.phase!=Session.Phase.FIGHT){p.say(player,"relic.combat-only");return true;}
            for(UUID uuid:session.mobs){Entity e=Bukkit.getEntity(uuid);if(e instanceof LivingEntity mob&&mob.getWorld().equals(player.getWorld())&&mob.getLocation().distanceSquared(player.getLocation())<=enemyRadius*enemyRadius){
                boolean boss=session.contribution.containsKey(uuid);
                if(cfg.contains("enemy-slow-seconds"))mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,(boss?3:cfg.getInt("enemy-slow-seconds",6))*20,boss?0:1,true,true,true));
                for(var effect:cfg.getMapList("enemy-effects")){
                    PotionEffectType type=PotionEffectType.getByName(String.valueOf(effect.get("type")));if(type==null)continue;
                    int seconds=((Number)effect.get("seconds")).intValue(),amplifier=((Number)effect.get("amplifier")).intValue();
                    if(boss&&effect.get("boss-seconds") instanceof Number n)seconds=n.intValue();
                    if(boss&&effect.get("boss-amplifier") instanceof Number n)amplifier=n.intValue();
                    mob.addPotionEffect(new PotionEffect(type,seconds*20,amplifier,true,true,true));
                }
                affectedEnemies++;
                mob.getWorld().spawnParticle(cfg.contains("enemy-slow-seconds")?Particle.SNOWFLAKE:Particle.ENCHANT,mob.getLocation().add(0,1,0),18,.4,.6,.4,.02);
            }}
            if(affectedEnemies==0){p.say(player,"relic.no-enemies");return true;}
        }
        if(radius>0&&session!=null)for(Player other:p.engine.players(session))if(other!=player&&other.getWorld().equals(player.getWorld())&&other.getLocation().distanceSquared(player.getLocation())<=radius*radius)targets.add(other);
        for(var effect:cfg.getMapList("effects")){
            PotionEffectType type=PotionEffectType.getByName(String.valueOf(effect.get("type")));if(type==null)continue;
            int duration=((Number)effect.get("seconds")).intValue()*20,amplifier=((Number)effect.get("amplifier")).intValue();
            for(Player target:targets)target.addPotionEffect(new PotionEffect(type,duration,amplifier,true,true,true));
        }
        if(consumed>0)held.setAmount(held.getAmount()-consumed);
        p.itemCooldowns.start(player,held,id,cfg.getLong("cooldown-seconds",60));
        for(Player target:targets){p.presentation.burst(target.getLocation());target.playSound(target.getLocation(),Sound.BLOCK_AMETHYST_BLOCK_CHIME,0.8f,1.2f);}
        p.say(player,"relic.activated","name",p.language.itemName(player,id));return true;
    }
    boolean ritual(Player player,org.bukkit.block.Block cauldron,ItemStack held){
        String id=id(held);if(!"chaos_fragment".equals(id))return false;
        p.ritualSystem.open(player,cauldron);return true;
    }
    void shutdown(){if(p.ritualSystem!=null)p.ritualSystem.shutdown();}
}
