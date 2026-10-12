package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.boss.BossBar;
import java.util.*;

final class Session {
    enum Phase { LOBBY, ENTERING, FIGHT, LOOT }
    final Portal portal;
    final Arena arena;
    final Set<UUID> members=new LinkedHashSet<>(), participants=new LinkedHashSet<>(), dead=new HashSet<>(), mobs=new HashSet<>(), drops=new HashSet<>();
    final Map<UUID,UUID> votes=new HashMap<>();
    final Map<UUID,Map<UUID,Double>> contribution=new HashMap<>();
    final Map<Location,UUID> blocks=new HashMap<>();
    final Map<Location,org.bukkit.block.data.BlockData> blockOriginal=new HashMap<>();
    final Map<UUID,Map<String,Integer>> uses=new HashMap<>();
    final Map<UUID,Long> pearlTimes=new HashMap<>(), appleTimes=new HashMap<>();
    UUID leader;
    int stage=1,wave=0,misses=0,stagePlayers=0;
    long phaseSince=System.currentTimeMillis();
    boolean miniboss;
    BossBar bar, hud;
    int waveStartMobs;
    Session(Portal portal,Arena arena,UUID leader){this.portal=portal;this.arena=arena;this.leader=leader;}
    boolean active(){return phase==Phase.ENTERING||phase==Phase.FIGHT;}
    Phase phase=Phase.LOBBY;
    void phase(Phase phase){this.phase=phase;phaseSince=System.currentTimeMillis();}
}
