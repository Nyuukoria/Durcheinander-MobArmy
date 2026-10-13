package de.yuuki.mobarmy;

import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import java.util.*;
import java.util.function.Function;

/** Plans exact deductions from storage slots, keeping catalysts/armor out of recipe inputs. */
final class RitualPayments {
    private RitualPayments(){}
    record Requirement(String customId,String material,int amount){
        Requirement{if(amount<1||(customId==null)==(material==null))throw new IllegalArgumentException("Invalid ritual ingredient");}
    }
    static Map<Integer,Integer> plan(ItemStack[] inventory,List<Requirement> requirements,Function<ItemStack,String> identify){
        Map<Integer,Integer> deductions=new LinkedHashMap<>();
        for(Requirement requirement:requirements){int left=requirement.amount;
            for(int slot=0;slot<inventory.length&&left>0;slot++){
                ItemStack item=inventory[slot];if(item==null||item.getAmount()<=0||item.getType()==Material.AIR||item.getType()==Material.CAVE_AIR||item.getType()==Material.VOID_AIR)continue;
                String id=identify.apply(item);boolean match=requirement.customId!=null?requirement.customId.equals(id):id==null&&item.getType().name().equals(requirement.material);
                if(!match)continue;int available=item.getAmount()-deductions.getOrDefault(slot,0),used=Math.min(left,available);if(used>0){deductions.merge(slot,used,Integer::sum);left-=used;}
            }
            if(left>0)return null;
        }
        return deductions;
    }
}
