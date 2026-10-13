package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

final class Rituals implements Listener {
    final MobArmy p;
    final Map<UUID,Cast> casts=new HashMap<>();
    final Map<UUID,Trial> guardians=new HashMap<>();
    Rituals(MobArmy p){this.p=p;}
    record Trial(UUID owner,ConfigurationSection recipe,long expires){}
    static final class View implements InventoryHolder {
        final UUID owner;final Location cauldron;final Map<Integer,String> recipes=new LinkedHashMap<>();Inventory inventory;
        View(UUID owner,Location cauldron){this.owner=owner;this.cauldron=cauldron;}
        @Override public Inventory getInventory(){return inventory;}
    }
    Set<String> ids(){return p.getConfig().getConfigurationSection("rituals").getKeys(false);}
    ConfigurationSection recipe(String id){var cfg=p.getConfig().getConfigurationSection("rituals."+id);if(cfg==null)throw new IllegalArgumentException("Unbekanntes Ritual: "+id);return cfg;}
    List<RitualPayments.Requirement> requirements(ConfigurationSection cfg){
        List<RitualPayments.Requirement> out=new ArrayList<>();for(var row:cfg.getMapList("ingredients"))out.add(new RitualPayments.Requirement((String)row.get("item"),(String)row.get("material"),((Number)row.get("amount")).intValue()));return out;
    }
    String ingredientName(RitualPayments.Requirement ingredient){return ingredient.customId()!=null?MobArmy.color(p.getConfig().getString("items."+ingredient.customId()+".name",ingredient.customId())):switch(ingredient.material()){case "IRON_INGOT"->"Eisenbarren";case "GOLD_INGOT"->"Goldbarren";case "DIAMOND"->"Diamant";case "LAPIS_LAZULI"->"Lapislazuli";case "EMERALD"->"Smaragd";default->ingredient.material();};}
    boolean locationValid(Player player,Location cauldron){return cauldron.getWorld()!=null&&player.getWorld().equals(cauldron.getWorld())&&player.getLocation().distanceSquared(cauldron.clone().add(.5,.5,.5))<=36&&cauldron.getBlock().getType().name().endsWith("CAULDRON")&&p.arena(player.getWorld())==null&&player.getWorld().getName().equals(p.getConfig().getString("ritual-world","world"));}
    void open(Player player,Block cauldron){
        if(!player.hasPermission("mobarmy.play"))return;
        if(!locationValid(player,cauldron.getLocation())){p.tell(player,"Rituale benötigen einen Kessel in der Default-Welt, höchstens 6 Blöcke entfernt.");return;}
        View v=new View(player.getUniqueId(),cauldron.getLocation());v.inventory=Bukkit.createInventory(v,54,MobArmy.color(p.text(player,"&5Kesselrituale")));int slot=10;
        for(String id:ids()){
            var cfg=recipe(id);ItemStack item=p.items.make(cfg.getString("display-item","ritual_core"),1);var meta=item.getItemMeta();meta.setDisplayName(MobArmy.color(cfg.getString("name",id)));
            List<String> lore=new ArrayList<>();lore.add(MobArmy.color("&7Typ: "+cfg.getString("mode")));lore.add(MobArmy.color("&7Katalysator bleibt erhalten."));
            for(var required:requirements(cfg))lore.add(MobArmy.color("&7Zutat: "+required.amount()+" × "+ingredientName(required)));
            if(cfg.contains("pool"))lore.add(MobArmy.color("&7Ergebnis: ein Item aus "+cfg.getString("pool")));
            if(cfg.contains("legendary-chance"))lore.add(MobArmy.color("&dLegendär: "+Math.round(100*cfg.getDouble("legendary-chance"))+"% nach Wächter-Sieg"));
            lore.add(MobArmy.color("&7Dauer: "+cfg.getInt("duration-seconds",8)+"s · Cooldown: "+cfg.getInt("cooldown-seconds",120)+"s"));lore.add(MobArmy.color("&eKlicken: Ritual beginnen"));meta.setLore(lore);item.setItemMeta(meta);
            if(slot%9==8)slot+=2;if(slot>=45)break;v.recipes.put(slot,id);v.inventory.setItem(slot++,item);
        }
        p.language.open(player,v.inventory);
    }
    @EventHandler public void click(InventoryClickEvent event){
        if(!(event.getView().getTopInventory().getHolder() instanceof View v))return;event.setCancelled(true);
        if(event.getWhoClicked() instanceof Player player&&v.owner.equals(player.getUniqueId())&&v.recipes.containsKey(event.getRawSlot())){
            String id=v.recipes.get(event.getRawSlot());player.closeInventory();try{start(player,v.cauldron,id);}catch(RuntimeException e){p.tell(player,"&c"+Objects.toString(e.getMessage(),"Ritual fehlgeschlagen"));}
        }
    }
    @EventHandler public void drag(InventoryDragEvent event){if(event.getView().getTopInventory().getHolder() instanceof View)event.setCancelled(true);}
    void start(Player player,Location cauldron,String id){
        if(!player.hasPermission("mobarmy.play"))throw new IllegalArgumentException("Keine Raid-Berechtigung.");
        var cfg=recipe(id);if(!locationValid(player,cauldron))throw new IllegalArgumentException("Kessel verlassen oder entfernt.");
        if(!cfg.getString("catalyst","chaos_fragment").equals(p.items.id(player.getInventory().getItemInMainHand())))throw new IllegalArgumentException("Halte den Ritual-Katalysator in der Haupthand.");
        if(casts.values().stream().anyMatch(c->c.owner.equals(player.getUniqueId())||c.cauldron.equals(cauldron)))throw new IllegalArgumentException("Du oder dieser Kessel führt schon ein Ritual aus.");
        if(guardians.size()+casts.size()>=p.getConfig().getInt("maximum-ritual-mobs",20))throw new IllegalArgumentException("Zu viele aktive Rituale.");
        long now=System.currentTimeMillis(),until=p.state.getLong("ritual-cooldowns."+player.getUniqueId()+"."+id);
        if(until>now)throw new IllegalArgumentException("Ritual bereit in "+Rules.clock((until-now+999)/1000)+".");
        if(cfg.getString("mode").equals("REPAIR")&&p.equipment.repairSlots(player).isEmpty())throw new IllegalArgumentException("Trage oder halte beschädigte Custom-Ausrüstung für die Reparatur.");
        ItemStack[] inventory=player.getInventory().getStorageContents();var deductions=RitualPayments.plan(inventory,requirements(cfg),p.items::id);
        if(deductions==null)throw new IllegalArgumentException("Zutaten fehlen. Echte Custom-Materialien sind erforderlich; nichts verbraucht.");
        Cast cast=new Cast(player.getUniqueId(),cauldron,id,cfg); // Construct visuals before taking payment.
        for(var deduction:deductions.entrySet()){
            ItemStack item=inventory[deduction.getKey()];ItemStack taken=item.clone();taken.setAmount(deduction.getValue());cast.payment.add(taken);
            int remaining=item.getAmount()-deduction.getValue();if(remaining==0)player.getInventory().setItem(deduction.getKey(),null);else{item.setAmount(remaining);player.getInventory().setItem(deduction.getKey(),item);}
        }
        casts.put(cast.key,cast);p.state.set("pending-rituals."+cast.key+".owner",player.getUniqueId().toString());p.state.set("pending-rituals."+cast.key+".items",cast.payment);
        p.state.set("ritual-cooldowns."+player.getUniqueId()+"."+id,now+cfg.getLong("cooldown-seconds",120)*1000);player.saveData();p.saveState();cast.runTaskTimer(p,0,2);
        p.tell(player,"&d"+MobArmy.color(cfg.getString("name"))+" beginnt. Bleib am Kessel; dein Katalysator bleibt erhalten.");
    }
    void tick(){
        for(var entry:new ArrayList<>(guardians.entrySet())){
            Entity entity=Bukkit.getEntity(entry.getKey());if(entity==null||entry.getValue().expires()<System.currentTimeMillis()){
                if(entity!=null)entity.remove();guardians.remove(entry.getKey());Player owner=Bukkit.getPlayer(entry.getValue().owner());if(owner!=null)p.tell(owner,"Der Ritualwächter ist verschwunden. Kein Sieg-Loot.");
            }
        }
    }
    void cancel(Player player,boolean all){
        if(all&&!player.hasPermission("mobarmy.admin"))throw new IllegalArgumentException("Alle Rituale beenden erfordert Admin-Rechte.");
        for(Cast cast:new ArrayList<>(casts.values()))if(cast.owner.equals(player.getUniqueId())||all)cast.abort();
        for(var entry:new ArrayList<>(guardians.entrySet()))if(entry.getValue().owner().equals(player.getUniqueId())||all){
            Entity entity=Bukkit.getEntity(entry.getKey());if(entity!=null)entity.remove();guardians.remove(entry.getKey());
        }
        p.tell(player,"Rituale/Wächter beendet. Keine Sieg-Beute; angefangene Zeremonien erstatten Zutaten.");
    }
    boolean killed(LivingEntity entity,List<ItemStack> drops){
        Trial trial=guardians.remove(entity.getUniqueId());if(trial==null)return false;drops.clear();
        if(entity.getKiller()==null){Player owner=Bukkit.getPlayer(trial.owner());if(owner!=null)p.tell(owner,"Der Ritualwächter wurde ohne Spieler-Sieg beseitigt. Kein Sieg-Loot.");return true;}
        var cfg=trial.recipe();String result=cfg.getString("result","");
        if(result.isBlank()){String pool=Math.random()<cfg.getDouble("legendary-chance",0)?cfg.getString("legendary-pool"):cfg.getString("pool");result=LootTables.choose(p.getConfig().getConfigurationSection("loot-pools."+pool),Math.random());}
        p.items.deliver(trial.owner(),p.items.make(result,1));Player owner=Bukkit.getPlayer(trial.owner());if(owner!=null)p.tell(owner,"&aRitualprüfung geschafft! Die Beute ist im Inventar oder /raid loot.");return true;
    }
    final class Cast extends BukkitRunnable {
        final UUID key=UUID.randomUUID(),owner;final Location cauldron,center;final String id;final ConfigurationSection cfg;final List<ItemStack> payment=new ArrayList<>();
        final ItemDisplay orb;final TextDisplay label;final Color color;int ticks;boolean done,committed;
        Cast(UUID owner,Location cauldron,String id,ConfigurationSection cfg){
            this.owner=owner;this.cauldron=cauldron.clone();this.id=id;this.cfg=cfg;center=cauldron.clone().add(.5,1,.5);
            List<Integer> rgb=cfg.getIntegerList("color");color=rgb.size()==3?Color.fromRGB(rgb.get(0),rgb.get(1),rgb.get(2)):Color.PURPLE;
            orb=center.getWorld().spawn(center,ItemDisplay.class);orb.setItemStack(p.items.make(cfg.getString("display-item","ritual_core"),1));orb.setPersistent(false);orb.getPersistentDataContainer().set(p.entityKey,org.bukkit.persistence.PersistentDataType.STRING,"ritual-animation");
            var t=orb.getTransformation();t.getScale().set(.6f);orb.setTransformation(t);orb.setInterpolationDuration(2);
            TextDisplay text;try{text=center.getWorld().spawn(center.clone().add(0,2,0),TextDisplay.class);Presentation.style(text);}catch(RuntimeException e){orb.remove();throw e;}label=text;label.getPersistentDataContainer().set(p.entityKey,org.bukkit.persistence.PersistentDataType.STRING,"ritual-animation");
        }
        @Override public void run(){
            Player player=Bukkit.getPlayer(owner);if(player==null||!player.isOnline()||!locationValid(player,cauldron)||!cfg.getString("catalyst","chaos_fragment").equals(p.items.id(player.getInventory().getItemInMainHand()))){abort();return;}
            int duration=Math.max(40,cfg.getInt("duration-seconds",8)*20);ticks+=2;double progress=Math.min(1,(double)ticks/duration);
            String phase=progress<.25?"KREIS ZIEHEN":progress<.55?"ENERGIE SAMMELN":progress<.85?"FORMEN":"VOLLENDEN";
            label.setText(MobArmy.color(p.text(player,cfg.getString("name"))+"\n&f"+p.text(player,phase)+" &d"+(int)(progress*100)+"%"));orb.teleport(center.clone().add(0,.2+Math.sin(ticks*.07)*.15+progress*.6,0));orb.setRotation(ticks*5,0);
            for(int ring=0;ring<2;ring++)for(int point=0;point<12;point++){
                double a=point*Math.PI/6+ticks*.06*(ring==0?1:-1),radius=ring==0?1.6-progress*.4:.9+progress*.2;
                center.getWorld().spawnParticle(Particle.DUST,center.clone().add(Math.cos(a)*radius,-.7+ring*(.3+progress*.7),Math.sin(a)*radius),1,0,0,0,0,new Particle.DustOptions(color,1));
            }
            if(progress>.25)for(int i=0;i<4;i++){double a=ticks*.12+i*Math.PI/2;center.getWorld().spawnParticle(Particle.REVERSE_PORTAL,center.clone().add(Math.cos(a)*.6,progress*1.6,Math.sin(a)*.6),2,0,0,0,.03);}
            if(ticks%40==2)center.getWorld().playSound(center,Sound.BLOCK_AMETHYST_BLOCK_CHIME,.6f,.6f+(float)progress);
            if(progress<1)return;
            try{complete(player);cleanup();}catch(RuntimeException e){p.getLogger().warning("Ritual "+id+" failed: "+e.getMessage());abort();}
        }
        void complete(Player player){
            switch(cfg.getString("mode")){
                case "FORGE"->{String result=LootTables.choose(p.getConfig().getConfigurationSection("loot-pools."+cfg.getString("pool")),Math.random());ItemStack reward=p.items.make(result,1);commit();p.items.deliver(owner,reward);}
                case "REPAIR"->{if(p.equipment.repairSlots(player).isEmpty())throw new IllegalStateException("Keine beschädigte Ausrüstung mehr gehalten/getragen");commit();int repaired=p.equipment.repair(player);player.saveData();p.tell(player,"&a"+repaired+" Ausrüstungsteile vollständig repariert.");}
                case "BUFF"->{List<PotionEffect> effects=new ArrayList<>();for(var effect:cfg.getMapList("effects")){PotionEffectType type=PotionEffectType.getByName(Objects.toString(effect.get("type")));if(type!=null)effects.add(new PotionEffect(type,((Number)effect.get("seconds")).intValue()*20,((Number)effect.get("amplifier")).intValue(),true,true,true));}commit();double radius=cfg.getDouble("radius",6);for(Player target:center.getWorld().getPlayers())if(target.getLocation().distanceSquared(center)<=radius*radius)effects.forEach(target::addPotionEffect);}
                case "SUMMON"->{summon(player);commit();}
                default->throw new IllegalArgumentException("Unknown ritual mode");
            }
            center.getWorld().spawnParticle(Particle.EXPLOSION,center,2,.3,.5,.3,0);center.getWorld().spawnParticle(Particle.ENCHANT,center,100,.8,1,.8,.3);center.getWorld().playSound(center,Sound.BLOCK_BEACON_ACTIVATE,1,.7f);
            p.tell(player,"&aRitual vollendet. Ergebnisse landen im Inventar oder /raid loot.");
        }
        void commit(){committed=true;p.state.set("pending-rituals."+key,null);p.saveState();}
        void summon(Player player){
            Location at=safeSummon();Entity e=center.getWorld().spawnEntity(at,EntityType.valueOf(cfg.getString("type","HUSK")));
            if(!(e instanceof Mob mob)){e.remove();throw new IllegalArgumentException("Guardian must be a mob");}
            try{mob.setPersistent(false);mob.setRemoveWhenFarAway(false);mob.getPersistentDataContainer().set(p.entityKey,org.bukkit.persistence.PersistentDataType.STRING,"ritual-guardian");
                double hp=Math.min(1024,cfg.getDouble("health",80));Engine.set(mob,org.bukkit.attribute.Attribute.MAX_HEALTH,hp);mob.setHealth(hp);Engine.set(mob,org.bukkit.attribute.Attribute.ATTACK_DAMAGE,cfg.getDouble("damage",6));
                mob.setCustomName(MobArmy.color(cfg.getString("guardian-name","&5Ritualwächter")));mob.setCustomNameVisible(true);mob.setCanPickupItems(false);mob.setGlowing(true);mob.setTarget(player);
                if(mob.getEquipment()!=null){mob.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));mob.getEquipment().setHelmetDropChance(0);}
                guardians.put(mob.getUniqueId(),new Trial(owner,cfg,System.currentTimeMillis()+600000));
            }catch(RuntimeException error){mob.remove();throw error;}
        }
        Location safeSummon(){
            for(int dx:new int[]{2,-2,0})for(int dz:new int[]{2,-2,0})for(int dy=1;dy>=-2;dy--){Location at=cauldron.clone().add(dx+.5,dy,dz+.5);if(at.getBlock().isPassable()&&at.clone().add(0,1,0).getBlock().isPassable()&&at.clone().subtract(0,1,0).getBlock().getType().isSolid())return at;}
            throw new IllegalStateException("Kein freier Wächter-Spawn am Kessel");
        }
        void cleanup(){if(done)return;done=true;cancel();orb.remove();label.remove();casts.remove(key);p.state.set("pending-rituals."+key,null);p.saveState();}
        void abort(){if(done)return;cleanup();if(!committed)for(ItemStack item:payment)p.items.store(owner,item);Player player=Bukkit.getPlayer(owner);if(player!=null)p.tell(player,committed?"&eRitual beendet; das Ergebnis wurde bereits ausgelöst.":"&eRitual abgebrochen. Zutaten zurück in /raid loot; Katalysator bleibt erhalten. Cooldown läuft weiter.");}
    }
    void shutdown(){for(Cast cast:new ArrayList<>(casts.values()))cast.abort();for(UUID id:guardians.keySet()){Entity e=Bukkit.getEntity(id);if(e!=null)e.remove();}guardians.clear();}
}
