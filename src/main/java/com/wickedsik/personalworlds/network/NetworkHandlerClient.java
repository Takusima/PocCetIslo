package com.wickedsik.personalworlds.network;

import com.wickedsik.personalworlds.PersonalWorldsMod;
import com.wickedsik.personalworlds.client.screen.PocketDimensionScreen;
import com.wickedsik.personalworlds.client.screen.VisitRequestScreen;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Обработчик клиентских сетевых пакетов.
 */
public final class NetworkHandlerClient {
   private static List<String> onlinePlayers = new java.util.ArrayList<>();
   private static String currentVisitRequest = null;

   private NetworkHandlerClient() {}

   public static void handleVisitRequest(String requesterName) {
      PersonalWorldsMod.LOGGER.info("Received visit request from: {}", requesterName);
      currentVisitRequest = requesterName;
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null) {
         client.execute(() -> client.setScreen(new VisitRequestScreen(requesterName)));
      }
   }

   public static void handleVisitDenied(String ownerName, String reasonKey) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null) {
         Text message = Text.translatable("pocketislands.gui.visit_denied", ownerName).formatted(Formatting.RED);
         client.execute(() -> client.player.sendMessage(message, false));
      }
   }

   public static void handleTeleportConfirm() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null) {
         Text message = Text.translatable("pocketislands.gui.teleport_confirm").formatted(Formatting.GREEN);
         client.execute(() -> client.player.sendMessage(message, false));
      }
   }

   public static void handlePlayerListSync(List<String> playerNames) {
      onlinePlayers = new java.util.ArrayList<>(playerNames);
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.screen instanceof PocketDimensionScreen screen) {
         client.execute(() -> screen.updatePlayerList(onlinePlayers));
      }
   }

   public static void handlePortalColorChanged(String colorName, String playerName) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null) {
         Text message = Text.translatable("pocketislands.gui.color_changed_broadcast", playerName,
            Text.translatable("pocketislands.portal.color." + colorName)).formatted(Formatting.LIGHT_PURPLE);
         client.execute(() -> client.player.sendMessage(message, false));
      }
   }

   public static void handleCommandResult(boolean success, String messageKey) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null) {
         Formatting color = success ? Formatting.GREEN : Formatting.RED;
         Text message = Text.translatable(messageKey).formatted(color);
         client.execute(() -> client.player.sendMessage(message, false));
      }
   }

   public static List<String> getOnlinePlayers() { return onlinePlayers; }
   public static String getCurrentVisitRequest() { return currentVisitRequest; }
   public static void clearCurrentVisitRequest() { currentVisitRequest = null; }
}
