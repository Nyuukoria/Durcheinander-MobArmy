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
        if(version>=6)return;
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
        copy(defaults.getConfigurationSection("boss-types"),p.getConfig().createSection("boss-types"));
        for(String id:defaults.getConfigurationSection("items").getKeys(false)){
            for(String field:List.of("name-en","lore-en","set","internal"))if(defaults.contains("items."+id+"."+field))p.getConfig().set("items."+id+"."+field,defaults.get("items."+id+"."+field));
            if(defaults.getBoolean("items."+id+".gear"))p.getConfig().set("items."+id+".lore",defaults.getStringList("items."+id+".lore"));
        }
        if(!p.getConfig().contains("default-language",true))p.getConfig().set("default-language","de");
        if(p.getConfig().getInt("balance.pearl-cooldown-seconds",10)==10)p.getConfig().set("balance.pearl-cooldown-seconds",3);
        if(!p.getConfig().contains("balance.mob-spawn-spacing",true))p.getConfig().set("balance.mob-spawn-spacing",4);
        p.getConfig().set("items.chaos_fragment.lore",defaults.getStringList("items.chaos_fragment.lore"));
        var weeks=p.getConfig().getConfigurationSection("weeks");
        if(version<3&&weeks!=null)for(String id:weeks.getKeys(false)){
            var c=weeks.getConfigurationSection(id);if(!c.getBoolean("generate-demo"))continue;
            var template=defaults.getConfigurationSection("weeks.w1");
            for(String section:List.of("arena","lobby","door"))copy(template.getConfigurationSection(section),c.createSection(section));
            for(String key:List.of("lobby-spawn","arena-spawn","spawn-points"))c.set(key,template.get(key));
        }
        if(weeks!=null)for(String id:weeks.getKeys(false)){var week=weeks.getConfigurationSection(id);if(!week.getBoolean("generate-demo"))continue;var template=defaults.getConfigurationSection("weeks."+(defaults.contains("weeks."+id)?id:"w1")+".stages");
            for(String stage:template.getKeys(false)){var target=week.getConfigurationSection("stages."+stage);if(target==null)continue;copy(template.getConfigurationSection(stage+".waves"),target.createSection("waves"));target.set("miniboss-pool",template.getStringList(stage+".miniboss-pool"));target.set("boss-pool",template.getStringList(stage+".boss-pool"));}
        }
        p.getConfig().set("configuration-version",6);p.saveConfig();
    }
    static void copy(ConfigurationSection from,ConfigurationSection to){if(from==null)return;for(String key:from.getKeys(false)){if(from.isConfigurationSection(key))copy(from.getConfigurationSection(key),to.createSection(key));else to.set(key,from.get(key));}}
}
