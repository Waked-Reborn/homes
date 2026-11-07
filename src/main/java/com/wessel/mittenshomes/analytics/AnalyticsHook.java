package com.wessel.mittenshomes.analytics;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/** Optional analytics; failures never change the outcome of a gameplay action. */
public final class AnalyticsHook {
    private static boolean warned;
    private AnalyticsHook() {}
    public static void record(UUID player, String metric, String detail, double value) {
        try {
            if (Bukkit.getServer() == null) return;
            Plugin target = Bukkit.getPluginManager().getPlugin("MittensAnalystics");
            if (target == null || !target.isEnabled()) return;
            target.getClass().getMethod("recordMetric", UUID.class, String.class, String.class, String.class, double.class)
                    .invoke(target, player, "MittensHomes", metric, detail, value);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            if (!warned && Bukkit.getServer() != null) {
                warned = true;
                Bukkit.getLogger().warning("MittensHomes analytics hook unavailable: " + ex.getClass().getSimpleName());
            }
        }
    }
}
