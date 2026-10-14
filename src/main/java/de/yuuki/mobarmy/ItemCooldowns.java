package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

/** Player-bound ability timers. Utility meters do not disable ordinary right-click mechanics. */
final class ItemCooldowns {
    final MobArmy p;
    ItemCooldowns(MobArmy p){this.p=p;}
    static ConfigurationSection ability(ConfigurationSection cfg){return cfg==null?null:cfg.getConfigurationSection(cfg.isConfigurationSection("skill")?"skill":"use");}
    static void decorate(String id,ConfigurationSection cfg,ItemMeta meta){
        ConfigurationSection use=ability(cfg);if(use==null)return;
        if((cfg.getBoolean("utility")||cfg.isConfigurationSection("skill"))){meta.setUseCooldown(null);return;}
        var component=meta.getUseCooldown();component.setCooldownGroup(new NamespacedKey("mobarmy","ability/"+id));component.setCooldownSeconds(Math.max(.05f,use.getLong("cooldown-seconds",60)));meta.setUseCooldown(component);
    }
    long remaining(UUID owner,String id,long now){return Math.max(0,p.state.getLong("item-cooldowns."+owner+"."+id)-now);}
    static int ticks(long millis){return (int)Math.min(Integer.MAX_VALUE,Math.max(0,(millis+49)/50));}
    static float progress(long left,long total){return (float)Math.max(0,Math.min(1,1-(double)left/Math.max(1,total)));}
    void start(Player player,ItemStack item,String id,long seconds){p.state.set("item-cooldowns."+player.getUniqueId()+"."+id,System.currentTimeMillis()+Math.max(1,seconds)*1000);p.saveState();sync(player);}
    void sync(Player player){
        long now=System.currentTimeMillis();Set<String> seen=new HashSet<>();
        for(int slot=0;slot<player.getInventory().getSize();slot++){
            ItemStack item=player.getInventory().getItem(slot);String id=p.items.id(item);var cfg=id==null?null:p.getConfig().getConfigurationSection("items."+id);var use=ability(cfg);if(use==null)continue;
            // Leave held components untouched during a charge or sustained block break.
            boolean busy=player.isHandRaised()||p.utilitySkills!=null&&p.utilitySkills.mining.contains(player.getUniqueId());
            if((cfg.getBoolean("utility")||cfg.isConfigurationSection("skill"))&&busy&&(slot==player.getInventory().getHeldItemSlot()||slot==40))continue;
            long left=remaining(player.getUniqueId(),id,now);var meta=item.getItemMeta();
            // Rebuild just the short localized tooltip, avoiding repeated countdown lines.
            String field=p.language!=null&&p.language.code(player).equals("en")?"lore-en":"lore";
            List<String> lore=new ArrayList<>(TextLayout.wrap(cfg.getStringList(field).stream().map(MobArmy::color).toList(),36));
            if(left>0){lore.add(MobArmy.color(p.tr(player,"skill.remaining","time",Rules.clock((left+999)/1000))));}
            meta.setLore(lore);
            if((cfg.getBoolean("utility")||cfg.isConfigurationSection("skill"))){
                var model=meta.getCustomModelDataComponent();model.setFlags(List.of(left>0));model.setFloats(List.of(progress(left,use.getLong("cooldown-seconds",60)*1000)));meta.setCustomModelDataComponent(model);
            }
            if(!meta.equals(item.getItemMeta())){item.setItemMeta(meta);player.getInventory().setItem(slot,item);}
            if(!(cfg.getBoolean("utility")||cfg.isConfigurationSection("skill"))&&seen.add(id)){
                int expected=ticks(left),actual=player.getCooldown(item);
                if(expected==0&&actual>0||expected>0&&(actual==0||Math.abs(expected-actual)>25))player.setCooldown(item,expected);
            }
        }
    }
    void tick(){for(Player player:Bukkit.getOnlinePlayers())sync(player);}
    void clear(UUID owner){for(Player player:Bukkit.getOnlinePlayers())if(owner==null||player.getUniqueId().equals(owner))sync(player);}
}
