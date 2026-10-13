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
    final Map<UUID,RitualCast> casts=new HashMap<>();
    Items(MobArmy p){
        this.p=p;
        var pending=p.state.getConfigurationSection("pending-rituals");
        if(pending!=null)for(String key:pending.getKeys(false)){
            UUID owner=UUID.fromString(pending.getString(key+".owner"));ItemStack item=pending.getItemStack(key+".item");if(item!=null){List<ItemStack> mail=mailbox(owner);mail.add(item);p.state.set("mail."+owner,mail);}
        }
        p.state.set("pending-rituals",null);
    }
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
    ItemStack refreshed(ItemStack item){
        String id=id(item);if(id==null||!ids().contains(id))return item.clone();
        ItemStack result=item.clone();var meta=result.getItemMeta();var template=make(id,item.getAmount()).getItemMeta();
        meta.setDisplayName(template.getDisplayName());meta.setLore(template.getLore());meta.setItemModel(template.getItemModel());
        if(template.hasCustomModelData())meta.setCustomModelData(template.getCustomModelData());result.setItemMeta(meta);return result;
    }
    void refresh(Player player){for(int slot=0;slot<player.getInventory().getSize();slot++){ItemStack item=player.getInventory().getItem(slot);if(item!=null&&id(item)!=null)player.getInventory().setItem(slot,refreshed(item));}}
    List<ItemStack> mailbox(UUID owner){List<ItemStack> result=new ArrayList<>();for(Object entry:p.state.getList("mail."+owner,List.of()))if(entry instanceof ItemStack item)result.add(refreshed(item));return result;}
    void mailbox(UUID owner,List<ItemStack> items){p.state.set("mail."+owner,items.isEmpty()?null:items);p.saveState();}
    void store(UUID owner,ItemStack item){List<ItemStack> mail=mailbox(owner);mail.add(item.clone());mailbox(owner,mail);}
    void deliver(UUID owner,ItemStack item){Player target=Bukkit.getPlayer(owner);if(target==null){store(owner,item);return;}for(ItemStack remainder:target.getInventory().addItem(item.clone()).values())store(owner,remainder);p.saveState();}
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
        if(until>now){p.tell(player,"Noch "+Rules.clock((until-now+999)/1000)+" Abklingzeit für dieses Item.");return true;}
        int consumed=cfg.getInt("consume",0);if(held.getAmount()<consumed)return true;
        List<Player> targets=new ArrayList<>();targets.add(player);double radius=cfg.getDouble("radius",0);Session session=p.session(player.getUniqueId());
        double enemyRadius=cfg.getDouble("enemy-radius",0);
        if(enemyRadius>0){
            if(session==null||session.phase!=Session.Phase.FIGHT){p.tell(player,"Dieses Relikt wirkt auf Gegner während einer Raid-Stage.");return true;}
            int affected=0;
            for(UUID uuid:session.mobs){Entity e=Bukkit.getEntity(uuid);if(e instanceof LivingEntity mob&&mob.getWorld().equals(player.getWorld())&&mob.getLocation().distanceSquared(player.getLocation())<=enemyRadius*enemyRadius){
                boolean boss=session.contribution.containsKey(uuid);mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,(boss?3:cfg.getInt("enemy-slow-seconds",6))*20,boss?0:1,true,true,true));affected++;
                mob.getWorld().spawnParticle(Particle.SNOWFLAKE,mob.getLocation().add(0,1,0),18,.4,.6,.4,.02);
            }}
            if(affected==0){p.tell(player,"Keine Raid-Gegner in Reichweite. Das Relikt bleibt bereit.");return true;}
        }
        if(radius>0&&session!=null)for(Player other:p.engine.players(session))if(other!=player&&other.getWorld().equals(player.getWorld())&&other.getLocation().distanceSquared(player.getLocation())<=radius*radius)targets.add(other);
        for(var effect:cfg.getMapList("effects")){
            PotionEffectType type=PotionEffectType.getByName(String.valueOf(effect.get("type")));if(type==null)continue;
            int duration=((Number)effect.get("seconds")).intValue()*20,amplifier=((Number)effect.get("amplifier")).intValue();
            for(Player target:targets)target.addPotionEffect(new PotionEffect(type,duration,amplifier,true,true,true));
        }
        if(consumed>0)held.setAmount(held.getAmount()-consumed);
        p.state.set("item-cooldowns."+player.getUniqueId()+"."+id,now+cfg.getLong("cooldown-seconds",60)*1000);p.saveState();
        for(Player target:targets){p.presentation.burst(target.getLocation());target.playSound(target.getLocation(),Sound.BLOCK_AMETHYST_BLOCK_CHIME,0.8f,1.2f);}
        p.tell(player,cfg.getString("message","Relikt aktiviert.")+" &7("+targets.size()+" Spieler)");return true;
    }
    boolean ritual(Player player,org.bukkit.block.Block cauldron,ItemStack held){
        String id=id(held);if(id==null)return false;var cfg=p.getConfig().getConfigurationSection("items."+id+".ritual");if(cfg==null)return false;
        if(p.arena(player.getWorld())!=null||!player.getWorld().getName().equals(p.getConfig().getString("ritual-world","world"))){p.tell(player,"Das Ritual funktioniert nur in der Default-Welt.");return true;}
        p.engine.rituals.keySet().removeIf(key->Bukkit.getEntity(key)==null);
        if(p.engine.rituals.size()+casts.size()>=p.getConfig().getInt("maximum-ritual-mobs",20)){p.tell(player,"Zu viele aktive Rituale.");return true;}
        if(casts.values().stream().anyMatch(c->c.cauldron.equals(cauldron.getLocation()))){p.tell(player,"An diesem Kessel läuft schon ein Ritual.");return true;}
        long now=System.currentTimeMillis();String cooldown="item-cooldowns."+player.getUniqueId()+"."+id;
        long until=p.state.getLong(cooldown);if(until>now){p.tell(player,"Ritual bereit in "+Rules.clock((until-now+999)/1000)+".");return true;}
        int consumed=Math.max(0,cfg.getInt("consume",0));if(held.getAmount()<consumed){p.tell(player,"Zu wenige Ritual-Items.");return true;}
        ItemStack payment=consumed>0?held.clone():null;if(payment!=null){payment.setAmount(consumed);held.setAmount(held.getAmount()-consumed);}
        RitualCast cast;try{cast=new RitualCast(player.getUniqueId(),cauldron.getLocation(),payment,cfg);}catch(RuntimeException ex){if(payment!=null)held.setAmount(held.getAmount()+consumed);p.getLogger().warning("Ritual could not start: "+ex.getMessage());p.tell(player,"Das Ritual konnte nicht starten. Dein Item bleibt erhalten.");return true;}casts.put(cast.key,cast);
        if(payment!=null){p.state.set("pending-rituals."+cast.key+".owner",player.getUniqueId().toString());p.state.set("pending-rituals."+cast.key+".item",payment);}
        p.state.set(cooldown,now+cfg.getLong("cooldown-seconds",120)*1000);p.saveState();
        cast.runTaskTimer(p,0,2);
        return true;
    }
    final class RitualCast extends BukkitRunnable {
        final UUID key=UUID.randomUUID(),owner;final Location cauldron,center;final ItemStack payment;final ConfigurationSection config;
        final ItemDisplay orb;final TextDisplay label;int tick;boolean done;
        RitualCast(UUID owner,Location cauldron,ItemStack payment,ConfigurationSection config){
            this.owner=owner;this.cauldron=cauldron;this.payment=payment;this.config=config;center=cauldron.clone().add(0.5,1,0.5);
            orb=center.getWorld().spawn(center,ItemDisplay.class);orb.setItemStack(make("ritual_core",1));orb.setPersistent(false);orb.getPersistentDataContainer().set(p.entityKey,PersistentDataType.STRING,"ritual-animation");
            var t=orb.getTransformation();t.getScale().set(0.55f);orb.setTransformation(t);orb.setInterpolationDuration(2);
            label=center.getWorld().spawn(center.clone().add(0,2,0),TextDisplay.class);Presentation.style(label);label.getPersistentDataContainer().set(p.entityKey,PersistentDataType.STRING,"ritual-animation");
            center.getWorld().playSound(center,Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,0.8f,0.6f);
        }
        public void run(){
            int total=Math.max(40,p.getConfig().getInt("ritual-animation.duration-seconds",6)*20);tick+=2;
            if(!cauldron.getBlock().getType().name().endsWith("CAULDRON")){abort();return;}
            double progress=Math.min(1,(double)tick/total),radius=1.6-progress*0.8;
            String phase=progress<.33?"ERWECKUNG":progress<.75?"VERDICHTUNG":"DURCHBRUCH";
            label.setText(MobArmy.color("&d&l"+phase+"\n&fRitual &b"+(int)(progress*100)+"%"));
            orb.teleport(center.clone().add(0,0.2+Math.sin(tick*0.06)*0.15+progress*0.4,0));orb.setRotation(tick*6,0);
            for(int i=0;i<16;i++){
                double angle=i*Math.PI/8+tick*.075;Location at=center.clone().add(Math.cos(angle)*radius,-0.7+progress*.3,Math.sin(angle)*radius);
                center.getWorld().spawnParticle(Particle.DUST,at,1,0,0,0,0,new Particle.DustOptions(i%2==0?Color.fromRGB(209,86,255):Color.fromRGB(86,229,232),1.0f));
            }
            if(progress>.33)for(int i=0;i<4;i++){double a=tick*.13+i*Math.PI/2;center.getWorld().spawnParticle(Particle.REVERSE_PORTAL,center.clone().add(Math.cos(a)*.55,progress*1.8,Math.sin(a)*.55),2,0,0,0,.03);}
            if(tick==Math.max(2,(total/3)/2*2))center.getWorld().playSound(center,Sound.BLOCK_BEACON_POWER_SELECT,.8f,.7f);
            if(progress<1)return;
            try{
                Entity entity=center.getWorld().spawnEntity(center,EntityType.valueOf(config.getString("type","HUSK")));
                if(!(entity instanceof Mob mob)){entity.remove();throw new IllegalArgumentException("Ritual entity must be a mob");}
                mob.getPersistentDataContainer().set(p.entityKey,PersistentDataType.STRING,"ritual");mob.setPersistent(false);
                double hp=Math.min(1024,config.getDouble("health",80));Engine.set(mob,org.bukkit.attribute.Attribute.MAX_HEALTH,hp);mob.setHealth(hp);
                Engine.set(mob,org.bukkit.attribute.Attribute.ATTACK_DAMAGE,config.getDouble("damage",6));mob.setCustomName(MobArmy.color(config.getString("name","&dRitualwächter")));mob.setCustomNameVisible(true);mob.setCanPickupItems(false);mob.setGlowing(true);
                Player player=Bukkit.getPlayer(owner);if(player!=null&&player.getWorld().equals(center.getWorld()))mob.setTarget(player);
                if(mob.getEquipment()!=null){mob.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));mob.getEquipment().setHelmetDropChance(0);}
                p.engine.rituals.put(mob.getUniqueId(),config.getString("result",""));
                center.getWorld().spawnParticle(Particle.EXPLOSION,center,3,.4,.5,.4,0);center.getWorld().spawnParticle(Particle.DRAGON_BREATH,center,100,.6,.8,.6,.04);
                center.getWorld().playSound(center,Sound.ENTITY_EVOKER_PREPARE_SUMMON,1,.65f);cleanup();
            }catch(RuntimeException ex){p.getLogger().warning("Ritual failed: "+ex.getMessage());abort();}
        }
        void cleanup(){if(done)return;done=true;cancel();orb.remove();label.remove();casts.remove(key);p.state.set("pending-rituals."+key,null);p.saveState();}
        void abort(){if(done)return;cleanup();if(payment!=null)store(owner,payment);Player player=Bukkit.getPlayer(owner);if(player!=null)p.tell(player,payment!=null?"Ritual abgebrochen – verbrauchte Items liegen im /raid loot Postfach.":"Ritual abgebrochen – dein Katalysator bleibt erhalten. Die Abklingzeit läuft weiter.");}
    }
    void shutdown(){for(RitualCast cast:new ArrayList<>(casts.values()))cast.abort();}
}
