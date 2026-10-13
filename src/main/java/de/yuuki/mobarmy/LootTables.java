package de.yuuki.mobarmy;

import org.bukkit.configuration.ConfigurationSection;
import java.util.*;
import java.util.function.DoubleSupplier;

/** One probability check per pool; a successful roll selects exactly one weighted item. */
final class LootTables {
    private LootTables(){}
    static boolean matches(ConfigurationSection pool,String source,int stage,String week){
        return source.equals(pool.getString("source"))&&stage>=pool.getInt("minimum-stage",1)&&stage<=pool.getInt("maximum-stage",5)&&(!pool.contains("week")||week.equals(pool.getString("week")));
    }
    static String choose(ConfigurationSection pool,double roll){
        if(pool==null)throw new IllegalArgumentException("Loot pool missing");
        List<Map<?,?>> rows=pool.getMapList("entries");double total=0;
        for(var row:rows){double weight=row.get("weight") instanceof Number n?n.doubleValue():1;if(!Double.isFinite(weight)||weight<=0)throw new IllegalArgumentException("Loot weight must be positive");total+=weight;}
        if(rows.isEmpty()||!Double.isFinite(total))throw new IllegalArgumentException("Loot pool is empty or invalid");
        double position=Math.max(0,Math.min(Math.nextDown(1.0),roll))*total;
        for(var row:rows){double weight=row.get("weight") instanceof Number n?n.doubleValue():1;position-=weight;if(position<0)return Objects.toString(row.get("item"));}
        return Objects.toString(rows.getLast().get("item"));
    }
    static List<String> roll(ConfigurationSection pools,String source,int stage,String week,DoubleSupplier random){
        List<String> result=new ArrayList<>();if(pools==null)return result;
        for(String id:pools.getKeys(false)){var pool=pools.getConfigurationSection(id);if(!matches(pool,source,stage,week))continue;
            double chance=pool.getDouble("chance",0);if(!Double.isFinite(chance)||chance<0||chance>1)throw new IllegalArgumentException("Loot chance must be 0..1");
            if(random.getAsDouble()<chance)result.add(choose(pool,random.getAsDouble()));
        }
        return result;
    }
}
