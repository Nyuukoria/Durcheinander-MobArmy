package de.yuuki.mobarmy;

import org.bukkit.configuration.*;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class ConfigUpgrade {
    private ConfigUpgrade() {}
    static void run(MobArmy p){
        int version=p.getConfig().getInt("configuration-version",1);
        if(version>=5)return;
        var defaults=YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(p.getResource("config.yml")),StandardCharsets.UTF_8));
        if(version<2)for(String section:List.of("presentation","ritual-animation"))copy(defaults.getConfigurationSection(section),p.getConfig().createSection(section));
        for(String key:List.of("balance.wind-charge-multiplier","balance.wind-charge-max-horizontal","balance.wind-charge-max-upward","messages.private-join","testing.disable-wipe-cooldown","stage-loot-timeout-seconds"))if(!p.getConfig().contains(key,true))p.getConfig().set(key,defaults.get(key));
        for(String id:defaults.getConfigurationSection("items").getKeys(false)){
            String path="items."+id;
            if(!p.getConfig().contains(path,true)){copy(defaults.getConfigurationSection(path),p.getConfig().createSection(path));continue;}
            if(version>=3)continue;
            for(String key:List.of("name","lore","item-model","shop-token"))if(defaults.contains(path+"."+key))p.getConfig().set(path+"."+key,defaults.get(path+"."+key));
            if(defaults.isConfigurationSection(path+".use"))copy(defaults.getConfigurationSection(path+".use"),p.getConfig().createSection(path+".use"));
            if(defaults.isConfigurationSection(path+".ritual"))copy(defaults.getConfigurationSection(path+".ritual"),p.getConfig().createSection(path+".ritual"));
        }
        for(String key:List.of("testing.local-resource-pack","resource-pack.port","resource-pack.url","resource-pack.sha1"))if(!p.getConfig().contains(key,true))p.getConfig().set(key,defaults.get(key));
        for(String section:List.of("equipment-sets","loot-pools","rituals"))for(String id:defaults.getConfigurationSection(section).getKeys(false))if(!p.getConfig().contains(section+"."+id,true))copy(defaults.getConfigurationSection(section+"."+id),p.getConfig().createSection(section+"."+id));
        p.getConfig().set("items.chaos_fragment.lore",defaults.getStringList("items.chaos_fragment.lore"));
        var weeks=p.getConfig().getConfigurationSection("weeks");
        if(version<3&&weeks!=null)for(String id:weeks.getKeys(false)){
            var c=weeks.getConfigurationSection(id);if(!c.getBoolean("generate-demo"))continue;
            var template=defaults.getConfigurationSection("weeks.w1");
            for(String section:List.of("arena","lobby","door"))copy(template.getConfigurationSection(section),c.createSection(section));
            for(String key:List.of("lobby-spawn","arena-spawn","spawn-points"))c.set(key,template.get(key));
        }
        p.getConfig().set("configuration-version",5);p.saveConfig();
    }
    static void copy(ConfigurationSection from,ConfigurationSection to){if(from==null)return;for(String key:from.getKeys(false)){if(from.isConfigurationSection(key))copy(from.getConfigurationSection(key),to.createSection(key));else to.set(key,from.get(key));}}
}
