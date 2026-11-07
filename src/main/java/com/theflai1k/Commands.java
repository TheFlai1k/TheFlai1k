package com.theflai1k;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Commands {
    private Commands() {}

    static void say(String s) { Tools.say(s); }

    static Config.Waypoint findWp(String name) {
        for (Config.Waypoint w : Config.waypoints) if (w.name.equalsIgnoreCase(name)) return w;
        return null;
    }

    /** returns true when the message should be sent to the server as normal chat */
    public static boolean handle(String msg) {
        if (!msg.startsWith(".") || msg.length() < 2) return true;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return true;
        String[] a = msg.substring(1).trim().split("\\s+");
        String cmd = a[0].toLowerCase(Locale.ROOT);
        try {
            switch (cmd) {
                case "help":
                case "theflai1k":
                    say("Commands:");
                    say(".gps <x> <y> <z> | .gps <x> <z> | .gps here | .gps shared | .gps off");
                    say(".friend add|remove <name> | .friend list");
                    say(".wp add <name> [x y z] | .wp del <name> | .wp list | .wp gps <name> | .wp share <name> | .wp accept");
                    say(".coords  (send your coordinates to chat)");
                    return false;
                case "coords":
                    Tools.sendCoords();
                    return false;
                case "gps": {
                    if (a.length == 1) { say("Usage: .gps x y z  |  .gps off"); return false; }
                    String s = a[1].toLowerCase(Locale.ROOT);
                    if (s.equals("off") || s.equals("clear")) { Modules.gps = null; say("GPS off"); return false; }
                    if (s.equals("here")) { Modules.gps = new double[]{mc.player.getX(), mc.player.getY(), mc.player.getZ()}; say("GPS set to your position"); return false; }
                    if (s.equals("shared")) {
                        if (Tools.shared == null) { say("No shared coordinates received."); return false; }
                        Modules.gps = new double[]{Tools.shared[0], Tools.shared[1], Tools.shared[2]};
                        say("GPS set to shared coordinates");
                        return false;
                    }
                    if (a.length >= 4) Modules.gps = new double[]{Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3])};
                    else if (a.length == 3) Modules.gps = new double[]{Double.parseDouble(a[1]), mc.player.getY(), Double.parseDouble(a[2])};
                    else { say("Usage: .gps x y z"); return false; }
                    say("GPS target set. Enable the GPS HUD in the menu.");
                    return false;
                }
                case "friend": {
                    String sub = a.length > 1 ? a[1].toLowerCase(Locale.ROOT) : "list";
                    if (sub.equals("add") && a.length > 2) {
                        boolean has = false;
                        for (String f : Config.friends) if (f.equalsIgnoreCase(a[2])) has = true;
                        if (!has) Config.friends.add(a[2]);
                        say("Friend added: " + a[2]);
                    } else if ((sub.equals("remove") || sub.equals("del")) && a.length > 2) {
                        Config.friends.removeIf(f -> f.equalsIgnoreCase(a[2]));
                        say("Friend removed: " + a[2]);
                    } else say("Friends: " + (Config.friends.isEmpty() ? "none" : String.join(", ", Config.friends)));
                    Config.save();
                    return false;
                }
                case "wp": {
                    String sub = a.length > 1 ? a[1].toLowerCase(Locale.ROOT) : "list";
                    switch (sub) {
                        case "add": {
                            if (a.length < 3) { say("Usage: .wp add <name> [x y z]"); break; }
                            int x = mc.player.getBlockX(), y = mc.player.getBlockY(), z = mc.player.getBlockZ();
                            if (a.length >= 6) { x = Integer.parseInt(a[3]); y = Integer.parseInt(a[4]); z = Integer.parseInt(a[5]); }
                            Tools.addWaypoint(a[2], x, y, z);
                            say("Waypoint '" + a[2] + "' saved at " + x + " " + y + " " + z);
                            Config.save();
                            break;
                        }
                        case "del":
                        case "remove": {
                            Config.Waypoint w = a.length > 2 ? findWp(a[2]) : null;
                            if (w == null) say("No such waypoint."); else { Config.waypoints.remove(w); say("Deleted " + w.name); Config.save(); }
                            break;
                        }
                        case "gps": {
                            Config.Waypoint w = a.length > 2 ? findWp(a[2]) : null;
                            if (w == null) say("No such waypoint."); else { Modules.gps = new double[]{w.x, w.y, w.z}; say("GPS -> " + w.name); }
                            break;
                        }
                        case "share": {
                            Config.Waypoint w = a.length > 2 ? findWp(a[2]) : null;
                            if (w == null) say("No such waypoint."); else Tools.shareWp(w);
                            break;
                        }
                        case "accept":
                            Tools.acceptPending();
                            Config.save();
                            break;
                        default: {
                            List<String> l = new ArrayList<>();
                            for (Config.Waypoint w : Config.waypoints) l.add(w.name + " (" + w.x + " " + w.y + " " + w.z + ", " + w.dim + ")");
                            say("Waypoints: " + (l.isEmpty() ? "none" : String.join("; ", l)));
                        }
                    }
                    return false;
                }
                default:
                    return true;
            }
        } catch (NumberFormatException e) {
            say("Invalid number.");
            return false;
        }
    }
}
