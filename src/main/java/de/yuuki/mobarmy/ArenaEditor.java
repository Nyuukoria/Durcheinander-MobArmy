package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Persistent map drafts and a temporary, recoverable creative inventory. */
final class ArenaEditor implements Listener {
    record Edit(UUID owner,String arena,YamlConfiguration draft){}
    final MobArmy p;final Map<String,Edit> edits=new LinkedHashMap<>();final Set<String> busy=new HashSet<>();final Set<UUID> transfers=new HashSet<>();
    final Map<UUID,Location> first=new HashMap<>(),second=new HashMap<>();final NamespacedKey wand=new NamespacedKey("mobarmy","arena_wand");int clock;
    ArenaEditor(MobArmy p){this.p=p;}
    boolean blocked(String id){Arena a=p.arenas.get(id);return busy.contains(id)||edits.containsKey(id)||a!=null&&a.config.getBoolean("arena-draft");}
    Arena arena(String id){if(!id.matches("[A-Za-z0-9_-]{1,40}"))throw new IllegalArgumentException("Invalid arena ID");Arena a=p.arenas.get(id);if(a==null)throw new IllegalArgumentException("Unknown arena: "+id);return a;}
    Edit own(Player player,String id){Edit e=edits.get(id);if(e==null||!e.owner().equals(player.getUniqueId()))throw new IllegalArgumentException(p.tr(player,"editor.edit-first","id",id));return e;}
    String selected(Player player){return edits.values().stream().filter(e->e.owner().equals(player.getUniqueId())).map(Edit::arena).findFirst().orElseThrow(()->new IllegalArgumentException(p.tr(player,"editor.no-edit")));}
    void idle(Player pl,String id){if(p.portals.values().stream().anyMatch(q->q.week.equals(id)&&q.session!=null))throw new IllegalArgumentException(p.tr(pl,"editor.raid-active"));}
    static boolean validId(String id){return id.matches("[a-z][a-z0-9_-]{0,31}");}
    YamlConfiguration copy(org.bukkit.configuration.ConfigurationSection c){var out=new YamlConfiguration();ConfigUpgrade.copy(c,out);return out;}
    File draftFile(String id){return new File(p.getDataFolder(),"arena-drafts/"+id+".yml");}
    void persist(Edit edit){try{File file=draftFile(edit.arena());file.getParentFile().mkdirs();edit.draft().save(file);}catch(IOException e){throw new IllegalStateException("Could not save arena draft",e);}}
    void command(Player pl,String[] args){
        String action=args.length>1?args[1].toLowerCase(Locale.ROOT):"help";String id=args.length>2?args[2]:null;
        if(action.equals("help")){p.say(pl,"editor.help");return;}if(action.equals("list")){p.say(pl,"editor.list","ids",p.arenas.keySet());return;}
        if(action.equals("pos1")||action.equals("pos2")){String editing=selected(pl);if(!pl.getWorld().equals(arena(editing).world))throw new IllegalArgumentException(p.tr(pl,"editor.same-world"));(action.equals("pos1")?first:second).put(pl.getUniqueId(),pl.getLocation());p.say(pl,"editor.position","number",action.equals("pos1")?1:2);return;}
        if(id==null){if(Set.of("save","done","validate","export","seal").contains(action))id=selected(pl);else throw new IllegalArgumentException("/raid arena "+action+" <arena>");}
        switch(action){
            case "create"->create(pl,id,args.length>3?args[3]:"w1");
            case "edit"->edit(pl,id);
            case "save"->save(pl,own(pl,id),true);
            case "done"->{Edit e=own(pl,id);save(pl,e,true);if(!recover(pl))throw new IllegalStateException(p.tr(pl,"editor.teleport-failed"));edits.remove(id);arena(id).config.set("arena-draft",e.draft().getBoolean("arena-draft"));p.saveConfig();p.say(pl,"editor.done");}
            case "validate"->report(pl,checks(id));
            case "export"->export(pl,id);
            case "seal"->seal(pl,own(pl,id));
            case "region"->{Edit e=own(pl,id);if(args.length!=4||!Set.of("arena","lobby","door").contains(args[3]))throw new IllegalArgumentException("/raid arena region <arena> arena|lobby|door");Location a=first.get(pl.getUniqueId()),b=second.get(pl.getUniqueId());if(a==null||b==null)throw new IllegalArgumentException(p.tr(pl,"editor.select"));Region r=Region.between(a,b);if(!r.world().equals(arena(id).world.getName()))throw new IllegalArgumentException(p.tr(pl,"editor.same-world"));e.draft().set(args[3]+".min",List.of(r.x1(),r.y1(),r.z1()));e.draft().set(args[3]+".max",List.of(r.x2(),r.y2(),r.z2()));persist(e);p.say(pl,"editor.region","region",args[3]);}
            case "set"->{if(args.length!=4)throw new IllegalArgumentException("/raid arena set <arena> arena-spawn|lobby-spawn|recipes|start");set(pl,own(pl,id),args[3]);}
            case "spawn"->{Edit e=own(pl,id);if(args.length<4)throw new IllegalArgumentException("/raid arena spawn <arena> add|clear|list|remove <number>");var spawns=new ArrayList<>(e.draft().getStringList("spawn-points"));switch(args[3]){case "add"->{if(!pl.getWorld().equals(arena(id).world))throw new IllegalArgumentException(p.tr(pl,"editor.same-world"));if(spawns.size()>=64)throw new IllegalArgumentException(p.tr(pl,"editor.spawn-limit"));Location at=pl.getLocation();spawns.add(at.getX()+","+at.getY()+","+at.getZ());}case "clear"->spawns.clear();case "remove"->{if(args.length!=5)throw new IllegalArgumentException("/raid arena spawn <arena> remove <number>");int index=Integer.parseInt(args[4])-1;if(index<0||index>=spawns.size())throw new IllegalArgumentException(p.tr(pl,"editor.spawn-number"));spawns.remove(index);}case "list"->{for(int i=0;i<spawns.size();i++)p.reply(pl,(i+1)+": "+spawns.get(i));return;}default->throw new IllegalArgumentException("add|clear|list|remove");}e.draft().set("spawn-points",spawns);persist(e);p.say(pl,"editor.spawns","count",spawns.size());}
            default->throw new IllegalArgumentException(p.tr(pl,"editor.help"));
        }
    }
    void create(Player pl,String id,String template){
        if(!validId(id)||p.arenas.containsKey(id))throw new IllegalArgumentException(p.tr(pl,"editor.new-id"));Arena base=arena(template);idle(pl,template);String name="mobarmy_arena_"+id;
        if(!edits.isEmpty())throw new IllegalArgumentException(p.tr(pl,"editor.finish-first"));if(new File(Bukkit.getWorldContainer(),name).exists()||Bukkit.getWorld(name)!=null)throw new IllegalArgumentException(p.tr(pl,"editor.world-exists"));
        queued(pl,template,()->{Arena current=arena(template);normalizeDemo(current);YamlConfiguration definition=copy(current.config);Path archive=snapshot(pl,current,definition,"arena-backups");
            try{ArenaArchive.extractWorld(archive,new File(Bukkit.getWorldContainer(),name).toPath());}catch(IOException error){throw new IllegalStateException("Could not copy arena world",error);}
            var cfg=p.getConfig().createSection("weeks."+id);ConfigUpgrade.copy(definition,cfg);cfg.set("world",name);cfg.set("generate-demo",false);cfg.set("arena-draft",true);cfg.set("lobby-sign",false);
            Arena created=new Arena(id,cfg);p.arenas.put(id,created);p.saveConfig();p.state.set("demo-layout."+id,Arena.DEMO_LAYOUT);p.saveState();edit(pl,id);
        });
    }
    void normalizeDemo(Arena a){if(!a.config.getBoolean("generate-demo"))return;for(String key:List.of("recipes","start")){Location at=key.equals("recipes")?a.recipesButton():a.startButton();a.config.set("controls."+key+".location",List.of(at.getBlockX(),at.getBlockY(),at.getBlockZ()));a.config.set("controls."+key+".face","EAST");}var list=new ArrayList<String>();for(Location at:a.spawns)list.add(at.getX()+","+(a.groundY(at.getBlockX(),at.getBlockZ())+1)+","+at.getZ());a.config.set("spawn-points",list);}
    void queued(Player pl,String id,Runnable action){
        if(!busy.add(id))throw new IllegalArgumentException(p.tr(pl,"editor.busy"));
        Bukkit.getScheduler().runTask(p,()->{try{if(pl.isOnline())action.run();}catch(RuntimeException e){p.reply(pl,"&c"+Objects.toString(e.getMessage(),"Arena operation failed"));p.getLogger().warning("Arena "+id+": "+e.getMessage());}finally{busy.remove(id);}});
    }
    void edit(Player pl,String id){
        Arena a=arena(id);idle(pl,id);if(p.state.contains("editor-returns."+pl.getUniqueId())&&!edits.containsKey(id)&&!recover(pl))throw new IllegalStateException(p.tr(pl,"editor.teleport-failed"));if(p.session(pl.getUniqueId())!=null)throw new IllegalArgumentException(p.tr(pl,"editor.leave-raid"));
        if(edits.containsKey(id)){own(pl,id);pl.teleport(a.lobbySpawn);return;}if(edits.values().stream().anyMatch(e->e.owner().equals(pl.getUniqueId())))throw new IllegalArgumentException(p.tr(pl,"editor.finish-first"));
        queued(pl,id,()->{
            Arena current=arena(id);normalizeDemo(current);Path backup=snapshot(pl,current,copy(current.config),"arena-backups");current=arena(id);
            YamlConfiguration draft=draftFile(id).isFile()?YamlConfiguration.loadConfiguration(draftFile(id)):copy(current.config);draft.set("generate-demo",false);draft.set("lobby-sign",false);draft.set("arena-draft",true);
            Location exit=p.arena(pl.getWorld())==null?pl.getLocation():outside(current.world);String root="editor-returns."+pl.getUniqueId();
            p.state.set(root+".location",exit);p.state.set(root+".mode",pl.getGameMode().name());p.state.set(root+".flight",pl.getAllowFlight());p.state.set(root+".flying",pl.isFlying());p.state.set(root+".slot",pl.getInventory().getHeldItemSlot());pl.closeInventory();p.state.set(root+".items",Arrays.asList(pl.getInventory().getContents()));p.state.set(root+".cursor",pl.getItemOnCursor());p.saveState();
            current.config.set("generate-demo",false);current.config.set("arena-draft",true);current.config.set("lobby-sign",false);p.saveConfig();
            Edit e=new Edit(pl.getUniqueId(),id,draft);edits.put(id,e);persist(e);p.commands.builders.add(pl.getUniqueId());pl.getInventory().clear();pl.setItemOnCursor(null);pl.setGameMode(GameMode.CREATIVE);pl.setAllowFlight(true);pl.setFlying(true);pl.teleport(current.lobbySpawn);
            ItemStack selector=new ItemStack(Material.BLAZE_ROD);var meta=selector.getItemMeta();meta.setDisplayName(MobArmy.color(p.tr(pl,"editor.wand")));meta.setLore(List.of(MobArmy.color(p.tr(pl,"editor.wand-lore"))));meta.getPersistentDataContainer().set(wand,PersistentDataType.BYTE,(byte)1);selector.setItemMeta(meta);pl.getInventory().setItem(8,selector);pl.saveData();p.presentation.reloadArena(current);
            p.say(pl,"editor.started","id",id,"backup",backup.getFileName());
        });
    }
    Location outside(World target){return Bukkit.getWorlds().stream().filter(w->!w.equals(target)&&p.arena(w)==null).findFirst().orElseThrow(()->new IllegalStateException("A non-raid return world is required")).getSpawnLocation();}
    List<String> checks(String id){Arena a=arena(id);Edit e=edits.get(id);YamlConfiguration cfg=e!=null?e.draft():draftFile(id).isFile()?YamlConfiguration.loadConfiguration(draftFile(id)):copy(a.config);return ArenaChecks.world(cfg,a.world);}
    void report(Player pl,List<String> issues){if(issues.isEmpty())p.say(pl,"editor.valid");else for(String code:issues)p.say(pl,"editor.issue."+code);}
    void save(Player pl,Edit e,boolean inform){
        Arena a=arena(e.arena());idle(pl,a.id);List<String> issues=ArenaChecks.world(e.draft(),a.world);e.draft().set("arena-draft",!issues.isEmpty());e.draft().set("generate-demo",false);persist(e);a.world.save();
        if(issues.isEmpty()){
            a.world.removePluginChunkTickets(p);var next=p.getConfig().createSection("weeks."+a.id);ConfigUpgrade.copy(e.draft(),next);Arena loaded=new Arena(a.id,next);p.arenas.put(a.id,loaded);next.set("arena-draft",true);p.saveConfig();p.presentation.reloadArena(loaded);
        }else{a.config.set("arena-draft",true);a.config.set("generate-demo",false);p.saveConfig();}
        if(inform){p.say(pl,issues.isEmpty()?"editor.saved":"editor.saved-draft","id",a.id);report(pl,issues);}
    }
    void export(Player pl,String id){
        Arena a=arena(id);idle(pl,id);Edit e=edits.get(id);if(e!=null){own(pl,id);save(pl,e,false);}
        YamlConfiguration cfg=e!=null?e.draft():draftFile(id).isFile()?YamlConfiguration.loadConfiguration(draftFile(id)):copy(a.config);
        queued(pl,id,()->{Path file=snapshot(pl,arena(id),cfg,"arena-exports");p.say(pl,"editor.exported","file",file.toAbsolutePath());});
    }
    Path snapshot(Player pl,Arena a,YamlConfiguration cfg,String folder){
        if(a.world.equals(Bukkit.getWorlds().getFirst()))throw new IllegalArgumentException(p.tr(pl,"editor.main-world"));idle(pl,a.id);Location safe=outside(a.world);Map<UUID,Location> positions=new LinkedHashMap<>();boolean unloaded=false;
        String suffix=DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneOffset.UTC).format(Instant.now());Path worldFolder=a.world.getWorldFolder().toPath();Path zip=p.getDataFolder().toPath().resolve(folder).resolve(a.id+"-"+suffix+".zip");
        var manifest=new YamlConfiguration();manifest.set("format","mobarmy-arena-v1");manifest.set("minecraft","26.3");manifest.set("plugin-version",p.getDescription().getVersion());manifest.set("arena-id",a.id);ConfigUpgrade.copy(cfg,manifest.createSection("weeks."+a.id));manifest.set("weeks."+a.id+".generate-demo",false);
        List<String> issues=ArenaChecks.world(cfg,a.world);String report=issues.isEmpty()?"VALID: enclosed layout and safe spawn points\n":"DRAFT: "+String.join(", ",issues)+"\n";
        try{
            for(Player player:new ArrayList<>(a.world.getPlayers())){positions.put(player.getUniqueId(),player.getLocation());transfers.add(player.getUniqueId());if(!player.teleport(safe))throw new IllegalStateException(p.tr(pl,"editor.teleport-failed"));}
            a.world.save();a.world.removePluginChunkTickets(p);if(!Bukkit.unloadWorld(a.world,true))throw new IllegalStateException(p.tr(pl,"editor.unload-failed"));unloaded=true;
            ArenaArchive.write(worldFolder,zip,manifest.saveToString(),report);return zip;
        }catch(IOException error){throw new IllegalStateException("Could not write arena ZIP: "+error.getMessage(),error);}
        finally{
            try{
                Arena loaded=unloaded||Bukkit.getWorld(a.world.getName())!=null?new Arena(a.id,p.getConfig().getConfigurationSection("weeks."+a.id)):a;p.arenas.put(a.id,loaded);p.presentation.reloadArena(loaded);
                for(var at:positions.entrySet()){Player player=Bukkit.getPlayer(at.getKey());if(player!=null&&player.isOnline()){Location target=at.getValue().clone();target.setWorld(loaded.world);player.teleport(target);}}
            }finally{positions.keySet().forEach(transfers::remove);}
        }
    }
    void set(Player pl,Edit e,String field){
        Arena a=arena(e.arena());if(!pl.getWorld().equals(a.world))throw new IllegalArgumentException(p.tr(pl,"editor.same-world"));
        if(Set.of("arena-spawn","lobby-spawn").contains(field)){Location at=pl.getLocation();e.draft().set(field,List.of(at.getX(),at.getY(),at.getZ(),(double)at.getYaw()));}
        else if(Set.of("recipes","start").contains(field)){
            Block wall=pl.getTargetBlockExact(5);if(wall==null||!wall.getType().isSolid())throw new IllegalArgumentException(p.tr(pl,"editor.wall"));BlockFace face=pl.getFacing().getOppositeFace();Location at=wall.getRelative(face).getLocation();Region lobby=Region.read(e.draft(),"lobby",a.world.getName());
            if(!lobby.contains(at)||!lobby.contains(at.clone().add(0,1,0))||!at.getBlock().isPassable()||!at.clone().add(0,1,0).getBlock().isPassable()||!wall.getRelative(0,1,0).getType().isSolid())throw new IllegalArgumentException(p.tr(pl,"editor.wall"));
            e.draft().set("controls."+field+".location",List.of(at.getBlockX(),at.getBlockY(),at.getBlockZ()));e.draft().set("controls."+field+".face",face.name());a.wallControl(at,field.equals("recipes")?"§dREZEPTE":"§aSTART",field.equals("recipes")?"§fRECIPES":"§fREADY",face);
        }else throw new IllegalArgumentException("arena-spawn|lobby-spawn|recipes|start");persist(e);p.say(pl,"editor.point","point",field);
    }
    void seal(Player pl,Edit e){
        Arena a=arena(e.arena());List<String> issues=ArenaChecks.layout(e.draft());if(!issues.isEmpty()){report(pl,issues);return;}
        for(String field:List.of("arena","lobby")){Region r=Region.read(e.draft(),field,a.world.getName());if(r.y1()<=a.world.getMinHeight()||r.y2()+1>=a.world.getMaxHeight())throw new IllegalArgumentException(p.tr(pl,"editor.issue.height"));
            for(int x=r.x1()-1;x<=r.x2()+1;x++)for(int z=r.z1()-1;z<=r.z2()+1;z++)for(int y=r.y1()-1;y<=r.y2()+1;y++)if(x==r.x1()-1||x==r.x2()+1||z==r.z1()-1||z==r.z2()+1||y==r.y1()-1||y==r.y2()+1){Block b=a.world.getBlockAt(x,y,z);if(b.isPassable())b.setType(Material.POLISHED_DEEPSLATE,false);}}
        a.world.save();p.say(pl,"editor.sealed");
    }
    @EventHandler(priority=EventPriority.HIGHEST) public void select(PlayerInteractEvent e){
        ItemStack item=e.getItem();if(e.getHand()!=EquipmentSlot.HAND||item==null||!item.hasItemMeta()||!item.getItemMeta().getPersistentDataContainer().has(wand))return;e.setCancelled(true);
        String id;try{id=selected(e.getPlayer());}catch(RuntimeException noEdit){return;}if(e.getClickedBlock()==null||!e.getPlayer().getWorld().equals(arena(id).world))return;
        if(e.getAction()==Action.LEFT_CLICK_BLOCK)first.put(e.getPlayer().getUniqueId(),e.getClickedBlock().getLocation());else if(e.getAction()==Action.RIGHT_CLICK_BLOCK)second.put(e.getPlayer().getUniqueId(),e.getClickedBlock().getLocation());else return;
        p.say(e.getPlayer(),"editor.position","number",e.getAction()==Action.LEFT_CLICK_BLOCK?1:2);
    }
    boolean recover(Player player){String path="editor-returns."+player.getUniqueId();if(!p.state.contains(path))return true;if(player.isDead())return false;Location exit=p.state.getLocation(path+".location");if(exit!=null&&exit.getWorld()!=null){transfers.add(player.getUniqueId());try{if(!player.teleport(exit))return false;}finally{transfers.remove(player.getUniqueId());}}
        ItemStack[] contents=new ItemStack[player.getInventory().getSize()];var saved=p.state.getList(path+".items",List.of());for(int i=0;i<Math.min(contents.length,saved.size());i++)if(saved.get(i) instanceof ItemStack item)contents[i]=item.clone();player.getInventory().setContents(contents);player.setItemOnCursor(p.state.getItemStack(path+".cursor"));player.getInventory().setHeldItemSlot(Math.min(8,Math.max(0,p.state.getInt(path+".slot"))));
        player.setGameMode(GameMode.valueOf(p.state.getString(path+".mode","SURVIVAL")));player.setAllowFlight(p.state.getBoolean(path+".flight"));player.setFlying(player.getAllowFlight()&&p.state.getBoolean(path+".flying"));p.commands.builders.remove(player.getUniqueId());player.saveData();p.state.set(path,null);p.saveState();p.items.refresh(player);return true;
    }
    @EventHandler public void quit(PlayerQuitEvent e){for(Edit edit:new ArrayList<>(edits.values()))if(edit.owner().equals(e.getPlayer().getUniqueId())){persist(edit);arena(edit.arena()).world.save();edits.remove(edit.arena());recover(e.getPlayer());}first.remove(e.getPlayer().getUniqueId());second.remove(e.getPlayer().getUniqueId());}
    @EventHandler public void death(PlayerDeathEvent event){for(Edit e:new ArrayList<>(edits.values()))if(e.owner().equals(event.getEntity().getUniqueId())){persist(e);arena(e.arena()).world.save();edits.remove(e.arena());p.commands.builders.remove(e.owner());event.setKeepInventory(true);event.getDrops().clear();}}
    @EventHandler public void respawn(PlayerRespawnEvent e){if(p.state.contains("editor-returns."+e.getPlayer().getUniqueId()))Bukkit.getScheduler().runTask(p,()->recover(e.getPlayer()));}
    void tick(){if(++clock%60==0)for(Edit e:edits.values()){persist(e);arena(e.arena()).world.save();}}
    void shutdown(){for(Edit e:new ArrayList<>(edits.values())){persist(e);arena(e.arena()).world.save();Player player=Bukkit.getPlayer(e.owner());if(player!=null)recover(player);}edits.clear();}
}
