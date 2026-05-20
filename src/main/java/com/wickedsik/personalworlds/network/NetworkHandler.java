package com.wickedsik.personalworlds.network;

import com.wickedsik.personalworlds.PersonalWorldsMod;
import com.wickedsik.personalworlds.command.service.PlayerLookupService;
import com.wickedsik.personalworlds.compat.*;
import com.wickedsik.personalworlds.config.ModConfig;
import com.wickedsik.personalworlds.dimension.DimensionRegistry;
import com.wickedsik.personalworlds.player.*;
import com.wickedsik.personalworlds.portal.PortalColor;
import com.wickedsik.personalworlds.portal.PortalHelper;
import com.wickedsik.personalworlds.util.VisualEffects;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.*;

/**
 * Обработчик серверных сетевых пакетов.
 */
public final class NetworkHandler {
   private static final int PRUNUS_XP_COST = 2;
   private static final int PRUNUS_FOOD_COST = 3;
   private static final int MALUS_XP_COST = 2;
   private static final int MALUS_FOOD_COST = 3;

   private NetworkHandler() {}

   // === Prunus (вход в карманное измерение) ===
   public static void handlePrunusCommand(ServerPlayerEntity player) {
      if (!canAffordCommand(player, PRUNUS_XP_COST, PRUNUS_FOOD_COST)) {
         sendCommandResult(player, false, "pocketislands.command.prunus.not_enough_resources");
         return;
      }
      ServerWorld currentWorld = EntityCompat.getServerWorld(player);
      if (PortalHelper.isInPersonalDimension(currentWorld)) {
         sendCommandResult(player, false, "pocketislands.command.prunus.already_in_dimension");
         return;
      }
      deductCommandCost(player, PRUNUS_XP_COST, PRUNUS_FOOD_COST);
      saveReturnData(player);
      VisualEffects.playTeleportSequenceStart(player);
      MinecraftServer server = EntityCompat.getServer(player);
      if (server != null) {
         DimensionRegistry registry = DimensionRegistry.get(server);
         Optional<ServerWorld> dimensionOpt = registry.getPlayerDimension(server, player.getUuid());
         if (dimensionOpt.isPresent()) {
            ServerWorld targetWorld = dimensionOpt.get();
            Vec3d spawnPos = Vec3d.ofCenter(WorldCompat.getSpawnPos(targetWorld));
            TeleportCompat.teleport(player, targetWorld, spawnPos, player.getYaw(), player.getPitch());
            VisualEffects.playTeleportSequenceEnd(player);
            VisualEffects.playDimensionEntryEffect(player);
            sendCommandResult(player, true, "pocketislands.command.prunus.success");
         } else {
            refundCommandCost(player, PRUNUS_XP_COST, PRUNUS_FOOD_COST);
            VisualEffects.removeBlindnessEffect(player);
            sendCommandResult(player, false, "pocketislands.command.prunus.no_dimension");
         }
      }
   }

