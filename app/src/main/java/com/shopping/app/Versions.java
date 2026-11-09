package com.shopping.app;

/** Compares release tags such as v1.11.0 with an app versionName. Pure Java, unit-testable. */
public final class Versions {
    private Versions() {}

    public static String stripV(String version) {
        if (version == null) return "";
        String v = version.trim();
        return (v.startsWith("v") || v.startsWith("V")) ? v.substring(1) : v;
    }

    /** Returns negative if {@code left} is older, zero if equal, positive if newer. */
    public static int compare(String left, String right) {
        int[] a = parts(left), b = parts(right);
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? a[i] : 0;
            int y = i < b.length ? b[i] : 0;
            if (x != y) return x < y ? -1 : 1;
        }
        return 0;
    }

    static int[] parts(String version) {
        String v = stripV(version);
        int suffix = v.indexOf('-');
        if (suffix < 0) suffix = v.indexOf('+');
        if (suffix >= 0) v = v.substring(0, suffix);
        java.util.List<Integer> out = new java.util.ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(v);
        while (m.find()) {
            String n = m.group();
            if (n.length() > 9) n = n.substring(0, 9);
            out.add(Integer.parseInt(n));
        }
        int[] result = new int[out.size()];
        for (int i = 0; i < result.length; i++) result[i] = out.get(i);
        return result;
    }
}
