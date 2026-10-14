package de.yuuki.mobarmy;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

final class ArenaChecks {
    static boolean overlap(Region a,Region b){return a.world().equals(b.world())&&a.x1()<=b.x2()&&a.x2()>=b.x1()&&a.y1()<=b.y2()&&a.y2()>=b.y1()&&a.z1()<=b.z2()&&a.z2()>=b.z1();}
    static List<String> layout(ConfigurationSection cfg){
        List<String> out=new ArrayList<>();String world=cfg.getString("world");
        try{
            Region arena=Region.read(cfg,"arena",world),lobby=Region.read(cfg,"lobby",world),door=Region.read(cfg,"door",world);
            if((long)arena.x2()-arena.x1()+1>100||(long)arena.z2()-arena.z1()+1>100||(long)arena.y2()-arena.y1()+1>128||oversized(door,10000)||oversized(lobby,100000))out.add("size");
            if(overlap(arena,lobby))out.add("overlap");
            for(String field:List.of("arena-spawn","lobby-spawn")){var xyz=cfg.getDoubleList(field);Region r=field.equals("arena-spawn")?arena:lobby;
                if(xyz.size()<3||xyz.stream().anyMatch(v->!Double.isFinite(v))||xyz.get(0)<r.x1()||xyz.get(0)>=r.x2()+1||xyz.get(1)<r.y1()||xyz.get(1)>=r.y2()+1||xyz.get(2)<r.z1()||xyz.get(2)>=r.z2()+1)out.add("point");}
            var spawns=cfg.getStringList("spawn-points");if(spawns.size()<4)out.add("spawns");
            Set<String> unique=new HashSet<>();for(String s:spawns){String[] xyz=s.split(",");if(xyz.length!=3)throw new IllegalArgumentException();double x=Double.parseDouble(xyz[0]),y=Double.parseDouble(xyz[1]),z=Double.parseDouble(xyz[2]);
                if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||x<arena.x1()||x>=arena.x2()+1||y<arena.y1()||y>=arena.y2()+1||z<arena.z1()||z>=arena.z2()+1)out.add("point");unique.add(s);}
            for(String key:List.of("recipes","start"))if(cfg.contains("controls."+key)){var xyz=cfg.getIntegerList("controls."+key+".location");if(xyz.size()!=3||xyz.get(0)<lobby.x1()||xyz.get(0)>lobby.x2()||xyz.get(1)<lobby.y1()||xyz.get(1)+1>lobby.y2()||xyz.get(2)<lobby.z1()||xyz.get(2)>lobby.z2())out.add("controls");}
            if(unique.size()<4&&!out.contains("spawns"))out.add("spawns");
        }catch(RuntimeException invalid){out.add("regions");}
        return out.stream().distinct().toList();
    }
    static boolean oversized(Region r,long max){long x=(long)r.x2()-r.x1()+1,y=(long)r.y2()-r.y1()+1,z=(long)r.z2()-r.z1()+1;return x>max||y>max||z>max||x*y>max||x*y*z>max;}
    static boolean standable(Location at){return at.getBlock().isPassable()&&!at.getBlock().isLiquid()&&at.clone().add(0,1,0).getBlock().isPassable()&&at.clone().add(0,2,0).getBlock().isPassable()&&at.clone().subtract(0,1,0).getBlock().getType().isSolid();}
    static List<String> world(ConfigurationSection cfg,World world){
        List<String> out=new ArrayList<>(layout(cfg));if(!out.isEmpty())return out;Region arena=Region.read(cfg,"arena",world.getName()),lobby=Region.read(cfg,"lobby",world.getName()),door=Region.read(cfg,"door",world.getName());
        if(arena.y1()<=world.getMinHeight()||arena.y2()+1>=world.getMaxHeight()||lobby.y1()<=world.getMinHeight()||lobby.y2()+1>=world.getMaxHeight()){out.add("height");return out;}
        for(String field:List.of("arena-spawn","lobby-spawn")){var v=cfg.getDoubleList(field);if(!standable(new Location(world,v.get(0),v.get(1),v.get(2))))out.add("clear");}
        for(String s:cfg.getStringList("spawn-points")){String[] v=s.split(",");if(!standable(new Location(world,Double.parseDouble(v[0]),Double.parseDouble(v[1]),Double.parseDouble(v[2]))))out.add("clear");}
        for(Region region:List.of(arena,lobby)){
            boolean hole=false;for(int x=region.x1();x<=region.x2()&&!hole;x++)for(int z=region.z1();z<=region.z2();z++)if(world.getBlockAt(x,region.y1()-1,z).isPassable()||world.getBlockAt(x,region.y2()+1,z).isPassable()){hole=true;break;}
            for(int y=region.y1();y<=region.y2()&&!hole;y++)for(int x=region.x1()-1;x<=region.x2()+1;x++)for(int z:new int[]{region.z1()-1,region.z2()+1})if(!door.contains(new Location(world,x,y,z))&&world.getBlockAt(x,y,z).isPassable())hole=true;
            for(int y=region.y1();y<=region.y2()&&!hole;y++)for(int z=region.z1();z<=region.z2();z++)for(int x:new int[]{region.x1()-1,region.x2()+1})if(!door.contains(new Location(world,x,y,z))&&world.getBlockAt(x,y,z).isPassable())hole=true;
            if(hole)out.add("shell");
        }
        return out.stream().distinct().toList();
    }
}
