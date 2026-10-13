package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import java.util.*;

/** Read-only in-game item/set/ritual book; browsing never grants loot. */
final class Catalogue implements Listener {
    final MobArmy p;
    Catalogue(MobArmy p){this.p=p;}
    static final class View implements InventoryHolder {
        final UUID owner;final String category;final int page;Inventory inventory;
        View(UUID owner,String category,int page){this.owner=owner;this.category=category;this.page=page;}
        @Override public Inventory getInventory(){return inventory;}
    }
    List<String> entries(String category){
        return switch(category){
            case "rituals"->new ArrayList<>(p.ritualSystem.ids());
            case "sets"->p.items.ids().stream().filter(id->p.getConfig().contains("items."+id+".set")).toList();
            case "gear"->p.items.ids().stream().filter(id->p.getConfig().getBoolean("items."+id+".gear")).toList();
            case "items"->p.items.ids().stream().filter(id->!id.equals("boss_model")).toList();
            default->throw new IllegalArgumentException("/raid codex items|gear|sets|rituals");
        };
    }
    void open(Player player,String category,int page){
        List<String> ids=entries(category);int current=Math.max(0,Math.min(page,Math.max(0,(ids.size()-1)/45)));
        View v=new View(player.getUniqueId(),category,current);v.inventory=Bukkit.createInventory(v,54,MobArmy.color("&dRaid-Kodex · "+category+" · "+(current+1)));
        for(int i=current*45;i<Math.min(ids.size(),(current+1)*45);i++){
            String id=ids.get(i);ItemStack icon;List<String> extra=new ArrayList<>();
            if(category.equals("rituals")){
                var cfg=p.ritualSystem.recipe(id);icon=p.items.make(cfg.getString("display-item","ritual_core"),1);var meta=icon.getItemMeta();meta.setDisplayName(MobArmy.color(cfg.getString("name")));meta.setLore(List.of(MobArmy.color("&7Kessel + Chaosfragment in der Haupthand")));icon.setItemMeta(meta);
                extra.add("&7Modus: "+cfg.getString("mode"));for(var ingredient:p.ritualSystem.requirements(cfg))extra.add("&7Zutat: "+ingredient.amount()+" × "+p.ritualSystem.ingredientName(ingredient));
                extra.add("&7Cooldown: "+cfg.getInt("cooldown-seconds")+"s");extra.add("&eAusführen: /raid ritual "+id);
            }else{
                icon=p.items.make(id,1);var cfg=p.getConfig().getConfigurationSection("items."+id);extra.add("&7Seltenheit: "+cfg.getString("rarity","common"));
                for(var drop:cfg.getMapList("drops")){double chance=drop.get("chance") instanceof Number n?n.doubleValue():1;extra.add("&7Drop: "+drop.get("source")+(drop.containsKey("stage")?" · Stage "+drop.get("stage"):"")+" · "+String.format(Locale.ROOT,"%.2f%%",100*chance));}
                var pools=p.getConfig().getConfigurationSection("loot-pools");for(String poolId:pools.getKeys(false)){
                    var pool=pools.getConfigurationSection(poolId);double total=0,weight=0;for(var row:pool.getMapList("entries")){double w=row.get("weight") instanceof Number n?n.doubleValue():1;total+=w;if(id.equals(row.get("item")))weight+=w;}
                    if(weight>0)extra.add("&7"+pool.getString("source")+" ab Stage "+pool.getInt("minimum-stage",1)+": "+String.format(Locale.ROOT,"%.2f%%",100*pool.getDouble("chance")*weight/total)+" für dieses Item");
                }
            }
            var meta=icon.getItemMeta();List<String> lore=new ArrayList<>(meta.hasLore()?meta.getLore():List.of());lore.add("");lore.addAll(extra.stream().map(MobArmy::color).toList());lore.add(MobArmy.color("&8ID: "+id));meta.setLore(lore);icon.setItemMeta(meta);v.inventory.setItem(i-current*45,icon);
        }
        v.inventory.setItem(45,p.menus.button(Material.ARROW,"&fZurück"));v.inventory.setItem(46,p.menus.button(Material.ARROW,"&fWeiter"));
        v.inventory.setItem(48,p.menus.button(Material.BOOK,"&dItems"));v.inventory.setItem(49,p.menus.button(Material.DIAMOND_SWORD,"&bAusrüstung"));v.inventory.setItem(50,p.menus.button(Material.DIAMOND_CHESTPLATE,"&eSets"));v.inventory.setItem(51,p.menus.button(Material.CAULDRON,"&5Rituale"));v.inventory.setItem(53,p.menus.button(Material.BARRIER,"&cSchließen"));player.openInventory(v.inventory);
    }
    @EventHandler public void click(InventoryClickEvent event){
        if(!(event.getView().getTopInventory().getHolder() instanceof View v))return;event.setCancelled(true);
        if(!(event.getWhoClicked() instanceof Player player)||!v.owner.equals(player.getUniqueId()))return;
        switch(event.getRawSlot()){case 45->open(player,v.category,v.page-1);case 46->open(player,v.category,v.page+1);case 48->open(player,"items",0);case 49->open(player,"gear",0);case 50->open(player,"sets",0);case 51->open(player,"rituals",0);case 53->player.closeInventory();default->{}}
    }
    @EventHandler public void drag(InventoryDragEvent event){if(event.getView().getTopInventory().getHolder() instanceof View)event.setCancelled(true);}
}