   // === Malus (выход из карманного измерения) ===
   public static void handleMalusCommand(ServerPlayerEntity player) {
      if (!canAffordCommand(player, MALUS_XP_COST, MALUS_FOOD_COST)) {
         sendCommandResult(player, false, "pocketislands.command.malus.not_enough_resources");
         return;
      }
      ServerWorld currentWorld = EntityCompat.getServerWorld(player);
      if (!PortalHelper.isInPersonalDimension(currentWorld)) {
         sendCommandResult(player, false, "pocketislands.command.malus.not_in_dimension");
         return;
      }
      deductCommandCost(player, MALUS_XP_COST, MALUS_FOOD_COST);
      VisualEffects.playTeleportSequenceStart(player);
      MinecraftServer server = EntityCompat.getServer(player);
      if (server != null) {
         PlayerDataManager dataManager = PlayerDataManager.get(server);
         Optional<ReturnData> returnDataOpt = dataManager.getReturnData(player.getUuid());
         ServerWorld targetWorld;
         Vec3d targetPos;
         float yaw, pitch;
         if (returnDataOpt.isPresent()) {
            ReturnData returnData = returnDataOpt.get();
            targetWorld = server.getWorld(returnData.dimension());
            if (targetWorld == null) {
               targetWorld = server.getOverworld();
               targetPos = Vec3d.ofCenter(WorldCompat.getSpawnPos(targetWorld));
            } else {
               targetPos = Vec3d.ofCenter(returnData.position());
            }
            yaw = returnData.yaw();
            pitch = returnData.pitch();
            dataManager.clearReturnData(player.getUuid());
         } else {
            targetWorld = server.getOverworld();
            targetPos = Vec3d.ofCenter(WorldCompat.getSpawnPos(targetWorld));
            yaw = player.getYaw();
            pitch = player.getPitch();
         }
         TeleportCompat.teleport(player, targetWorld, targetPos, yaw, pitch);
         VisualEffects.playTeleportSequenceEnd(player);
         VisualEffects.playDimensionExitEffect(player);
         VoiceChatCompatibility.removePlayerFromAllVoiceGroups(player.getUuid());
         sendCommandResult(player, true, "pocketislands.command.malus.success");
      }
   }

   // === Смена цвета портала ===
   public static void handleChangePortalColor(ServerPlayerEntity player, String colorName) {
      PortalColor newColor = PortalColor.fromString(colorName);
      if (newColor == null) return;
      MinecraftServer server = EntityCompat.getServer(player);
      if (server != null) {
         PlayerDataManager dataManager = PlayerDataManager.get(server);
         dataManager.setPortalColor(player.getUuid(), newColor);
         Text message = Text.translatable("pocketislands.gui.color_changed",
            Text.translatable("pocketislands.portal.color." + newColor.getName())).formatted(Formatting.GREEN);
         player.sendMessage(message, true);
         syncPortalColorToClient(player, newColor, player.getName().getString());
      }
   }

   // === Отправка приглашения ===
   public static void handleInvitationRequest(ServerPlayerEntity sender, String targetName) {
      MinecraftServer server = EntityCompat.getServer(sender);
      if (server == null) return;
      Optional<ServerPlayerEntity> targetOpt = PlayerLookupService.findPlayer(server, targetName);
      if (targetOpt.isEmpty()) {
         sendCommandResult(sender, false, "pocketislands.invitation.target_not_found");
         return;
      }
      ServerPlayerEntity target = targetOpt.get();
      if (sender.getUuid().equals(target.getUuid())) {
         sendCommandResult(sender, false, "pocketislands.invitation.cannot_invite_self");
         return;
      }
      sendVisitRequest(target, sender.getName().getString());
      sendCommandResult(sender, true, "pocketislands.invitation.request_sent");
   }

   // === Ответ на приглашение ===
   public static void handleInvitationResponse(ServerPlayerEntity responder, String senderName, boolean accepted) {
      MinecraftServer server = EntityCompat.getServer(responder);
      if (server == null) return;
      Optional<ServerPlayerEntity> senderOpt = PlayerLookupService.findPlayer(server, senderName);
      if (senderOpt.isEmpty()) {
         sendCommandResult(responder, false, "pocketislands.invitation.sender_not_found");
         return;
      }
      ServerPlayerEntity sender = senderOpt.get();
      if (accepted) {
         ServerWorld responderWorld = EntityCompat.getServerWorld(responder);
         Vec3d responderPos = EntityCompat.getPos(responder);
         saveReturnData(sender);
         VisualEffects.playTeleportSequenceStart(sender);
         TeleportCompat.teleport(sender, responderWorld, responderPos, sender.getYaw(), sender.getPitch());
         VisualEffects.playTeleportSequenceEnd(sender);
         VoiceChatCompatibility.createVoiceGroup(responder, sender);
         sendCommandResult(sender, true, "pocketislands.invitation.teleported_to_owner");
         sendCommandResult(responder, true, "pocketislands.invitation.accepted_sender");
      } else {
         sendVisitDenied(sender, responder.getName().getString(), "pocketislands.invitation.denied");
         sendCommandResult(responder, true, "pocketislands.invitation.declined");
      }
   }

