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
            for(Entity e:w.getNearbyEntities(region.center(),3,3,3))if(e instanceof TextDisplay&&("portal:"+id).equals(e.getPersistentDataContainer().get(plugin.entityKey,org.bukkit.persistence.PersistentDataType.STRING)))e.remove();
            label=w.spawn(region.center(),TextDisplay.class);Presentation.style(label);label.getPersistentDataContainer().set(plugin.entityKey,org.bukkit.persistence.PersistentDataType.STRING,"portal:"+id);
        }
        String timer=rewards()?"&aRewards &f"+Rules.clock((deadline-System.currentTimeMillis())/1000):bossKilled?"&7Boss bereits abgeschlossen":"&7Training · keine Rewards";
        String status=!open?"&cGESCHLOSSEN":session!=null&&session.phase!=Session.Phase.LOBBY?"&cSTAGE "+session.stage+" LÄUFT":"&aBETRETEN";
        label.setText(MobArmy.color("&d&l◆ MOB ARMY ◆\n&f"+id+" &8· &b"+week+"\n"+status+"\n"+timer+"\n&7Hineingehen → Vorraum"));
    }
}
