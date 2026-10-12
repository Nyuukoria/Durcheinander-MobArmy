package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.time.*;
import java.util.*;

public final class MobArmy extends JavaPlugin {
    final Map<String,Arena> arenas=new LinkedHashMap<>();
    final Map<String,Portal> portals=new LinkedHashMap<>();
    final Map<UUID,Location> returns=new HashMap<>();
    YamlConfiguration state;
    Items items; Engine engine; Menus menus; RaidCommand commands; Presentation presentation; Motion motion;
    NamespacedKey entityKey, itemKey;
    ZoneId zone;
    @Override public void onEnable() {
        saveDefaultConfig(); entityKey=new NamespacedKey(this,"raid_entity");itemKey=new NamespacedKey(this,"custom_item");
        try {
            zone=ZoneId.of(getConfig().getString("timezone","Europe/Berlin"));
            state=YamlConfiguration.loadConfiguration(new File(getDataFolder(),"state.yml"));
            ConfigUpgrade.run(this);
            var weeks=getConfig().getConfigurationSection("weeks");
            if(weeks==null)throw new IllegalArgumentException("Missing weeks");
            for(String id:weeks.getKeys(false)) {
                Arena a=new Arena(id,weeks.getConfigurationSection(id));
                if(arena(a.world)!=null)throw new IllegalArgumentException("Each week needs its own world: "+a.world.getName());
                arenas.put(id,a);
                // Reloads/restarts always reopen the configured gate and remove stale raid entities.
                a.door.each(b->{if(b.getType()==Material.BARRIER)b.setType(Material.AIR,false);});
                for(Entity e:a.world.getEntities())if(e.getPersistentDataContainer().has(entityKey))e.remove();
            }
            var ps=state.getConfigurationSection("portals");
            if(ps!=null)for(String id:ps.getKeys(false)){Portal p=Portal.load(this,id,ps.getConfigurationSection(id));if(arenas.containsKey(p.week))portals.put(id,p);}
            var rs=state.getConfigurationSection("returns");
            if(rs!=null)for(String id:rs.getKeys(false)){Location l=rs.getLocation(id);if(l!=null)returns.put(UUID.fromString(id),l);}
            items=new Items(this);Recovery.restore(this);engine=new Engine(this);menus=new Menus(this);commands=new RaidCommand(this);presentation=new Presentation(this);motion=new Motion(this);
            for(Arena a:arenas.values())if(a.config.getBoolean("generate-demo")&&state.getInt("demo-layout."+a.id)<2){a.demo();state.set("demo."+a.id,true);state.set("demo-layout."+a.id,2);}
            Objects.requireNonNull(getCommand("raid")).setExecutor(commands);getCommand("raid").setTabCompleter(commands);
            getServer().getPluginManager().registerEvents(new Protection(this),this);
            getServer().getPluginManager().registerEvents(menus,this);
            getServer().getPluginManager().registerEvents(motion,this);
            applyDemoPvp();
            getServer().getScheduler().runTaskTimer(this,engine::tick,20,20);
            getServer().getScheduler().runTaskTimer(this,()->{presentation.animate();motion.tick();},1,1);
            for(Player player:Bukkit.getOnlinePlayers()){items.refresh(player);recover(player);}
            saveState();getLogger().info("Loaded "+arenas.size()+" raid arenas for Spigot 26.3.");
        }catch(Exception ex){getLogger().log(java.util.logging.Level.SEVERE,"Invalid raid setup; plugin disabled",ex);getServer().getPluginManager().disablePlugin(this);}
    }
    @Override public void onDisable(){if(items!=null)items.shutdown();if(engine!=null)engine.shutdown();if(presentation!=null)presentation.shutdown();for(Portal p:portals.values())p.removeLabel();if(state!=null)saveState();}
    public String getCustomItemId(org.bukkit.inventory.ItemStack item){return items==null?null:items.id(item);}
    public org.bukkit.inventory.ItemStack createCustomItem(String id,int amount){return items.make(id,amount);}
    public int countCustomItems(Player player,String id){return items.count(player,id);}
    public boolean removeCustomItems(Player player,String id,int amount){return items.take(player,id,amount);}
    void recover(Player p){Location l=returns.remove(p.getUniqueId());if(l!=null){p.teleport(l);state.set("returns."+p.getUniqueId(),null);saveState();}}
    void saveState(){
        if(state==null)return;
        if(engine!=null)Recovery.snapshot(this);
        for(Portal p:portals.values())p.save(state.createSection("portals."+p.id));
        state.set("returns",null);returns.forEach((id,l)->state.set("returns."+id,l));
        File target=new File(getDataFolder(),"state.yml"), temp=new File(getDataFolder(),"state.yml.tmp");
        try{state.save(temp);try{java.nio.file.Files.move(temp.toPath(),target.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING,java.nio.file.StandardCopyOption.ATOMIC_MOVE);}catch(java.nio.file.AtomicMoveNotSupportedException e){java.nio.file.Files.move(temp.toPath(),target.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);}}
        catch(IOException e){getLogger().log(java.util.logging.Level.SEVERE,"Could not save raid state",e);}
    }
    LocalDate today(){return LocalDate.now(zone);}
    void applyDemoPvp(){if(getConfig().getBoolean("testing.disable-pvp"))Bukkit.getWorlds().forEach(w->w.setPVP(false));}
    Arena arena(World w){return arenas.values().stream().filter(a->a.world.equals(w)).findFirst().orElse(null);}
    Session session(UUID id){return portals.values().stream().map(p->p.session).filter(Objects::nonNull).filter(s->s.members.contains(id)).findFirst().orElse(null);}
    Portal portalAt(Location l){return portals.values().stream().filter(p->p.region.contains(l)).findFirst().orElse(null);}
    void tell(Player p,String text){p.sendMessage(ChatColor.LIGHT_PURPLE+"[Raid] "+ChatColor.WHITE+color(text));}
    static String color(String s){return ChatColor.translateAlternateColorCodes('&',s);}
    void announcement(Portal p,String path,String stage){if(p.rewards())Bukkit.broadcastMessage(color(getConfig().getString("messages."+path,"").replace("{stage}",stage)));}
}