   // === Открытие GUI ===
   public static void handleOpenGui(ServerPlayerEntity player) {
      MinecraftServer server = EntityCompat.getServer(player);
      if (server == null) return;
      List<String> playerNames = new ArrayList<>();
      for (ServerPlayerEntity onlinePlayer : server.getPlayerManager().getPlayerList()) {
         if (!onlinePlayer.getUuid().equals(player.getUuid())) {
            playerNames.add(onlinePlayer.getName().getString());
         }
      }
      sendPlayerListSync(player, playerNames);
      sendOpenGui(player);
   }

   // === Вспомогательные методы ===
   private static boolean canAffordCommand(ServerPlayerEntity player, int xpCost, int foodCost) {
      return player.experienceLevel >= xpCost && player.getHungerManager().getFoodLevel() >= foodCost;
   }

   private static void deductCommandCost(ServerPlayerEntity player, int xpCost, int foodCost) {
      player.addExperienceLevels(-xpCost);
      player.getHungerManager().setFoodLevel(player.getHungerManager().getFoodLevel() - foodCost);
   }

   private static void refundCommandCost(ServerPlayerEntity player, int xpCost, int foodCost) {
      player.addExperienceLevels(xpCost);
      player.getHungerManager().setFoodLevel(player.getHungerManager().getFoodLevel() + foodCost);
   }

   private static void saveReturnData(ServerPlayerEntity player) {
      MinecraftServer server = EntityCompat.getServer(player);
      if (server != null) {
         PlayerDataManager dataManager = PlayerDataManager.get(server);
         ServerWorld world = EntityCompat.getServerWorld(player);
         Vec3d pos = EntityCompat.getPos(player);
         ReturnData returnData = new ReturnData(world.getRegistryKey(),
            new BlockPos((int) pos.x, (int) pos.y, (int) pos.z), player.getYaw(), player.getPitch());
         dataManager.setReturnData(player.getUuid(), returnData);
      }
   }

   // === Отправка пакетов клиенту ===
   private static void sendCommandResult(ServerPlayerEntity player, boolean success, String messageKey) {
      PacketByteBuf buf = ModNetworking.createBuf();
      buf.writeBoolean(success);
      buf.writeString(messageKey);
      ServerPlayNetworking.send(player, ModNetworking.COMMAND_RESULT, buf);
   }

   private static void sendVisitRequest(ServerPlayerEntity target, String requesterName) {
      PacketByteBuf buf = ModNetworking.createBuf();
      buf.writeString(requesterName);
      ServerPlayNetworking.send(target, ModNetworking.VISIT_REQUEST, buf);
   }

   private static void sendVisitDenied(ServerPlayerEntity target, String ownerName, String reasonKey) {
      PacketByteBuf buf = ModNetworking.createBuf();
      buf.writeString(ownerName);
      buf.writeString(reasonKey);
      ServerPlayNetworking.send(target, ModNetworking.VISIT_DENIED, buf);
   }

   private static void sendPlayerListSync(ServerPlayerEntity player, List<String> playerNames) {
      PacketByteBuf buf = ModNetworking.createBuf();
      buf.writeVarInt(playerNames.size());
      for (String name : playerNames) buf.writeString(name);
      ServerPlayNetworking.send(player, ModNetworking.SYNC_PLAYER_LIST, buf);
   }

   private static void syncPortalColorToClient(ServerPlayerEntity player, PortalColor color, String playerName) {
      PacketByteBuf buf = ModNetworking.createBuf();
      buf.writeString(color.getName());
      buf.writeString(playerName);
      ServerPlayNetworking.send(player, ModNetworking.PORTAL_COLOR_CHANGED, buf);
   }

   private static void sendOpenGui(ServerPlayerEntity player) {
      PacketByteBuf buf = ModNetworking.createBuf();
      ServerPlayNetworking.send(player, ModNetworking.OPEN_GUI, buf);
   }
}
