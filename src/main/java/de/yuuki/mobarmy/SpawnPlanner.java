package de.yuuki.mobarmy;

import java.util.*;

final class SpawnPlanner {
    record Point(double x,double y,double z){double distanceSquared(Point other){double dx=x-other.x,dy=y-other.y,dz=z-other.z;return dx*dx+dy*dy+dz*dz;}}
    private SpawnPlanner(){}
    static int choose(List<Point> candidates,List<Point> players,List<Point> mobs,double playerGap,double mobGap,Random random){
        List<Integer> clear=new ArrayList<>();int best=-1;double bestGap=-1;
        for(int i=0;i<candidates.size();i++){
            Point point=candidates.get(i);if(players.stream().anyMatch(p->point.distanceSquared(p)<playerGap*playerGap))continue;
            double gap=mobs.stream().mapToDouble(point::distanceSquared).min().orElse(Double.POSITIVE_INFINITY);
            if(gap>=mobGap*mobGap)clear.add(i);if(gap>bestGap){bestGap=gap;best=i;}
        }
        return clear.isEmpty()?best:clear.get(random.nextInt(clear.size()));
    }
}
