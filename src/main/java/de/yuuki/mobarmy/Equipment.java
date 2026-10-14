package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.potion.*;
import java.util.*;

final class Equipment implements Listener {
    final MobArmy p;
    Equipment(MobArmy p){this.p=p;}
    static void decorate(ConfigurationSection cfg,ItemMeta meta){
        if(cfg.contains("rarity")){String rarity=cfg.getString("rarity","common").toUpperCase(Locale.ROOT);meta.setRarity(ItemRarity.valueOf(rarity.equals("LEGENDARY")?"EPIC":rarity));}
        var enchants=cfg.getConfigurationSection("enchantments");
        if(enchants!=null)for(String key:enchants.getKeys(false)){
            var enchant=Registry.ENCHANTMENT.getOrThrow(NamespacedKey.minecraft(key));int level=enchants.getInt(key);
            if(level<1||level>10)throw new IllegalArgumentException("Gear enchantment level must be 1..10: "+key);
            meta.addEnchant(enchant,Math.max(level,meta.getEnchantLevel(enchant)),true);
        }
        if(meta instanceof ArmorMeta armor&&cfg.isConfigurationSection("trim"))armor.setTrim(new ArmorTrim(
            Registry.TRIM_MATERIAL.getOrThrow(NamespacedKey.minecraft(cfg.getString("trim.material"))),
            Registry.TRIM_PATTERN.getOrThrow(NamespacedKey.minecraft(cfg.getString("trim.pattern")))));
    }
    Map<String,Integer> sets(Player player){
        Map<String,Integer> result=new LinkedHashMap<>();Set<String> used=new HashSet<>();String[] slots={"boots","leggings","chestplate","helmet"};int index=0;
        for(ItemStack item:player.getInventory().getArmorContents()){
            String id=p.items.id(item);var cfg=id==null?null:p.getConfig().getConfigurationSection("items."+id);
            if(cfg!=null&&cfg.contains("set")&&slots[index].equals(cfg.getString("slot"))&&used.add(id))result.merge(cfg.getString("set"),1,Integer::sum);index++;
        }
        for(ItemStack item:new ItemStack[]{player.getInventory().getItemInMainHand(),player.getInventory().getItemInOffHand()}){String id=p.items.id(item);var cfg=id==null?null:p.getConfig().getConfigurationSection("items."+id);if(cfg!=null&&cfg.getBoolean("gear")&&!cfg.contains("slot")&&cfg.contains("set")&&used.add(id))result.merge(cfg.getString("set"),1,Integer::sum);}
        return result;
    }
    void tick(){
        for(Player player:Bukkit.getOnlinePlayers()){
            for(var set:sets(player).entrySet()){
                var bonuses=p.getConfig().getConfigurationSection("equipment-sets."+set.getKey()+".bonuses");if(bonuses==null)continue;
                for(String required:bonuses.getKeys(false))if(set.getValue()>=Integer.parseInt(required))for(var effect:bonuses.getMapList(required)){
                    PotionEffectType type=PotionEffectType.getByName(Objects.toString(effect.get("type")));if(type!=null)player.addPotionEffect(new PotionEffect(type,60,((Number)effect.get("amplifier")).intValue(),true,false,true));
                }
            }
        }
    }
    boolean custom(ItemStack... items){return Arrays.stream(items).anyMatch(item->p.items.id(item)!=null);}
    @EventHandler public void craft(PrepareItemCraftEvent e){if(custom(e.getInventory().getMatrix()))e.getInventory().setResult(null);}
    @EventHandler(ignoreCancelled=true) public void crafted(CraftItemEvent e){if(custom(e.getInventory().getMatrix()))e.setCancelled(true);}
    boolean utility(ItemStack item){String id=p.items.id(item);return id!=null&&p.getConfig().getBoolean("items."+id+".utility");}
    boolean utilityInputs(ItemStack first,ItemStack second){return utility(first)&&(!custom(second)||Objects.equals(p.items.id(first),p.items.id(second)));}
    @EventHandler public void anvil(PrepareAnvilEvent e){
        ItemStack first=e.getInventory().getItem(0),second=e.getInventory().getItem(1);
        if(utilityInputs(first,second)){
            ItemStack result=e.getResult();if(result==null)return;var meta=result.getItemMeta();meta.getPersistentDataContainer().set(p.itemKey,org.bukkit.persistence.PersistentDataType.STRING,p.items.id(first));
            String name=e.getInventory().getRenameText(),old=ChatColor.stripColor(first.getItemMeta().getDisplayName());if(name!=null&&!name.equals(old)){var key=new NamespacedKey(p,"utility_name");if(name.isBlank())meta.getPersistentDataContainer().remove(key);else meta.getPersistentDataContainer().set(key,org.bukkit.persistence.PersistentDataType.STRING,name);}
            result.setItemMeta(meta);e.setResult(result);return;
        }if(custom(first,second))e.setResult(null);
    }
    ItemStack ordinary(ItemStack result){if(result==null)return null;var meta=result.getItemMeta();meta.getPersistentDataContainer().remove(p.itemKey);meta.getPersistentDataContainer().remove(new NamespacedKey(p,"utility_name"));meta.setItemModel(null);meta.setUseCooldown(null);meta.setCustomModelDataComponent(null);meta.setDisplayName(null);meta.setLore(null);meta.setRarity(null);result.setItemMeta(meta);return result;}
    @EventHandler public void grind(PrepareGrindstoneEvent e){if(utilityInputs(e.getInventory().getItem(0),e.getInventory().getItem(1))||utilityInputs(e.getInventory().getItem(1),e.getInventory().getItem(0)))e.setResult(ordinary(e.getResult()));else if(custom(e.getInventory().getItem(0),e.getInventory().getItem(1)))e.setResult(null);}
    boolean utilitySmith(org.bukkit.inventory.SmithingInventory inv){return utility(inv.getItem(1))&&!custom(inv.getItem(0),inv.getItem(2));}
    @EventHandler public void smith(PrepareSmithingEvent e){if(utilitySmith(e.getInventory()))e.setResult(ordinary(e.getResult()));else if(custom(e.getInventory().getItem(0),e.getInventory().getItem(1),e.getInventory().getItem(2)))e.setResult(null);}
    @EventHandler(ignoreCancelled=true) public void smithClick(SmithItemEvent e){if(!utilitySmith(e.getInventory())&&custom(e.getInventory().getItem(0),e.getInventory().getItem(1),e.getInventory().getItem(2)))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void enchant(PrepareItemEnchantEvent e){if(custom(e.getItem()))e.setCancelled(true);}
    List<Integer> repairSlots(Player player){
        var inv=player.getInventory();Set<Integer> candidates=new LinkedHashSet<>();for(int i=36;i<=39;i++)candidates.add(i);for(int i=0;i<9;i++)candidates.add(i);candidates.add(40);
        return candidates.stream().filter(slot->{ItemStack item=inv.getItem(slot);String id=p.items.id(item);return id!=null&&p.getConfig().getBoolean("items."+id+".gear")&&item.getItemMeta() instanceof Damageable d&&d.getDamage()>0;}).toList();
    }
    int repair(Player player){int repaired=0;for(int slot:repairSlots(player)){ItemStack item=player.getInventory().getItem(slot);var meta=(Damageable)item.getItemMeta();meta.setDamage(0);item.setItemMeta(meta);player.getInventory().setItem(slot,item);repaired++;}return repaired;}
}
