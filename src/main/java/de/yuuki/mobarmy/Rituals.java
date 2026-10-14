package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.util.Vector;
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
    record Trial(UUID owner,ConfigurationSection recipe,long expires,Cast cast){}
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
    String ingredientName(Player player,RitualPayments.Requirement required){return required.customId()!=null?MobArmy.color(p.language.itemName(player,required.customId())):p.text(player,ingredientName(required));}
    ItemStack icon(Player player,ConfigurationSection cfg){
        ItemStack icon=p.items.make(cfg.getString("display-item","ritual_core"),1);var meta=icon.getItemMeta();meta.setDisplayName(MobArmy.color(p.language.recipeName(player,cfg)));meta.setLore(TextLayout.wrap(cfg.getStringList(p.language.code(player).equals("en")?"description-en":"description").stream().map(MobArmy::color).toList(),36));meta.addItemFlags(ItemFlag.HIDE_ENCHANTS,ItemFlag.HIDE_ATTRIBUTES);icon.setItemMeta(meta);return icon;
    }
    boolean locationValid(Player player,Location cauldron){return cauldron.getWorld()!=null&&player.getWorld().equals(cauldron.getWorld())&&player.getLocation().distanceSquared(cauldron.clone().add(.5,.5,.5))<=36&&cauldron.getBlock().getType().name().endsWith("CAULDRON")&&p.arena(player.getWorld())==null&&player.getWorld().getName().equals(p.getConfig().getString("ritual-world","world"));}
    void open(Player player,Block cauldron){
        if(!player.hasPermission("mobarmy.play"))return;
        if(!locationValid(player,cauldron.getLocation())){p.say(player,"ritual.place","world",p.getConfig().getString("ritual-world","world"));return;}
        View v=new View(player.getUniqueId(),cauldron.getLocation());v.inventory=Bukkit.createInventory(v,54,MobArmy.color(p.tr(player,"ritual.title")));int index=0;
        for(String id:ids()){
            if(index>=28)break;var cfg=recipe(id);ItemStack item=icon(player,cfg);var meta=item.getItemMeta();List<String> lore=new ArrayList<>(meta.hasLore()?meta.getLore():List.of());
            lore.add("");lore.add(MobArmy.color(p.tr(player,"gui.start-ritual")));lore.add(MobArmy.color(p.tr(player,"gui.read-ritual")));meta.setLore(lore);item.setItemMeta(meta);int slot=TextLayout.gridSlot(index++);v.recipes.put(slot,id);v.inventory.setItem(slot,item);
        }
        p.language.open(player,v.inventory);
    }
    @EventHandler public void click(InventoryClickEvent event){
        if(!(event.getView().getTopInventory().getHolder() instanceof View v))return;event.setCancelled(true);
        if(event.getWhoClicked() instanceof Player player&&v.owner.equals(player.getUniqueId())&&v.recipes.containsKey(event.getRawSlot())){
            String id=v.recipes.get(event.getRawSlot());if(event.isRightClick()){p.catalogue.recipe(player,id,"rituals",0);return;}player.closeInventory();try{start(player,v.cauldron,id);}catch(RuntimeException e){p.tell(player,"&c"+Objects.toString(e.getMessage(),"Ritual fehlgeschlagen"));}
        }
    }
    @EventHandler public void drag(InventoryDragEvent event){if(event.getView().getTopInventory().getHolder() instanceof View)event.setCancelled(true);}
    void start(Player player,Location cauldron,String id){
        if(!player.hasPermission("mobarmy.play"))throw new IllegalArgumentException(p.tr(player,"ritual.permission"));
        var cfg=recipe(id);if(!locationValid(player,cauldron))throw new IllegalArgumentException(p.tr(player,"ritual.location-lost"));
        if(!cfg.getString("catalyst","chaos_fragment").equals(p.items.id(player.getInventory().getItemInMainHand())))throw new IllegalArgumentException(p.tr(player,"ritual.catalyst"));
        if(casts.values().stream().anyMatch(c->c.owner.equals(player.getUniqueId())||c.cauldron.equals(cauldron)))throw new IllegalArgumentException(p.tr(player,"ritual.active"));
        if(casts.size()>=p.getConfig().getInt("maximum-ritual-mobs",20))throw new IllegalArgumentException(p.tr(player,"ritual.limit"));
        long now=System.currentTimeMillis(),until=p.state.getLong("ritual-cooldowns."+player.getUniqueId()+"."+id);
        if(until>now)throw new IllegalArgumentException(p.tr(player,"ritual.cooldown","time",Rules.clock((until-now+999)/1000)));
        if(cfg.getString("mode").equals("REPAIR")&&p.equipment.repairSlots(player).isEmpty())throw new IllegalArgumentException(p.tr(player,"ritual.repair-empty"));
        ItemStack[] inventory=player.getInventory().getStorageContents();int heldSlot=player.getInventory().getHeldItemSlot();ItemStack catalyst=inventory[heldSlot].clone();catalyst.setAmount(1);inventory[heldSlot]=null;var deductions=RitualPayments.plan(inventory,requirements(cfg),p.items::id);
        if(deductions==null)throw new IllegalArgumentException(p.tr(player,"ritual.ingredients-missing"));
        Cast cast=new Cast(player.getUniqueId(),cauldron,id,cfg); // Construct visuals and validate the trial space before payment.
        try{cast.safeSummon(player);}catch(RuntimeException error){cast.cleanupUnstarted();throw error;}
        cast.slot=heldSlot;cast.catalyst=catalyst;
        for(var deduction:deductions.entrySet()){
            ItemStack item=inventory[deduction.getKey()];ItemStack taken=item.clone();taken.setAmount(deduction.getValue());cast.payment.add(taken);
            int remaining=item.getAmount()-deduction.getValue();if(remaining==0)player.getInventory().setItem(deduction.getKey(),null);else{item.setAmount(remaining);player.getInventory().setItem(deduction.getKey(),item);}
        }
        ItemStack held=player.getInventory().getItem(heldSlot);if(held.getAmount()==1)player.getInventory().setItem(heldSlot,null);else{held.setAmount(held.getAmount()-1);player.getInventory().setItem(heldSlot,held);}
        cast.payment.add(catalyst);casts.put(cast.key,cast);p.state.set("pending-rituals."+cast.key+".owner",player.getUniqueId().toString());p.state.set("pending-rituals."+cast.key+".items",cast.payment);
        p.state.set("ritual-cooldowns."+player.getUniqueId()+"."+id,now+cfg.getLong("cooldown-seconds",120)*1000);player.saveData();p.saveState();cast.runTaskTimer(p,0,2);
        p.say(player,"ritual.begin","name",p.language.recipeName(player,cfg));p.say(player,"ritual.bound");
    }
    void tick(){
        for(var entry:new ArrayList<>(guardians.entrySet()))if(entry.getValue().expires()<System.currentTimeMillis())entry.getValue().cast().abort();
    }
    void cancel(Player player,boolean all){
        if(all&&!player.hasPermission("mobarmy.admin"))throw new IllegalArgumentException("Alle Rituale beenden erfordert Admin-Rechte.");
        for(Cast cast:new ArrayList<>(casts.values()))if(cast.owner.equals(player.getUniqueId())||all)cast.abort();
        p.say(player,"ritual.cancelled");
    }
    boolean killed(LivingEntity entity,List<ItemStack> drops){
        Trial trial=guardians.remove(entity.getUniqueId());if(trial==null)return false;drops.clear();Cast cast=trial.cast();
        Player owner=Bukkit.getPlayer(trial.owner());
        if(!cast.valid(owner)||!cast.ownerHit||entity.getKiller()==null){if(cast.valid(owner)&&!cast.ownerHit)p.say(owner,"ritual.owner-hit");cast.abort();return true;}
        try{cast.reward(owner);cast.cleanup();}catch(RuntimeException error){p.getLogger().warning("Ritual reward failed: "+error.getMessage());cast.abort();}return true;
    }
    Cast channel(UUID player){return casts.values().stream().filter(c->c.owner.equals(player)&&c.guardian==null&&!c.done).findFirst().orElse(null);}
    @EventHandler(ignoreCancelled=true) public void held(PlayerItemHeldEvent e){Cast c=channel(e.getPlayer().getUniqueId());if(c!=null&&e.getNewSlot()!=c.slot){e.setCancelled(true);c.lockHint(e.getPlayer());}}
    @EventHandler(ignoreCancelled=true) public void swap(PlayerSwapHandItemsEvent e){Cast c=channel(e.getPlayer().getUniqueId());if(c!=null){e.setCancelled(true);c.lockHint(e.getPlayer());}}
    @EventHandler(ignoreCancelled=true) public void drop(PlayerDropItemEvent e){if(channel(e.getPlayer().getUniqueId())!=null)e.setCancelled(true);}
    @EventHandler public void boundInventory(InventoryClickEvent e){if(channel(e.getWhoClicked().getUniqueId())!=null)e.setCancelled(true);}
    @EventHandler public void boundDrag(InventoryDragEvent e){if(channel(e.getWhoClicked().getUniqueId())!=null)e.setCancelled(true);}
    @EventHandler public void quit(PlayerQuitEvent e){for(Cast c:new ArrayList<>(casts.values()))if(c.owner.equals(e.getPlayer().getUniqueId()))c.abort();}
    @EventHandler public void death(PlayerDeathEvent e){for(Cast c:new ArrayList<>(casts.values()))if(c.owner.equals(e.getEntity().getUniqueId()))c.abort();}
    Entity source(Entity entity){return entity instanceof Projectile pr&&pr.getShooter() instanceof Entity shooter?shooter:entity;}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void guardianDamage(EntityDamageEvent e){
        if(e instanceof EntityDamageByEntityEvent attack){Trial attacker=guardians.get(source(attack.getDamager()).getUniqueId());if(attacker!=null&&(!e.getEntity().getUniqueId().equals(attacker.owner())||attacker.cast().fightTicks<30)){e.setCancelled(true);return;}}
        Trial trial=guardians.get(e.getEntity().getUniqueId());if(trial==null)return;
        if(!(e instanceof EntityDamageByEntityEvent by)||!(source(by.getDamager()) instanceof Player)||trial.cast().fightTicks<30)e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void participant(EntityDamageByEntityEvent e){Trial t=guardians.get(e.getEntity().getUniqueId());if(t!=null&&source(e.getDamager()) instanceof Player player&&player.getUniqueId().equals(t.owner())&&e.getFinalDamage()>0)t.cast().ownerHit=true;}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void target(EntityTargetLivingEntityEvent e){Trial t=guardians.get(e.getEntity().getUniqueId());if(t!=null&&(e.getTarget()==null||!e.getTarget().getUniqueId().equals(t.owner())))e.setCancelled(true);}
    final class Cast extends BukkitRunnable {
        final UUID key=UUID.randomUUID(),owner;final Location cauldron,center;final String id;final ConfigurationSection cfg;final List<ItemStack> payment=new ArrayList<>();
        final ItemDisplay orb;final TextDisplay label;final Color color;final List<ItemDisplay> satellites=new ArrayList<>();final BossBar bar;
        ItemStack catalyst;Mob guardian;int slot,ticks,fightTicks;long lastHint;boolean done,committed,ownerHit,enraged;Location marked;int impactAt;
        Cast(UUID owner,Location cauldron,String id,ConfigurationSection cfg){
            this.owner=owner;this.cauldron=cauldron.clone();this.id=id;this.cfg=cfg;center=cauldron.clone().add(.5,1,.5);
            List<Integer> rgb=cfg.getIntegerList("color");color=rgb.size()==3?Color.fromRGB(rgb.get(0),rgb.get(1),rgb.get(2)):Color.PURPLE;
            orb=center.getWorld().spawn(center,ItemDisplay.class);orb.setItemStack(p.items.make(cfg.getString("display-item","ritual_core"),1));orb.setPersistent(false);orb.getPersistentDataContainer().set(p.entityKey,org.bukkit.persistence.PersistentDataType.STRING,"ritual-animation");
            var t=orb.getTransformation();t.getScale().set(.6f);orb.setTransformation(t);orb.setInterpolationDuration(2);
            TextDisplay text;try{text=center.getWorld().spawn(center.clone().add(0,2,0),TextDisplay.class);Presentation.style(text);}catch(RuntimeException e){orb.remove();throw e;}label=text;label.getPersistentDataContainer().set(p.entityKey,org.bukkit.persistence.PersistentDataType.STRING,"ritual-animation");
            bar=Bukkit.createBossBar("",BarColor.PURPLE,BarStyle.SEGMENTED_10);Player pl=Bukkit.getPlayer(owner);if(pl!=null)bar.addPlayer(pl);
            try{for(int i=0;i<3;i++){ItemDisplay shard=center.getWorld().spawn(center,ItemDisplay.class);shard.setItemStack(p.items.make("chaos_fragment",1));shard.setPersistent(false);shard.getPersistentDataContainer().set(p.entityKey,org.bukkit.persistence.PersistentDataType.STRING,"ritual-animation");var transform=shard.getTransformation();transform.getScale().set(.3f);shard.setTransformation(transform);shard.setInterpolationDuration(2);satellites.add(shard);}}catch(RuntimeException error){cleanupUnstarted();throw error;}
        }
        @Override public void run(){
            Player player=Bukkit.getPlayer(owner);if(!valid(player)){abort();return;}
            if(guardian!=null){fight(player);return;}
            int duration=Math.max(40,cfg.getInt("duration-seconds",8)*20);ticks+=2;double progress=Math.min(1,(double)ticks/duration);
            String phase=p.tr(player,"ritual.phase."+(progress<.25?"circle":progress<.55?"energy":progress<.85?"shape":"complete"));
            bar.setTitle(MobArmy.color(p.language.recipeName(player,cfg)+" · "+phase));bar.setProgress(progress);
            label.setText(MobArmy.color(p.language.recipeName(player,cfg)+"\n&f"+phase+" &d"+(int)(progress*100)+"%"));orb.teleport(center.clone().add(0,.2+Math.sin(ticks*.07)*.15+progress*.6,0));orb.setRotation(ticks*5,0);
            for(int ring=0;ring<2;ring++)for(int point=0;point<12;point++){
                double a=point*Math.PI/6+ticks*.06*(ring==0?1:-1),radius=ring==0?1.6-progress*.4:.9+progress*.2;
                center.getWorld().spawnParticle(Particle.DUST,center.clone().add(Math.cos(a)*radius,-.7+ring*(.3+progress*.7),Math.sin(a)*radius),1,0,0,0,0,new Particle.DustOptions(color,1));
            }
            if(progress>.25)for(int i=0;i<4;i++){double a=ticks*.12+i*Math.PI/2;center.getWorld().spawnParticle(Particle.REVERSE_PORTAL,center.clone().add(Math.cos(a)*.6,progress*1.6,Math.sin(a)*.6),2,0,0,0,.03);}
            for(int i=0;i<satellites.size();i++){double angle=ticks*.07+i*Math.PI*2/3;ItemDisplay shard=satellites.get(i);shard.teleport(center.clone().add(Math.cos(angle)*(1.2-progress*.5),.5+progress+Math.sin(angle*2)*.2,Math.sin(angle)*(1.2-progress*.5)));shard.setRotation(-ticks*4,0);}
            if(progress>.55&&ticks%4==0)for(int step=0;step<8;step++){double angle=ticks*.1+step*.8;center.getWorld().spawnParticle(Particle.END_ROD,center.clone().add(Math.cos(angle)*.4,step*.28,Math.sin(angle)*.4),1,0,0,0,0);}
            if(ticks==2||ticks==duration/2)center.getWorld().playSound(center,Sound.BLOCK_BEACON_AMBIENT,.8f,.6f);
            if(ticks%40==2)center.getWorld().playSound(center,Sound.BLOCK_AMETHYST_BLOCK_CHIME,.6f,.6f+(float)progress);
            if(progress<1)return;
            try{summon(player);p.say(player,"ritual.trial-start");center.getWorld().playSound(center,Sound.BLOCK_BEACON_ACTIVATE,1,.6f);center.getWorld().spawnParticle(Particle.ENCHANT,center,60,1,1,1,.2);}catch(RuntimeException e){p.getLogger().warning("Ritual "+id+" failed: "+e.getMessage());abort();}
        }
        void reward(Player player){
            switch(cfg.getString("mode")){
                case "FORGE"->{String result=LootTables.choose(p.getConfig().getConfigurationSection("loot-pools."+cfg.getString("pool")),Math.random());ItemStack reward=p.items.make(result,1);commit();p.items.deliver(owner,reward);}
                case "REPAIR"->{if(p.equipment.repairSlots(player).isEmpty())throw new IllegalStateException("Keine beschädigte Ausrüstung mehr gehalten/getragen");commit();int repaired=p.equipment.repair(player);player.saveData();p.say(player,"ritual.repaired","count",repaired);}
                case "BUFF"->{List<PotionEffect> effects=new ArrayList<>();for(var effect:cfg.getMapList("effects")){PotionEffectType type=PotionEffectType.getByName(Objects.toString(effect.get("type")));if(type!=null)effects.add(new PotionEffect(type,((Number)effect.get("seconds")).intValue()*20,((Number)effect.get("amplifier")).intValue(),true,true,true));}commit();for(Player target:buffTargets(center.getWorld().getPlayers(),owner,center,cfg.getDouble("radius",6)))effects.forEach(target::addPotionEffect);}
                case "SUMMON"->{String result=cfg.getString("result","");if(result.isBlank()){String pool=Math.random()<cfg.getDouble("legendary-chance",0)?cfg.getString("legendary-pool"):cfg.getString("pool");result=LootTables.choose(p.getConfig().getConfigurationSection("loot-pools."+pool),Math.random());}ItemStack reward=p.items.make(result,1);commit();p.items.deliver(owner,reward);}
                default->throw new IllegalArgumentException("Unknown ritual mode");
            }
            p.presentation.ritualFinale(center);p.say(player,"ritual.complete");
        }
        void commit(){committed=true;p.state.set("pending-rituals."+key,null);p.saveState();p.items.deliver(owner,catalyst);}
        void summon(Player player){
            Location at=safeSummon(player);at.setDirection(player.getLocation().toVector().subtract(at.toVector()));Entity e=center.getWorld().spawnEntity(at,EntityType.valueOf(cfg.getString("trial.type",cfg.getString("type","HUSK"))));
            if(!(e instanceof Mob mob)){e.remove();throw new IllegalArgumentException("Guardian must be a mob");}
            try{mob.setPersistent(false);mob.setRemoveWhenFarAway(false);mob.getPersistentDataContainer().set(p.entityKey,org.bukkit.persistence.PersistentDataType.STRING,"ritual-guardian");
                double hp=Math.min(1024,cfg.getDouble("trial.health",cfg.getDouble("health",80)));Engine.set(mob,org.bukkit.attribute.Attribute.MAX_HEALTH,hp);mob.setHealth(hp);Engine.set(mob,org.bukkit.attribute.Attribute.ATTACK_DAMAGE,cfg.getDouble("trial.damage",cfg.getDouble("damage",6)));
                mob.setCustomName(MobArmy.color(p.tr(player,"ritual.guardian-name","name",p.language.recipeName(player,cfg))));mob.setCustomNameVisible(true);mob.setCanPickupItems(false);mob.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,200,0,true,false,true));mob.setAI(false);mob.setTarget(player);
                if(mob.getEquipment()!=null){mob.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));mob.getEquipment().setHelmetDropChance(0);}
                if(mob.getEquipment()!=null&&cfg.contains("trial.weapon")){mob.getEquipment().setItemInMainHand(new ItemStack(Material.valueOf(cfg.getString("trial.weapon"))));mob.getEquipment().setItemInMainHandDropChance(0);}
                if(cfg.contains("trial.model-item"))p.bosses.model(mob,cfg.getConfigurationSection("trial"),"ritual-guardian");
                guardian=mob;guardians.put(mob.getUniqueId(),new Trial(owner,cfg,System.currentTimeMillis()+180000,this));bar.setColor(BarColor.RED);
            }catch(RuntimeException error){if(p.bosses!=null)p.bosses.unmodel(mob.getUniqueId());mob.remove();throw error;}
        }
        Location safeSummon(Player player){
            for(int radius=6;radius<=9;radius++)for(int i=0;i<16;i++)for(int dy=1;dy>=-2;dy--){double angle=i*Math.PI/8;Location at=cauldron.clone().add(Math.cos(angle)*radius+.5,dy,Math.sin(angle)*radius+.5);
                if(center.getWorld().getPlayers().stream().anyMatch(pl->pl.getLocation().distanceSquared(at)<25))continue;
                if(at.getBlock().isPassable()&&at.clone().add(0,1,0).getBlock().isPassable()&&at.clone().add(0,2,0).getBlock().isPassable()&&at.clone().subtract(0,1,0).getBlock().getType().isSolid())return at;
            }
            throw new IllegalStateException(p.tr(player,"ritual.space"));
        }
        void fight(Player player){
            if(!guardian.isValid()||guardian.isDead()){abort();return;}fightTicks+=2;
            if(guardian.getLocation().distanceSquared(center)>625){abort();return;}
            if(fightTicks==30){guardian.setAI(true);guardian.setTarget(player);}
            double maximum=guardian.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue(),health=guardian.getHealth();
            bar.setProgress(Math.max(0,Math.min(1,health/maximum)));String title=p.tr(player,"ritual.trial-bar","name",p.language.recipeName(player,cfg),"health",(int)Math.ceil(health),"maximum",(int)maximum);bar.setTitle(MobArmy.color(title));label.setText(MobArmy.color(title));
            if(!enraged&&health<=maximum*.5){enraged=true;Engine.set(guardian,org.bukkit.attribute.Attribute.MOVEMENT_SPEED,.28);p.say(player,"ritual.enraged");center.getWorld().playSound(center,Sound.ENTITY_IRON_GOLEM_HURT,.8f,.7f);}
            if(fightTicks%4==0)for(int i=0;i<satellites.size();i++){double angle=fightTicks*.05+i*Math.PI*2/3;satellites.get(i).teleport(center.clone().add(Math.cos(angle)*1.1,1+Math.sin(angle)*.2,Math.sin(angle)*1.1));}
            int period=enraged?120:180;
            if(marked==null&&(fightTicks==60||fightTicks>60&&fightTicks%period==0)){marked=player.getLocation().clone();impactAt=fightTicks+30;p.say(player,"ritual.dodge");player.playSound(player.getLocation(),Sound.BLOCK_NOTE_BLOCK_BELL,.8f,.6f);}
            if(marked!=null){
                for(int i=0;i<16;i++){double angle=i*Math.PI/8;center.getWorld().spawnParticle(Particle.DUST,marked.clone().add(Math.cos(angle)*2.4,.15,Math.sin(angle)*2.4),1,0,0,0,0,new Particle.DustOptions(fightTicks<impactAt-10?Color.YELLOW:Color.RED,1.2f));}
                if(fightTicks>=impactAt){
                    center.getWorld().spawnParticle(Particle.EXPLOSION,marked.clone().add(0,.3,0),1,0,0,0,0);center.getWorld().playSound(marked,Sound.ENTITY_GENERIC_EXPLODE,.6f,1.4f);
                    if(inStrike(player.getLocation(),marked)&&guardian.hasLineOfSight(player)){
                        player.damage(4,guardian);String attack=cfg.getString("trial.attack","PULSE");
                        if(attack.equals("FROST"))player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,60,0,true,false,true));
                        if(attack.equals("EMBER"))player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,60,0,true,false,true));
                        if(attack.equals("GALE")){Vector direction=player.getLocation().toVector().subtract(guardian.getLocation().toVector()).setY(0);if(direction.lengthSquared()>.01)player.setVelocity(direction.normalize().multiply(.5).setY(.15));}
                    }marked=null;
                }
            }
        }
        void lockHint(Player player){if(System.currentTimeMillis()-lastHint>1500){p.say(player,"ritual.channel-lock");lastHint=System.currentTimeMillis();}}
        boolean valid(Player player){return player!=null&&player.isOnline()&&!player.isDead()&&player.getWorld().equals(center.getWorld())&&cauldron.getBlock().getType().name().endsWith("CAULDRON")&&player.getLocation().distanceSquared(center)<=(guardian==null?36:400);}
        void cleanupUnstarted(){orb.remove();label.remove();satellites.forEach(Entity::remove);bar.removeAll();}
        void cleanup(){if(done)return;done=true;cancel();cleanupUnstarted();if(guardian!=null){guardians.remove(guardian.getUniqueId());if(p.bosses!=null)p.bosses.unmodel(guardian.getUniqueId());if(!guardian.isDead())guardian.remove();}casts.remove(key);p.state.set("pending-rituals."+key,null);p.saveState();}
        void abort(){if(done)return;cleanup();if(!committed)for(ItemStack item:payment)p.items.store(owner,item);Player player=Bukkit.getPlayer(owner);if(player!=null)p.say(player,committed?"ritual.complete":"ritual.refund");}
    }
    static boolean inStrike(Location player,Location mark){if(!player.getWorld().equals(mark.getWorld())||Math.abs(player.getY()-mark.getY())>3)return false;double dx=player.getX()-mark.getX(),dz=player.getZ()-mark.getZ();return dx*dx+dz*dz<=2.4*2.4;}
    static List<Player> buffTargets(List<Player> players,UUID owner,Location center,double radius){return players.stream().filter(pl->pl.getUniqueId().equals(owner)||pl.getLocation().distanceSquared(center)<=radius*radius).toList();}
    void shutdown(){for(Cast cast:new ArrayList<>(casts.values()))cast.abort();guardians.clear();}
}
