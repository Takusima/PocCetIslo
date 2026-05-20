package com.wickedsik.personalworlds.command.executor;

import com.wickedsik.personalworlds.command.CommandResult;
import com.wickedsik.personalworlds.command.service.PlayerLookupService;
import com.wickedsik.personalworlds.command.service.TeleportHelper;
import com.wickedsik.personalworlds.compat.EntityCompat;
import com.wickedsik.personalworlds.compat.TeleportCompat;
import com.wickedsik.personalworlds.config.ModConfig;
import com.wickedsik.personalworlds.dimension.DimensionManager;
import com.wickedsik.personalworlds.dimension.DimensionRegistry;
import com.wickedsik.personalworlds.dimension.PlayerDimensionData;
import com.wickedsik.personalworlds.player.PlayerDataManager;
import com.wickedsik.personalworlds.player.ReturnData;
import com.wickedsik.personalworlds.portal.PortalOwnershipManager;
import com.wickedsik.personalworlds.registry.ModBlocks;
import com.wickedsik.personalworlds.registry.ModItems;
import com.wickedsik.personalworlds.util.VisualEffects;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Админ-команды: list, info, delete, tp, reload.
 */
public class AdminCommandExecutor {
   private final PlayerLookupService playerLookup;
   private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");

   public AdminCommandExecutor(PlayerLookupService playerLookup) {
      this.playerLookup = playerLookup;
   }

   public void list(ServerCommandSource source) {
      MinecraftServer server = source.getServer();
      DimensionRegistry registry = DimensionRegistry.get(server);
      var dimensions = registry.getAllDimensions();

      if (dimensions.isEmpty()) {
         source.sendFeedback(() -> Text.translatable("pocketislands.command.list.empty").formatted(Formatting.DARK_GRAY), false);
         return;
      }

      MutableText header = Text.translatable("pocketislands.command.list.header").formatted(Formatting.WHITE);
      source.sendFeedback(() -> header, false);

      for (var entry : dimensions.entrySet()) {
         PlayerDimensionData data = entry.getValue();
         boolean loaded = DimensionManager.isDimensionLoaded(data.ownerUuid());
         int playerCount = loaded ? DimensionManager.getLoadedDimension(data.ownerUuid()).getPlayers().size() : 0;

         MutableText line = Text.literal("• ")
            .append(Text.literal(data.ownerName()).formatted(loaded ? Formatting.GREEN : Formatting.DARK_GRAY))
            .append(Text.literal(" (").formatted(Formatting.YELLOW))
            .append(Text.literal(data.generatorType().name()).formatted(Formatting.BOLD))
            .append(Text.literal(") ").formatted(Formatting.YELLOW));

         if (loaded) {
            line.append(Text.literal("[").formatted(Formatting.GREEN))
               .append(Text.translatable("pocketislands.command.list.loaded").formatted(Formatting.GREEN));
            if (playerCount > 0) {
               line.append(Text.literal(", " + playerCount + " player" + (playerCount > 1 ? "s" : "")).formatted(Formatting.GRAY));
            }
            line.append(Text.literal("]").formatted(Formatting.GREEN));
         } else {
            line.append(Text.translatable("pocketislands.command.list.unloaded").formatted(Formatting.DARK_GRAY));
         }

         source.sendFeedback(() -> line, false);
      }
   }

   public CommandResult info(ServerCommandSource source, String playerName) {
      MinecraftServer server = source.getServer();
      var ref = playerLookup.findPlayer(server, playerName);
      if (ref.isEmpty()) return CommandResult.error(Text.translatable("pocketislands.command.error.player_not_found", playerName));

      DimensionRegistry registry = DimensionRegistry.get(server);
      if (!registry.hasDimension(ref.uuid())) {
         return CommandResult.error(Text.translatable("pocketislands.command.error.no_dimension", playerName));
      }

      PlayerDimensionData data = registry.getDimensionData(ref.uuid()).get();
      String createdStr = dateFormat.format(new Date(data.createdAt()));
      boolean loaded = DimensionManager.isDimensionLoaded(data.ownerUuid());
      int playerCount = loaded ? DimensionManager.getLoadedDimension(data.ownerUuid()).getPlayers().size() : 0;
      int inviteCount = PlayerDataManager.get(server).getSentInvitations(ref.uuid()).size();
      BlockPos spawn = data.spawnPoint();

      source.sendFeedback(() -> Text.translatable("pocketislands.command.info.header", data.ownerName()).formatted(Formatting.WHITE), false);
      source.sendFeedback(() -> Text.translatable("pocketislands.command.info.owner", data.ownerName()), false);
      source.sendFeedback(() -> Text.translatable("pocketislands.command.info.created", createdStr), false);
      source.sendFeedback(() -> Text.translatable("pocketislands.command.info.world_type", data.generatorType().name()), false);

      if (loaded) {
         source.sendFeedback(() -> Text.translatable("pocketislands.command.info.status_loaded", playerCount), false);
      } else {
         source.sendFeedback(() -> Text.translatable("pocketislands.command.info.status_unloaded"), false);
      }

      source.sendFeedback(() -> Text.translatable("pocketislands.command.info.invitations", inviteCount), false);
      source.sendFeedback(() -> Text.translatable("pocketislands.command.info.spawn", spawn.getX(), spawn.getY(), spawn.getZ()), false);

      return CommandResult.silent();
   }

