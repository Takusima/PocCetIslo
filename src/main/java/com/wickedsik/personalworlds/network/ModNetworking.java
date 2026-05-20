package com.wickedsik.personalworlds.network;

import com.wickedsik.personalworlds.PersonalWorldsMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * Центральный класс для регистрации и обработки сетевых пакетов.
 */
public final class ModNetworking {
   public static final Identifier CHANGE_PORTAL_COLOR = new Identifier(PersonalWorldsMod.MOD_ID, "change_portal_color");
   public static final Identifier SEND_INVITATION_REQUEST = new Identifier(PersonalWorldsMod.MOD_ID, "send_invitation_request");
   public static final Identifier INVITATION_RESPONSE = new Identifier(PersonalWorldsMod.MOD_ID, "invitation_response");
   public static final Identifier VISIT_REQUEST = new Identifier(PersonalWorldsMod.MOD_ID, "visit_request");
   public static final Identifier VISIT_DENIED = new Identifier(PersonalWorldsMod.MOD_ID, "visit_denied");
   public static final Identifier TELEPORT_CONFIRM = new Identifier(PersonalWorldsMod.MOD_ID, "teleport_confirm");
   public static final Identifier OPEN_GUI = new Identifier(PersonalWorldsMod.MOD_ID, "open_gui");
   public static final Identifier SYNC_PLAYER_LIST = new Identifier(PersonalWorldsMod.MOD_ID, "sync_player_list");
   public static final Identifier PORTAL_COLOR_CHANGED = new Identifier(PersonalWorldsMod.MOD_ID, "portal_color_changed");
   public static final Identifier COMMAND_PRUNUS = new Identifier(PersonalWorldsMod.MOD_ID, "command_prunus");
   public static final Identifier COMMAND_MALUS = new Identifier(PersonalWorldsMod.MOD_ID, "command_malus");
   public static final Identifier COMMAND_RESULT = new Identifier(PersonalWorldsMod.MOD_ID, "command_result");

   private ModNetworking() {}

   public static void registerServerReceivers() {
      PersonalWorldsMod.LOGGER.info("Registering server network receivers...");

      ServerPlayNetworking.registerGlobalReceiver(CHANGE_PORTAL_COLOR, (server, player, handler, buf, responseSender) -> {
         String colorName = buf.readString();
         server.execute(() -> NetworkHandler.handleChangePortalColor(player, colorName));
      });

      ServerPlayNetworking.registerGlobalReceiver(SEND_INVITATION_REQUEST, (server, player, handler, buf, responseSender) -> {
         String targetName = buf.readString();
         server.execute(() -> NetworkHandler.handleInvitationRequest(player, targetName));
      });

      ServerPlayNetworking.registerGlobalReceiver(INVITATION_RESPONSE, (server, player, handler, buf, responseSender) -> {
         String senderName = buf.readString();
         boolean accepted = buf.readBoolean();
         server.execute(() -> NetworkHandler.handleInvitationResponse(player, senderName, accepted));
      });

      ServerPlayNetworking.registerGlobalReceiver(OPEN_GUI, (server, player, handler, buf, responseSender) -> {
         server.execute(() -> NetworkHandler.handleOpenGui(player));
      });

      ServerPlayNetworking.registerGlobalReceiver(COMMAND_PRUNUS, (server, player, handler, buf, responseSender) -> {
         server.execute(() -> NetworkHandler.handlePrunusCommand(player));
      });

      ServerPlayNetworking.registerGlobalReceiver(COMMAND_MALUS, (server, player, handler, buf, responseSender) -> {
         server.execute(() -> NetworkHandler.handleMalusCommand(player));
      });

      PersonalWorldsMod.LOGGER.info("Server network receivers registered successfully");
   }

   public static void registerClientReceivers() {
      PersonalWorldsMod.LOGGER.info("Registering client network receivers...");

      ClientPlayNetworking.registerGlobalReceiver(VISIT_REQUEST, (client, handler, buf, responseSender) -> {
         String requesterName = buf.readString();
         client.execute(() -> NetworkHandlerClient.handleVisitRequest(requesterName));
      });

      ClientPlayNetworking.registerGlobalReceiver(VISIT_DENIED, (client, handler, buf, responseSender) -> {
         String ownerName = buf.readString();
         String reason = buf.readString();
         client.execute(() -> NetworkHandlerClient.handleVisitDenied(ownerName, reason));
      });

      ClientPlayNetworking.registerGlobalReceiver(TELEPORT_CONFIRM, (client, handler, buf, responseSender) -> {
         client.execute(() -> NetworkHandlerClient.handleTeleportConfirm());
      });

      ClientPlayNetworking.registerGlobalReceiver(SYNC_PLAYER_LIST, (client, handler, buf, responseSender) -> {
         int count = buf.readVarInt();
         java.util.List<String> playerNames = new java.util.ArrayList<>();
         for (int i = 0; i < count; i++) playerNames.add(buf.readString());
         client.execute(() -> NetworkHandlerClient.handlePlayerListSync(playerNames));
      });

      ClientPlayNetworking.registerGlobalReceiver(PORTAL_COLOR_CHANGED, (client, handler, buf, responseSender) -> {
         String colorName = buf.readString();
         String playerName = buf.readString();
         client.execute(() -> NetworkHandlerClient.handlePortalColorChanged(colorName, playerName));
      });

      ClientPlayNetworking.registerGlobalReceiver(COMMAND_RESULT, (client, handler, buf, responseSender) -> {
         boolean success = buf.readBoolean();
         String message = buf.readString();
         client.execute(() -> NetworkHandlerClient.handleCommandResult(success, message));
      });

      PersonalWorldsMod.LOGGER.info("Client network receivers registered successfully");
   }

   public static void sendToServer(Identifier channel, PacketByteBuf buf) {
      ClientPlayNetworking.send(channel, buf);
   }

   public static PacketByteBuf createBuf() {
      return PacketByteBufs.create();
   }
}
