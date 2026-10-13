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
    final Map<String,TextDisplay> labels=new HashMap<>();final Map<UUID,String> shown=new HashMap<>();
    Session session;
    Portal(MobArmy plugin,String id,String week,Region region,Location exit){this.plugin=plugin;this.id=id;this.week=week;this.region=region;this.exit=exit;this.day=plugin.today();}
    boolean rewards(){return Rules.rewards(bossKilled,deadline,day,System.currentTimeMillis(),plugin.zone);}
    String lockReason(){
        if(!open)return "&cPortal geschlossen.";
        if(session!=null&&session.phase!=Session.Phase.LOBBY)return "&cStufe "+session.stage+" läuft – Beitritt gesperrt.";
        if(plugin.portals.values().stream().anyMatch(q->q!=this&&q.week.equals(week)&&q.session!=null))return "&cArena belegt – Beitritt gesperrt.";
        return null;
    }
    void setTimer(long duration){day=plugin.today();deadline=System.currentTimeMillis()+duration;plugin.saveState();}
    void save(ConfigurationSection c){c.set("week",week);c.set("world",region.world());c.set("min",List.of(region.x1(),region.y1(),region.z1()));c.set("max",List.of(region.x2(),region.y2(),region.z2()));c.set("exit",exit);c.set("open",open);c.set("boss-killed",bossKilled);c.set("deadline",deadline);c.set("event-day",day.toString());}
    static Portal load(MobArmy p,String id,ConfigurationSection c){
        Portal out=new Portal(p,id,c.getString("week"),Region.read(c,"",c.getString("world")),c.getLocation("exit"));
        out.open=c.getBoolean("open");out.bossKilled=c.getBoolean("boss-killed");out.deadline=c.getLong("deadline");out.day=LocalDate.parse(c.getString("event-day"));return out;
    }
    void removeLabel(){labels.values().forEach(Entity::remove);labels.clear();shown.clear();if(label!=null)label.remove();label=null;}
    boolean ownsLabel(UUID id){return labels.values().stream().anyMatch(e->e.getUniqueId().equals(id));}
    void refreshVisibility(Player player){shown.remove(player.getUniqueId());visibility(player);}
    void visibility(Player player){
        String lang=plugin.language==null?"de":plugin.language.code(player);if(lang.equals(shown.get(player.getUniqueId())))return;
        labels.forEach((code,text)->{if(code.equals(lang))player.showEntity(plugin,text);else player.hideEntity(plugin,text);});shown.put(player.getUniqueId(),lang);
    }
    void tick(){
        World w=Bukkit.getWorld(region.world());if(w==null)return;
        if(labels.size()!=2||labels.values().stream().anyMatch(e->!e.isValid())){
            removeLabel();
            for(Entity e:w.getNearbyEntities(region.center(),4,16,4))if(e instanceof TextDisplay||e instanceof ArmorStand){String tag=e.getPersistentDataContainer().get(plugin.entityKey,org.bukkit.persistence.PersistentDataType.STRING);if(("portal:"+id).equals(tag)||"portal".equals(tag)||tag!=null&&tag.startsWith("portal-v2:"+id+":"))e.remove();}
            for(String lang:List.of("de","en")){
                Location above=region.center();above.setY(region.y2()+2);
                TextDisplay text=w.spawn(above,TextDisplay.class,e->{Presentation.style(e);e.setVisibleByDefault(false);e.setLineWidth(440);var t=e.getTransformation();t.getScale().set(.65f);e.setTransformation(t);e.getPersistentDataContainer().set(plugin.entityKey,org.bukkit.persistence.PersistentDataType.STRING,"portal-v2:"+id+":"+lang);});labels.put(lang,text);
            }
            label=labels.get("de");
        }
        String reason=lockReason();String displayId=id.length()>20?id.substring(0,19)+"…":id;
        labels.forEach((lang,text)->{
            String timer=plugin.language==null?"Training":plugin.language.trCode(lang,rewards()?"portal.rewards":bossKilled?"portal.finished":"portal.training","time",Rules.clock((deadline-System.currentTimeMillis())/1000));
            String status=plugin.language==null?(open?"OPEN":"CLOSED"):plugin.language.trCode(lang,reason==null?"portal.open":!open?"portal.closed":session!=null?"portal.stage":"portal.busy","stage",session==null?1:session.stage);
            String hint=plugin.language==null?"":plugin.language.trCode(lang,reason==null?"portal.enter":"portal.wait");
            text.setText(MobArmy.color("&d&l◆ MOB ARMY ◆\n&f"+displayId+" &8· &b"+week+"\n"+status+"\n"+timer+"\n"+hint));
        });
        shown.keySet().removeIf(uuid->Bukkit.getPlayer(uuid)==null);for(Player player:w.getPlayers())visibility(player);
    }
}
