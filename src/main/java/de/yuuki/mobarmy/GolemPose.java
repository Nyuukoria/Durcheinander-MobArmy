package de.yuuki.mobarmy;

/** Bone pivots in blocks; native iron golem height is 2.7 blocks. */
final class GolemPose {
    static final double HEIGHT=2.72125;
    record Bone(double x,double y,double z,double pitch,double yaw,double roll){}
    static double scale(double carrierHeight){return Math.max(.25,carrierHeight/HEIGHT);}
    static Bone bone(String name,double phase,double moving,double idle,double attack,double hurt,double look){
        double step=Math.sin(phase)*.48*moving,sway=Math.sin(idle*.055)*.025,bob=Math.abs(Math.sin(phase))*.025*moving;
        return switch(name){
            case "body"->new Bone(0,1.65+bob,0,hurt*.12,0,sway*.25);
            case "head"->new Bone(0,2.44+bob,0,hurt*.1,look,sway*.25);
            case "left_arm"->new Bone(.65,2.08+bob,0,-step*.75+attack*1.7+sway,0,-.025);
            case "right_arm"->new Bone(-.65,2.08+bob,0,step*.75+attack*1.7-sway,0,.025);
            case "left_leg"->new Bone(.24,1.125,0,step,0,0);
            case "right_leg"->new Bone(-.24,1.125,0,-step,0,0);
            default->throw new IllegalArgumentException("Unknown golem bone: "+name);
        };
    }
    static double angle(double from,double to){return Math.atan2(Math.sin(to-from),Math.cos(to-from));}
    private GolemPose(){}
}
