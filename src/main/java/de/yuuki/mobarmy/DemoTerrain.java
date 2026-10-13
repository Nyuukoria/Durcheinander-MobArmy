package de.yuuki.mobarmy;

/** Stable gentle hills; a cross-shaped path and all combat spawn pads remain flat. */
final class DemoTerrain {
    private DemoTerrain(){}
    static int height(int x,int z){
        if(Math.abs(x)<=5||Math.abs(z)<=5||near(x,z,0,-36))return 0;
        for(int sx:new int[]{-32,32})for(int sz:new int[]{-32,32})if(near(x,z,sx,sz))return 0;
        double value=0;
        for(int hx:new int[]{-20,20})for(int hz:new int[]{-18,18}){
            double d=Math.hypot(x-hx,z-hz);value=Math.max(value,Math.max(0,4-d/5));
        }
        // Taper into paths and spawn pads instead of abrupt cliff edges.
        value=Math.min(value,(Math.min(Math.abs(x),Math.abs(z))-5)/2.0);
        for(int sx:new int[]{-32,32})for(int sz:new int[]{-32,32})value=Math.min(value,Math.max(0,(Math.max(Math.abs(x-sx),Math.abs(z-sz))-5)/2.0));
        return (int)Math.floor(Math.max(0,value));
    }
    static boolean near(int x,int z,int sx,int sz){return Math.abs(x-sx)<=5&&Math.abs(z-sz)<=5;}
}
