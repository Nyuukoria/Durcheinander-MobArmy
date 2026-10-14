package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.*;

final class RaidCommand implements CommandExecutor,TabCompleter {
    final MobArmy p;
    final Map<UUID,Location> first=new HashMap<>(),second=new HashMap<>();
    final Set<UUID> builders=new HashSet<>();
    RaidCommand(MobArmy p){this.p=p;}
    Player player(CommandSender sender){if(sender instanceof Player player)return player;throw new IllegalArgumentException(p.tr(sender,"error.players-only"));}
    void permission(CommandSender sender,String permission){if(!sender.hasPermission(permission))throw new IllegalArgumentException(p.tr(sender,"error.permission","permission",permission));}
    Portal portal(CommandSender sender,String[] args,int index){
        if(args.length>index){Portal portal=p.portals.get(args[index]);if(portal==null)throw new IllegalArgumentException(p.tr(sender,"error.portal","id",args[index]));return portal;}
        if(sender instanceof Player player){Session s=p.session(player.getUniqueId());if(s!=null)return s.portal;Portal at=p.portalAt(player.getLocation());if(at!=null)return at;}
        if(p.portals.size()==1)return p.portals.values().iterator().next();
        throw new IllegalArgumentException(p.tr(sender,"error.portal-needed","ids",p.portals.keySet()));
    }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        try{
            if(command.getName().equalsIgnoreCase("rezepte")){permission(sender,"mobarmy.play");p.catalogue.open(player(sender),args.length>0?args[0].toLowerCase(Locale.ROOT):"items",0);return true;}
            if(args.length==0){permission(sender,"mobarmy.play");p.menus.party(player(sender),0);return true;}
            switch(args[0].toLowerCase(Locale.ROOT)){
                case "arena"->{permission(sender,"mobarmy.build");p.editor.command(player(sender),args);}
                case "help"->{p.say(sender,"help.player");if(sender.hasPermission("mobarmy.admin"))p.say(sender,"help.admin");}
                case "join"->{permission(sender,"mobarmy.play");p.engine.join(player(sender),portal(sender,args,1));}
                case "start","ready"->{permission(sender,"mobarmy.play");p.engine.ready(player(sender));}
                case "leave"->{Player player=player(sender);player.closeInventory();p.engine.leave(player,false);}
                case "loot"->{permission(sender,"mobarmy.play");p.menus.mail(player(sender),0);}
                case "codex","rezepte","recipes"->{permission(sender,"mobarmy.play");p.catalogue.open(player(sender),args.length>1?args[1].toLowerCase(Locale.ROOT):"items",0);}
                case "language","lang","sprache"->{Player pl=player(sender);if(args.length!=2)throw new IllegalArgumentException("/raid language de|en");p.language.set(pl,args[1].toLowerCase(Locale.ROOT));}
                case "skip"->{permission(sender,"mobarmy.admin");if(args.length<2||!Set.of("wave","stage").contains(args[1].toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("/raid skip wave|stage [portal]");Portal q=portal(sender,args,2);demoOnly(q);if(q.session==null)throw new IllegalArgumentException("Zuerst eine Raid-Gruppe betreten.");if(args[1].equalsIgnoreCase("wave"))p.engine.skipWave(q.session);else p.engine.skipStage(q.session);p.say(sender,"admin.skip");}
                case "boss"->{permission(sender,"mobarmy.admin");if(!p.getConfig().getBoolean("testing.solo")&&!(sender instanceof Player pl&&p.session(pl.getUniqueId())!=null&&p.session(pl.getUniqueId()).portal.id.startsWith("demo_")))throw new IllegalArgumentException("Demo-Steuerung ist nur in der Demo verfügbar.");if(args.length==1||args[1].equalsIgnoreCase("list")){p.say(sender,"admin.boss-list","ids",p.bosses.ids());break;}if(args[1].equalsIgnoreCase("summon")&&args.length==3)p.bosses.summon(player(sender),args[2]);else if(args[1].equalsIgnoreCase("clear")&&args.length<=3){boolean all=args.length==3&&args[2].equalsIgnoreCase("all");if(args.length==3&&!all)throw new IllegalArgumentException("/raid boss clear [all]");p.bosses.clear(player(sender),all);p.say(sender,"admin.boss-cleared");}else throw new IllegalArgumentException("/raid boss list|summon <id>|clear [all]");}
                case "gear"->{permission(sender,"mobarmy.play");Player pl=player(sender);p.say(pl,"gear.status","sets",p.language.sets(pl,p.equipment.sets(pl)));}
                case "ritual","rituals"->{permission(sender,"mobarmy.play");Player pl=player(sender);if(args.length>1&&args[1].equalsIgnoreCase("cancel")){p.ritualSystem.cancel(pl,args.length>2&&args[2].equalsIgnoreCase("all"));break;}var block=pl.getTargetBlockExact(6);if(block==null||!block.getType().name().endsWith("CAULDRON"))throw new IllegalArgumentException("Schau einen Kessel innerhalb 6 Blöcken an und halte ein Chaosfragment.");if(args.length>1&&args[0].equalsIgnoreCase("ritual"))p.ritualSystem.start(pl,block.getLocation(),args[1]);else p.ritualSystem.open(pl,block);}
                case "pack"->{permission(sender,"mobarmy.play");Player pl=player(sender);if(args.length>1&&args[1].equalsIgnoreCase("reload"))p.packs.offer(pl);else if(args.length==1||args.length==2&&args[1].equalsIgnoreCase("status"))p.say(pl,"pack.status","state",p.packs.status(pl));else throw new IllegalArgumentException("/raid pack status|reload");}
                case "status"->{for(Portal q:p.portals.values())p.say(sender,"raid.portal-status","id",q.id,"week",q.week,"state",p.tr(sender,q.open?"portal.open":"portal.closed"),"phase",q.session==null?p.tr(sender,"phase.LOBBY"):p.tr(sender,"raid.enter-title","stage",q.session.stage)+" · "+p.tr(sender,"phase."+q.session.phase));if(p.getConfig().getBoolean("testing.solo"))p.say(sender,"admin.demo-enabled");}
                case "portal"->{permission(sender,"mobarmy.admin");if(args.length<2)throw new IllegalArgumentException("/raid portal pos1|pos2|create|remove|info");switch(args[1].toLowerCase(Locale.ROOT)){
                    case "pos1"->{Player player=player(sender);first.put(player.getUniqueId(),player.getLocation());p.say(player,"admin.position","number",1);}
                    case "pos2"->{Player player=player(sender);second.put(player.getUniqueId(),player.getLocation());p.say(player,"admin.position","number",2);}
                    case "create"->{Player player=player(sender);if(args.length<3)throw new IllegalArgumentException("/raid portal create <week> [id]");Location a=first.get(player.getUniqueId()),b=second.get(player.getUniqueId());if(a==null||b==null)throw new IllegalArgumentException("Zuerst pos1 und pos2 auswählen.");create(player,args[2],args.length>3?args[3]:args[2],Region.between(a,b));}
                    case "remove","delete"->{if(args.length>2&&args[2].equalsIgnoreCase("all")){int count=p.portals.size();for(Portal q:new ArrayList<>(p.portals.values()))remove(q);p.say(sender,"admin.portals-deleted","count",count);}else{Portal q=portal(sender,args,2);remove(q);p.say(sender,"admin.portal-deleted","id",q.id);}}
                    case "info"->{Portal q=portal(sender,args,2);p.reply(sender,q.id+" | "+q.week+" | offen="+q.open+" | Rewards="+q.rewards()+" | Timer="+Rules.clock((q.deadline-System.currentTimeMillis())/1000));}
                    default->throw new IllegalArgumentException("Unknown portal action");}}
                case "open","close"->{permission(sender,"mobarmy.admin");Portal q=portal(sender,args,1);q.open=args[0].equalsIgnoreCase("open");q.tick();p.saveState();p.reply(sender,q.id+": "+(q.open?"open":"closed"));}
                case "stop"->{permission(sender,"mobarmy.admin");Portal q=portal(sender,args,1);if(q.session!=null)p.engine.finish(q.session);}
                case "timer"->{permission(sender,"mobarmy.timer");if(args.length<2)throw new IllegalArgumentException("/raid timer set|add <duration> [id] OR /raid timer clear [id]");boolean clear=args[1].equals("clear");Portal q=portal(sender,args,clear?2:3);
                    if(clear)q.deadline=0;else{if(args.length<3)throw new IllegalArgumentException("Duration required");long duration=Rules.duration(args[2]);if(args[1].equals("set"))q.setTimer(duration);else if(args[1].equals("add")){q.day=p.today();q.deadline=Math.max(System.currentTimeMillis(),q.deadline)+duration;}else throw new IllegalArgumentException("Use set, add or clear");}
                    p.saveState();p.say(sender,"admin.timer-updated");}
                case "demo"->{permission(sender,"mobarmy.admin");
                    if(args.length>1&&args[1].equalsIgnoreCase("reset")){if(args.length>2&&args[2].equalsIgnoreCase("all")){for(Portal q:p.portals.values())resetDemo(q);}else resetDemo(portal(sender,args,2));clearCooldowns(null);p.say(sender,"admin.demo-reset");}
                    else if(args.length>1&&args[1].equalsIgnoreCase("rebuild")){if(args.length<3||!p.arenas.containsKey(args[2]))throw new IllegalArgumentException("/raid demo rebuild <week>");Arena a=p.arenas.get(args[2]);if(!a.config.getBoolean("generate-demo"))throw new IllegalArgumentException("Diese Arena ist keine generierte Demo.");if(p.portals.values().stream().anyMatch(q->q.session!=null&&q.week.equals(a.id)))throw new IllegalArgumentException("Zuerst die Gruppe mit /raid stop beenden.");a.demo();p.state.set("demo-layout."+a.id,Arena.DEMO_LAYOUT);p.saveState();p.say(sender,"admin.demo-rebuilt","week",a.id);}
                    else{Player player=player(sender);String week=args.length>1?args[1]:"w1";Location l=player.getLocation().clone().add(4,0,0);Region region=new Region(l.getWorld().getName(),l.getBlockX(),l.getBlockY(),l.getBlockZ(),l.getBlockX()+2,l.getBlockY()+3,l.getBlockZ());create(player,week,"demo_"+week,region);}}
                case "cooldown"->{permission(sender,"mobarmy.admin");if(args.length<2||!args[1].equalsIgnoreCase("clear"))throw new IllegalArgumentException("/raid cooldown clear [spieler|all]");UUID target=args.length>2&&args[2].equalsIgnoreCase("all")?null:(args.length>2?online(args[2]).getUniqueId():player(sender).getUniqueId());clearCooldowns(target);p.say(sender,p.getConfig().getBoolean("testing.solo")?"admin.cooldowns":"admin.wipe-cleared");}
                case "test"->{permission(sender,"mobarmy.admin");if(p.portals.values().stream().anyMatch(q->q.session!=null))throw new IllegalArgumentException("Stop all sessions before changing test mode");if(args.length!=2||(!args[1].equalsIgnoreCase("on")&&!args[1].equalsIgnoreCase("off")))throw new IllegalArgumentException("/raid test on|off");boolean on=args[1].equalsIgnoreCase("on");p.getConfig().set("testing.solo",on);p.getConfig().set("testing.disable-wipe-cooldown",on);p.saveConfig();p.say(sender,on?"admin.test-on":"admin.test-off");}
                case "build"->{permission(sender,"mobarmy.build");Player player=player(sender);if(p.portals.values().stream().anyMatch(q->q.session!=null))throw new IllegalArgumentException("Stop all sessions before enabling build mode");if(!builders.remove(player.getUniqueId()))builders.add(player.getUniqueId());p.say(player,builders.contains(player.getUniqueId())?"admin.build-on":"admin.build-off");}
                case "visit"->{permission(sender,"mobarmy.build");Player player=player(sender);if(args.length<2||!p.arenas.containsKey(args[1]))throw new IllegalArgumentException("/raid visit <week>");if(!builders.contains(player.getUniqueId()))throw new IllegalArgumentException("Enable /raid build first");player.teleport(p.arenas.get(args[1]).lobbySpawn);}
                case "kit"->{permission(sender,"mobarmy.admin");Player player=player(sender);String kind=args.length>1?args[1].toLowerCase(Locale.ROOT):"basic";
                    if(kind.equals("gear")){for(String id:p.items.ids())if(p.getConfig().getBoolean("items."+id+".gear"))p.items.deliver(player.getUniqueId(),p.items.make(id,1));p.say(player,"admin.kit-gear");break;}
                    if(kind.equals("ritual")){for(String id:List.of("chaos_fragment","storm_essence","void_essence"))p.items.deliver(player.getUniqueId(),p.items.make(id,id.equals("chaos_fragment")?1:8));for(int i=0;i<2;i++)p.items.deliver(player.getUniqueId(),p.items.make("raid_shard",64));for(Material m:List.of(Material.IRON_INGOT,Material.GOLD_INGOT,Material.DIAMOND,Material.LAPIS_LAZULI,Material.EMERALD))p.items.deliver(player.getUniqueId(),new ItemStack(m,32));p.items.deliver(player.getUniqueId(),new ItemStack(Material.CAULDRON));p.say(player,"admin.kit-ritual");break;}
                    if(!kind.equals("basic"))throw new IllegalArgumentException("/raid kit basic|gear|ritual");basicArmor(player);for(Material m:List.of(Material.NETHERITE_SWORD,Material.BOW,Material.MACE,Material.SHIELD,Material.WHITE_SHULKER_BOX))p.items.deliver(player.getUniqueId(),new ItemStack(m));p.items.deliver(player.getUniqueId(),new ItemStack(Material.ARROW,64));p.items.deliver(player.getUniqueId(),new ItemStack(Material.COOKED_BEEF,64));p.items.deliver(player.getUniqueId(),new ItemStack(Material.GOLDEN_APPLE,6));p.items.deliver(player.getUniqueId(),new ItemStack(Material.ENDER_PEARL,16));p.items.deliver(player.getUniqueId(),new ItemStack(Material.WIND_CHARGE,32));p.items.deliver(player.getUniqueId(),new ItemStack(Material.OAK_PLANKS,32));for(String relic:List.of("chaos_fragment","ritual_core","chaos_crown","frost_talisman","healing_bloom","gale_sigil","ember_charm","guardian_rune","hunter_lens","weakening_fang"))p.items.deliver(player.getUniqueId(),p.items.make(relic,1));}
                case "item"->{permission(sender,"mobarmy.item");if(args.length<3||!List.of("give","take","count").contains(args[1].toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("/raid item give|take|count <id> [spieler] [anzahl]");if(!p.items.ids().contains(args[2]))throw new IllegalArgumentException("Unbekannte Item-ID: "+args[2]);Player target=args.length>3?online(args[3]):player(sender);String action=args[1].toLowerCase(Locale.ROOT);if(action.equals("count")){p.reply(sender,target.getName()+": "+p.items.count(target,args[2])+" × "+args[2]);break;}int amount=args.length>4?Integer.parseInt(args[4]):1;if(amount<1||amount>(action.equals("give")?64:4096))throw new IllegalArgumentException("Ungültige Anzahl.");if(action.equals("give"))p.items.deliver(target.getUniqueId(),p.items.make(args[2],amount));else if(!p.items.take(target,args[2],amount))throw new IllegalArgumentException("Zu wenige echte Items; nichts entfernt.");else p.reply(sender,amount+" × "+args[2]+" entfernt.");}
                case "reload"->{permission(sender,"mobarmy.admin");if(p.editor!=null&&!p.editor.edits.isEmpty())throw new IllegalArgumentException(p.tr(sender,"editor.finish-first"));if(p.portals.values().stream().anyMatch(q->q.session!=null))throw new IllegalArgumentException("Stop all sessions before reload");if(!p.ritualSystem.casts.isEmpty()||!p.ritualSystem.guardians.isEmpty())throw new IllegalArgumentException("Zuerst alle Rituale/Wächter beenden oder Server neu starten");p.reloadConfig();Map<String,Arena> loaded=new LinkedHashMap<>();var cfg=p.getConfig().getConfigurationSection("weeks");if(cfg==null)throw new IllegalArgumentException("Missing weeks");for(String id:cfg.getKeys(false))loaded.put(id,new Arena(id,cfg.getConfigurationSection(id)));if(p.portals.values().stream().anyMatch(q->!loaded.containsKey(q.week)))throw new IllegalArgumentException("Remove portals before removing their weeks");p.arenas.clear();p.arenas.putAll(loaded);p.zone=java.time.ZoneId.of(p.getConfig().getString("timezone","Europe/Berlin"));p.applyDemoPvp();p.packs.restart();p.presentation.shutdown();p.presentation=new Presentation(p);p.say(sender,"admin.reloaded");}
                default->throw new IllegalArgumentException("Unknown subcommand. /raid help");
            }
        }catch(RuntimeException ex){p.reply(sender,"&c"+Objects.toString(ex.getMessage(),"Invalid command"));}
        return true;
    }
    void create(Player player,String week,String id,Region region){
        if(!id.matches("[A-Za-z0-9_-]{1,40}"))throw new IllegalArgumentException("Portal ID: letters, numbers, - or _ (1-40 chars)");
        if(!p.arenas.containsKey(week))throw new IllegalArgumentException("Unknown week: "+week);
        if(p.portals.containsKey(id))throw new IllegalArgumentException("Portal already exists: "+id);
        if(p.arena(player.getWorld())!=null)throw new IllegalArgumentException("Create the portal in your spawn/default world");
        if(region.volume()>10000)throw new IllegalArgumentException("Portal selection exceeds 10,000 blocks");
        if(p.portals.values().stream().anyMatch(q->q.region.world().equals(region.world())&&q.region.x1()<=region.x2()&&q.region.x2()>=region.x1()&&q.region.y1()<=region.y2()&&q.region.y2()>=region.y1()&&q.region.z1()<=region.z2()&&q.region.z2()>=region.z1()))throw new IllegalArgumentException("Portal regions overlap");
        Location exit=player.getLocation().clone();if(region.contains(exit))exit.setZ(region.z1()-2);
        if(!exit.getBlock().isPassable()||!exit.clone().add(0,1,0).getBlock().isPassable()||!exit.clone().subtract(0,1,0).getBlock().getType().isSolid())throw new IllegalArgumentException("Stand on a safe floor outside the portal selection before creating it");
        Portal portal=new Portal(p,id,week,region,exit);p.portals.put(id,portal);p.saveState();p.menus.timer(player,portal);
    }
    void basicArmor(Player player){
        ItemStack[] armor={new ItemStack(Material.NETHERITE_BOOTS),new ItemStack(Material.NETHERITE_LEGGINGS),new ItemStack(Material.NETHERITE_CHESTPLATE),new ItemStack(Material.NETHERITE_HELMET)};
        if(Arrays.stream(player.getInventory().getArmorContents()).anyMatch(item->p.items.id(item)!=null))for(ItemStack item:armor)p.items.deliver(player.getUniqueId(),item);
        else player.getInventory().setArmorContents(armor);
    }
    Player online(String name){Player target=Bukkit.getPlayerExact(name);if(target==null)throw new IllegalArgumentException("Spieler ist offline: "+name);return target;}
    void clearCooldowns(UUID target){
        if(target==null)p.state.set("cooldowns",null);else{var c=p.state.getConfigurationSection("cooldowns");if(c!=null)for(String day:c.getKeys(false))p.state.set("cooldowns."+day+"."+target,null);}
        if(p.getConfig().getBoolean("testing.solo"))for(String section:List.of("item-cooldowns","ritual-cooldowns"))p.state.set(target==null?section:section+"."+target,null);
        for(Portal q:p.portals.values())if(q.session!=null){if(target==null)q.session.dead.clear();else q.session.dead.remove(target);}p.saveState();if(p.itemCooldowns!=null)p.itemCooldowns.clear(target);
    }
    void resetDemo(Portal q){if(!q.id.startsWith("demo_")&&!p.getConfig().getBoolean("testing.solo"))throw new IllegalArgumentException("Nur Demo-Portale oder Testmodus können so zurückgesetzt werden.");if(q.session!=null)p.engine.finish(q.session);q.bossKilled=false;q.deadline=0;q.day=p.today();p.state.set("progress."+q.id,null);p.saveState();}
    void demoOnly(Portal q){if(!q.id.startsWith("demo_")&&!p.getConfig().getBoolean("testing.solo"))throw new IllegalArgumentException("Demo-Steuerung ist nur in der Demo verfügbar.");}
    void remove(Portal q){
        if(q.session!=null)p.engine.finish(q.session);q.removeLabel();World w=Bukkit.getWorld(q.region.world());
        for(Player player:Bukkit.getOnlinePlayers())if(player.getOpenInventory().getTopInventory().getHolder() instanceof Menus.View view&&view.portal.equals(q.id))player.closeInventory();
        if(w!=null)for(var e:w.getEntities())if(e instanceof org.bukkit.entity.TextDisplay||e instanceof org.bukkit.entity.ArmorStand){String tag=e.getPersistentDataContainer().get(p.entityKey,org.bukkit.persistence.PersistentDataType.STRING);if(("portal:"+q.id).equals(tag)||tag!=null&&tag.startsWith("portal-v2:"+q.id+":"))e.remove();}
        p.portals.remove(q.id);p.state.set("portals."+q.id,null);p.state.set("progress."+q.id,null);p.saveState();
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){
        if(command.getName().equalsIgnoreCase("rezepte"))return sender.hasPermission("mobarmy.play")&&args.length==1?List.of("items","gear","sets","rituals").stream().filter(x->x.startsWith(args[0].toLowerCase(Locale.ROOT))).toList():List.of();
        Set<String> allowed=new LinkedHashSet<>(List.of("help","leave","status","pack","language","lang","sprache"));
        if(sender.hasPermission("mobarmy.play"))allowed.addAll(List.of("join","start","ready","loot","rezepte","recipes","codex","gear","ritual","rituals"));
        if(sender.hasPermission("mobarmy.admin"))allowed.addAll(List.of("portal","open","close","stop","reload","demo","test","kit","cooldown","skip","boss"));
        if(sender.hasPermission("mobarmy.timer"))allowed.add("timer");if(sender.hasPermission("mobarmy.item"))allowed.add("item");if(sender.hasPermission("mobarmy.build"))allowed.addAll(List.of("build","visit","arena"));
        return Completion.suggest(args,allowed,p.arenas.keySet(),p.portals.keySet(),p.items.ids(),Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(),p.ritualSystem.ids(),p.bosses.ids());
    }
}
