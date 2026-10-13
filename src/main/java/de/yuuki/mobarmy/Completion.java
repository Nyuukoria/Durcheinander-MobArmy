package de.yuuki.mobarmy;

import java.util.*;

/** Command grammar independent of Bukkit so every argument position can be tested. */
final class Completion {
    static List<String> suggest(String[] args,Collection<String> allowed,Collection<String> weeks,Collection<String> portals,Collection<String> items,Collection<String> players){
        if(args.length==0)return List.of();List<String> out=new ArrayList<>();String root=args[0].toLowerCase(Locale.ROOT);
        if(args.length==1)out.addAll(allowed);
        else if(allowed.contains(root))switch(root){
            case "portal"->{
                if(args.length==2)out.addAll(List.of("pos1","pos2","create","remove","info"));
                else if(args.length==3&&args[1].equalsIgnoreCase("create"))out.addAll(weeks);
                else if(args.length==3&&(args[1].equalsIgnoreCase("remove")||args[1].equalsIgnoreCase("info"))){out.addAll(portals);if(args[1].equalsIgnoreCase("remove"))out.add("all");}
                else if(args.length==4&&args[1].equalsIgnoreCase("create"))out.add(args[2]+"_event");
            }
            case "timer"->{if(args.length==2)out.addAll(List.of("set","add","clear"));else if(args.length==3&&args[1].equalsIgnoreCase("clear"))out.addAll(portals);else if(args.length==3&&(args[1].equalsIgnoreCase("set")||args[1].equalsIgnoreCase("add")))out.addAll(List.of("15m","30m","1h","2h"));else if(args.length==4&&!args[1].equalsIgnoreCase("clear"))out.addAll(portals);}
            case "item"->{if(args.length==2)out.addAll(List.of("give","take","count"));else if(args.length==3)out.addAll(items);else if(args.length==4)out.addAll(players);else if(args.length==5&&!args[1].equalsIgnoreCase("count"))out.addAll(List.of("1","8","16","32","64"));}
            case "cooldown"->{if(args.length==2)out.add("clear");else if(args.length==3&&args[1].equalsIgnoreCase("clear")){out.addAll(players);out.add("all");}}
            case "demo"->{if(args.length==2){out.addAll(weeks);out.addAll(List.of("reset","rebuild"));}else if(args.length==3&&args[1].equalsIgnoreCase("reset")){out.addAll(portals);out.add("all");}else if(args.length==3&&args[1].equalsIgnoreCase("rebuild"))out.addAll(weeks);}
            case "pack"->{if(args.length==2)out.addAll(List.of("status","reload"));}
            case "test"->{if(args.length==2)out.addAll(List.of("on","off"));}
            case "visit"->{if(args.length==2)out.addAll(weeks);}
            case "join","open","close","stop","reset"->{if(args.length==2)out.addAll(portals);}
            default->{}
        }
        String prefix=args[args.length-1].toLowerCase(Locale.ROOT);return out.stream().distinct().filter(x->x.toLowerCase(Locale.ROOT).startsWith(prefix)).sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }
}
