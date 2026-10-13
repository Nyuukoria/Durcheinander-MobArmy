package de.yuuki.mobarmy;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class RecipeLayoutTest {
    YamlConfiguration config(String file){return YamlConfiguration.loadConfiguration(new InputStreamReader(getClass().getResourceAsStream("/"+file),StandardCharsets.UTF_8));}
    @Test void tooltipsAreShortHaveSpacingAndEveryUtilityHasItsOwnModel(){
        var c=config("config.yml");int utility=0;
        for(String id:c.getConfigurationSection("items").getKeys(false)){if(c.getBoolean("items."+id+".internal"))continue;
            for(String field:List.of("lore","lore-en")){List<String> lines=TextLayout.wrap(c.getStringList("items."+id+"."+field).stream().map(MobArmy::color).toList(),36);assertTrue(lines.contains(""),id);assertTrue(lines.size()<=10,id+": "+lines.size());for(String line:lines)assertTrue(ChatColor.stripColor(line).length()<=36,id+": "+line);}
            if(c.getBoolean("items."+id+".utility")){utility++;assertEquals("mobarmy:"+id,c.getString("items."+id+".item-model"));assertFalse(c.contains("items."+id+".use"));}
        }assertEquals(8,utility);
    }
    @Test void recipeRoutesIncludeVanillaToolsAndTheGuardianCoreWithoutRepeatingIngredients(){
        MobArmy p=mock(MobArmy.class);var cfg=config("config.yml");when(p.getConfig()).thenReturn(cfg);p.ritualSystem=new Rituals(p);Catalogue catalogue=new Catalogue(p);
        assertEquals(List.of("wanderer_forge"),catalogue.recipesFor("angler_rod"));assertEquals(List.of("awakening"),catalogue.recipesFor("ritual_core"));assertTrue(catalogue.recipesFor("abyss_sword").contains("abyss_trial"));
        Set<Integer> slots=new HashSet<>();for(int i=0;i<28;i++){int slot=TextLayout.gridSlot(i);assertTrue(slot>=10&&slot<=43);assertTrue(slot%9>=1&&slot%9<=7);assertTrue(slots.add(slot));}
    }
    @Test void bothLanguagesHaveCompleteTemplatesWithTheSamePlaceholdersAndNoMissingStaticKeys()throws Exception{
        var c=config("ui-language.yml");var de=c.getConfigurationSection("de");var en=c.getConfigurationSection("en");assertEquals(de.getKeys(true),en.getKeys(true));Pattern placeholders=Pattern.compile("\\{[^}]+}");
        for(String key:de.getKeys(true))if(de.isString(key)){Set<String> a=new TreeSet<>(),b=new TreeSet<>();placeholders.matcher(de.getString(key)).results().forEach(m->a.add(m.group()));placeholders.matcher(en.getString(key)).results().forEach(m->b.add(m.group()));assertEquals(a,b,key);}
        Pattern keys=Pattern.compile("(?:p\\.tr|p\\.say|trCode)\\([^\\n]*?,\\s*\"([a-z]+\\.[a-zA-Z.-]+)\"");
        try(var files=Files.list(Path.of("src/main/java/de/yuuki/mobarmy"))){for(Path file:files.toList()){var matcher=keys.matcher(Files.readString(file));while(matcher.find()){String key=matcher.group(1);if(key.endsWith("."))continue;assertTrue(de.contains(key),file+": "+key);}}}
        assertEquals("Ender pearl ready in 00:03.",Language.format(en.getString("pearl.cooldown"),"time","00:03"));assertTrue(en.getString("raid.welcome").contains("/raid"));
    }
}
