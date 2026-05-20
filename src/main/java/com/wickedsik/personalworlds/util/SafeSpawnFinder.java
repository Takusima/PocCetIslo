package com.wickedsik.personalworlds.util;

import com.wickedsik.personalworlds.PersonalWorldsMod;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

/**
 * Поиск безопасной позиции для спавна игрока.
 */
public class SafeSpawnFinder {
   private static final int SEARCH_RADIUS = 16;

   public static BlockPos findSafePosition(ServerWorld world, BlockPos target) {
      if (isSafeSpawn(world, target)) return target;
      BlockPos vertical = searchVertically(world, target);
      if (vertical != null) return vertical;
      BlockPos spiral = searchSpiral(world, target);
      if (spiral != null) return spiral;
      BlockPos worldSpawn = findSafeNearSpawn(world);
      if (worldSpawn != null) return worldSpawn;
      return new BlockPos(0, 100, 0);
   }

   public static boolean isSafeSpawn(ServerWorld world, BlockPos pos) {
      BlockState ground = world.getBlockState(pos.down());
      if (!ground.isSolidBlock(world, pos.down())) return false;
      BlockState feet = world.getBlockState(pos);
      BlockState head = world.getBlockState(pos.up());
      return feet.isAir() && head.isAir() && !ground.getFluidState().isEmpty() == false;
   }

   private static BlockPos searchVertically(ServerWorld world, BlockPos target) {
      for (int dy = 0; dy <= 32; ++dy) {
         BlockPos check = target.up(dy);
         if (check.getY() < WorldCompat.getTopY(world) && isSafeSpawn(world, check)) return check;
      }
      for (int dy = 1; dy <= 32; ++dy) {
         BlockPos check = target.down(dy);
         if (check.getY() > world.getBottomY() && isSafeSpawn(world, check)) return check;
      }
      return null;
   }

   private static BlockPos searchSpiral(ServerWorld world, BlockPos target) {
      for (int radius = 1; radius <= SEARCH_RADIUS; ++radius) {
         for (int dx = -radius; dx <= radius; ++dx) {
            for (int dz = -radius; dz <= radius; ++dz) {
               if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
                  BlockPos horizontal = target.add(dx, 0, dz);
                  int surfaceY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, horizontal.getX(), horizontal.getZ());
                  BlockPos surface = new BlockPos(horizontal.getX(), surfaceY, horizontal.getZ());
                  if (isSafeSpawn(world, surface)) return surface;
                  BlockPos vertical = searchVertically(world, horizontal);
                  if (vertical != null) return vertical;
               }
            }
         }
      }
      return null;
   }

   private static BlockPos findSafeNearSpawn(ServerWorld world) {
      BlockPos spawn = WorldCompat.getSpawnPos(world);
      return isSafeSpawn(world, spawn) ? spawn : searchSpiral(world, spawn);
   }
}
