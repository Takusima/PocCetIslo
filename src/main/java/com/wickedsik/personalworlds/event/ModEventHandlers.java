package com.wickedsik.personalworlds.event;

import com.wickedsik.personalworlds.PersonalWorldsMod;
import com.wickedsik.personalworlds.compat.EntityCompat;
import com.wickedsik.personalworlds.config.ModConfig;
import com.wickedsik.personalworlds.dimension.DimensionManager;
import com.wickedsik.personalworlds.dimension.DimensionRecoveryScanner;
import com.wickedsik.personalworlds.dimension.DimensionRegistry;
import com.wickedsik.personalworlds.portal.ConcurrentPortalGuard;
import com.wickedsik.personalworlds.portal.PortalHelper;
import com.wickedsik.personalworlds.registry.ModBlocks;
import com.wickedsik.personalworlds.recovery.CrashRecoveryHandler;
import com.wickedsik.personalworlds.util.PerformanceMonitor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Обработчики событий сервера.
 */
public class ModEventHandlers {
   private static int tickCounter = 0;
   private static final int UNLOAD_CHECK_INTERVAL = 600;
   private static int guardCleanupCounter = 0;
   private static final int GUARD_CLEANUP_INTERVAL = 200;

   public static void register() {
      ServerLifecycleEvents.SERVER_STARTED.register(ModEventHandlers::onServerStarted);
      ServerLifecycleEvents.SERVER_STOPPING.register(ModEventHandlers::onServerStopping);
      ServerTickEvents.END_SERVER_TICK.register(ModEventHandlers::onServerTick);
      UseBlockCallback.EVENT.register(ModEventHandlers::onUseBlock);
      ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
         CrashRecoveryHandler.onPlayerJoin(handler.getPlayer()));
      ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
         ConcurrentPortalGuard.forceRelease(handler.getPlayer().getUuid()));
      PersonalWorldsMod.LOGGER.info("Event handlers registered");
   }

   private static void onServerStarted(MinecraftServer server) {
      PersonalWorldsMod.LOGGER.info("Server started - scanning for orphaned dimensions");
      DimensionRecoveryScanner.scanAndRecover(server);
      PersonalWorldsMod.LOGGER.info("Restoring player dimensions from registry");
      DimensionRegistry.get(server).restoreAllDimensions(server);
   }

   private static void onServerStopping(MinecraftServer server) {
      PersonalWorldsMod.LOGGER.info("Server stopping - unloading all dimensions");
      DimensionManager.unloadAll();
   }

   private static void onServerTick(MinecraftServer server) {
      checkVoidFalling(server);
      ++tickCounter;
      if (tickCounter >= UNLOAD_CHECK_INTERVAL) {
         tickCounter = 0;
         DimensionManager.unloadEmptyDimensions();
         PerformanceMonitor.logStatus(server);
      }
      ++guardCleanupCounter;
      if (guardCleanupCounter >= GUARD_CLEANUP_INTERVAL) {
         guardCleanupCounter = 0;
         ConcurrentPortalGuard.cleanup();
      }
   }

   private static void checkVoidFalling(MinecraftServer server) {
      for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
         ServerWorld world = EntityCompat.getServerWorld(player);
         if (PortalHelper.isInPersonalDimension(world) && player.getY() <= 0.0) {
            player.fallDistance = 0.0F;
            PortalHelper.teleportToReturnPosition(player, server);
            player.sendMessage(Text.translatable("pocketislands.void_ejection"), false);
         }
      }
   }

   private static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
      if (world.isClient()) return ActionResult.PASS;
      if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;

      BlockPos clickedPos = hitResult.getBlockPos();
      BlockState clickedState = world.getBlockState(clickedPos);
      boolean clickedOnFrame = false;

      for (int i = 0; i < ModConfig.get().portalTypes.size(); ++i) {
         if (clickedState.getBlock() == ModBlocks.getFrameBlock(i)) {
            clickedOnFrame = true;
            break;
         }
      }

      BlockPos targetPos;
      if (clickedOnFrame) {
         targetPos = clickedPos.offset(hitResult.getSide());
      } else {
         targetPos = clickedPos;
      }

      if (!world.getBlockState(targetPos).isAir()) {
         return ActionResult.PASS;
      }

      return PortalHelper.tryActivatePortal(world, targetPos, serverPlayer, player.getStackInHand(hand).getItem())
         ? ActionResult.SUCCESS : ActionResult.PASS;
   }
}
