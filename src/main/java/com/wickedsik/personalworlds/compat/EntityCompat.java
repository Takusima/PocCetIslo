package com.wickedsik.personalworlds.compat;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Утилиты для работы с сущностями (игроками).
 */
public final class EntityCompat {
   private EntityCompat() {
   }

   // Получает сервер, на котором находится игрок
   public static MinecraftServer getServer(ServerPlayerEntity player) {
      return player.getServer();
   }

   // Получает мир сервера, в котором находится игрок
   public static ServerWorld getServerWorld(ServerPlayerEntity player) {
      return player.getServerWorld();
   }

   // Получает позицию игрока
   public static Vec3d getPos(ServerPlayerEntity player) {
      return player.getPos();
   }

   // Получает позицию точки возрождения игрока (кровать / якорь)
   public static @Nullable BlockPos getSpawnPointPosition(ServerPlayerEntity player) {
      return player.getSpawnPointPosition();
   }

   // Получает измерение точки возрождения игрока
   public static @Nullable RegistryKey<World> getSpawnPointDimension(ServerPlayerEntity player) {
      return player.getSpawnPointDimension();
   }
}
