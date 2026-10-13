package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import java.util.*;

/** Read-only recipes: short cards, then separate drop/ritual/ingredient panels. */
final class Catalogue implements Listener {
    final MobArmy p;
    Catalogue(MobArmy p){this.p=p;}
    static final class View implements InventoryHolder {
        final UUID owner;final String category;final int page;final boolean detail;final Map<Integer,String> cards=new HashMap<>(),recipes=new HashMap<>();Inventory inventory;
        View(UUID owner,String category,int page,boolean detail){this.owner=owner;this.category=category;this.page=page;this.detail=detail;}
        @Override public Inventory getInventory(){return inventory;}
    }
    List<String> entries(String category){return switch(category){
        case "rituals"->new ArrayList<>(p.ritualSystem.ids());case "sets"->p.items.ids().stream().filter(id->p.getConfig().contains("items."+id+".set")).toList();case "gear"->p.items.ids().stream().filter(id->p.getConfig().getBoolean("items."+id+".gear")).toList();case "items"->p.items.ids().stream().filter(id->!p.getConfig().getBoolean("items."+id+".internal")).toList();default->throw new IllegalArgumentException("/rezepte items|gear|sets|rituals");};}
    View view(Player pl,String category,int page,boolean detail,String title){View v=new View(pl.getUniqueId(),category,page,detail);v.inventory=Bukkit.createInventory(v,54,MobArmy.color(title));return v;}
    ItemStack item(Player pl,String id){return p.language.localized(pl.getUniqueId(),p.items.make(id,1));}
    ItemStack card(Player pl,Material material,String name,List<String> lore){ItemStack item=p.menus.button(material,name);var meta=item.getItemMeta();meta.setLore(TextLayout.wrap(lore.stream().map(MobArmy::color).toList(),36));item.setItemMeta(meta);return item;}
    String category(Player pl,String category){return p.tr(pl,"gui."+(category.equals("rituals")?"rituals":category.equals("gear")?"gear":category.equals("sets")?"sets":"items"));}
    void navigation(Player pl,View v){
        v.inventory.setItem(45,p.menus.button(Material.ARROW,p.tr(pl,"gui.back")));if(!v.detail)v.inventory.setItem(46,p.menus.button(Material.ARROW,p.tr(pl,"gui.next")));
        v.inventory.setItem(48,p.menus.button(Material.BOOK,p.tr(pl,"gui.items")));v.inventory.setItem(49,p.menus.button(Material.DIAMOND_SWORD,p.tr(pl,"gui.gear")));v.inventory.setItem(50,p.menus.button(Material.DIAMOND_CHESTPLATE,p.tr(pl,"gui.sets")));v.inventory.setItem(51,p.menus.button(Material.CAULDRON,p.tr(pl,"gui.rituals")));v.inventory.setItem(53,p.menus.button(Material.BARRIER,p.tr(pl,"gui.close")));
    }
    void open(Player pl,String category,int page){
        List<String> ids=entries(category);int current=Math.max(0,Math.min(page,Math.max(0,(ids.size()-1)/28)));View v=view(pl,category,current,false,p.tr(pl,"gui.catalogue-title","category",ChatColor.stripColor(MobArmy.color(category(pl,category))),"page",current+1));
        for(int i=current*28;i<Math.min(ids.size(),(current+1)*28);i++){
            String id=ids.get(i);ItemStack icon;
            if(category.equals("rituals")){var cfg=p.ritualSystem.recipe(id);icon=p.ritualSystem.icon(pl,cfg);}
            else icon=item(pl,id);
            var meta=icon.getItemMeta();List<String> lore=new ArrayList<>(meta.hasLore()?meta.getLore():List.of());lore.add("");lore.add(MobArmy.color(p.tr(pl,"gui.details")));meta.setLore(TextLayout.wrap(lore,36));icon.setItemMeta(meta);int slot=TextLayout.gridSlot(i-current*28);v.cards.put(slot,id);v.inventory.setItem(slot,icon);
        }
        navigation(pl,v);p.language.open(pl,v.inventory);
    }
    List<String> recipesFor(String item){
        List<String> found=new ArrayList<>();for(String id:p.ritualSystem.ids()){
            var cfg=p.ritualSystem.recipe(id);boolean matches=item.equals(cfg.getString("result"));for(String field:List.of("pool","legendary-pool")){var pool=p.getConfig().getConfigurationSection("loot-pools."+cfg.getString(field,""));if(pool!=null&&pool.getMapList("entries").stream().anyMatch(row->item.equals(row.get("item"))))matches=true;}
            if(matches)found.add(id);
        }return found;
    }
    void detail(Player pl,String id,String category,int page){
        View v=view(pl,category,page,true,p.tr(pl,"gui.item-title","name",ChatColor.stripColor(MobArmy.color(p.language.itemName(pl,id)))));v.inventory.setItem(13,item(pl,id));
        List<String> drops=new ArrayList<>();var cfg=p.getConfig().getConfigurationSection("items."+id);
        for(var drop:cfg.getMapList("drops")){
            String source=drop.get("source").toString();int stage=drop.get("stage") instanceof Number n?n.intValue():source.equals("boss")?5:1;double chance=drop.get("chance") instanceof Number n?n.doubleValue():1;
            dropInfo(pl,drops,source,stage,chance,drop.containsKey("stage"));
        }
        var pools=p.getConfig().getConfigurationSection("loot-pools");for(String poolId:pools.getKeys(false)){
            var pool=pools.getConfigurationSection(poolId);double total=0,weight=0;for(var row:pool.getMapList("entries")){double w=row.get("weight") instanceof Number n?n.doubleValue():1;total+=w;if(id.equals(row.get("item")))weight+=w;}
            if(weight>0&&total>0){String source=pool.getString("source");dropInfo(pl,drops,source,Math.max(source.equals("boss")?5:1,pool.getInt("minimum-stage",1)),pool.getDouble("chance")*weight/total,source.equals("boss"));}
        }
        if(drops.isEmpty())drops.add(p.tr(pl,"gui.drop-none"));else drops.add(p.tr(pl,"gui.rewards-only"));
        v.inventory.setItem(20,card(pl,Material.PAPER,p.tr(pl,"gui.drops"),drops));
        List<String> recipes=recipesFor(id);if(recipes.isEmpty())v.inventory.setItem(24,card(pl,Material.CAULDRON,p.tr(pl,"gui.rituals"),List.of(p.tr(pl,"gui.no-recipe"))));
        else for(int i=0;i<Math.min(7,recipes.size());i++){String recipe=recipes.get(i);int slot=28+i;var recipeCfg=p.ritualSystem.recipe(recipe);v.recipes.put(slot,recipe);v.inventory.setItem(slot,card(pl,Material.CAULDRON,p.language.recipeName(pl,recipeCfg),List.of("",p.tr(pl,"gui.ritual-route"))));}
        navigation(pl,v);p.language.open(pl,v.inventory);
    }
    void dropInfo(Player pl,List<String> lines,String source,int stage,double chance,boolean exact){
        if(!lines.isEmpty())lines.add("");lines.add(p.tr(pl,exact?"gui.drop-exact":"gui.drop-source","source",p.tr(pl,"source."+source),"stage",stage));lines.add(p.tr(pl,"gui.drop-chance","chance",String.format(p.language.code(pl).equals("de")?Locale.GERMANY:Locale.UK,"%.2f",100*chance)));
    }
    void recipe(Player pl,String id,String category,int page){
        ConfigurationSection cfg=p.ritualSystem.recipe(id);View v=view(pl,category,page,true,p.tr(pl,"gui.ritual-title","name",ChatColor.stripColor(MobArmy.color(p.language.recipeName(pl,cfg)))));v.inventory.setItem(13,p.ritualSystem.icon(pl,cfg));
        List<String> instructions=List.of("",p.tr(pl,"gui.recipe-step1","world",p.getConfig().getString("ritual-world","world")),p.tr(pl,"gui.recipe-step2"),p.tr(pl,"gui.recipe-step3"),"",p.tr(pl,"gui.recipe-catalyst"));v.inventory.setItem(20,card(pl,Material.CAULDRON,p.tr(pl,"gui.recipe-instructions"),instructions));
        v.inventory.setItem(24,card(pl,Material.CLOCK,p.tr(pl,"gui.duration","seconds",cfg.getInt("duration-seconds",8)),List.of("",p.tr(pl,"gui.cooldown","seconds",cfg.getInt("cooldown-seconds",120)))));
        List<RitualPayments.Requirement> requirements=p.ritualSystem.requirements(cfg);for(int i=0;i<Math.min(7,requirements.size());i++){
            var required=requirements.get(i);ItemStack ingredient=required.customId()!=null?item(pl,required.customId()):new ItemStack(Material.valueOf(required.material()));var meta=ingredient.getItemMeta();meta.setDisplayName(MobArmy.color("&f"+required.amount()+" × "+p.ritualSystem.ingredientName(pl,required)));meta.setLore(List.of("",MobArmy.color(p.tr(pl,"gui.ingredient-consumed"))));ingredient.setItemMeta(meta);v.inventory.setItem(28+i,ingredient);
        }
        if(requirements.isEmpty())v.inventory.setItem(31,card(pl,Material.PAPER,p.tr(pl,"gui.ingredients"),List.of(p.tr(pl,"gui.no-ingredients"))));navigation(pl,v);p.language.open(pl,v.inventory);
    }
    @EventHandler public void click(InventoryClickEvent e){
        if(!(e.getView().getTopInventory().getHolder() instanceof View v))return;e.setCancelled(true);if(!(e.getWhoClicked() instanceof Player pl)||!v.owner.equals(pl.getUniqueId()))return;int slot=e.getRawSlot();
        if(v.recipes.containsKey(slot)){recipe(pl,v.recipes.get(slot),v.category,v.page);return;}if(v.cards.containsKey(slot)){String id=v.cards.get(slot);if(v.category.equals("rituals"))recipe(pl,id,v.category,v.page);else detail(pl,id,v.category,v.page);return;}
        switch(slot){case 45->open(pl,v.category,v.detail?v.page:v.page-1);case 46->{if(!v.detail)open(pl,v.category,v.page+1);}case 48->open(pl,"items",0);case 49->open(pl,"gear",0);case 50->open(pl,"sets",0);case 51->open(pl,"rituals",0);case 53->pl.closeInventory();default->{}}
    }
    @EventHandler public void drag(InventoryDragEvent e){if(e.getView().getTopInventory().getHolder() instanceof View)e.setCancelled(true);}
}
