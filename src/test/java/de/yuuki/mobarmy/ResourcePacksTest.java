package de.yuuki.mobarmy;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResourcePacksTest {
    @Test void jarContainsCompleteWorkingBundleIncludingNewItemAssets()throws Exception{
        MobArmy p=mock(MobArmy.class);when(p.getConfig()).thenReturn(new YamlConfiguration());
        when(p.getResource(anyString())).thenAnswer(i->getClass().getResourceAsStream("/"+i.getArgument(0)));
        byte[] bundle=new ResourcePacks(p).bundle();var files=new HashMap<String,byte[]>();
        try(var zip=new ZipInputStream(new ByteArrayInputStream(bundle))){for(ZipEntry entry;(entry=zip.getNextEntry())!=null;)files.put(entry.getName(),zip.readAllBytes());}
        assertTrue(files.containsKey("pack.mcmeta"));assertFalse(files.containsKey("index.txt"));
        var cfg=YamlConfiguration.loadConfiguration(new InputStreamReader(getClass().getResourceAsStream("/config.yml"),java.nio.charset.StandardCharsets.UTF_8));
        for(String id:cfg.getConfigurationSection("items").getKeys(false)){if(cfg.getBoolean("items."+id+".internal"))continue;
            assertTrue(files.containsKey("assets/mobarmy/items/"+id+".json"),id);assertTrue(files.containsKey("assets/mobarmy/models/item/"+id+".json"),id);
            var png=ImageIO.read(new ByteArrayInputStream(files.get("assets/mobarmy/textures/item/"+id+".png")));assertNotNull(png,id);assertEquals(16,png.getWidth());assertEquals(16,png.getHeight());assertTrue(png.getColorModel().hasAlpha());
            var colors=new HashSet<Integer>();boolean transparent=false;
            for(int y=0;y<16;y++)for(int x=0;x<16;x++){int rgba=png.getRGB(x,y),alpha=rgba>>>24;assertTrue(alpha==0||alpha==255,id+" has a soft edge");if(alpha==255)colors.add(rgba);else transparent=true;}
            assertTrue(transparent,id);assertFalse(colors.isEmpty(),id);assertTrue(colors.size()<=6,id+" has too many colors");
        }
        String rod=new String(files.get("assets/mobarmy/items/angler_rod.json"),java.nio.charset.StandardCharsets.UTF_8);
        String crossbow=new String(files.get("assets/mobarmy/items/ranger_crossbow.json"),java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(rod.contains("minecraft:fishing_rod/cast"));assertTrue(files.containsKey("assets/mobarmy/models/item/angler_rod_cast.json"));
        for(String state:List.of("pulling_0","pulling_1","pulling_2","arrow","rocket"))assertTrue(files.containsKey("assets/mobarmy/models/item/ranger_crossbow_"+state+".json"));
        for(String property:List.of("minecraft:using_item","minecraft:crossbow/pull","minecraft:charge_type"))assertTrue(crossbow.contains(property));
        for(int step=0;step<8;step++)assertTrue(files.containsKey("assets/mobarmy/models/item/cooldown/meter_"+step+".json"));
        for(String id:cfg.getConfigurationSection("items").getKeys(false))if(cfg.getBoolean("items."+id+".utility")){
            String definition=new String(files.get("assets/mobarmy/items/"+id+".json"),java.nio.charset.StandardCharsets.UTF_8);
            for(String property:List.of("minecraft:display_context","minecraft:custom_model_data","minecraft:composite"))assertTrue(definition.contains(property),id);
        }
    }
}
