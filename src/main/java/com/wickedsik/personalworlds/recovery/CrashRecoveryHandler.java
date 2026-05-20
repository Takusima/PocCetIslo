package com.wickedsik.personalworlds.recovery;

import com.wickedsik.personalworlds.PersonalWorldsMod;
import com.wickedsik.personalworlds.compat.*;
import com.wickedsik.personalworlds.dimension.*;
import com.wickedsik.personalworlds.player.*;
import com.wickedsik.personalworlds.portal.PortalHelper;
import com.wickedsik.personalworlds.util.SafeSpawnFinder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Обработка восстановления после сбоев сервера.
 * Проверяет позиции игроков при входе и телепортирует их в безопасное место.
 */
public class CrashRecoveryHandler {
   public static void onPlayerJoin(ServerPlayerEntity player) {
      MinecraftServer server = EntityCompat.getServer(player);
      if (server == null) return;
      UUID playerUuid = player.getUuid();
      ServerWorld currentWorld = EntityCompat.getServerWorld(player);
      PlayerDataManager dataManager = PlayerDataManager.get(server);

      if (PortalHelper.isInPersonalDimension(currentWorld)) {
         handleLoginInPersonalDimension(player, server, currentWorld);
      } else {
         Optional<RegistryKey<World>> expectedDimension = dataManager.getCurrentPocketDimension(playerUuid);
         if (expectedDimension.isPresent()) {
            handleMisplacedPlayer(player, server, expectedDimension.get());
         } else {
            if (dataManager.hasReturnData(playerUuid)) {
               dataManager.clearReturnData(playerUuid);
            }
         }
      }
   }

   private static void handleLoginInPersonalDimension(ServerPlayerEntity player, MinecraftServer server, ServerWorld personalWorld) {
      UUID playerUuid = player.getUuid();
      Optional<UUID> ownerOpt = PortalHelper.getDimensionOwner(personalWorld);
      if (ownerOpt.isEmpty()) {
         emergencyEvacuate(player, server, "Invalid dimension detected");
         return;
      }
      UUID ownerUuid = ownerOpt.get();
      if (!playerUuid.equals(ownerUuid) && !InvitationManager.canVisit(server, playerUuid, ownerUuid)) {
         player.sendMessage(Text.translatable("pocketislands.message.ejected_offline").formatted(Formatting.GOLD), false);
         PlayerDataManager dataManager = PlayerDataManager.get(server);
         dataManager.clearCurrentPocketDimension(playerUuid);
         teleportToFallbackPosition(player, server, dataManager);
      } else {
         PlayerDataManager dataManager = PlayerDataManager.get(server);
         dataManager.setCurrentPocketDimension(playerUuid, personalWorld.getRegistryKey());
      }
   }

   private static void handleMisplacedPlayer(ServerPlayerEntity player, MinecraftServer server, RegistryKey<World> expectedDimension) {
      UUID playerUuid = player.getUuid();
      PlayerDataManager dataManager = PlayerDataManager.get(server);
      Optional<UUID> ownerOpt = PortalHelper.getDimensionOwner(expectedDimension);
      if (ownerOpt.isEmpty()) {
         dataManager.clearCurrentPocketDimension(playerUuid);
         teleportToFallbackPosition(player, server, dataManager);
         return;
      }
      UUID ownerUuid = ownerOpt.get();
      if (!playerUuid.equals(ownerUuid) && !InvitationManager.canVisit(server, playerUuid, ownerUuid)) {
         dataManager.clearCurrentPocketDimension(playerUuid);
         player.sendMessage(Text.translatable("pocketislands.message.ejected_offline").formatted(Formatting.GOLD), false);
         teleportToFallbackPosition(player, server, dataManager);
         return;
      }
      DimensionRegistry registry = DimensionRegistry.get(server);
      Optional<PlayerDimensionData> dimDataOpt = registry.getDimensionData(ownerUuid);
      if (dimDataOpt.isEmpty()) {
         dataManager.clearCurrentPocketDimension(playerUuid);
         teleportToFallbackPosition(player, server, dataManager);
         return;
      }
      PlayerDimensionData dimData = dimDataOpt.get();
      try {
         ServerWorld targetWorld = DimensionManager.getOrCreatePlayerDimension(server, ownerUuid, dimData.ownerName(), dimData.generatorType(), dimData.portalTypeIndex());
         BlockPos safePos = SafeSpawnFinder.findSafePosition(targetWorld, dimData.spawnPoint());
         TeleportCompat.teleportToBlockPreserveRotation(player, targetWorld, safePos);
         player.sendMessage(Text.translatable("pocketislands.message.dimension_restored"), false);
      } catch (Exception e) {
         dataManager.clearCurrentPocketDimension(playerUuid);
         teleportToFallbackPosition(player, server, dataManager);
      }
   }

