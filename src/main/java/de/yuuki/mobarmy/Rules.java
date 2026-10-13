package de.yuuki.mobarmy;

import java.time.*;
import java.util.Locale;

/** Pure rules shared by the event engine and regression tests. */
public final class Rules {
    private Rules() {}
    public static boolean rewards(boolean bossKilled, long deadline, LocalDate day, long now, ZoneId zone) {
        return !bossKilled && now < deadline && day.equals(Instant.ofEpochMilli(now).atZone(zone).toLocalDate());
    }
    public static double damage(double incoming, double maxHealth, double dealt, boolean mace,
                                boolean boss, double normalCap, double maceCap, double share) {
        double cap = maxHealth * (mace ? maceCap : normalCap);
        if (boss) cap = Math.min(cap, Math.max(0, maxHealth * share - dealt));
        return Math.max(0, Math.min(incoming, cap));
    }
    public static double healthEpsilon(double maximum){return Math.max(1e-6,8.0*Math.ulp((float)maximum));}
    public static boolean finishedHealth(double maximum,double remaining,double credited){double epsilon=healthEpsilon(maximum);return remaining>0&&remaining<=epsilon&&credited>=maximum-epsilon;}
    public static double percentUsed(double dealt,double maximum){return maximum<=0?0:Math.max(0,Math.min(100,Math.floor(1000*dealt/maximum)/10.0));}
    public static int units(int base, int players, int minimum, double scale) {
        return Math.max(1, (int) Math.ceil(base * (1 + Math.max(0, players - minimum) * scale)));
    }
    public static long duration(String input) {
        String text = input.toLowerCase(Locale.ROOT);
        if (!text.matches("[0-9]+[smh]?")) throw new IllegalArgumentException("Use a duration such as 30m, 2h or 90s");
        long factor = switch (text.charAt(text.length()-1)) { case 's' -> 1000; case 'm' -> 60000; case 'h' -> 3600000; default -> 60000; };
        if (Character.isLetter(text.charAt(text.length()-1))) text = text.substring(0, text.length()-1);
        long result = Math.multiplyExact(Long.parseLong(text), factor);
        if (result <= 0 || result > 7L * 86400000) throw new IllegalArgumentException("Duration must be between 1 second and 7 days");
        return result;
    }
    public static boolean majority(int votes, int members) { return votes > members / 2; }
    public static double bossShare(int minimum) { return 1.0 / Math.max(1, minimum); }
    public static String clock(long seconds) { long s=Math.max(0,seconds);return s>=3600?String.format(Locale.ROOT,"%d:%02d:%02d",s/3600,(s/60)%60,s%60):String.format(Locale.ROOT,"%02d:%02d",s/60,s%60); }
    public static boolean mayClimb(boolean playerBlocks,boolean originalBlocks){return playerBlocks&&!originalBlocks;}
}
