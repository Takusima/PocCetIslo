package com.wickedsik.personalworlds.portal;

import com.wickedsik.personalworlds.PersonalWorldsMod;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

/**
 * Защита от одновременного использования одного портала несколькими игроками.
 * Предотвращает двойную телепортацию при быстром нажатии.
 */
public class ConcurrentPortalGuard {
   private static final Map<UUID, Long> playersInTransit = new ConcurrentHashMap<>();
   private static final Map<Long, UUID> portalsProcessing = new ConcurrentHashMap<>();
   private static final long TELEPORT_COOLDOWN_MS = 1000L;
   private static final long MAX_LOCK_TIME_MS = 5000L;

   // Пытается захватить блокировку для телепортации
   public static boolean tryAcquire(ServerPlayerEntity player, BlockPos portalPos) {
      UUID playerUuid = player.getUuid();
      long now = System.currentTimeMillis();
      long portalHash = hashPosition(portalPos);
      Long lastTeleport = playersInTransit.get(playerUuid);
      if (lastTeleport != null) {
         if (now - lastTeleport < TELEPORT_COOLDOWN_MS) {
            return false;
         }
         if (now - lastTeleport > MAX_LOCK_TIME_MS) {
            PersonalWorldsMod.LOGGER.warn("Clearing stale transit lock for player {}", player.getName().getString());
         }
      }
      UUID processingPlayer = portalsProcessing.get(portalHash);
      if (processingPlayer != null && !processingPlayer.equals(playerUuid)) {
         Long processingTime = playersInTransit.get(processingPlayer);
         if (processingTime != null && now - processingTime < MAX_LOCK_TIME_MS) {
            return false;
         }
      }
      playersInTransit.put(playerUuid, now);
      portalsProcessing.put(portalHash, playerUuid);
      return true;
   }

   // Освобождает блокировку портала
   public static void release(ServerPlayerEntity player, BlockPos portalPos) {
      UUID playerUuid = player.getUuid();
      long portalHash = hashPosition(portalPos);
      portalsProcessing.remove(portalHash, playerUuid);
   }

   // Принудительно освобождает все блокировки игрока (при выходе из игры)
   public static void forceRelease(UUID playerUuid) {
      playersInTransit.remove(playerUuid);
      portalsProcessing.values().removeIf((uuid) -> uuid.equals(playerUuid));
   }

   // Очищает устаревшие блокировки
   public static void cleanup() {
      long now = System.currentTimeMillis();
      long staleThreshold = now - MAX_LOCK_TIME_MS;
      playersInTransit.entrySet().removeIf((entry) -> entry.getValue() < staleThreshold);
      portalsProcessing.entrySet().removeIf((entry) -> !playersInTransit.containsKey(entry.getValue()));
   }

   // Хеширует позицию блока для быстрого сравнения
   static long hashPosition(BlockPos pos) {
      return ((long) pos.getX() & 67108863L) << 38 | ((long) pos.getY() & 65535L) << 20 | (long) pos.getZ() & 16777215L;
   }

   // Версия для работы с UUID напрямую (без объекта игрока)
   static boolean tryAcquire(UUID playerUuid, BlockPos portalPos) {
      long now = System.currentTimeMillis();
      long portalHash = hashPosition(portalPos);
      Long lastTeleport = playersInTransit.get(playerUuid);
      if (lastTeleport != null && now - lastTeleport < TELEPORT_COOLDOWN_MS) {
         return false;
      }
      UUID processingPlayer = portalsProcessing.get(portalHash);
      if (processingPlayer != null && !processingPlayer.equals(playerUuid)) {
         Long processingTime = playersInTransit.get(processingPlayer);
         if (processingTime != null && now - processingTime < MAX_LOCK_TIME_MS) {
            return false;
         }
      }
      playersInTransit.put(playerUuid, now);
      portalsProcessing.put(portalHash, playerUuid);
      return true;
   }

   static void release(UUID playerUuid, BlockPos portalPos) {
      long portalHash = hashPosition(portalPos);
      portalsProcessing.remove(portalHash, playerUuid);
   }

   // Полная очистка (для тестов)
   static void clearAllForTesting() {
      playersInTransit.clear();
      portalsProcessing.clear();
   }
}
