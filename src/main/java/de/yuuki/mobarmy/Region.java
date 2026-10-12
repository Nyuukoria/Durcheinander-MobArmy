package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import java.util.function.Consumer;

public record Region(String world, int x1, int y1, int z1, int x2, int y2, int z2) {
    public static Region between(Location a, Location b) {
        if (!a.getWorld().equals(b.getWorld())) throw new IllegalArgumentException("Both positions must be in the same world");
        return new Region(a.getWorld().getName(), Math.min(a.getBlockX(), b.getBlockX()), Math.min(a.getBlockY(),b.getBlockY()), Math.min(a.getBlockZ(),b.getBlockZ()), Math.max(a.getBlockX(),b.getBlockX()), Math.max(a.getBlockY(),b.getBlockY()), Math.max(a.getBlockZ(),b.getBlockZ()));
    }
    public static Region read(ConfigurationSection c, String path, String world) {
        String prefix=path.isEmpty()?"":path+".";
        var a=c.getIntegerList(prefix+"min"); var b=c.getIntegerList(prefix+"max");
        if (a.size()!=3 || b.size()!=3) throw new IllegalArgumentException("Missing region: "+path);
        if (a.get(0)>b.get(0)||a.get(1)>b.get(1)||a.get(2)>b.get(2)) throw new IllegalArgumentException("Reversed region: "+path);
        return new Region(world,a.get(0),a.get(1),a.get(2),b.get(0),b.get(1),b.get(2));
    }
    public boolean contains(Location l) {
        return l!=null && l.getWorld()!=null && l.getWorld().getName().equals(world) && l.getX()>=x1 && l.getX()<x2+1 && l.getY()>=y1 && l.getY()<y2+1 && l.getZ()>=z1 && l.getZ()<z2+1;
    }
    public long volume() { return (long)(x2-x1+1)*(y2-y1+1)*(z2-z1+1); }
    public Location center() { return new Location(Bukkit.getWorld(world),(x1+x2+1)/2.0,y2+2,(z1+z2+1)/2.0); }
    public void each(Consumer<org.bukkit.block.Block> action) {
        World w=Bukkit.getWorld(world); if(w==null)return;
        for(int x=x1;x<=x2;x++)for(int y=y1;y<=y2;y++)for(int z=z1;z<=z2;z++)action.accept(w.getBlockAt(x,y,z));
    }
}
