package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.util.Vector;
import java.util.*;

final class Motion implements Listener {
    final MobArmy p;
    final Map<UUID,Double> lastSpiderY=new HashMap<>();
    final Map<UUID,Double> blockedSpiderY=new HashMap<>();
    final Map<UUID,Long> impacts=new HashMap<>(),scaled=new HashMap<>();
    Motion(MobArmy p){this.p=p;}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void windKnockback(EntityKnockbackEvent e){
        if(p.arena(e.getEntity().getWorld())==null)return;
        boolean wind=e instanceof EntityKnockbackByEntityEvent by&&by.getSourceEntity() instanceof AbstractWindCharge;
        if(!wind&&e.getCause().name().contains("WIND"))wind=true;
        if(!wind&&e.getCause().name().contains("EXPLOSION")&&impacts.getOrDefault(e.getEntity().getUniqueId(),0L)>System.currentTimeMillis())wind=true;
        if(!wind)return;
        Vector velocity=e.getFinalKnockback().multiply(p.getConfig().getDouble("balance.wind-charge-multiplier",0.65));
        double limit=p.getConfig().getDouble("balance.wind-charge-max-horizontal",0.9),up=p.getConfig().getDouble("balance.wind-charge-max-upward",0.75);
        double h=Math.hypot(velocity.getX(),velocity.getZ());if(h>limit){velocity.setX(velocity.getX()*limit/h);velocity.setZ(velocity.getZ()*limit/h);}velocity.setY(Math.min(velocity.getY(),up));e.setFinalKnockback(velocity);scaled.put(e.getEntity().getUniqueId(),System.currentTimeMillis()+250);
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void impact(ProjectileHitEvent e){if(e.getEntity() instanceof AbstractWindCharge&&p.arena(e.getEntity().getWorld())!=null)for(Entity entity:e.getEntity().getNearbyEntities(5,5,5))if(entity instanceof LivingEntity)impacts.put(entity.getUniqueId(),System.currentTimeMillis()+250);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void windVelocity(org.bukkit.event.player.PlayerVelocityEvent e){
        UUID id=e.getPlayer().getUniqueId();long now=System.currentTimeMillis();if(p.arena(e.getPlayer().getWorld())==null||impacts.getOrDefault(id,0L)<now||scaled.getOrDefault(id,0L)>now)return;
        Vector v=e.getVelocity().multiply(p.getConfig().getDouble("balance.wind-charge-multiplier",.65));double limit=p.getConfig().getDouble("balance.wind-charge-max-horizontal",.9),h=Math.hypot(v.getX(),v.getZ());if(h>limit){v.setX(v.getX()*limit/h);v.setZ(v.getZ()*limit/h);}v.setY(Math.min(v.getY(),p.getConfig().getDouble("balance.wind-charge-max-upward",.75)));e.setVelocity(v);scaled.put(id,now+250);
    }
    void tick(){
        long now=System.currentTimeMillis();impacts.values().removeIf(t->t<now);scaled.values().removeIf(t->t<now);
        Set<UUID> alive=new HashSet<>();
        for(Portal portal:p.portals.values()){
            Session s=portal.session;if(s==null||s.phase!=Session.Phase.FIGHT)continue;
            for(UUID id:s.mobs){Entity entity=Bukkit.getEntity(id);if(!(entity instanceof Spider spider))continue;alive.add(id);
                Location at=spider.getLocation();double last=lastSpiderY.getOrDefault(id,at.getY());
                int surface=surfaces(spider,s);boolean original=(surface&2)!=0,allowed=Rules.mayClimb((surface&1)!=0,original);
                if(!allowed&&original&&(spider.isClimbing()||blockedSpiderY.containsKey(id))){
                    last=blockedSpiderY.computeIfAbsent(id,key->lastSpiderY.getOrDefault(key,at.getY()));
                    Vector v=spider.getVelocity();v.setY(Math.min(0,v.getY()));spider.setVelocity(v);
                    if(at.getY()>last+0.035){
                        Location down=at.clone();down.setY(last);
                        List<Entity> riders=new ArrayList<>(spider.getPassengers());riders.forEach(spider::removePassenger);
                        if(!spider.teleport(down))last=at.getY();
                        for(Entity rider:riders){rider.teleport(spider.getLocation());spider.addPassenger(rider);}
                    }
                    lastSpiderY.put(id,Math.min(last,spider.getLocation().getY()));
                }else{blockedSpiderY.remove(id);lastSpiderY.put(id,at.getY());}
            }
        }
        lastSpiderY.keySet().retainAll(alive);
        blockedSpiderY.keySet().retainAll(alive);
    }
    int surfaces(Spider spider,Session s){
        var box=spider.getBoundingBox();boolean owned=false,original=false;World w=spider.getWorld();
        int minX=(int)Math.floor(box.getMinX()-0.08),maxX=(int)Math.floor(box.getMaxX()+0.08),minZ=(int)Math.floor(box.getMinZ()-0.08),maxZ=(int)Math.floor(box.getMaxZ()+0.08);
        for(int y=(int)Math.floor(box.getMinY()+0.05);y<=(int)Math.floor(box.getMaxY());y++)for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++){
            if(x>minX&&x<maxX&&z>minZ&&z<maxZ)continue;Block b=w.getBlockAt(x,y,z);if(!b.getType().isSolid())continue;
            if(s.blocks.containsKey(b.getLocation()))owned=true;else original=true;
        }
        return (owned?1:0)|(original?2:0);
    }
}
