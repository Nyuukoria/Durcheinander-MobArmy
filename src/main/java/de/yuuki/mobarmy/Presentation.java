package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import java.util.*;

final class Presentation {
    final MobArmy p;
    final List<TextDisplay> signs=new ArrayList<>();
    long ticks;
    Presentation(MobArmy p){this.p=p;for(Arena a:p.arenas.values())if(a.config.getBoolean("lobby-sign",a.config.getBoolean("generate-demo")))lobbySign(a);}
    void reloadArena(Arena a){signs.removeIf(sign->{if(sign.getWorld().getName().equals(a.world.getName())){sign.remove();return true;}return false;});if(a.config.getBoolean("lobby-sign",a.config.getBoolean("generate-demo")))lobbySign(a);}
    void lobbySign(Arena a){
        TextDisplay text=a.world.spawn(a.lobbySpawn.clone().add(0,2.8,-3.5),TextDisplay.class);
        style(text);text.setText(MobArmy.color("&d&lVORBEREITUNG / PREPARATION\n&f/raid &7→ Leader vote\n&f/rezepte &7→ Rezepte / Recipes\n&7Start: nur Leader / leader only"));
        text.getPersistentDataContainer().set(p.entityKey,PersistentDataType.STRING,"lobby:"+a.id);signs.add(text);
    }
    static void style(TextDisplay text){
        text.setBillboard(Display.Billboard.CENTER);text.setPersistent(false);text.setLineWidth(280);text.setShadowed(true);text.setSeeThrough(false);text.setDefaultBackground(false);
        text.setBackgroundColor(Color.fromARGB(185,18,12,35));text.setViewRange(0.5f);text.setAlignment(TextDisplay.TextAlignment.CENTER);
        var t=text.getTransformation();t.getScale().set(0.8f);text.setTransformation(t);
    }
    void animate(){if(++ticks%2!=0)return;for(Portal portal:p.portals.values())portalParticles(portal);}
    void portalParticles(Portal portal){
        World w=Bukkit.getWorld(portal.region.world());if(w==null)return;
        Location center=new Location(w,(portal.region.x1()+portal.region.x2()+1)/2.0,(portal.region.y1()+portal.region.y2()+1)/2.0,(portal.region.z1()+portal.region.z2()+1)/2.0);
        double distance=p.getConfig().getDouble("presentation.portal-particle-range",28);
        if(w.getPlayers().stream().noneMatch(pl->pl.getLocation().distanceSquared(center)<distance*distance))return;
        boolean xy=portal.region.x2()-portal.region.x1()>=portal.region.z2()-portal.region.z1();
        double width=(xy?portal.region.x2()-portal.region.x1()+1:portal.region.z2()-portal.region.z1()+1)/2.0,height=Math.max(1.2,(portal.region.y2()-portal.region.y1()+1)/2.0);
        boolean locked=portal.lockReason()!=null;Color primary=locked?Color.fromRGB(230,76,92):Color.fromRGB(213,103,255);
        for(int i=0;i<12;i++){
            double angle=i*Math.PI/6+ticks*0.028,u=Math.cos(angle)*width,v=Math.sin(angle)*height;
            Location at=center.clone().add(xy?u:0,v,xy?0:u);w.spawnParticle(Particle.DUST,at,1,0,0,0,0,new Particle.DustOptions(!locked&&i%3==0?Color.fromRGB(97,235,235):primary,1.0f));
        }
        if(locked){if(ticks%10==0)for(int i=-5;i<=5;i++)for(int sign:new int[]{-1,1}){double u=width*.75*i/5.0,v=sign*height*.75*i/5.0;w.spawnParticle(Particle.DUST,center.clone().add(xy?u:0,v,xy?0:u),1,0,0,0,0,new Particle.DustOptions(primary,1.2f));}return;}
        double spin=ticks*0.055;Location core=center.clone().add(xy?Math.cos(spin)*width*0.45:0,Math.sin(spin*1.3)*height*0.65,xy?0:Math.cos(spin)*width*0.45);
        w.spawnParticle(Particle.REVERSE_PORTAL,core,3,0.12,0.18,0.12,0.03);
        if(ticks%20==0)w.spawnParticle(Particle.ENCHANT,center,6,width*0.3,height*0.3,0.15,0.3);
    }
    void hud(Session s){
        List<Player> players=p.engine.players(s);Set<UUID> present=new HashSet<>();players.forEach(pl->present.add(pl.getUniqueId()));
        for(UUID id:new ArrayList<>(s.privateHud.keySet()))if(!present.contains(id))remove(s,id);
        for(Player pl:players){
            BossBar hud=s.privateHud.computeIfAbsent(pl.getUniqueId(),id->{BossBar bar=Bukkit.createBossBar("",BarColor.BLUE,BarStyle.SEGMENTED_10);bar.addPlayer(pl);return bar;});
            String wave=p.tr(pl,s.miniboss?"raid.miniboss":s.stage==5&&s.wave==5?"raid.boss":"raid.wave-title","wave",s.wave);
            String phase=switch(s.phase){case LOBBY->p.tr(pl,"hud.lobby");case ENTERING->p.tr(pl,"hud.entering","seconds",Math.max(0,p.getConfig().getLong("presentation.entry-countdown-seconds",3)-(System.currentTimeMillis()-s.phaseSince)/1000));case FIGHT->p.tr(pl,"hud.fight","wave",wave,"count",s.mobs.size());case LOOT->p.tr(pl,"hud.loot");};
            hud.setTitle(MobArmy.color(p.tr(pl,"hud.title","arena",s.arena.id,"stage",s.stage,"phase",phase)));hud.setColor(s.phase==Session.Phase.FIGHT?BarColor.PURPLE:BarColor.BLUE);
            hud.setProgress(s.phase==Session.Phase.FIGHT&&s.waveStartMobs>0?Math.min(1,(double)s.mobs.size()/s.waveStartMobs):Math.min(1,(s.stage-1)/5.0));
            if(s.bar!=null){BossBar boss=s.privateBoss.computeIfAbsent(pl.getUniqueId(),id->{BossBar bar=Bukkit.createBossBar("",BarColor.RED,BarStyle.SEGMENTED_10);bar.addPlayer(pl);return bar;});boss.setTitle(MobArmy.color(p.text(pl,s.bar.getTitle())));boss.setColor(s.bar.getColor());boss.setProgress(s.bar.getProgress());}
            else{BossBar boss=s.privateBoss.remove(pl.getUniqueId());if(boss!=null)boss.removeAll();}
            Player lead=Bukkit.getPlayer(s.leader);String leader=lead==null?"—":lead.getName(),timer=s.portal.rewards()?p.tr(pl,"hud.rewards","time",Rules.clock((s.portal.deadline-System.currentTimeMillis())/1000)):p.tr(pl,"hud.training"),extra="";
            if(s.bar!=null&&!s.contribution.isEmpty()){
                UUID mob=s.contribution.keySet().iterator().next();double used=s.contribution.get(mob).getOrDefault(pl.getUniqueId(),0.0);Entity entity=Bukkit.getEntity(mob);
                if(entity instanceof LivingEntity living){double hp=living.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();extra=p.tr(pl,"hud.share","used",String.format(Locale.ROOT,"%.1f",Rules.percentUsed(used,hp)),"limit",String.format(Locale.ROOT,"%.1f",100*Rules.bossShare(s.arena.minimum())));}
            }
            String text=MobArmy.color(p.tr(pl,"hud.action","leader",leader,"count",s.members.size(),"minimum",s.arena.minimum(),"rewards",timer,"share",extra));pl.spigot().sendMessage(ChatMessageType.ACTION_BAR,TextComponent.fromLegacyText(text));
        }
    }
    void remove(Session s,UUID player){BossBar hud=s.privateHud.remove(player),boss=s.privateBoss.remove(player);if(hud!=null)hud.removeAll();if(boss!=null)boss.removeAll();}
    void clear(Session s){s.privateHud.values().forEach(BossBar::removeAll);s.privateBoss.values().forEach(BossBar::removeAll);s.privateHud.clear();s.privateBoss.clear();}
    void burst(Location at){at.getWorld().spawnParticle(Particle.REVERSE_PORTAL,at.clone().add(0,1,0),50,0.45,0.7,0.45,0.1);}
    void ritualFinale(Location at){at.getWorld().spawnParticle(Particle.EXPLOSION,at,2,.3,.5,.3,0);at.getWorld().spawnParticle(Particle.ENCHANT,at,100,.8,1,.8,.3);at.getWorld().playSound(at,Sound.BLOCK_BEACON_ACTIVATE,1,.7f);}
    void transfer(Player player){burst(player.getLocation());player.playSound(player.getLocation(),Sound.BLOCK_PORTAL_TRAVEL,0.4f,1.2f);}
    void shutdown(){signs.forEach(Entity::remove);signs.clear();}
}
