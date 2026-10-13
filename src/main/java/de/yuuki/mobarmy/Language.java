package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

final class Language {
    record Phrase(String de,String en){}
    final MobArmy p;final List<Phrase> phrases;
    Language(MobArmy p){this.p=p;var cfg=YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(p.getResource("languages.yml")),StandardCharsets.UTF_8));phrases=cfg.getMapList("phrases").stream().map(row->new Phrase(row.get("de").toString(),row.get("en").toString())).toList();}
    String code(UUID player){return p.state.getString("languages."+player,p.getConfig().getString("default-language","de"));}
    String code(CommandSender sender){return sender instanceof Player player?code(player.getUniqueId()):p.getConfig().getString("default-language","de");}
    void set(Player player,String code){if(!Set.of("de","en").contains(code))throw new IllegalArgumentException("/raid language de|en");p.state.set("languages."+player.getUniqueId(),code);p.saveState();p.items.refresh(player);for(Portal portal:p.portals.values())portal.refreshVisibility(player);p.tell(player,code.equals("en")?"Language set to English.":"Sprache auf Deutsch gesetzt.");}
    String text(CommandSender player,String text){return translate(code(player),text,phrases);}
    String text(UUID player,String text){return translate(code(player),text,phrases);}
    static String translate(String code,String source,List<Phrase> phrases){
        if(source==null)return "";String out=source.replace("/raid codex","/rezepte");
        Map<String,String> commands=new LinkedHashMap<>();Matcher matcher=Pattern.compile("https?://\\S+|/(?:raid|rezepte|recipes|codex)(?: (?:item|give|take|count|kit|gear|ritual|rituals|cancel|all|demo|rebuild|reset|skip|wave|stage|boss|summon|clear|language|lang|sprache|de|en|portal|remove|delete|info|create|pos1|pos2|timer|set|add|open|close|stop|ready|start|leave|loot|pack|reload|status|test|on|off|basic|visit|build|help|items|sets)(?![\\w-]))*").matcher(out);
        StringBuffer protectedText=new StringBuffer();int index=0;while(matcher.find()){String token="\u0001"+index+++"\u0002";commands.put(token,matcher.group());matcher.appendReplacement(protectedText,Matcher.quoteReplacement(token));}matcher.appendTail(protectedText);out=protectedText.toString();
        Matcher colors=Pattern.compile("(?i)[&§][0-9a-fk-or]").matcher(out);StringBuffer colored=new StringBuffer();while(colors.find()){String token="\u0003"+index+++"\u0004";commands.put(token,colors.group());colors.appendReplacement(colored,Matcher.quoteReplacement(token));}colors.appendTail(colored);out=colored.toString();
        List<Phrase> ordered=new ArrayList<>(phrases);ordered.sort(Comparator.comparingInt((Phrase phrase)->(code.equals("en")?phrase.de:phrase.en).length()).reversed());
        Map<String,String> replacements=new LinkedHashMap<>();for(Phrase phrase:ordered){String from=code.equals("en")?phrase.de:phrase.en,to=code.equals("en")?phrase.en:phrase.de;if(!from.equals(to)&&!from.isBlank())replacements.putIfAbsent(from,to);}
        if(!replacements.isEmpty()){
            Pattern pattern=Pattern.compile("(?<![\\p{L}\\p{N}_])(?:"+String.join("|",replacements.keySet().stream().map(Pattern::quote).toList())+")(?![\\p{L}\\p{N}_])");
            out=pattern.matcher(out).replaceAll(match->Matcher.quoteReplacement(replacements.get(match.group())));
        }
        for(var command:commands.entrySet())out=out.replace(command.getKey(),command.getValue());return out;
    }
    ItemStack localized(UUID owner,ItemStack item){
        if(item==null)return null;ItemStack result=item.clone();var meta=result.getItemMeta();if(meta==null)return result;String id=p.items==null?null:p.items.id(item);var cfg=id==null?null:p.getConfig().getConfigurationSection("items."+id);boolean english=code(owner).equals("en");
        if(cfg!=null){meta.setDisplayName(MobArmy.color(cfg.getString(english?"name-en":"name",cfg.getString("name",id))));List<String> lore=cfg.getStringList(english&&cfg.contains("lore-en")?"lore-en":"lore");meta.setLore(lore.stream().map(line->MobArmy.color(text(owner,line))).toList());}
        else{if(meta.hasDisplayName())meta.setDisplayName(MobArmy.color(text(owner,meta.getDisplayName())));if(meta.hasLore())meta.setLore(meta.getLore().stream().map(line->MobArmy.color(text(owner,line))).toList());}
        result.setItemMeta(meta);return result;
    }
    void open(Player player,Inventory inventory){
        for(int slot=0;slot<inventory.getSize();slot++){ItemStack item=inventory.getItem(slot);if(item==null)continue;
            // Recipe previews have additional lines that must survive localization.
            var meta=item.getItemMeta();if(meta.hasDisplayName())meta.setDisplayName(MobArmy.color(text(player,meta.getDisplayName())));if(meta.hasLore())meta.setLore(meta.getLore().stream().map(line->MobArmy.color(text(player,line))).toList());item.setItemMeta(meta);inventory.setItem(slot,item);
        }
        player.openInventory(inventory);
    }
}
