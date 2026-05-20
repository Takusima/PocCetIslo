package com.wickedsik.personalworlds.player;

import com.wickedsik.personalworlds.PersonalWorldsMod;
import com.wickedsik.personalworlds.compat.EntityCompat;
import com.wickedsik.personalworlds.compat.TeleportCompat;
import com.wickedsik.personalworlds.compat.TextCompat;
import com.wickedsik.personalworlds.compat.WorldCompat;
import com.wickedsik.personalworlds.config.ModConfig;
import com.wickedsik.personalworlds.dimension.DimensionRegistry;
import com.wickedsik.personalworlds.dimension.PlayerDimensionData;
import com.wickedsik.personalworlds.portal.PortalHelper;
import com.wickedsik.personalworlds.util.VisualEffects;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Управление приглашениями на посещение карманных измерений.
 */
public class InvitationManager {
   // Проверяет, может ли гость посетить измерение владельца
   public static boolean canVisit(MinecraftServer server, UUID visitorUuid, UUID ownerUuid) {
      if (visitorUuid.equals(ownerUuid)) return true;
      return PlayerDataManager.get(server).hasInvitationFrom(visitorUuid, ownerUuid);
   }

   // Проверяет право на посещение с подробной причиной отказа
   public static VisitDenialReason checkVisitAccess(MinecraftServer server, ServerPlayerEntity visitor, UUID ownerUuid) {
      UUID visitorUuid = visitor.getUuid();
      if (visitor.hasPermissionLevel(2)) return VisitDenialReason.ALLOWED;
      if (visitorUuid.equals(ownerUuid)) return VisitDenialReason.ALLOWED;
      PlayerDataManager dataManager = PlayerDataManager.get(server);
      if (!dataManager.hasInvitationFrom(visitorUuid, ownerUuid)) return VisitDenialReason.NOT_INVITED;
      if (ModConfig.get().enableAlwaysWelcome && dataManager.isAlwaysWelcome(ownerUuid, visitorUuid)) return VisitDenialReason.ALLOWED;
      ServerPlayerEntity host = server.getPlayerManager().getPlayer(ownerUuid);
      if (host == null) return VisitDenialReason.HOST_OFFLINE;
      if (!ModConfig.get().allowVisitWhenHostNotHome && !isPlayerHome(host, ownerUuid)) return VisitDenialReason.HOST_NOT_HOME;
      return VisitDenialReason.ALLOWED;
   }

   private static boolean isPlayerHome(ServerPlayerEntity player, UUID playerUuid) {
      ServerWorld world = EntityCompat.getServerWorld(player);
      if (!PortalHelper.isInPersonalDimension(world)) return false;
      Optional<UUID> dimensionOwner = PortalHelper.getDimensionOwner(world);
      return dimensionOwner.isPresent() && dimensionOwner.get().equals(playerUuid);
   }

   // Уведомляет владельца о попытке посещения
   public static void notifyHostOfVisitAttempt(MinecraftServer server, UUID ownerUuid, String visitorName, VisitDenialReason reason) {
      if (reason == VisitDenialReason.HOST_NOT_HOME) {
         ServerPlayerEntity host = server.getPlayerManager().getPlayer(ownerUuid);
         if (host != null) {
            host.sendMessage(Text.translatable("pocketislands.visit.attempted.not_home", visitorName).formatted(Formatting.YELLOW), false);
         }
      }
   }

   // Приглашает игрока
   public static boolean invite(MinecraftServer server, ServerPlayerEntity owner, ServerPlayerEntity guest) {
      return invite(server, owner, guest, false);
   }

   public static boolean invite(MinecraftServer server, ServerPlayerEntity owner, ServerPlayerEntity guest, boolean alwaysWelcome) {
      UUID ownerUuid = owner.getUuid();
      UUID guestUuid = guest.getUuid();
      if (ownerUuid.equals(guestUuid)) {
         owner.sendMessage(Text.translatable("pocketislands.message.cannot_invite_self").formatted(Formatting.RED), false);
         return false;
      }
      PlayerDataManager dataManager = PlayerDataManager.get(server);
      boolean added = dataManager.addInvitation(ownerUuid, owner.getName().getString(), guestUuid, alwaysWelcome);
      if (added) {
         String messageKey = alwaysWelcome ? "pocketislands.command.invited_always_welcome" : "pocketislands.message.invite_sent";
         owner.sendMessage(Text.translatable(messageKey, guest.getName().getString()), false);
         guest.sendMessage(Text.translatable("pocketislands.message.invite_received", owner.getName().getString()), false);
         VisualEffects.playInvitationSentEffect(owner);
         VisualEffects.playInvitationReceivedEffect(guest);
      } else {
         owner.sendMessage(Text.translatable("pocketislands.message.already_invited", guest.getName().getString()), false);
      }
      return added;
   }

   // Отзывает приглашение
   public static boolean uninvite(MinecraftServer server, ServerPlayerEntity owner, UUID guestUuid, String guestName) {
      UUID ownerUuid = owner.getUuid();
      PlayerDataManager dataManager = PlayerDataManager.get(server);
      boolean removed = dataManager.removeInvitation(ownerUuid, guestUuid);
      if (removed) {
         owner.sendMessage(Text.translatable("pocketislands.message.invite_revoked", guestName), false);
         ServerPlayerEntity guest = server.getPlayerManager().getPlayer(guestUuid);
         if (guest != null) handleRevocationWhileVisiting(server, owner, guest);
      } else {
         owner.sendMessage(Text.translatable("pocketislands.message.not_invited", guestName), false);
      }
      return removed;
   }

