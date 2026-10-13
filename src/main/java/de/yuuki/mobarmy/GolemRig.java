package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.*;

/** Separate display bones follow the carrier's feet; no passenger/fixed-item offsets. */
final class GolemRig {
    static final List<String> BONES=List.of("body","head","left_arm","right_arm","left_leg","right_leg");
    final MobArmy p;final LivingEntity carrier;final Map<String,ItemDisplay> bones=new LinkedHashMap<>();
    Location previous;double phase,moving,look;int ticks,attackSince=-100,hurtSince=-100;final boolean initiallyInvisible;
    GolemRig(MobArmy p,LivingEntity carrier,String variant,String tag){
        this.p=p;this.carrier=carrier;previous=carrier.getLocation().clone();initiallyInvisible=carrier.isInvisible();
        try{
            for(String name:BONES){
                ItemStack item=new ItemStack(Material.PAPER);var meta=item.getItemMeta();meta.setItemModel(NamespacedKey.fromString("mobarmy:golem/"+variant+"/"+name));item.setItemMeta(meta);
                ItemDisplay display=carrier.getWorld().spawn(previous,ItemDisplay.class,e->{
                    e.setItemStack(item);e.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);e.setBillboard(Display.Billboard.FIXED);e.setPersistent(false);e.setGravity(false);e.setInvulnerable(true);
                    e.setInterpolationDuration(2);e.setTeleportDuration(2);e.setViewRange(1.5f);e.setDisplayWidth(4);e.setDisplayHeight(4);
                    e.getPersistentDataContainer().set(p.entityKey,PersistentDataType.STRING,"golem-rig:"+carrier.getUniqueId());
                });bones.put(name,display);
            }
            carrier.setInvisible(true);animate();
        }catch(RuntimeException e){remove();throw e;}
    }
    void attack(){attackSince=ticks;}
    void hurt(){hurtSince=ticks;}
    void animate(){
        Location at=carrier.getLocation().clone();double distance=at.getWorld().equals(previous.getWorld())?Math.hypot(at.getX()-previous.getX(),at.getZ()-previous.getZ()):0;
        double pace=Math.min(1,distance/.18);moving+=(pace-moving)*.35;phase+=Math.min(.6,distance*5.5);ticks+=2;
        double attack=ticks-attackSince<16?Math.sin(Math.PI*(ticks-attackSince)/16.0):0,hurt=ticks-hurtSince<10?Math.sin(Math.PI*(ticks-hurtSince)/10.0):0;
        double facing=0;if(carrier instanceof Mob mob&&mob.getTarget()!=null){Location target=mob.getTarget().getLocation();double desired=Math.atan2(-(target.getX()-at.getX()),target.getZ()-at.getZ());facing=Math.max(-.55,Math.min(.55,GolemPose.angle(Math.toRadians(at.getYaw()),desired)));}look+=(facing-look)*.3;
        double scale=GolemPose.scale(carrier.getHeight());Location root=at.clone();root.setYaw(at.getYaw()+180);root.setPitch(0);
        for(var entry:bones.entrySet()){
            var b=GolemPose.bone(entry.getKey(),phase,moving,ticks,attack,hurt,look);ItemDisplay display=entry.getValue();
            display.teleport(root);display.setInterpolationDelay(0);display.setTransformation(new Transformation(new Vector3f((float)(b.x()*scale),(float)(b.y()*scale),(float)(b.z()*scale)),new Quaternionf().rotationXYZ((float)b.pitch(),(float)-b.yaw(),(float)b.roll()),new Vector3f((float)scale),new Quaternionf()));
            display.setGlowing(carrier.isGlowing());
        }
        previous=at;
    }
    boolean valid(){return carrier.isValid()&&!carrier.isDead()&&bones.values().stream().allMatch(Entity::isValid);}
    void remove(){bones.values().forEach(Entity::remove);bones.clear();if(carrier.isValid()&&carrier.getHealth()>0)carrier.setInvisible(initiallyInvisible);}
}
