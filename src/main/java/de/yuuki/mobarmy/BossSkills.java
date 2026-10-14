package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;
import java.util.*;

/** Announced, dodgeable attacks. No skill changes the arena's blocks or boss health. */
final class BossSkills {
    final MobArmy p;final NamespacedKey profile=new NamespacedKey("mobarmy","boss_profile");
    final Map<UUID,State> states=new HashMap<>();final Set<UUID> adds=new HashSet<>();long frames;
    static final Set<String> TYPES=Set.of("SLAM","CHARGE","SHOCKWAVE","FROST","NOVA","RAIN","MARK","PULL","METEOR","SUMMON");
    static final class State {int cycle;boolean enraged;long next;Attack attack;}
    record Attack(Session session,Mob mob,String type,Location origin,List<Location> marks,Vector direction,long impact){}
    BossSkills(MobArmy p){this.p=p;}
    static boolean area(String type,double dx,double dy,double dz,double vx,double vz){
        if(Math.abs(dy)>3)return false;double radius=Math.sqrt(dx*dx+dz*dz);
        if(type.equals("CHARGE")){double forward=dx*vx+dz*vz,lateral=Math.abs(dx*vz-dz*vx);return forward>=0&&forward<=7&&lateral<=1.2;}
        if(type.equals("SHOCKWAVE"))return radius>=3&&radius<=7;
        return radius<=(type.equals("NOVA")?4.5:type.equals("METEOR")?3:2.5);
    }
    void tick(){
        long now=System.currentTimeMillis();Set<UUID> alive=new HashSet<>(),allMobs=new HashSet<>();
        for(Portal portal:p.portals.values()){
            Session s=portal.session;if(s==null||s.phase!=Session.Phase.FIGHT)continue;allMobs.addAll(s.mobs);
            for(UUID id:new ArrayList<>(s.contribution.keySet())){
                Entity entity=Bukkit.getEntity(id);if(!(entity instanceof Mob mob)||mob.isDead())continue;alive.add(id);State state=states.computeIfAbsent(id,ignored->new State());
                String boss=mob.getPersistentDataContainer().get(profile,PersistentDataType.STRING);var cfg=boss==null?null:p.getConfig().getConfigurationSection("boss-types."+boss);List<String> skills=cfg==null?List.of("SLAM","SHOCKWAVE"):cfg.getStringList("skills");if(skills.isEmpty())skills=List.of("SLAM");
                if(!state.enraged&&mob.getHealth()<=mob.getAttribute(Attribute.MAX_HEALTH).getValue()*.5){state.enraged=true;var speed=mob.getAttribute(Attribute.MOVEMENT_SPEED);if(speed!=null)speed.setBaseValue(speed.getBaseValue()*1.12);for(Player pl:p.engine.players(s))p.say(pl,"combat.enrage");}
                if(state.attack!=null){if(now>=state.attack.impact()){resolve(state.attack,state.enraged);state.attack=null;state.next=now+(state.enraged?5000:7500);}continue;}
                if(now<state.next)continue;List<Player> players=p.engine.players(s).stream().filter(pl->!pl.isDead()&&pl.getWorld().equals(mob.getWorld())).toList();if(players.isEmpty())continue;
                Player target=players.get(state.cycle%players.size());String type=skills.get(state.cycle++%skills.size()).toUpperCase(Locale.ROOT);if(!TYPES.contains(type))type="SLAM";
                Location origin=mob.getLocation().clone();Vector v=target.getLocation().toVector().subtract(origin.toVector()).setY(0);v=v.lengthSquared()>.01?v.normalize():new Vector(0,0,1);
                List<Location> marks=new ArrayList<>();if(Set.of("NOVA","SHOCKWAVE","CHARGE","SUMMON").contains(type))marks.add(origin.clone());else marks.add(target.getLocation().clone());
                if(type.equals("RAIN")){marks.add(target.getLocation().clone().add(3,0,0));marks.add(target.getLocation().clone().add(-3,0,0));}
                state.attack=new Attack(s,mob,type,origin,marks,v,now+2000);
                for(Player pl:players)p.say(pl,"combat.skill","name",p.tr(pl,"combat.names."+type));
            }
        }
        states.keySet().retainAll(alive);adds.retainAll(allMobs);
    }
    void resolve(Attack attack,boolean enraged){
        Mob mob=attack.mob();Session s=attack.session();if(!mob.isValid()||s.phase!=Session.Phase.FIGHT||!s.mobs.contains(mob.getUniqueId()))return;String type=attack.type();
        GolemRig rig=p.bosses.rigs.get(mob.getUniqueId());if(rig!=null)rig.attack();
        for(Location mark:attack.marks()){mob.getWorld().spawnParticle(type.equals("METEOR")?Particle.LAVA:type.equals("FROST")||type.equals("NOVA")?Particle.SNOWFLAKE:Particle.ENCHANT,mark.clone().add(0,.7,0),35,1,.5,1,.06);mob.getWorld().playSound(mark,Sound.ENTITY_GENERIC_EXPLODE,.55f,type.equals("METEOR")?.8f:1.5f);}
        if(type.equals("SUMMON")){summon(s,mob);return;}
        if(type.equals("CHARGE"))mob.setVelocity(attack.direction().clone().multiply(.65).setY(.08));
        for(Player pl:p.engine.players(s)){
            if(pl.isDead()||!pl.getWorld().equals(mob.getWorld())||!mob.hasLineOfSight(pl))continue;boolean hit=false;
            for(Location mark:attack.marks()){Location at=pl.getLocation();if(area(type,at.getX()-mark.getX(),at.getY()-mark.getY(),at.getZ()-mark.getZ(),attack.direction().getX(),attack.direction().getZ())){hit=true;break;}}
            if(!hit)continue;double damage=Set.of("METEOR","SLAM").contains(type)?6:type.equals("SHOCKWAVE")?5:3;pl.damage(damage*(enraged?1.15:1),mob);
            if(type.equals("FROST")||type.equals("NOVA")||type.equals("RAIN"))pl.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,60,0,true,false,true));
            if(type.equals("MARK"))pl.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,120,0,true,false,true));
            if(type.equals("PULL")){Vector pull=mob.getLocation().toVector().subtract(pl.getLocation().toVector()).setY(0);if(pull.lengthSquared()>.01)pl.setVelocity(pull.normalize().multiply(.45).setY(.08));}
            if(type.equals("SHOCKWAVE")||type.equals("CHARGE"))UtilitySkills.push(pl,attack.origin(),.4);
        }
    }
    void summon(Session s,Mob boss){
        long existing=s.mobs.stream().filter(adds::contains).count();int maximum=p.getConfig().getInt("balance.maximum-wave-mobs",250);
        for(int i=(int)existing;i<3&&s.mobs.size()<maximum;i++){
            Location at=p.engine.spawnPoint(s);if(at==null)break;var cfg=new YamlConfiguration();cfg.set("type",boss instanceof Slime?"SLIME":"HUSK");cfg.set("health",16);cfg.set("damage",3);cfg.set("speed",.24);
            LivingEntity mob=p.engine.spawn(s,at,cfg,false);if(mob instanceof Slime slime){slime.setSize(1);Engine.set(mob,Attribute.MAX_HEALTH,16);mob.setHealth(16);}adds.add(mob.getUniqueId());if(mob instanceof Mob m)m.setTarget(p.engine.players(s).stream().findFirst().orElse(null));
        }s.waveStartMobs=Math.max(s.waveStartMobs,s.mobs.size());
    }
    void animate(){
        if(++frames%4!=0)return;
        for(State state:states.values()){Attack a=state.attack;if(a==null||!a.mob().isValid())continue;Color color=Set.of("FROST","NOVA","RAIN").contains(a.type())?Color.AQUA:a.type().equals("METEOR")?Color.ORANGE:Color.PURPLE;
            for(Location mark:a.marks()){
                if(a.type().equals("CHARGE")){for(int i=0;i<=7;i++)for(double side:new double[]{-1.2,1.2})dust(mark.clone().add(a.direction().getX()*i-a.direction().getZ()*side,.15,a.direction().getZ()*i+a.direction().getX()*side),color);}
                else for(double radius:a.type().equals("SHOCKWAVE")?new double[]{3,7}:new double[]{a.type().equals("NOVA")?4.5:a.type().equals("METEOR")?3:2.5})for(int point=0;point<20;point++){double angle=point*Math.PI/10;dust(mark.clone().add(Math.cos(angle)*radius,.15,Math.sin(angle)*radius),color);}
            }
        }
    }
    void dust(Location at,Color color){at.getWorld().spawnParticle(Particle.DUST,at,1,0,0,0,0,new Particle.DustOptions(color,1.2f));}
    void clear(){states.clear();adds.clear();}
}