   public CommandResult deletePrompt(ServerCommandSource source, String playerName) {
      MinecraftServer server = source.getServer();
      var ref = playerLookup.findPlayer(server, playerName);
      if (ref.isEmpty()) return CommandResult.error(Text.translatable("pocketislands.command.error.player_not_found", playerName));

      DimensionRegistry registry = DimensionRegistry.get(server);
      if (!registry.hasDimension(ref.uuid())) {
         return CommandResult.error(Text.translatable("pocketislands.command.error.no_dimension", playerName));
      }

      PlayerDimensionData data = registry.getDimensionData(ref.uuid()).get();
      boolean loaded = DimensionManager.isDimensionLoaded(data.ownerUuid());
      int playerCount = loaded ? DimensionManager.getLoadedDimension(data.ownerUuid()).getPlayers().size() : 0;

      source.sendFeedback(() -> Text.translatable("pocketislands.command.delete.warning", data.ownerName()), false);

      if (playerCount > 0) {
         source.sendFeedback(() -> Text.translatable("pocketislands.command.delete.players_ejected", playerCount).formatted(Formatting.WHITE), false);
      }

      source.sendFeedback(() -> Text.translatable("pocketislands.command.delete.confirm", data.ownerName()), false);

      return CommandResult.silent();
   }

   public CommandResult deleteConfirm(ServerCommandSource source, String playerName) {
      MinecraftServer server = source.getServer();
      var ref = playerLookup.findPlayer(server, playerName);
      if (ref.isEmpty()) return CommandResult.error(Text.translatable("pocketislands.command.error.player_not_found", playerName));

      UUID ownerUuid = ref.uuid();
      DimensionRegistry registry = DimensionRegistry.get(server);
      if (!registry.hasDimension(ownerUuid)) {
         return CommandResult.error(Text.translatable("pocketislands.command.error.no_dimension", playerName));
      }

      PlayerDimensionData data = registry.getDimensionData(ownerUuid).get();

      // Выгоняем всех игроков из измерения
      if (DimensionManager.isDimensionLoaded(ownerUuid)) {
         ServerWorld dimension = DimensionManager.getLoadedDimension(ownerUuid);
         ServerWorld overworld = server.getOverworld();
         for (ServerPlayerEntity player : new ArrayList<>(dimension.getPlayers())) {
            TeleportCompat.teleport(player, overworld, TeleportHelper.toWorldSpawn(overworld, player));
            player.sendMessage(Text.translatable("pocketislands.message.admin_ejected").formatted(Formatting.RED), false);
         }
      }

      // Удаляем порталы
      PortalOwnershipManager ownershipManager = PortalOwnershipManager.get(server);
      int portalsCleared = ownershipManager.clearPortalsOwnedBy(ownerUuid);

      // Удаляем данные
      registry.removeDimension(ownerUuid);
      DimensionManager.deleteDimension(server, ownerUuid);

      PlayerDataManager dataManager = PlayerDataManager.get(server);
      dataManager.clearAllInvitationsFor(ownerUuid);

      if (portalsCleared > 0) {
         source.sendFeedback(() -> Text.translatable("pocketislands.command.info.cleared_portals", portalsCleared).formatted(Formatting.DARK_GRAY), false);
      }

      return CommandResult.successBroadcast(
         Text.translatable("pocketislands.command.info.deleted", data.ownerName()).formatted(Formatting.GREEN));
   }

   public CommandResult teleport(ServerPlayerEntity admin, String playerName) {
      MinecraftServer server = admin.getServer();
      var ref = playerLookup.findPlayer(server, playerName);
      if (ref.isEmpty()) return CommandResult.error(Text.translatable("pocketislands.command.error.player_not_found", playerName));

      DimensionRegistry registry = DimensionRegistry.get(server);
      if (!registry.hasDimension(ref.uuid())) {
         return CommandResult.error(Text.translatable("pocketislands.command.error.no_dimension", playerName));
      }

      PlayerDimensionData data = registry.getDimensionData(ref.uuid()).get();
      ServerWorld dimension = DimensionManager.getOrCreatePlayerDimension(server, ref.uuid(), data.ownerName(), data.generatorType(), data.portalTypeIndex());
      TeleportCompat.teleport(admin, dimension, TeleportHelper.toBlockPos(dimension, data.spawnPoint(), admin));

      return CommandResult.successBroadcast(Text.translatable("pocketislands.command.info.teleported", data.ownerName()));
   }

   public CommandResult reload(ServerCommandSource source) {
      ModConfig.reload();
      return CommandResult.successBroadcast(Text.translatable("pocketislands.command.info.reloaded"));
   }
}
