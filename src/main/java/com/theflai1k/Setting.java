package com.theflai1k;

import java.util.Locale;

public class Setting {
    public final String name;
    public double value, min, max, step;
    public final boolean bool;

    public Setting(String name, boolean v) {
        this.name = name; this.value = v ? 1 : 0; this.bool = true; this.min = 0; this.max = 1; this.step = 1;
    }

    public Setting(String name, double v, double min, double max, double step) {
        this.name = name; this.value = v; this.min = min; this.max = max; this.step = step; this.bool = false;
    }

    public boolean on() { return value > 0.5; }
    public float f() { return (float) value; }
    public int i() { return (int) Math.round(value); }

    public String display() {
        if (bool) return on() ? "ON" : "OFF";
        if (step >= 1) return String.valueOf(i());
        return String.format(Locale.ROOT, "%.2f", value);
    }

    public void setFromFraction(double t) {
        t = Math.max(0, Math.min(1, t));
        double v = min + t * (max - min);
        v = Math.round(v / step) * step;
        value = Math.max(min, Math.min(max, v));
    }
}
