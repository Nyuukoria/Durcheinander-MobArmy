package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.attribute.*;
import org.bukkit.boss.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;

final class Engine {
    final MobArmy p;
    final Map<UUID,String> rituals=new HashMap<>();
    final Map<Session,Long> nextWave=new HashMap<>();
    final Map<UUID,Long> entryTimes=new HashMap<>();
    Engine(MobArmy p){this.p=p;}
    List<Player> players(Session s){return s.members.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).toList();}
    boolean join(Player player,Portal portal){
        UUID id=player.getUniqueId();long now=System.currentTimeMillis();
        if(!player.hasPermission("mobarmy.play")||now-entryTimes.getOrDefault(id,0L)<2000)return false;
        entryTimes.put(id,now);
        if(p.session(id)!=null)return false;
        if(!portal.open){p.tell(player,"Portal geschlossen.");return false;}
        if(portal.session!=null&&portal.session.phase!=Session.Phase.LOBBY){p.tell(player,"Während einer Stage ist kein Beitritt möglich.");return false;}
        if(portal.session!=null&&portal.session.dead.contains(id)){p.tell(player,"Du bist in diesem Durchlauf ausgeschieden.");return false;}
        if(portal.day.equals(p.today())&&p.state.getLong("cooldowns."+p.today()+"."+id)>now){p.tell(player,"Wipe-Sperre aktiv. /raid status zeigt die Portalinfos.");return false;}
        // One physical arena can serve only one portal at a time.
        if(p.portals.values().stream().anyMatch(q->q!=portal&&q.week.equals(portal.week)&&q.session!=null)){p.tell(player,"Diese Arena wird schon benutzt.");return false;}
        if(portal.session==null){
            Session s=new Session(portal,p.arenas.get(portal.week),id);
            int progress=p.state.getInt("progress."+portal.id+"."+id,1);
            s.stage=Math.max(1,Math.min(5,progress));portal.session=s;
            p.announcement(portal,"join","");
        }
        Session s=portal.session;s.members.add(id);s.participants.add(id);
        p.returns.put(id,portal.exit.clone());p.saveState();
        if(!player.teleport(s.arena.lobbySpawn)){leave(player,false);return false;}
        player.playSound(player.getLocation(),Sound.BLOCK_PORTAL_TRAVEL,0.4f,1.2f);
        p.menus.party(player,0);return true;
    }
    void ready(Player player){
        Session s=p.session(player.getUniqueId());if(s==null)return;
        if(!s.leader.equals(player.getUniqueId())){p.tell(player,"Nur der Leader kann starten.");return;}
        if(s.phase!=Session.Phase.LOBBY)return;
        if(s.members.size()<s.arena.minimum()){p.tell(player,"Mindestens "+s.arena.minimum()+" Spieler erforderlich.");return;}
        s.uses.clear();s.appleTimes.clear();s.pearlTimes.clear();s.votes.clear();s.wave=0;s.miniboss=false;
        s.stagePlayers=s.members.size();s.arena.gate(false);s.phase(Session.Phase.ENTERING);
        players(s).forEach(pl->{pl.closeInventory();pl.sendTitle("Stage "+s.stage,"Betretet die Arena – das Tor wartet auf alle.",10,70,10);});
    }
    void tick(){
        for(Portal portal:p.portals.values()){
            portal.tick();Session s=portal.session;if(s==null)continue;
            long now=System.currentTimeMillis();
            if(s.members.isEmpty()){finish(s);continue;}
            if(s.phase==Session.Phase.ENTERING){
                boolean all=players(s).size()==s.members.size()&&players(s).stream().allMatch(pl->s.arena.bounds.contains(pl.getLocation()));
                if(all){s.arena.gate(true);s.phase(Session.Phase.FIGHT);nextWave.put(s,now+2000);p.saveState();}
                else if(now-s.phaseSince>p.getConfig().getLong("entry-timeout-seconds",120)*1000){s.phase(Session.Phase.LOBBY);players(s).forEach(pl->pl.teleport(s.arena.lobbySpawn));}
            }
            if(s.phase==Session.Phase.FIGHT){
                // A removed/unloaded entity is not a kill. Retry the current stage instead of granting progress.
                if(s.mobs.stream().anyMatch(id->Bukkit.getEntity(id)==null)){abortStage(s,"Ein Raid-Mob fehlt. Stage bitte neu starten.");continue;}
                if(s.bar!=null&&!s.mobs.isEmpty()){
                    var boss=s.mobs.stream().map(Bukkit::getEntity).filter(e->e instanceof LivingEntity).map(e->(LivingEntity)e).findFirst();
                    boss.ifPresent(e->s.bar.setProgress(Math.max(0,Math.min(1,e.getHealth()/e.getAttribute(Attribute.MAX_HEALTH).getValue()))));
                }
                if(s.mobs.isEmpty()&&nextWave.getOrDefault(s,Long.MAX_VALUE)<=now)spawnWave(s);
                aggro(s);
            }
            if(s.phase==Session.Phase.LOOT){
                s.drops.removeIf(id->{Entity e=Bukkit.getEntity(id);return e==null||!e.isValid();});
                if(s.drops.isEmpty()||now-s.phaseSince>=p.getConfig().getLong("loot-timeout-seconds",120)*1000)finish(s);
            }
        }
    }
    void spawnWave(Session s){
        nextWave.remove(s);
        int next=s.wave+1;
        var stage=s.arena.stage(s.stage);
        if(stage==null){abortStage(s,"Stage fehlt in der Config.");return;}
        boolean mini=s.stage<5&&next>3;
        boolean boss=s.stage==5&&next==5;
        var wave=mini?stage.getConfigurationSection("miniboss"):boss?stage.getConfigurationSection("boss"):stage.getConfigurationSection("waves."+next);
        if(wave==null){abortStage(s,"Wave fehlt in der Config.");return;}
        Location spawn=spawnPoint(s);
        if(spawn==null){
            if(++s.misses>30){abortStage(s,"Kein freier Spawnpunkt mit Mindestabstand. Stage bitte neu starten.");return;}
            nextWave.put(s,System.currentTimeMillis()+1000);return;
        }
        s.misses=0;s.wave=next;s.miniboss=mini;
        try {
            if(mini||boss){
                LivingEntity mob=spawn(s,spawn,wave,true);
                s.bar=Bukkit.createBossBar(MobArmy.color(wave.getString("name",boss?"&dChaoswächter":"&cMiniboss")),boss?BarColor.PURPLE:BarColor.RED,BarStyle.SEGMENTED_10);
                players(s).forEach(s.bar::addPlayer);
                if(boss){
                    ItemStack model=p.items.make(wave.getString("model-item","boss_model"),1);
                    ItemDisplay display=s.arena.world.spawn(mob.getLocation(),ItemDisplay.class);
                    display.setItemStack(model);display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                    display.setPersistent(false);display.getPersistentDataContainer().set(p.entityKey,PersistentDataType.STRING,s.portal.id);
                    var transform=display.getTransformation();transform.getTranslation().set(0,-0.45f,0);display.setTransformation(transform);
                    mob.addPassenger(display);mob.setInvisible(true);
                }
            }else{
                var specs=wave.getMapList("mobs");int total=0;
                for(var spec:specs){
                    ConfigurationSection cfg=new org.bukkit.configuration.file.YamlConfiguration();spec.forEach((k,v)->cfg.set(String.valueOf(k),v));
                    int count=Rules.units(cfg.getInt("count",1),s.members.size(),s.arena.minimum(),p.getConfig().getDouble("balance.count-per-extra-player",0.25));
                    total+=count;if(total>p.getConfig().getInt("balance.maximum-wave-mobs",250))throw new IllegalArgumentException("Wave exceeds configured safety limit");
                    for(int i=0;i<count;i++){
                        Location at=spawnPoint(s);if(at==null)at=spawn;
                        LivingEntity mob=spawn(s,at,cfg,false);
                        String rider=cfg.getString("rider");
                        if(rider!=null&&next>=3){ConfigurationSection rc=new org.bukkit.configuration.file.YamlConfiguration();rc.set("type",rider);rc.set("health",cfg.getDouble("health",24));rc.set("damage",cfg.getDouble("damage",4));LivingEntity riding=spawn(s,at,rc,false);mob.addPassenger(riding);}
                    }
                }
                if(s.mobs.isEmpty())throw new IllegalArgumentException("Empty wave");
            }
            players(s).forEach(pl->pl.sendTitle("Stage "+s.stage,mini?"Miniboss":boss?"Chaoswächter":"Wave "+s.wave,5,40,5));
            aggro(s);
        }catch(Exception e){p.getLogger().log(java.util.logging.Level.SEVERE,"Failed to spawn wave",e);abortStage(s,"Fehler in der Wave-Config. Admin informieren.");}
    }
    Location spawnPoint(Session s){
        double min=p.getConfig().getDouble("balance.minimum-spawn-distance",8);List<Player> ps=players(s);
        return s.arena.spawns.stream().filter(l->l.getBlock().isPassable()&&l.clone().add(0,1,0).getBlock().isPassable()&&l.clone().subtract(0,1,0).getBlock().getType().isSolid())
            .filter(l->ps.stream().allMatch(pl->pl.getWorld().equals(l.getWorld())&&pl.getLocation().distanceSquared(l)>=min*min))
            .max(Comparator.comparingDouble(l->ps.stream().mapToDouble(pl->pl.getLocation().distanceSquared(l)).min().orElse(0))).map(Location::clone).orElse(null);
    }
    LivingEntity spawn(Session s,Location at,ConfigurationSection cfg,boolean boss){
        EntityType type=EntityType.valueOf(cfg.getString("type","ZOMBIE"));
        Entity e=at.getWorld().spawnEntity(at,type);
        if(!(e instanceof Mob mob)){e.remove();throw new IllegalArgumentException("Raid entities must be mobs: "+type);}
        s.mobs.add(e.getUniqueId());e.setPersistent(false);e.getPersistentDataContainer().set(p.entityKey,PersistentDataType.STRING,s.portal.id);
        double strength=s.arena.config.getDouble("strength",1)*(1+(s.stage-1)*s.arena.config.getDouble("stage-strength-step",0.15));
        double extra=1+Math.max(0,s.members.size()-s.arena.minimum())*p.getConfig().getDouble("balance.health-per-extra-player",0.04);
        set(mob,Attribute.MAX_HEALTH,Math.min(1024,cfg.getDouble("health",24)*strength*extra));mob.setHealth(mob.getAttribute(Attribute.MAX_HEALTH).getValue());
        set(mob,Attribute.ATTACK_DAMAGE,cfg.getDouble("damage",4)*strength);set(mob,Attribute.MOVEMENT_SPEED,cfg.getDouble("speed",0.24));
        set(mob,Attribute.ARMOR,cfg.getDouble("armor",0));set(mob,Attribute.KNOCKBACK_RESISTANCE,cfg.getDouble("knockback-resistance",boss?0.5:0.1));set(mob,Attribute.FOLLOW_RANGE,80);
        set(mob,Attribute.SPAWN_REINFORCEMENTS,0);mob.setRemoveWhenFarAway(false);mob.setCanPickupItems(false);
        if(mob instanceof Zombie z)z.setBaby(false);
        EntityEquipment eq=mob.getEquipment();if(eq!=null){
            eq.setHelmet(new ItemStack(Material.IRON_HELMET));eq.setHelmetDropChance(0);eq.setItemInMainHand(new ItemStack(type==EntityType.SKELETON?Material.BOW:Material.IRON_SWORD));eq.setItemInMainHandDropChance(0);
            var equipment=cfg.getConfigurationSection("equipment");if(equipment!=null)for(String slot:equipment.getKeys(false)){
                ItemStack item=new ItemStack(Material.valueOf(equipment.getString(slot)));switch(slot){case "helmet"->eq.setHelmet(item);case "chestplate"->eq.setChestplate(item);case "leggings"->eq.setLeggings(item);case "boots"->eq.setBoots(item);case "hand"->eq.setItemInMainHand(item);default->throw new IllegalArgumentException("Unknown equipment slot "+slot);}
            }
            eq.setChestplateDropChance(0);eq.setLeggingsDropChance(0);eq.setBootsDropChance(0);eq.setItemInOffHandDropChance(0);
        }
        if(boss){mob.setCustomName(MobArmy.color(cfg.getString("name","Miniboss")));mob.setCustomNameVisible(true);s.contribution.put(mob.getUniqueId(),new HashMap<>());}
        return mob;
    }
    static void set(LivingEntity mob,Attribute a,double value){AttributeInstance attr=mob.getAttribute(a);if(attr!=null)attr.setBaseValue(value);}
    void aggro(Session s){List<Player> ps=players(s).stream().filter(pl->!pl.isDead()&&s.arena.bounds.contains(pl.getLocation())).toList();if(ps.isEmpty())return;int i=0;for(UUID id:s.mobs){Entity e=Bukkit.getEntity(id);if(e instanceof Mob m)m.setTarget(ps.get(i++%ps.size()));}}
    void killed(LivingEntity entity,List<ItemStack> drops){
        if(rituals.containsKey(entity.getUniqueId())){String result=rituals.remove(entity.getUniqueId());drops.clear();if(!result.isEmpty())drops.add(p.items.make(result,1));return;}
        Session s=find(entity.getUniqueId());if(s==null)return;
        for(Entity passenger:entity.getPassengers())if(passenger instanceof ItemDisplay)passenger.remove();
        drops.clear();boolean rewards=s.portal.rewards();
        if(rewards) {
            String source=s.contribution.containsKey(entity.getUniqueId())?(s.stage==5?"boss":"miniboss"):"mob";
            List<ItemStack> custom=p.items.drops(source,s.stage,s.portal.week);
            if(source.equals("boss")){for(ItemStack item:custom)p.items.deliver(s.leader,item);}
            else drops.addAll(custom);
        }
        s.mobs.remove(entity.getUniqueId());s.contribution.remove(entity.getUniqueId());
        if(s.mobs.isEmpty()){
            if(s.bar!=null){s.bar.removeAll();s.bar=null;}
            if(s.miniboss||(s.stage==5&&s.wave==5))completeStage(s,rewards);
            else nextWave.put(s,System.currentTimeMillis()+p.getConfig().getLong("wave-delay-seconds",4)*1000);
        }
    }
    Session find(UUID mob){return p.portals.values().stream().map(q->q.session).filter(Objects::nonNull).filter(s->s.mobs.contains(mob)).findFirst().orElse(null);}
    void completeStage(Session s,boolean wasRewarded){
        if(wasRewarded)p.announcement(s.portal,"stage",s.arena.stage(s.stage).getString("name","Stage "+s.stage));
        s.arena.gate(false);restoreBlocks(s);
        if(s.stage==5){s.portal.bossKilled=true;s.phase(Session.Phase.LOOT);p.state.set("loot-recipients."+s.leader,s.participants.stream().map(UUID::toString).toList());for(UUID id:s.participants)p.state.set("progress."+s.portal.id+"."+id,1);}
        else{s.stage++;p.state.set("progress."+s.portal.id+"."+s.leader,s.stage);s.phase(Session.Phase.LOBBY);players(s).forEach(pl->pl.teleport(s.arena.lobbySpawn));}
        p.saveState();
    }
    void leave(Player player,boolean died){
        UUID id=player.getUniqueId();Session s=p.session(id);if(s==null)return;
        s.members.remove(id);s.votes.remove(id);s.votes.values().removeIf(id::equals);
        if(died)s.dead.add(id);else{p.returns.remove(id);if(player.isOnline())player.teleport(s.portal.exit);}
        if(s.bar!=null)s.bar.removePlayer(player);
        if(s.members.isEmpty()){
            if(died){long until=System.currentTimeMillis()+p.getConfig().getLong("wipe-cooldown-minutes",30)*60000;for(UUID member:s.participants){p.state.set("progress."+s.portal.id+"."+member,1);if(s.portal.day.equals(p.today()))p.state.set("cooldowns."+p.today()+"."+member,until);}}
            finish(s);
        }else if(s.leader.equals(id)){List<UUID> ids=new ArrayList<>(s.members);s.leader=ids.get(new Random().nextInt(ids.size()));p.state.set("progress."+s.portal.id+"."+s.leader,s.stage);}
        p.saveState();
    }
    void vote(Player voter,UUID candidate){Session s=p.session(voter.getUniqueId());if(s==null||s.phase!=Session.Phase.LOBBY||!s.members.contains(candidate))return;s.votes.put(voter.getUniqueId(),candidate);long votes=s.votes.values().stream().filter(candidate::equals).count();if(Rules.majority((int)votes,s.members.size())){s.leader=candidate;s.votes.clear();p.state.set("progress."+s.portal.id+"."+candidate,s.stage);p.saveState();}}
    void abortStage(Session s,String message){clearMobs(s);s.arena.gate(false);restoreBlocks(s);s.phase(Session.Phase.LOBBY);s.wave=0;players(s).forEach(pl->{pl.teleport(s.arena.lobbySpawn);p.tell(pl,message);});}
    void clearMobs(Session s){nextWave.remove(s);for(UUID id:s.mobs){Entity e=Bukkit.getEntity(id);if(e!=null){for(Entity passenger:e.getPassengers())passenger.remove();e.remove();}}s.mobs.clear();s.contribution.clear();if(s.bar!=null){s.bar.removeAll();s.bar=null;}}
    void restoreBlocks(Session s){
        for(var entry:s.blocks.entrySet()) {
            org.bukkit.block.Block block=entry.getKey().getBlock();ItemStack returned=Recovery.placedItem(block);
            if(!returned.getType().isAir())p.items.deliver(entry.getValue(),returned);
        }
        s.blockOriginal.forEach((l,d)->l.getBlock().setBlockData(d,false));s.blocks.clear();s.blockOriginal.clear();p.saveState();
    }
    void finish(Session s){clearMobs(s);s.arena.gate(false);restoreBlocks(s);for(UUID id:s.members){Player pl=Bukkit.getPlayer(id);if(pl!=null&&!pl.isDead()){pl.closeInventory();pl.teleport(s.portal.exit);p.returns.remove(id);}}s.portal.session=null;p.saveState();}
    void shutdown(){for(Portal portal:p.portals.values())if(portal.session!=null)finish(portal.session);for(UUID id:rituals.keySet()){Entity e=Bukkit.getEntity(id);if(e!=null)e.remove();}}
}
