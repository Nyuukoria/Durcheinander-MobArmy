package de.yuuki.mobarmy;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class GolemModelTest {
    @Test void everyAnimatedPartUsesObsidianAndLegalNativeModelCoordinates(){
        for(String variant:List.of("abyss","frost","ember"))for(String bone:GolemRig.BONES){String path="/mobarmy-pack/assets/mobarmy/models/item/golem/"+variant+"/"+bone+".json";
            var stream=getClass().getResourceAsStream(path);assertNotNull(stream,path);var cfg=YamlConfiguration.loadConfiguration(new InputStreamReader(stream,StandardCharsets.UTF_8));assertEquals("minecraft:block/obsidian",cfg.getString("textures.stone"));
            for(var element:cfg.getMapList("elements"))for(String side:List.of("from","to"))for(Object value:(List<?>)element.get(side)){double v=((Number)value).doubleValue();assertTrue(v>=-16&&v<=32,path+": "+v);}
        }
    }
    @Test void feetStayAtGroundAndHeadMatchesTheCarriersHeightInsteadOfPassengerOffsets(){
        for(double height:new double[]{1.95,2.7,3.5}){double scale=GolemPose.scale(height);var leg=GolemPose.bone("left_leg",0,0,0,0,0,0);var head=GolemPose.bone("head",0,0,0,0,0,0);assertEquals(0,(leg.y()-18.0/16)*scale,1e-8);assertEquals(height,(head.y()+4.5/16)*scale,1e-8);}
    }
    @Test void walkingHasOppositeLegMotionAndAttackingRaisesBothArms(){
        GolemPose.Bone left=GolemPose.bone("left_leg",Math.PI/2,1,0,0,0,0),right=GolemPose.bone("right_leg",Math.PI/2,1,0,0,0,0);assertTrue(left.pitch()>.4);assertEquals(-left.pitch(),right.pitch());
        assertTrue(GolemPose.bone("left_arm",0,0,0,1,0,0).pitch()>1.5);assertTrue(GolemPose.bone("right_arm",0,0,0,1,0,0).pitch()>1.5);
        assertEquals(.5,GolemPose.bone("head",0,0,0,0,0,.5).yaw());
    }
}
