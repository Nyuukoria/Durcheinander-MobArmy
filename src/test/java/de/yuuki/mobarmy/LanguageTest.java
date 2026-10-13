package de.yuuki.mobarmy;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class LanguageTest {
    List<Language.Phrase> phrases(){var yaml=YamlConfiguration.loadConfiguration(new InputStreamReader(getClass().getResourceAsStream("/languages.yml"),StandardCharsets.UTF_8));return yaml.getMapList("phrases").stream().map(row->new Language.Phrase(row.get("de").toString(),row.get("en").toString())).toList();}
    @Test void translationPreservesCommandsColorsIdsAndUsesWholePhrases(){
        assertEquals("&7Set bonuses work in all worlds.",Language.translate("en","&7Setboni wirken in allen Welten.",phrases()));
        assertEquals("§dRecipes · /raid ritual gale_forge",Language.translate("en","§dRezepte · /raid ritual gale_forge",phrases()));
        assertEquals("&f/rezepte sets shows details.",Language.translate("en","&f/raid codex sets zeigt Details.",phrases()));
        assertEquals("&f/raid language de",Language.translate("en","&f/raid language de",phrases()));
        assertEquals("Nur der Leader kann starten.",Language.translate("de","Only the leader can start.",phrases()));
    }
    @Test void replacementsAreNotTranslatedAgainAndEnglishWordsDoNotChangeInsideIds(){
        var pairs=List.of(new Language.Phrase("a","b"),new Language.Phrase("b","c"));assertEquals("b",Language.translate("en","a",pairs));
        assertEquals("warden_blade",Language.translate("en","warden_blade",phrases()));
    }
    @Test void everyPlayerItemHasFullyEnglishLoreAndAName(){
        var cfg=YamlConfiguration.loadConfiguration(new InputStreamReader(getClass().getResourceAsStream("/config.yml"),StandardCharsets.UTF_8));
        for(String id:cfg.getConfigurationSection("items").getKeys(false)){if(cfg.getBoolean("items."+id+".internal"))continue;assertNotNull(cfg.getString("items."+id+".name-en"));assertFalse(cfg.getStringList("items."+id+".lore-en").isEmpty());
            for(String line:cfg.getStringList("items."+id+".lore-en"))assertFalse(line.matches(".*(?:Wiederverwendbar|Rechtsklick|Abklingzeit|Rüstung|Zählt|Verbraucht|Boni in|Kesselritual|Sekunden).*"),id+": "+line);
        }
    }
}
