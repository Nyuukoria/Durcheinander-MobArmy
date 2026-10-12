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
    Player player(CommandSender sender){if(sender instanceof Player player)return player;throw new IllegalArgumentException("Run this command in-game");}
    void permission(CommandSender sender,String permission){if(!sender.hasPermission(permission))throw new IllegalArgumentException("Missing permission: "+permission);}
    Portal portal(CommandSender sender,String[] args,int index){
        if(args.length>index){Portal portal=p.portals.get(args[index]);if(portal==null)throw new IllegalArgumentException("Unknown portal: "+args[index]);return portal;}
        if(sender instanceof Player player){Session s=p.session(player.getUniqueId());if(s!=null)return s.portal;Portal at=p.portalAt(player.getLocation());if(at!=null)return at;}
        if(p.portals.size()==1)return p.portals.values().iterator().next();
        throw new IllegalArgumentException("Specify portal ID. Available: "+p.portals.keySet());
    }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        try{
            if(args.length==0){permission(sender,"mobarmy.play");p.menus.party(player(sender),0);return true;}
            switch(args[0].toLowerCase(Locale.ROOT)){
                case "help"->sender.sendMessage(MobArmy.color("&d/raid &f— GUI\n/raid join <portal> | start | leave | loot | status\n&dAdmin: &fportal pos1|pos2|create <week> [id]|remove [id]\n/raid open|close|stop [id] | timer set|add|clear <duration> [id]\n/raid demo <week> | item give <id> [player] [amount]\n/raid test on|off | kit | build | visit <week> | reload\nDurations: 30m, 2h, 90s (plain numbers = minutes)."));
                case "join"->{permission(sender,"mobarmy.play");p.engine.join(player(sender),portal(sender,args,1));}
                case "start","ready"->{permission(sender,"mobarmy.play");p.engine.ready(player(sender));}
                case "leave"->{Player player=player(sender);player.closeInventory();p.engine.leave(player,false);}
                case "loot"->{permission(sender,"mobarmy.play");p.menus.mail(player(sender),0);}
                case "status"->{for(Portal q:p.portals.values())sender.sendMessage(q.id+" | "+q.week+" | "+(q.open?"open":"closed")+" | rewards="+q.rewards()+" | "+(q.session==null?"idle":"Stage "+q.session.stage+" / Wave "+q.session.wave+" / "+q.session.phase));if(p.getConfig().getBoolean("testing.solo"))sender.sendMessage("TEST MODE: solo enabled; production minimum/share limits are overridden.");}
                case "portal"->{permission(sender,"mobarmy.admin");Player player=player(sender);if(args.length<2)throw new IllegalArgumentException("/raid portal pos1|pos2|create <week> [id]|remove [id]");switch(args[1]){
                    case "pos1"->{first.put(player.getUniqueId(),player.getLocation());p.tell(player,"Position 1 gesetzt.");}
                    case "pos2"->{second.put(player.getUniqueId(),player.getLocation());p.tell(player,"Position 2 gesetzt.");}
                    case "create"->{if(args.length<3)throw new IllegalArgumentException("/raid portal create <week> [id]");Location a=first.get(player.getUniqueId()),b=second.get(player.getUniqueId());if(a==null||b==null)throw new IllegalArgumentException("Select pos1 and pos2 first");create(player,args[2],args.length>3?args[3]:args[2],Region.between(a,b));}
                    case "remove"->{Portal q=portal(sender,args,2);if(q.session!=null)throw new IllegalArgumentException("Stop this portal's raid first");q.removeLabel();p.portals.remove(q.id);p.state.set("portals."+q.id,null);p.saveState();}
                    default->throw new IllegalArgumentException("Unknown portal action");}}
                case "open","close"->{permission(sender,"mobarmy.admin");Portal q=portal(sender,args,1);q.open=args[0].equals("open");p.saveState();sender.sendMessage(q.id+": "+(q.open?"open":"closed"));}
                case "stop"->{permission(sender,"mobarmy.admin");Portal q=portal(sender,args,1);if(q.session!=null)p.engine.finish(q.session);}
                case "timer"->{permission(sender,"mobarmy.timer");if(args.length<2)throw new IllegalArgumentException("/raid timer set|add <duration> [id] OR /raid timer clear [id]");boolean clear=args[1].equals("clear");Portal q=portal(sender,args,clear?2:3);
                    if(clear)q.deadline=0;else{if(args.length<3)throw new IllegalArgumentException("Duration required");long duration=Rules.duration(args[2]);if(args[1].equals("set"))q.setTimer(duration);else if(args[1].equals("add")){q.day=p.today();q.deadline=Math.max(System.currentTimeMillis(),q.deadline)+duration;}else throw new IllegalArgumentException("Use set, add or clear");}
                    p.saveState();sender.sendMessage("Reward timer updated. A completed boss remains no-rewards; create a new portal ID for a new event.");}
                case "demo"->{permission(sender,"mobarmy.admin");Player player=player(sender);String week=args.length>1?args[1]:"w1";Location l=player.getLocation().clone().add(4,0,0);Region region=new Region(l.getWorld().getName(),l.getBlockX(),l.getBlockY(),l.getBlockZ(),l.getBlockX()+2,l.getBlockY()+3,l.getBlockZ());create(player,week,"demo_"+week,region);}
                case "test"->{permission(sender,"mobarmy.admin");if(p.portals.values().stream().anyMatch(q->q.session!=null))throw new IllegalArgumentException("Stop all sessions before changing test mode");boolean on=args.length>1&&args[1].equals("on");p.getConfig().set("testing.solo",on);p.saveConfig();sender.sendMessage("Solo testing "+(on?"ON: minimum 1 and boss share 100%":"OFF: production rules restored"));}
                case "build"->{permission(sender,"mobarmy.build");Player player=player(sender);if(p.portals.values().stream().anyMatch(q->q.session!=null))throw new IllegalArgumentException("Stop all sessions before enabling build mode");if(!builders.remove(player.getUniqueId()))builders.add(player.getUniqueId());p.tell(player,"Build mode: "+builders.contains(player.getUniqueId()));}
                case "visit"->{permission(sender,"mobarmy.build");Player player=player(sender);if(args.length<2||!p.arenas.containsKey(args[1]))throw new IllegalArgumentException("/raid visit <week>");if(!builders.contains(player.getUniqueId()))throw new IllegalArgumentException("Enable /raid build first");player.teleport(p.arenas.get(args[1]).lobbySpawn);}
                case "kit"->{permission(sender,"mobarmy.admin");Player player=player(sender);player.getInventory().setArmorContents(new ItemStack[]{new ItemStack(Material.NETHERITE_BOOTS),new ItemStack(Material.NETHERITE_LEGGINGS),new ItemStack(Material.NETHERITE_CHESTPLATE),new ItemStack(Material.NETHERITE_HELMET)});for(Material m:List.of(Material.NETHERITE_SWORD,Material.BOW,Material.MACE))p.items.deliver(player.getUniqueId(),new ItemStack(m));p.items.deliver(player.getUniqueId(),new ItemStack(Material.ARROW,64));p.items.deliver(player.getUniqueId(),new ItemStack(Material.COOKED_BEEF,64));p.items.deliver(player.getUniqueId(),new ItemStack(Material.GOLDEN_APPLE,6));p.items.deliver(player.getUniqueId(),new ItemStack(Material.ENDER_PEARL,16));}
                case "item"->{permission(sender,"mobarmy.item");if(args.length<3||!args[1].equals("give"))throw new IllegalArgumentException("/raid item give <id> [player] [amount]");Player target=args.length>3?Bukkit.getPlayerExact(args[3]):player(sender);if(target==null)throw new IllegalArgumentException("Player is offline");int amount=args.length>4?Integer.parseInt(args[4]):1;if(amount<1||amount>64)throw new IllegalArgumentException("Amount must be 1-64");p.items.deliver(target.getUniqueId(),p.items.make(args[2],amount));}
                case "reload"->{permission(sender,"mobarmy.admin");if(p.portals.values().stream().anyMatch(q->q.session!=null))throw new IllegalArgumentException("Stop all sessions before reload");p.reloadConfig();Map<String,Arena> loaded=new LinkedHashMap<>();var cfg=p.getConfig().getConfigurationSection("weeks");if(cfg==null)throw new IllegalArgumentException("Missing weeks");for(String id:cfg.getKeys(false))loaded.put(id,new Arena(id,cfg.getConfigurationSection(id)));if(p.portals.values().stream().anyMatch(q->!loaded.containsKey(q.week)))throw new IllegalArgumentException("Remove portals before removing their weeks");p.arenas.clear();p.arenas.putAll(loaded);p.zone=java.time.ZoneId.of(p.getConfig().getString("timezone","Europe/Berlin"));p.applyDemoPvp();sender.sendMessage("Raid config reloaded.");}
                default->throw new IllegalArgumentException("Unknown subcommand. /raid help");
            }
        }catch(RuntimeException ex){sender.sendMessage(ChatColor.RED+Objects.toString(ex.getMessage(),"Invalid command"));}
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
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){
        List<String> options=new ArrayList<>();if(args.length==1)options.addAll(List.of("help","join","start","leave","loot","status","portal","timer","open","close","stop","reload","demo","test","build","visit","kit","item"));
        else if(args.length==2&&args[0].equals("portal"))options.addAll(List.of("pos1","pos2","create","remove"));
        else if(args.length==2&&args[0].equals("timer"))options.addAll(List.of("set","add","clear"));
        else if(args.length==2&&args[0].equals("test"))options.addAll(List.of("on","off"));
        else if((args.length==2&&(args[0].equals("demo")||args[0].equals("visit")))||(args.length==3&&args[0].equals("portal")&&args[1].equals("create")))options.addAll(p.arenas.keySet());
        else if(args.length==3&&args[0].equals("item"))options.addAll(p.items.ids());
        else options.addAll(p.portals.keySet());String prefix=args[args.length-1].toLowerCase(Locale.ROOT);return options.stream().filter(s->s.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