   private static void teleportToFallbackPosition(ServerPlayerEntity player, MinecraftServer server, PlayerDataManager dataManager) {
      UUID playerUuid = player.getUuid();
      float yaw = player.getYaw();
      float pitch = player.getPitch();
      Optional<ReturnData> returnDataOpt = dataManager.getReturnData(playerUuid);
      ServerWorld targetWorld;
      Vec3d targetPos;
      if (returnDataOpt.isPresent()) {
         ReturnData returnData = returnDataOpt.get();
         targetWorld = server.getWorld(returnData.dimension());
         if (targetWorld != null) {
            BlockPos safePos = SafeSpawnFinder.findSafePosition(targetWorld, returnData.position());
            targetPos = Vec3d.ofCenter(safePos);
            yaw = returnData.yaw();
            pitch = returnData.pitch();
            dataManager.clearReturnData(playerUuid);
            teleportPlayer(player, targetWorld, targetPos, yaw, pitch);
            return;
         }
         dataManager.clearReturnData(playerUuid);
      }
      BlockPos bedPos = EntityCompat.getSpawnPointPosition(player);
      if (bedPos != null) {
         RegistryKey<World> bedWorldKey = EntityCompat.getSpawnPointDimension(player);
         if (bedWorldKey != null) {
            ServerWorld bedWorld = server.getWorld(bedWorldKey);
            if (bedWorld != null) {
               BlockPos safePos = SafeSpawnFinder.findSafePosition(bedWorld, bedPos);
               targetPos = Vec3d.ofCenter(safePos);
               teleportPlayer(player, bedWorld, targetPos, yaw, pitch);
               return;
            }
         }
      }
      targetWorld = server.getOverworld();
      BlockPos safePos = SafeSpawnFinder.findSafePosition(targetWorld, WorldCompat.getSpawnPos(targetWorld));
      targetPos = Vec3d.ofCenter(safePos);
      teleportPlayer(player, targetWorld, targetPos, yaw, pitch);
   }

   private static void teleportPlayer(ServerPlayerEntity player, ServerWorld world, Vec3d pos, float yaw, float pitch) {
      TeleportCompat.teleport(player, world, pos, yaw, pitch);
      player.sendMessage(Text.translatable("pocketislands.message.returned_overworld"), true);
   }

   private static void emergencyEvacuate(ServerPlayerEntity player, MinecraftServer server, String reason) {
      ServerWorld overworld = server.getOverworld();
      BlockPos safePos = SafeSpawnFinder.findSafePosition(overworld, WorldCompat.getSpawnPos(overworld));
      TeleportCompat.teleportToBlockPreserveRotation(player, overworld, safePos);
      player.sendMessage(Text.translatable("pocketislands.message.emergency_teleport", reason).formatted(Formatting.RED), false);
      PlayerDataManager dataManager = PlayerDataManager.get(server);
      dataManager.clearReturnData(player.getUuid());
      dataManager.clearCurrentPocketDimension(player.getUuid());
   }
}
