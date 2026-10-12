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
    Presentation(MobArmy p){this.p=p;for(Arena a:p.arenas.values())lobbySign(a);}
    void lobbySign(Arena a){
        TextDisplay text=a.world.spawn(a.lobbySpawn.clone().add(0,2.8,-4),TextDisplay.class);
        style(text);text.setText(MobArmy.color("&d&lVORBEREITUNG\n&f/raid &7→ Leader wählen & Öffnen\n&7Der Leader startet mit &aReady\n&7Nach jeder Stage kommt ihr hierher zurück."));
        text.getPersistentDataContainer().set(p.entityKey,PersistentDataType.STRING,"lobby:"+a.id);signs.add(text);
    }
    static void style(TextDisplay text){
        text.setBillboard(Display.Billboard.CENTER);text.setPersistent(false);text.setLineWidth(280);text.setShadowed(true);text.setSeeThrough(false);text.setDefaultBackground(false);
        text.setBackgroundColor(Color.fromARGB(185,18,12,35));text.setViewRange(0.5f);text.setAlignment(TextDisplay.TextAlignment.CENTER);
        var t=text.getTransformation();t.getScale().set(0.8f);text.setTransformation(t);
    }
    void animate(){if(++ticks%2!=0)return;for(Portal portal:p.portals.values())portalParticles(portal);}
    void portalParticles(Portal portal){
        World w=Bukkit.getWorld(portal.region.world());if(w==null||!portal.open)return;
        Location center=new Location(w,(portal.region.x1()+portal.region.x2()+1)/2.0,(portal.region.y1()+portal.region.y2()+1)/2.0,(portal.region.z1()+portal.region.z2()+1)/2.0);
        double distance=p.getConfig().getDouble("presentation.portal-particle-range",28);
        if(w.getPlayers().stream().noneMatch(pl->pl.getLocation().distanceSquared(center)<distance*distance))return;
        boolean xy=portal.region.x2()-portal.region.x1()>=portal.region.z2()-portal.region.z1();
        double width=(xy?portal.region.x2()-portal.region.x1()+1:portal.region.z2()-portal.region.z1()+1)/2.0,height=Math.max(1.2,(portal.region.y2()-portal.region.y1()+1)/2.0);
        Color primary=portal.session!=null&&portal.session.phase!=Session.Phase.LOBBY?Color.fromRGB(230,76,92):Color.fromRGB(213,103,255);
        for(int i=0;i<12;i++){
            double angle=i*Math.PI/6+ticks*0.028,u=Math.cos(angle)*width,v=Math.sin(angle)*height;
            Location at=center.clone().add(xy?u:0,v,xy?0:u);w.spawnParticle(Particle.DUST,at,1,0,0,0,0,new Particle.DustOptions(i%3==0?Color.fromRGB(97,235,235):primary,1.0f));
        }
        double spin=ticks*0.055;Location core=center.clone().add(xy?Math.cos(spin)*width*0.45:0,Math.sin(spin*1.3)*height*0.65,xy?0:Math.cos(spin)*width*0.45);
        w.spawnParticle(Particle.REVERSE_PORTAL,core,3,0.12,0.18,0.12,0.03);
        if(ticks%20==0)w.spawnParticle(Particle.ENCHANT,center,6,width*0.3,height*0.3,0.15,0.3);
    }
    void hud(Session s){
        if(s.hud==null)s.hud=Bukkit.createBossBar("",BarColor.BLUE,BarStyle.SEGMENTED_10);
        List<Player> ps=p.engine.players(s);Set<Player> current=new HashSet<>(ps);for(Player old:new ArrayList<>(s.hud.getPlayers()))if(!current.contains(old))s.hud.removePlayer(old);ps.forEach(s.hud::addPlayer);
        String phase=switch(s.phase){case LOBBY->"Vorraum · Leader startet mit /raid";case ENTERING->"Teleport in "+Math.max(0,p.getConfig().getLong("presentation.entry-countdown-seconds",3)-(System.currentTimeMillis()-s.phaseSince)/1000)+"s";case FIGHT->(s.miniboss?"Miniboss":s.stage==5&&s.wave==5?"Endboss":"Wave "+s.wave)+" · Gegner: "+s.mobs.size();case LOOT->"Beute einsammeln · Rückkehr folgt";};
        s.hud.setTitle(MobArmy.color("&d"+s.arena.id+" &7| &fStage "+s.stage+"/5 &7| &f"+phase));
        s.hud.setColor(s.phase==Session.Phase.FIGHT?BarColor.PURPLE:BarColor.BLUE);
        s.hud.setProgress(s.phase==Session.Phase.FIGHT&&s.waveStartMobs>0?Math.min(1,(double)s.mobs.size()/s.waveStartMobs):Math.min(1,(s.stage-1)/5.0));
        Player lead=Bukkit.getPlayer(s.leader);String leader=lead==null?"—":lead.getName();
        String timer=s.portal.rewards()?"&aRewards "+Rules.clock((s.portal.deadline-System.currentTimeMillis())/1000):"&7Keine Rewards";
        for(Player pl:ps){
            String extra="";if(s.bar!=null&&!s.mobs.isEmpty()){
                UUID mob=s.mobs.iterator().next();double used=s.contribution.getOrDefault(mob,Map.of()).getOrDefault(pl.getUniqueId(),0.0);Entity entity=Bukkit.getEntity(mob);
                if(entity instanceof LivingEntity living){double hp=living.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();extra=String.format(Locale.ROOT," &7| &fDein Boss-Anteil %.0f/%.0f%%",100*used/hp,100*Rules.bossShare(s.arena.minimum()));}
            }
            String text=MobArmy.color("&fLeader: &d"+leader+" &7| &fGruppe: "+s.members.size()+"/"+s.arena.minimum()+"+ &7| "+timer+extra);
            pl.spigot().sendMessage(ChatMessageType.ACTION_BAR,TextComponent.fromLegacyText(text));
        }
    }
    void burst(Location at){at.getWorld().spawnParticle(Particle.REVERSE_PORTAL,at.clone().add(0,1,0),50,0.45,0.7,0.45,0.1);}
    void transfer(Player player){burst(player.getLocation());player.playSound(player.getLocation(),Sound.BLOCK_PORTAL_TRAVEL,0.4f,1.2f);}
    void shutdown(){signs.forEach(Entity::remove);signs.clear();}
}
