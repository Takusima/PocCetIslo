package com.wickedsik.personalworlds.util;

import com.wickedsik.personalworlds.PersonalWorldsMod;
import com.wickedsik.personalworlds.dimension.DimensionManager;
import java.lang.management.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.server.MinecraftServer;

/**
 * Мониторинг производительности (для отладки).
 */
public class PerformanceMonitor {
   private static final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
   private static final ConcurrentHashMap<String, Long> timers = new ConcurrentHashMap<>();
   private static boolean enabled = false;

   public static void enable() { enabled = true; PersonalWorldsMod.LOGGER.info("Performance monitoring ENABLED"); }
   public static void disable() { enabled = false; counters.clear(); timers.clear(); PersonalWorldsMod.LOGGER.info("Performance monitoring DISABLED"); }
   public static boolean isEnabled() { return enabled; }

   public static void startTimer(String operation) { if (enabled) timers.put(operation, System.nanoTime()); }

   public static long stopTimer(String operation) {
      if (!enabled) return 0L;
      Long start = timers.remove(operation);
      if (start == null) return 0L;
      long elapsedMs = (System.nanoTime() - start) / 1000000L;
      PersonalWorldsMod.LOGGER.info("[PERF] {}: {}ms", operation, elapsedMs);
      return elapsedMs;
   }

   public static void increment(String counter) {
      if (enabled) counters.computeIfAbsent(counter, k -> new AtomicLong(0L)).incrementAndGet();
   }

   public static long getCounter(String counter) {
      AtomicLong value = counters.get(counter);
      return value != null ? value.get() : 0L;
   }

   public static void logStatus(MinecraftServer server) {
      if (!enabled) return;
      MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
      long heapUsed = memoryBean.getHeapMemoryUsage().getUsed() / 1048576L;
      long heapMax = memoryBean.getHeapMemoryUsage().getMax() / 1048576L;
      int loadedDimensions = DimensionManager.getLoadedDimensionCount();
      int onlinePlayers = server.getPlayerManager().getPlayerCount();
      PersonalWorldsMod.LOGGER.info("[PERF] Status: {} dims loaded, {} players, heap {}/{}MB",
         loadedDimensions, onlinePlayers, heapUsed, heapMax);
   }

   public static String getStatusSummary(MinecraftServer server) {
      MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
      long heapUsed = memoryBean.getHeapMemoryUsage().getUsed() / 1048576L;
      long heapMax = memoryBean.getHeapMemoryUsage().getMax() / 1048576L;
      int loadedDimensions = DimensionManager.getLoadedDimensionCount();
      int onlinePlayers = server.getPlayerManager().getPlayerCount();
      StringBuilder sb = new StringBuilder();
      sb.append("=== Performance Status ===\n");
      sb.append(String.format("Loaded dimensions: %d\n", loadedDimensions));
      sb.append(String.format("Online players: %d\n", onlinePlayers));
      sb.append(String.format("Heap memory: %d/%dMB (%.1f%%)\n", heapUsed, heapMax, (double) heapUsed / (double) heapMax * 100.0));
      if (enabled && !counters.isEmpty()) {
         sb.append("\n=== Counters ===\n");
         counters.forEach((name, count) -> sb.append(String.format("%s: %d\n", name, count.get())));
      }
      return sb.toString();
   }

   public static boolean isMemoryPressureHigh() {
      MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
      return (double) memoryBean.getHeapMemoryUsage().getUsed() / (double) memoryBean.getHeapMemoryUsage().getMax() > 0.85;
   }

   public static void resetCounters() { counters.clear(); }
}
