package de.yuuki.mobarmy;
import org.bukkit.ChatColor;
import java.util.*;
/** Keep each tooltip line short and preserve deliberately empty lines. */
final class TextLayout {
    static List<String> wrap(List<String> source,int width){
        List<String> out=new ArrayList<>();for(String line:source){
            if(line.isBlank()){out.add("");continue;}String current="";
            for(String word:line.split(" ")){String next=current.isEmpty()?word:current+" "+word;
                if(!current.isEmpty()&&ChatColor.stripColor(next).length()>width){out.add(current);current=ChatColor.getLastColors(current)+word;}else current=next;
            }out.add(current);
        }return out;
    }
    static int gridSlot(int index){if(index<0||index>=28)throw new IllegalArgumentException("Grid index must be 0..27");return 10+(index/7)*9+index%7;}
    private TextLayout(){}
}
