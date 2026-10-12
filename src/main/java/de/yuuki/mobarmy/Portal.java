package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import java.time.*;
import java.util.*;

final class Portal {
    final MobArmy plugin;
    final String id, week;
    final Region region;
    final Location exit;
    boolean open, bossKilled;
    long deadline;
    LocalDate day;
    TextDisplay label;
    Session session;
    Portal(MobArmy plugin,String id,String week,Region region,Location exit){this.plugin=plugin;this.id=id;this.week=week;this.region=region;this.exit=exit;this.day=plugin.today();}
    boolean rewards(){return Rules.rewards(bossKilled,deadline,day,System.currentTimeMillis(),plugin.zone);}
    void setTimer(long duration){day=plugin.today();deadline=System.currentTimeMillis()+duration;plugin.saveState();}
    void save(ConfigurationSection c){c.set("week",week);c.set("world",region.world());c.set("min",List.of(region.x1(),region.y1(),region.z1()));c.set("max",List.of(region.x2(),region.y2(),region.z2()));c.set("exit",exit);c.set("open",open);c.set("boss-killed",bossKilled);c.set("deadline",deadline);c.set("event-day",day.toString());}
    static Portal load(MobArmy p,String id,ConfigurationSection c){
        Portal out=new Portal(p,id,c.getString("week"),Region.read(c,"",c.getString("world")),c.getLocation("exit"));
        out.open=c.getBoolean("open");out.bossKilled=c.getBoolean("boss-killed");out.deadline=c.getLong("deadline");out.day=LocalDate.parse(c.getString("event-day"));return out;
    }
    void removeLabel(){if(label!=null){label.remove();label=null;}}
    void tick(){
        World w=Bukkit.getWorld(region.world());if(w==null)return;
        if(label==null||!label.isValid()) {
            // Remove persisted labels from a previous server process.
            for(Entity e:w.getNearbyEntities(region.center(),2,3,2))if(e instanceof TextDisplay&&e.getPersistentDataContainer().has(plugin.entityKey))e.remove();
            label=w.spawn(region.center(),TextDisplay.class);label.setBillboard(Display.Billboard.CENTER);label.setPersistent(false);label.getPersistentDataContainer().set(plugin.entityKey,org.bukkit.persistence.PersistentDataType.STRING,"portal");
        }
        String timer=rewards()?Math.max(0,(deadline-System.currentTimeMillis())/1000)/60+" min":"Keine Rewards";
        label.setText(MobArmy.color("&d"+id+" &7• "+week+"\n"+(open?"&aOffen":"&cGeschlossen")+" &7• "+timer+(session!=null&&session.phase!=Session.Phase.LOBBY?"\n&cStage läuft":"")));
        if(open)for(int i=0;i<8;i++){Location l=new Location(w,region.x1()+Math.random()*(region.x2()-region.x1()+1),region.y1()+Math.random()*(region.y2()-region.y1()+1),region.z1()+Math.random()*(region.z2()-region.z1()+1));w.spawnParticle(Particle.PORTAL,l,2,0,0,0,0.1);}
    }
}
