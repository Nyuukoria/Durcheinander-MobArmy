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
}
