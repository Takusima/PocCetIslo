package com.wickedsik.personalworlds.compat;

import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;

/**
 * Утилиты для телепортации игроков между измерениями.
 */
public final class TeleportCompat {
   private TeleportCompat() {
   }

   // Телепортирует игрока в другое измерение с указанием позиции и поворота
   public static void teleport(ServerPlayerEntity player, ServerWorld targetWorld, Vec3d position, float yaw, float pitch) {
      TeleportTarget target = new TeleportTarget(position, Vec3d.ZERO, yaw, pitch);
      teleport(player, targetWorld, target);
   }

   // Телепортирует игрока с помощью TeleportTarget
   public static void teleport(ServerPlayerEntity player, ServerWorld targetWorld, TeleportTarget target) {
      FabricDimensions.teleport(player, targetWorld, target);
   }

   // Телепортирует игрока к блоку с указанием поворота
   public static void teleportToBlock(ServerPlayerEntity player, ServerWorld targetWorld, BlockPos blockPos, float yaw, float pitch) {
      Vec3d position = Vec3d.ofCenter(blockPos);
      teleport(player, targetWorld, position, yaw, pitch);
   }

   // Телепортирует игрока, сохраняя его текущий поворот
   public static void teleportPreserveRotation(ServerPlayerEntity player, ServerWorld targetWorld, Vec3d position) {
      teleport(player, targetWorld, position, player.getYaw(), player.getPitch());
   }

   // Телепортирует игрока к блоку, сохраняя его текущий поворот
   public static void teleportToBlockPreserveRotation(ServerPlayerEntity player, ServerWorld targetWorld, BlockPos blockPos) {
      Vec3d position = Vec3d.ofCenter(blockPos);
      teleportPreserveRotation(player, targetWorld, position);
   }
}
