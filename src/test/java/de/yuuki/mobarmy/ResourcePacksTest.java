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
        for(String id:List.of("chaos_fragment","ritual_core","chaos_crown","raid_shard","frost_talisman","healing_bloom","gale_sigil")){
            assertTrue(files.containsKey("assets/mobarmy/items/"+id+".json"),id);assertTrue(files.containsKey("assets/mobarmy/models/item/"+id+".json"),id);
            var png=ImageIO.read(new ByteArrayInputStream(files.get("assets/mobarmy/textures/item/"+id+".png")));assertNotNull(png,id);assertEquals(png.getWidth(),png.getHeight());assertTrue(png.getColorModel().hasAlpha());
        }
    }
}
