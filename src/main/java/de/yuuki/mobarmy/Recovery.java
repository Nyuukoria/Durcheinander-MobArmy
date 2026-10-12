package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import java.util.*;

/** Persist temporary gates/placements so an interrupted server does not leave a sealed arena. */
final class Recovery {
    private Recovery() {}
    static ItemStack placedItem(org.bukkit.block.Block block){
        ItemStack item=new ItemStack(block.getType());
        if(block.getState() instanceof ShulkerBox shulker&&item.getItemMeta() instanceof BlockStateMeta meta){meta.setBlockState(shulker);item.setItemMeta(meta);}
        return item;
    }
    static void snapshot(MobArmy p){
        List<Map<String,Object>> saved=new ArrayList<>();
        for(Portal portal:p.portals.values())if(portal.session!=null){
            Session s=portal.session;
            s.blockOriginal.forEach((l,data)->{Map<String,Object> row=new LinkedHashMap<>();row.put("location",l);row.put("original",data.getAsString());row.put("owner",s.blocks.get(l).toString());saved.add(row);});
            s.arena.doorOriginal.forEach((l,data)->{Map<String,Object> row=new LinkedHashMap<>();row.put("location",l);row.put("original",data.getAsString());saved.add(row);});
        }
        p.state.set("recovery-blocks",saved.isEmpty()?null:saved);
    }
    static void restore(MobArmy p){
        Map<UUID,List<ItemStack>> returned=new HashMap<>();
        for(Map<?,?> row:p.state.getMapList("recovery-blocks")){
            if(!(row.get("location") instanceof Location l)||l.getWorld()==null)continue;
            if(row.containsKey("owner")){
                ItemStack item=placedItem(l.getBlock());if(!item.getType().isAir())returned.computeIfAbsent(UUID.fromString(row.get("owner").toString()),id->new ArrayList<>()).add(item);
            }
            l.getBlock().setBlockData(Bukkit.createBlockData(row.get("original").toString()),false);
        }
        p.state.set("recovery-blocks",null);
        returned.forEach((id,items)->{List<ItemStack> mail=p.items.mailbox(id);mail.addAll(items);p.state.set("mail."+id,mail);});
    }
}
