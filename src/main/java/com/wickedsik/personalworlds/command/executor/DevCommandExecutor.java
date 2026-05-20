package com.wickedsik.personalworlds.command.executor;

import com.wickedsik.personalworlds.command.CommandResult;
import com.wickedsik.personalworlds.command.service.TeleportHelper;
import com.wickedsik.personalworlds.compat.EntityCompat;
import com.wickedsik.personalworlds.compat.TeleportCompat;
import com.wickedsik.personalworlds.dimension.DimensionManager;
import com.wickedsik.personalworlds.dimension.DimensionRegistry;
import com.wickedsik.personalworlds.dimension.PlayerDimensionData;
import com.wickedsik.personalworlds.dimension.WorldGenType;
import com.wickedsik.personalworlds.portal.PortalHelper;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

/**
 * Команды для разработчиков (создание/вход/выход из измерений).
 */
public class DevCommandExecutor {
   public CommandResult createDimension(ServerPlayerEntity player, String typeStr) {
      WorldGenType type = WorldGenType.fromString(typeStr);
      UUID playerUuid = player.getUuid();
      String playerName = player.getName().getString();
      MinecraftServer server = EntityCompat.getServer(player);

      try {
         ServerWorld dimension = DimensionManager.getOrCreatePlayerDimension(server, playerUuid, playerName, type, 0);
         TeleportCompat.teleport(player, dimension, TeleportHelper.toDefaultSpawn(dimension, player));
         return CommandResult.successBroadcast(Text.translatable("pocketislands.command.info.dimension_created", type.name()));
      } catch (Exception e) {
         return CommandResult.error(Text.translatable("pocketislands.command.error.create_failed", e.getMessage()));
      }
   }

   public CommandResult enterDimension(ServerPlayerEntity player) {
      UUID playerUuid = player.getUuid();
      MinecraftServer server = EntityCompat.getServer(player);
      DimensionRegistry registry = DimensionRegistry.get(server);
      if (!registry.hasDimension(playerUuid)) {
         return CommandResult.error(Text.translatable("pocketislands.command.error.no_dimension"));
      }
      Optional<PlayerDimensionData> optData = registry.getDimensionData(playerUuid);
      if (optData.isEmpty()) {
         return CommandResult.error(Text.translatable("pocketislands.command.error.load_failed"));
      }
      PlayerDimensionData data = optData.get();
      try {
         ServerWorld dimension = DimensionManager.getOrCreatePlayerDimension(server, playerUuid, player.getName().getString(), data.generatorType(), data.portalTypeIndex());
         TeleportCompat.teleport(player, dimension, TeleportHelper.toBlockPos(dimension, data.spawnPoint(), player));
         return CommandResult.successBroadcast(Text.translatable("pocketislands.command.enter.success"));
      } catch (Exception e) {
         return CommandResult.error(Text.translatable("pocketislands.command.error.enter_failed", e.getMessage()));
      }
   }

   public CommandResult leaveDimension(ServerPlayerEntity player) {
      PortalHelper.teleportToReturnPosition(player, EntityCompat.getServer(player));
      return CommandResult.silent();
   }
}
