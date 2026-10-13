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
    static final int DEMO_LAYOUT=4;
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
        if(config.getBoolean("generate-demo")){doorOriginal.forEach((l,d)->l.getBlock().setBlockData(d,false));doorOriginal.clear();return;}
        if(closed)door.each(b->{doorOriginal.putIfAbsent(b.getLocation(),b.getBlockData().clone());b.setType(Material.BARRIER,false);});
        else { doorOriginal.forEach((l,d)->l.getBlock().setBlockData(d,false));doorOriginal.clear(); }
    }
    int groundY(int x,int z){
        if(!config.getBoolean("generate-demo"))return bounds.y1()-1;
        return 63+DemoTerrain.height(x,z);
    }
    void demo() {
        Material surface=switch(id){case "w2"->Material.WARPED_NYLIUM;case "w3"->Material.END_STONE;default->Material.GRASS_BLOCK;};
        Material rock=switch(id){case "w2"->Material.BASALT;case "w3"->Material.PURPUR_BLOCK;default->Material.MOSSY_COBBLESTONE;};
        // Remove the entire previous lobby so no old benches/walls survive an upgrade.
        for(int x=79;x<=97;x++)for(int z=-9;z<=9;z++)for(int y=63;y<=71;y++)world.getBlockAt(x,y,z).setType(Material.AIR,false);
        for(int x=-51;x<=50;x++)for(int z=-51;z<=50;z++){
            int ground=groundY(x,z);boolean edge=x==-51||x==50||z==-51||z==50;
            for(int y=62;y<=86;y++){
                Material block=Material.AIR;
                if(y<ground)block=Material.STONE;
                else if(y==ground)block=Math.abs(x)<=3||Math.abs(z)<=3?Material.STONE_BRICKS:surface;
                if(y==ground&&x%12==0&&z%12==0)block=Material.SEA_LANTERN;
                if(edge&&y>=63||y==86)block=Material.POLISHED_DEEPSLATE;
                if(edge&&y==70)block=Material.PURPLE_CONCRETE;
                if(y==86&&x%10==0&&z%10==0)block=Material.SEA_LANTERN;
                world.getBlockAt(x,y,z).setType(block,false);
            }
        }
        // Broken low ruins and small trees frame the open paths; spawn pads stay clear.
        for(int x:new int[]{-22,22})for(int z:new int[]{-17,17}){
            int base=groundY(x,z)+1;
            for(int dx=-3;dx<=3;dx++)for(int dy=0;dy<2;dy++)world.getBlockAt(x+dx,groundY(x+dx,z)+1+dy,z).setType(rock,false);
            for(int y=base;y<base+4;y++)world.getBlockAt(x-3,y,z).setType(rock,false);
            world.getBlockAt(x-3,base+4,z).setType(Material.SEA_LANTERN,false);
        }
        for(int x:new int[]{-40,40})for(int z:new int[]{-14,14}){
            int base=groundY(x,z)+1;
            for(int y=base;y<base+4;y++)world.getBlockAt(x,y,z).setType(id.equals("w1")?Material.OAK_LOG:rock,false);
            for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)if(Math.abs(dx)+Math.abs(dz)<=3){
                var b=world.getBlockAt(x+dx,base+4,z+dz);
                b.setType(id.equals("w1")?Material.OAK_LEAVES:Material.AMETHYST_BLOCK,false);
                if(b.getBlockData() instanceof org.bukkit.block.data.type.Leaves leaves){leaves.setPersistent(true);b.setBlockData(leaves,false);}
            }
        }
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
        for(int z=-2;z<=2;z++)world.getBlockAt(lx1+1,lobby.y1(),z).setType(Material.SMOOTH_QUARTZ_SLAB,false);
        world.getBlockAt(lx2-2,lobby.y1(),0).setType(Material.CRAFTING_TABLE,false);
        world.getBlockAt(lx2-2,lobby.y1(),2).setType(Material.ENDER_CHEST,false);
        world.getBlockAt(lx2-2,lobby.y1(),-2).setType(Material.ANVIL,false);
        repairDemo();
        world.setSpawnLocation(lobbySpawn);
    }
    List<Location> spawnCandidates(){
        List<Location> candidates=new ArrayList<>();
        if(config.getBoolean("generate-demo"))for(int x=-42;x<=42;x+=12)for(int z=-42;z<=42;z+=12)candidates.add(new Location(world,x+.5,groundY(x,z)+1,z+.5));
        for(Location spawn:spawns)for(int dx=-4;dx<=4;dx+=2)for(int dz=-4;dz<=4;dz+=2){Location at=spawn.clone().add(dx,0,dz);if(config.getBoolean("generate-demo"))at.setY(groundY(at.getBlockX(),at.getBlockZ())+1);candidates.add(at);}
        return candidates.stream().filter(bounds::contains).filter(at->at.getBlock().isPassable()&&at.clone().add(0,1,0).getBlock().isPassable()&&at.clone().add(0,2,0).getBlock().isPassable()&&at.clone().subtract(0,1,0).getBlock().getType().isSolid()).toList();
    }
    Location recipesButton(){return new Location(world,lobby.x1(),lobby.y1()+1,-2);}
    Location startButton(){return new Location(world,lobby.x1(),lobby.y1()+1,2);}
    void repairDemo(){
        if(!config.getBoolean("generate-demo"))return;
        // The separate lobby uses teleportation: the arena boundary never needs an opening.
        for(int x=-51;x<=50;x++)for(int z=-51;z<=50;z++){
            int ground=groundY(x,z);for(int y=62;y<=ground;y++)if(world.getBlockAt(x,y,z).getType().isAir())world.getBlockAt(x,y,z).setType(y==ground?(id.equals("w2")?Material.WARPED_NYLIUM:id.equals("w3")?Material.END_STONE:Material.GRASS_BLOCK):Material.STONE,false);
            if(x==-51||x==50||z==-51||z==50)for(int y=63;y<=86;y++)world.getBlockAt(x,y,z).setType(y==70?Material.PURPLE_CONCRETE:Material.POLISHED_DEEPSLATE,false);
            if(world.getBlockAt(x,86,z).getType().isAir())world.getBlockAt(x,86,z).setType(Material.POLISHED_DEEPSLATE,false);
        }
        var chest=world.getBlockAt(lobby.x2()-1,lobby.y1(),2);chest.setType(Material.ENDER_CHEST,false);var facing=(org.bukkit.block.data.Directional)chest.getBlockData();facing.setFacing(org.bukkit.block.BlockFace.WEST);chest.setBlockData(facing,false);
        wallControl(recipesButton(),"§dREZEPTE","§fRECIPES");wallControl(startButton(),"§aSTART","§fREADY");
    }
    void wallControl(Location at,String first,String second){
        var button=at.getBlock();button.setType(Material.STONE_BUTTON,false);var data=(org.bukkit.block.data.type.Switch)button.getBlockData();data.setAttachedFace(org.bukkit.block.data.FaceAttachable.AttachedFace.WALL);data.setFacing(org.bukkit.block.BlockFace.EAST);button.setBlockData(data,false);
        var block=at.clone().add(0,1,0).getBlock();block.setType(Material.OAK_WALL_SIGN,false);var wall=(org.bukkit.block.data.Directional)block.getBlockData();wall.setFacing(org.bukkit.block.BlockFace.EAST);block.setBlockData(wall,false);var sign=(org.bukkit.block.Sign)block.getState();sign.setLine(0,first);sign.setLine(1,second);sign.setLine(2,"§7Rechtsklick");sign.setLine(3,"§7Right click");sign.setWaxed(true);sign.update(true,false);
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
