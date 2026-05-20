package com.wickedsik.personalworlds.compat;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Утилиты для работы с мирами.
 */
public final class WorldCompat {
   private WorldCompat() {
   }

   // Получает позицию спавна мира
   public static BlockPos getSpawnPos(ServerWorld world) {
      return world.getSpawnPos();
   }

   // Получает максимальную высоту мира
   public static int getTopY(World world) {
      return world.getTopY();
   }
}
