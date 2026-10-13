package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.entity.Player;
import java.util.*;

/** A pearl hit can land inside an entity or slightly inside the impacted block. */
final class Pearls {
    private Pearls(){}
    static List<Location> candidates(Location impact){
        List<Location> out=new ArrayList<>();out.add(impact.clone());
        for(int radius=0;radius<=3;radius++)for(int dy=2;dy>=-32;dy--)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
            out.add(new Location(impact.getWorld(),impact.getBlockX()+dx+.5,impact.getBlockY()+dy,impact.getBlockZ()+dz+.5,impact.getYaw(),impact.getPitch()));
        }
        out.sort(Comparator.comparingDouble(impact::distanceSquared));return out;
    }
    static Location safe(Player player,Arena arena,Location impact){
        Region b=arena.bounds;if(!impact.getWorld().equals(arena.world)||impact.getX()<b.x1()-.75||impact.getX()>b.x2()+1.75||impact.getZ()<b.z1()-.75||impact.getZ()>b.z2()+1.75||impact.getY()<b.y1()-1||impact.getY()>b.y2()+2)return null;
        Location center=impact.clone();center.setX(Math.max(b.x1()+.35,Math.min(b.x2()+.65,center.getX())));center.setZ(Math.max(b.z1()+.35,Math.min(b.z2()+.65,center.getZ())));center.setY(Math.max(b.y1(),Math.min(b.y2()-1,center.getY())));
        for(Location at:candidates(center)){
            if(!arena.bounds.contains(at)||!clear(at))continue;
            var box=player.getBoundingBox();double half=Math.max(.31,(box.getMaxX()-box.getMinX())/2),height=1.8;
            boolean room=true;
            for(double x:new double[]{at.getX()-half,at.getX()+half})for(double z:new double[]{at.getZ()-half,at.getZ()+half})for(double y:new double[]{at.getY()+.05,at.getY()+height-.05})
                if(!b.contains(new Location(at.getWorld(),x,y,z))||!new Location(at.getWorld(),x,y,z).getBlock().isPassable())room=false;
            if(room)return at;
        }
        return null;
    }
    static boolean clear(Location at){return at.getBlock().isPassable()&&at.clone().add(0,1,0).getBlock().isPassable()&&at.clone().subtract(0,.05,0).getBlock().getType().isSolid();}
}
