package de.yuuki.mobarmy;

import org.bukkit.configuration.*;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class ConfigUpgrade {
    private ConfigUpgrade() {}
    static void run(MobArmy p){
        if(p.getConfig().getInt("configuration-version",1)>=2)return;
        var defaults=YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(p.getResource("config.yml")),StandardCharsets.UTF_8));
        for(String section:List.of("presentation","ritual-animation"))copy(defaults.getConfigurationSection(section),p.getConfig().createSection(section));
        for(String key:List.of("balance.wind-charge-multiplier","balance.wind-charge-max-horizontal","balance.wind-charge-max-upward","messages.private-join","testing.disable-wipe-cooldown","stage-loot-timeout-seconds"))if(!p.getConfig().contains(key,true))p.getConfig().set(key,defaults.get(key));
        for(String id:List.of("chaos_fragment","ritual_core","chaos_crown","raid_shard")){
            String path="items."+id;
            if(!p.getConfig().isConfigurationSection(path))continue;
            for(String key:List.of("name","lore","item-model","shop-token"))if(defaults.contains(path+"."+key))p.getConfig().set(path+"."+key,defaults.get(path+"."+key));
            if(defaults.isConfigurationSection(path+".use"))copy(defaults.getConfigurationSection(path+".use"),p.getConfig().createSection(path+".use"));
        }
        var weeks=p.getConfig().getConfigurationSection("weeks");
        if(weeks!=null)for(String id:weeks.getKeys(false)){
            var c=weeks.getConfigurationSection(id);if(!c.getBoolean("generate-demo"))continue;
            if(c.getIntegerList("lobby.min").equals(List.of(-23,64,-30))){
                c.set("lobby.min",List.of(80,64,-8));c.set("lobby.max",List.of(96,70,8));c.set("lobby-spawn",List.of(88.5,64.0,0.5,90.0));
                var max=new ArrayList<>(c.getIntegerList("arena.max"));if(max.size()==3){max.set(1,75);c.set("arena.max",max);}
            }
        }
        p.getConfig().set("configuration-version",2);p.saveConfig();
    }
    static void copy(ConfigurationSection from,ConfigurationSection to){if(from==null)return;for(String key:from.getKeys(false)){if(from.isConfigurationSection(key))copy(from.getConfigurationSection(key),to.createSection(key));else to.set(key,from.get(key));}}
}