   private static void handleRevocationWhileVisiting(MinecraftServer server, ServerPlayerEntity owner, ServerPlayerEntity guest) {
      ServerWorld guestWorld = EntityCompat.getServerWorld(guest);
      if (PortalHelper.isInPersonalDimension(guestWorld)) {
         String dimPath = guestWorld.getRegistryKey().getValue().getPath();
         String ownerDimPath = "pw_" + owner.getUuid().toString();
         if (dimPath.equals(ownerDimPath)) {
            VisualEffects.playInvitationRevokedEffect(guest);
            guest.sendMessage(Text.translatable("pocketislands.message.ejected").formatted(Formatting.GOLD), false);
            PlayerDataManager dataManager = PlayerDataManager.get(server);
            Optional<ReturnData> returnDataOpt = dataManager.getReturnData(guest.getUuid());
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
               dataManager.clearReturnData(guest.getUuid());
            } else {
               targetWorld = server.getOverworld();
               targetPos = Vec3d.ofCenter(WorldCompat.getSpawnPos(targetWorld));
               yaw = guest.getYaw();
               pitch = guest.getPitch();
            }
            TeleportCompat.teleport(guest, targetWorld, targetPos, yaw, pitch);
         }
      }
   }

   // Показывает список приглашений игроку
   public static void showInvitations(ServerPlayerEntity player) {
      MinecraftServer server = EntityCompat.getServer(server);
      if (server == null) return;
      PlayerDataManager dataManager = PlayerDataManager.get(server);
      UUID playerUuid = player.getUuid();
      player.sendMessage(Text.translatable("pocketislands.invitations.header").formatted(Formatting.GOLD), false);
      Set<UUID> sent = dataManager.getSentInvitations(playerUuid);
      player.sendMessage(Text.translatable("pocketislands.invitations.sent.header").formatted(Formatting.GREEN), false);
      if (sent.isEmpty()) {
         player.sendMessage(Text.translatable("pocketislands.invitations.sent.none").formatted(Formatting.YELLOW), false);
      } else {
         boolean alwaysWelcomeEnabled = ModConfig.get().enableAlwaysWelcome;
         for (UUID guestUuid : sent) {
            String guestName = getPlayerName(server, guestUuid);
            MutableText entryText = Text.literal(guestName).formatted(Formatting.WHITE);
            if (alwaysWelcomeEnabled) {
               boolean isAlwaysWelcome = dataManager.isAlwaysWelcome(playerUuid, guestUuid);
               String toggleIcon = isAlwaysWelcome ? "★" : "☆";
               Formatting toggleColor = isAlwaysWelcome ? Formatting.GREEN : Formatting.YELLOW;
               MutableText toggleButton = Text.literal("[" + toggleIcon + "]").formatted(toggleColor)
                  .styled(style -> style.withClickEvent(TextCompat.runCommand("/pi togglewelcome " + guestName))
                     .withHoverEvent(TextCompat.showText(Text.translatable(isAlwaysWelcome ? "pocketislands.invitations.sent.toggle_off_tooltip" : "pocketislands.invitations.sent.toggle_on_tooltip"))));
               entryText = entryText.append(" ").append(toggleButton);
            }
            MutableText revokeButton = Text.translatable("pocketislands.invitations.sent.revoke_button").formatted(Formatting.RED)
               .styled(style -> style.withClickEvent(TextCompat.runCommand("/pw uninvite " + guestName))
                  .withHoverEvent(TextCompat.showText(Text.translatable("pocketislands.invitations.sent.revoke_tooltip"))));
            entryText = entryText.append(" ").append(revokeButton);
            player.sendMessage(Text.translatable("pocketislands.invitations.sent.entry", entryText), false);
         }
      }
      List<InvitationData> received = dataManager.getReceivedInvitations(playerUuid);
      player.sendMessage(Text.empty(), false);
      player.sendMessage(Text.translatable("pocketislands.invitations.received.header").formatted(Formatting.LIGHT_PURPLE), false);
      if (received.isEmpty()) {
         player.sendMessage(Text.translatable("pocketislands.invitations.received.none").formatted(Formatting.YELLOW), false);
      } else {
         for (InvitationData inv : received) {
            MutableText worldLink = Text.translatable("pocketislands.invitations.received.world_name", inv.ownerName()).formatted(Formatting.WHITE)
               .styled(style -> style.withClickEvent(TextCompat.runCommand("/pw go " + inv.ownerName()))
                  .withHoverEvent(TextCompat.showText(Text.translatable("pocketislands.invitations.received.visit_tooltip", inv.ownerName()))));
            player.sendMessage(Text.translatable("pocketislands.invitations.received.entry", worldLink), false);
         }
      }
   }

   private static String getPlayerName(MinecraftServer server, UUID playerUuid) {
      ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
      if (player != null) return player.getName().getString();
      DimensionRegistry registry = DimensionRegistry.get(server);
      return registry.getDimensionData(playerUuid)
         .map(PlayerDimensionData::ownerName)
         .orElse(playerUuid.toString().substring(0, 8));
   }
}
