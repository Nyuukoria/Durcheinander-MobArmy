package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.block.data.BlockData;
import java.util.*;

public final class Arena {
    final String id;
    final ConfigurationSection config;
    final World world;
    final Region bounds, lobby, door;
    final Location lobbySpawn, arenaSpawn;
    final List<Location> spawns = new ArrayList<>();
    final Map<Location, BlockData> doorOriginal = new HashMap<>();
    public Arena(String id, ConfigurationSection config) {
        this.id=id; this.config=config;
        String name=config.getString("world", "raid_"+id);
        world=new WorldCreator(name).generator(new VoidGenerator()).generateStructures(false).createWorld();
        if(world==null)throw new IllegalStateException("Cannot load "+name);
        bounds=Region.read(config,"arena",name); lobby=Region.read(config,"lobby",name); door=Region.read(config,"door",name);
        if(bounds.x2()-bounds.x1()+1>100||bounds.z2()-bounds.z1()+1>100||door.volume()>10000)throw new IllegalArgumentException("Arena exceeds 100x100 or oversized door: "+id);
        lobbySpawn=point(config.getDoubleList("lobby-spawn")); arenaSpawn=point(config.getDoubleList("arena-spawn"));
        for(var s:config.getStringList("spawn-points")) { String[] xyz=s.split(","); spawns.add(new Location(world,Double.parseDouble(xyz[0]),Double.parseDouble(xyz[1]),Double.parseDouble(xyz[2]))); }
        if(spawns.isEmpty())throw new IllegalArgumentException("No spawn points for "+id);
        if(!lobby.contains(lobbySpawn)||!bounds.contains(arenaSpawn)||spawns.stream().anyMatch(l->!bounds.contains(l)))throw new IllegalArgumentException("Spawns outside configured regions: "+id);
        world.setGameRule(GameRule.KEEP_INVENTORY,true); world.setGameRule(GameRule.SPAWN_MOBS,false);
        world.setGameRule(GameRule.MOB_GRIEFING,false); world.setGameRule(GameRule.ADVANCE_TIME,false);
        world.setPVP(false);
        for(int x=bounds.x1()>>4;x<=bounds.x2()>>4;x++)for(int z=bounds.z1()>>4;z<=bounds.z2()>>4;z++)world.addPluginChunkTicket(x,z,org.bukkit.plugin.java.JavaPlugin.getPlugin(MobArmy.class));
        for(int x=lobby.x1()>>4;x<=lobby.x2()>>4;x++)for(int z=lobby.z1()>>4;z<=lobby.z2()>>4;z++)world.addPluginChunkTicket(x,z,org.bukkit.plugin.java.JavaPlugin.getPlugin(MobArmy.class));
        world.setTime(18000);
    }
    Location point(List<Double> v) { if(v.size()<3)throw new IllegalArgumentException("Spawn needs [x,y,z]");return new Location(world,v.get(0),v.get(1),v.get(2),v.size()>3?v.get(3).floatValue():0,0); }
    int minimum() { return org.bukkit.plugin.java.JavaPlugin.getPlugin(MobArmy.class).getConfig().getBoolean("testing.solo")?1:config.getInt("minimum-players",2); }
    ConfigurationSection stage(int n) { return config.getConfigurationSection("stages."+n); }
    Location entryPoint(int seed){
        for(int i=0;i<25;i++){
            int slot=Math.floorMod(seed+i,25);Location at=arenaSpawn.clone().add((slot%5)-2,0,(slot/5)-2);
            if(bounds.contains(at)&&at.getBlock().isPassable()&&at.clone().add(0,1,0).getBlock().isPassable()&&at.clone().subtract(0,1,0).getBlock().getType().isSolid())return at;
        }
        return arenaSpawn.clone();
    }
    void gate(boolean closed) {
        if(closed)door.each(b->{doorOriginal.putIfAbsent(b.getLocation(),b.getBlockData().clone());b.setType(Material.BARRIER,false);});
        else { doorOriginal.forEach((l,d)->l.getBlock().setBlockData(d,false));doorOriginal.clear(); }
    }
    void demo() {
        Material floor=Material.matchMaterial(config.getString("demo-floor","DEEPSLATE_TILES"));
        // Replace the old connected waiting strip in generated demo worlds.
        for(int x=-24;x<=24;x++)for(int z=-31;z<=24;z++) {
            world.getBlockAt(x,63,z).setType(z < -24?Material.AIR:floor,false);
            for(int y=64;y<=76;y++) {
                boolean wall=z>=-24&&(x==-24||x==24||z==-24||z==24||y==76);
                Material block=wall?Material.POLISHED_DEEPSLATE:Material.AIR;
                if(wall&&y==67&&(Math.abs(x)==24||Math.abs(z)==24))block=Material.PURPLE_CONCRETE;
                if(y==76&&x%8==0&&z%8==0&&z>=-24)block=Material.SEA_LANTERN;
                world.getBlockAt(x,y,z).setType(block,false);
            }
        }
        for(int x:new int[]{-16,16})for(int z:new int[]{-16,16}) {
            for(int y=64;y<=66;y++)world.getBlockAt(x,y,z).setType(Material.POLISHED_DEEPSLATE,false);
            world.getBlockAt(x,67,z).setType(Material.SEA_LANTERN,false);
        }
        // Low cover gives a moment to heal but remains reachable by mobs.
        for(int x:new int[]{-10,10})for(int z=-4;z<=4;z++)world.getBlockAt(x,64,z).setType(Material.DEEPSLATE_BRICK_SLAB,false);
        // A separate enclosed preparation room across a void gap, never walkable into the fight.
        int lx1=lobby.x1()-1,lx2=lobby.x2()+1,lz1=lobby.z1()-1,lz2=lobby.z2()+1,roof=lobby.y2()+1;
        for(int x=lx1;x<=lx2;x++)for(int z=lz1;z<=lz2;z++)for(int y=lobby.y1()-1;y<=roof;y++){
            boolean shell=x==lx1||x==lx2||z==lz1||z==lz2||y==lobby.y1()-1||y==roof;
            Material block=shell?Material.POLISHED_DEEPSLATE:Material.AIR;
            if(y==lobby.y1()-1)block=Material.SMOOTH_QUARTZ;
            if(shell&&y==lobby.y1()+2&&(x==lx1||x==lx2))block=Material.PURPLE_STAINED_GLASS;
            if(y==roof&&(x-lx1)%4==0&&(z-lz1)%4==0)block=Material.SEA_LANTERN;
            world.getBlockAt(x,y,z).setType(block,false);
        }
        // Raised decorative pillars, benches and storage for equipping between stages.
        for(int x:new int[]{lx1+2,lx2-2})for(int z:new int[]{lz1+2,lz2-2}){
            for(int y=lobby.y1();y<roof;y++)world.getBlockAt(x,y,z).setType(Material.CHISELED_QUARTZ_BLOCK,false);
            world.getBlockAt(x,roof-1,z).setType(Material.SEA_LANTERN,false);
        }
        for(int z=lz1+4;z<=lz2-4;z++)world.getBlockAt(lx1+2,lobby.y1(),z).setType(Material.SMOOTH_QUARTZ_SLAB,false);
        world.getBlockAt(lx2-2,lobby.y1(),0).setType(Material.CRAFTING_TABLE,false);
        world.getBlockAt(lx2-2,lobby.y1(),2).setType(Material.ENDER_CHEST,false);
        world.getBlockAt(lx2-2,lobby.y1(),-2).setType(Material.ANVIL,false);
        world.setSpawnLocation(lobbySpawn);
    }
    static final class VoidGenerator extends ChunkGenerator {
        @Override public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) { return createChunkData(world); }
        @Override public boolean shouldGenerateNoise(){return false;}
        @Override public boolean shouldGenerateSurface(){return false;}
        @Override public boolean shouldGenerateCaves(){return false;}
        @Override public boolean shouldGenerateDecorations(){return false;}
        @Override public boolean shouldGenerateMobs(){return false;}
        @Override public boolean shouldGenerateStructures(){return false;}
    }
}
