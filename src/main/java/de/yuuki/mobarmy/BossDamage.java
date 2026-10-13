package de.yuuki.mobarmy;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import java.util.*;

/** Credit health that Minecraft actually removed, after the event has been applied. */
final class BossDamage {
    final MobArmy p;
    record Pending(Session session,LivingEntity mob,UUID player,double before,double allowed,double maximum){}
    final Map<UUID,Pending> pending=new HashMap<>();
    BossDamage(MobArmy p){this.p=p;}
    void accepted(Session session,LivingEntity mob,Player player,double allowed,double maximum){
        flush(mob);
        if(allowed<=0||!session.contribution.containsKey(mob.getUniqueId()))return;
        Pending hit=new Pending(session,mob,player.getUniqueId(),mob.getHealth(),allowed,maximum);pending.put(mob.getUniqueId(),hit);
        Bukkit.getScheduler().runTask(p,()->{if(pending.get(mob.getUniqueId())==hit)flush(mob);});
    }
    void flush(LivingEntity mob){
        Pending hit=pending.remove(mob.getUniqueId());if(hit==null)return;
        var ledger=hit.session.contribution.get(mob.getUniqueId());if(ledger==null)return;
        double maximum=hit.maximum;
        double actual=Math.max(0,Math.min(hit.before-mob.getHealth(),hit.allowed+Rules.healthEpsilon(maximum)));
        if(actual>0){ledger.merge(hit.player,actual,Double::sum);if(p.bosses!=null)p.bosses.hurt(mob.getUniqueId());}
        // Minecraft stores entity health as a float. Close only the tiny remainder
        // left by a real, approved final hit, never a full-health or skipped boss.
        double health=mob.getHealth(),total=ledger.values().stream().mapToDouble(Double::doubleValue).sum();
        if(health>0&&Rules.finishedHealth(maximum,health,total))mob.setHealth(0);
    }
    void discard(UUID mob){pending.remove(mob);}
}
