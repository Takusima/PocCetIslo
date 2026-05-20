package com.wickedsik.personalworlds.portal;

import com.wickedsik.personalworlds.PersonalWorldsMod;
import com.wickedsik.personalworlds.compat.*;
import com.wickedsik.personalworlds.config.ModConfig;
import com.wickedsik.personalworlds.dimension.*;
import com.wickedsik.personalworlds.player.*;
import com.wickedsik.personalworlds.registry.*;
import com.wickedsik.personalworlds.util.*;
import java.util.*;
import net.minecraft.block.*;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;

/**
 * Основная логика портала: активация, вход, телепортация.
 */
public class PortalHelper {
   private static final int PORTAL_WIDTH = 2;
   private static final int PORTAL_HEIGHT = 3;

   // Пытается активировать портал по клику на рамку
   public static boolean tryActivatePortal(World world, BlockPos clickedPos, ServerPlayerEntity player, Item activationItem) {
      if (world.isClient()) return false;
      MinecraftServer server = EntityCompat.getServer(player);
      if (server == null) return false;

      Optional<Integer> portalTypeOpt = detectPortalType(world, clickedPos, activationItem);
      if (portalTypeOpt.isEmpty()) return false;

      int portalTypeIndex = portalTypeOpt.get();
      Optional<PortalFrame> frame = detectFrame(world, clickedPos, portalTypeIndex);
      if (frame.isEmpty()) return false;

      PortalFrame portalFrame = frame.get();
      PortalColor color = ModBlocks.getPortalColor(portalTypeIndex);
      BlockState portalState = ModBlocks.PERSONAL_PORTAL.getDefaultState()
         .with(PersonalPortalBlock.AXIS, portalFrame.axis())
         .with(PersonalPortalBlock.COLOR, color);

      for (BlockPos pos : portalFrame.getInteriorPositions()) {
         world.setBlockState(pos, portalState);
      }

      PortalOwnershipManager ownershipManager = PortalOwnershipManager.get(server);
      for (BlockPos pos : portalFrame.getInteriorPositions()) {
         ownershipManager.registerPortal(world, pos, player.getUuid(), portalTypeIndex);
      }

      world.playSound(null, portalFrame.getCenter(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
      VisualEffects.playPortalActivationEffects(world, portalFrame.getCenter());
      return true;
   }

   // Обрабатывает вход игрока в портал
   public static void handlePortalEntry(ServerPlayerEntity player, BlockPos portalPos) {
      MinecraftServer server = EntityCompat.getServer(player);
      if (server == null) return;
      if (ConcurrentPortalGuard.tryAcquire(player, portalPos)) {
         try {
            ServerWorld currentWorld = EntityCompat.getServerWorld(player);
            if (isInPersonalDimension(currentWorld)) {
               teleportToReturnPosition(player, server);
            } else {
               handleForwardPortalEntry(player, server, currentWorld, portalPos);
            }
         } finally {
            ConcurrentPortalGuard.release(player, portalPos);
         }
      }
   }

   private static void handleForwardPortalEntry(ServerPlayerEntity player, MinecraftServer server, ServerWorld fromWorld, BlockPos portalPos) {
      PortalOwnershipManager ownershipManager = PortalOwnershipManager.get(server);
      Optional<UUID> portalOwnerOpt = ownershipManager.getOwner(fromWorld, portalPos);
      if (portalOwnerOpt.isEmpty()) {
         ownershipManager.registerPortal(fromWorld, portalPos, player.getUuid(), 0);
         teleportToOwnerDimension(player, server, fromWorld, player.getUuid(), 0);
         return;
      }
      UUID portalOwner = portalOwnerOpt.get();
      int portalTypeIndex = ownershipManager.getPortalType(fromWorld, portalPos).orElse(0);
      VisitDenialReason denialReason = InvitationManager.checkVisitAccess(server, player, portalOwner);
      if (denialReason.isAllowed()) {
         teleportToOwnerDimension(player, server, fromWorld, portalOwner, portalTypeIndex);
      } else {
         String ownerName = ownershipManager.getOwnerName(server, portalOwner);
         InvitationManager.notifyHostOfVisitAttempt(server, portalOwner, player.getName().getString(), denialReason);
         Text denialMessage = switch (denialReason) {
            case NOT_INVITED -> Text.translatable("pocketislands.command.error.not_invited", ownerName);
            case HOST_OFFLINE -> Text.translatable("pocketislands.visit.denied.offline", ownerName);
            case HOST_NOT_HOME -> Text.translatable("pocketislands.visit.denied.not_home", ownerName);
            default -> Text.empty();
         };
         player.sendMessage(denialMessage.copy().formatted(Formatting.RED), false);
      }
   }

   private static boolean teleportToOwnerDimension(ServerPlayerEntity player, MinecraftServer server, ServerWorld fromWorld, UUID ownerUuid, int portalTypeIndex) {
      UUID playerUuid = player.getUuid();
      boolean isOwnDimension = playerUuid.equals(ownerUuid);
      DimensionRegistry registry = DimensionRegistry.get(server);
      Optional<PlayerDimensionData> dimDataOpt = registry.getDimensionData(ownerUuid);

      String ownerName;
      WorldGenType genType;
      if (dimDataOpt.isPresent()) {
         PlayerDimensionData dimData = dimDataOpt.get();
         ownerName = dimData.ownerName();
         genType = dimData.generatorType();
      } else {
         if (!isOwnDimension) {
            player.sendMessage(Text.literal("This portal's dimension no longer exists.").formatted(Formatting.RED), false);
            return false;
         }
         ownerName = player.getName().getString();
         genType = WorldGenType.VOID;
      }

      PlayerDataManager dataManager = PlayerDataManager.get(server);
      if (!isInPersonalDimension(fromWorld)) {
         BlockPos returnPos = player.getBlockPos().offset(player.getHorizontalFacing().getOpposite());
         ReturnData returnData = new ReturnData(fromWorld.getRegistryKey(), returnPos, player.getYaw(), player.getPitch());
         dataManager.setReturnData(playerUuid, returnData);
      }

      ServerWorld targetWorld = DimensionManager.getOrCreatePlayerDimension(server, ownerUuid, ownerName, genType, portalTypeIndex);
      BlockPos destinationPos = findExistingPortal(targetWorld)
         .map(portalPos -> findSafePositionNearPortal(targetWorld, portalPos))
         .orElseGet(() -> getOrCreateSpawnPlatform(targetWorld, genType, portalTypeIndex));
      dataManager.setCurrentPocketDimension(playerUuid, targetWorld.getRegistryKey());
      VisualEffects.playTeleportDepartureEffects(player);
      TeleportCompat.teleportToBlockPreserveRotation(player, targetWorld, destinationPos);
      VisualEffects.playTeleportArrivalEffects(player);
      VisualEffects.playDimensionEntryEffect(player);
      return true;
   }

   // Телепортирует игрока обратно в основной мир
   public static void teleportToReturnPosition(ServerPlayerEntity player, MinecraftServer server) {
      UUID playerUuid = player.getUuid();
      PlayerDataManager dataManager = PlayerDataManager.get(server);
      Optional<ReturnData> returnDataOpt = dataManager.getReturnData(playerUuid);
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
         dataManager.clearReturnData(playerUuid);
      } else {
         targetWorld = server.getOverworld();
         targetPos = Vec3d.ofCenter(WorldCompat.getSpawnPos(targetWorld));
         yaw = player.getYaw();
         pitch = player.getPitch();
      }
      dataManager.clearCurrentPocketDimension(playerUuid);
      VisualEffects.playTeleportDepartureEffects(player);
      VisualEffects.playDimensionExitEffect(player);
      TeleportCompat.teleport(player, targetWorld, targetPos, yaw, pitch);
      VisualEffects.playTeleportArrivalEffects(player);
   }

   // Проверяет, находится ли мир в персональном измерении
   public static boolean isInPersonalDimension(ServerWorld world) {
      String namespace = world.getRegistryKey().getValue().getNamespace();
      String path = world.getRegistryKey().getValue().getPath();
      return "personalworlds".equals(namespace) && path.startsWith("pw_");
   }

   // Получает владельца измерения по миру
   public static Optional<UUID> getDimensionOwner(ServerWorld world) {
      if (!isInPersonalDimension(world)) return Optional.empty();
      String path = world.getRegistryKey().getValue().getPath();
      String uuidStr = path.substring(3);
      if (uuidStr.length() == 32 && !uuidStr.contains("-")) {
         uuidStr = uuidStr.substring(0, 8) + "-" + uuidStr.substring(8, 12) + "-" + uuidStr.substring(12, 16) + "-" + uuidStr.substring(16, 20) + "-" + uuidStr.substring(20);
      }
      try { return Optional.of(UUID.fromString(uuidStr)); }
      catch (IllegalArgumentException e) { return Optional.empty(); }
   }

   // Определяет тип портала по предмету активации
   private static Optional<Integer> detectPortalType(World world, BlockPos clickedPos, Item activationItem) {
      List<ModConfig.PortalConfig> portalTypes = ModConfig.get().portalTypes;
      for (int i = 0; i < portalTypes.size(); ++i) {
         Item configItem = ModItems.getActivationItem(i);
         if (configItem == activationItem) {
            Block configFrame = ModBlocks.getFrameBlock(i);
            Optional<PortalFrame> frame = detectFrameForAxis(world, clickedPos, configFrame, Direction.Axis.X);
            if (frame.isEmpty()) frame = detectFrameForAxis(world, clickedPos, configFrame, Direction.Axis.Z);
            if (frame.isPresent()) return Optional.of(i);
         }
      }
      return Optional.empty();
   }

   // Определяет рамку портала
   public static Optional<PortalFrame> detectFrame(World world, BlockPos clickedPos, int portalTypeIndex) {
      Block frameBlock = ModBlocks.getFrameBlock(portalTypeIndex);
      Optional<PortalFrame> xFrame = detectFrameForAxis(world, clickedPos, frameBlock, Direction.Axis.X);
      return xFrame.isPresent() ? xFrame : detectFrameForAxis(world, clickedPos, frameBlock, Direction.Axis.Z);
   }

   private static Optional<PortalFrame> detectFrameForAxis(World world, BlockPos clickedPos, Block frameBlock, Direction.Axis axis) {
      Direction horizontal = axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
      BlockPos searchPos = clickedPos;
      for (int i = 0; i < 3; ++i) {
         BlockPos nextPos = searchPos.offset(horizontal);
         if (world.getBlockState(nextPos).getBlock() == frameBlock) break;
         searchPos = nextPos;
      }
      for (int i = 0; i < 4; ++i) {
         BlockPos downPos = searchPos.down();
         if (world.getBlockState(downPos).getBlock() == frameBlock) break;
         searchPos = downPos;
      }
      BlockPos bottomLeftFrame = searchPos.offset(horizontal).down();
      PortalFrame frame = new PortalFrame(bottomLeftFrame, 2, 3, axis);
      return isValidFrame(world, frame, frameBlock) ? Optional.of(frame) : Optional.empty();
   }

   private static boolean isValidFrame(World world, PortalFrame frame, Block frameBlock) {
      for (BlockPos pos : frame.getFramePositions()) {
         if (world.getBlockState(pos).getBlock() != frameBlock) return false;
      }
      for (BlockPos pos : frame.getInteriorPositions()) {
         BlockState state = world.getBlockState(pos);
         if (!state.isAir() && state.getBlock() != ModBlocks.PERSONAL_PORTAL) return false;
      }
      return true;
   }

   // Проверяет, что рамка портала целая
   public static boolean isFrameValidForPortal(World world, BlockPos portalPos, Direction.Axis axis) {
      for (int i = 0; i < ModConfig.get().portalTypes.size(); ++i) {
         Block frameBlock = ModBlocks.getFrameBlock(i);
         if (detectFrameForAxis(world, portalPos, frameBlock, axis).isPresent()) return true;
      }
      return false;
   }

   // Ищет существующий портал в мире
   private static Optional<BlockPos> findExistingPortal(ServerWorld world) {
      BlockPos center = new BlockPos(0, 64, 0);
      for (int y = -128; y <= 128; ++y) {
         for (int x = -128; x <= 128; ++x) {
            for (int z = -128; z <= 128; ++z) {
               BlockPos checkPos = center.add(x, y, z);
               if (world.getBlockState(checkPos).getBlock() == ModBlocks.PERSONAL_PORTAL) {
                  return Optional.of(checkPos);
               }
            }
         }
      }
      return Optional.empty();
   }

   // Находит безопасную позицию рядом с порталом
   private static BlockPos findSafePositionNearPortal(ServerWorld world, BlockPos portalPos) {
      BlockState portalState = world.getBlockState(portalPos);
      Direction.Axis axis = PersonalPortalBlock.getAxis(portalState);
      Direction[] checkDirections = axis == Direction.Axis.X
         ? new Direction[]{Direction.NORTH, Direction.EAST}
         : new Direction[]{Direction.SOUTH, Direction.WEST};

      BlockPos bottomPortal = portalPos;
      while (world.getBlockState(bottomPortal.down()).getBlock() == ModBlocks.PERSONAL_PORTAL) {
         bottomPortal = bottomPortal.down();
      }

      for (Direction dir : checkDirections) {
         BlockPos sidePos = bottomPortal.offset(dir);
         for (int yOffset = 0; yOffset >= -3; --yOffset) {
            BlockPos groundCheck = sidePos.add(0, yOffset - 1, 0);
            BlockPos feetPos = sidePos.add(0, yOffset, 0);
            BlockPos headPos = sidePos.add(0, yOffset + 1, 0);
            if (!world.getBlockState(groundCheck).isAir() && world.getBlockState(feetPos).isAir() && world.getBlockState(headPos).isAir()) {
               return feetPos;
            }
         }
      }
      return bottomPortal;
   }

   // Создаёт стартовую платформу если портала нет
   private static BlockPos getOrCreateSpawnPlatform(ServerWorld world, WorldGenType genType, int portalTypeIndex) {
      BlockPos spawnPos = new BlockPos(0, 65, 0);
      if (genType == WorldGenType.VOID) {
         if (world.getBlockState(spawnPos.down()).isAir()) {
            createStarterPlatform(world, new BlockPos(0, 64, 0), portalTypeIndex);
         }
      }
      return spawnPos;
   }

   private static void createStarterPlatform(ServerWorld world, BlockPos center, int portalTypeIndex) {
      BlockState platformMaterial = Blocks.GRASS_BLOCK.getDefaultState();
      ModConfig.PortalConfig config = ModConfig.get().portalTypes.get(portalTypeIndex);
      if (config.islandLayers.length > 0) {
         Identifier id = IdentifierCompat.tryParse(config.islandLayers[0]);
         Block block = id != null ? net.minecraft.registry.Registries.BLOCK.get(id) : Blocks.GRASS_BLOCK;
         if (block != Blocks.AIR || config.islandLayers[0].equals("minecraft:air")) {
            platformMaterial = block.getDefaultState();
         }
      }
      for (int x = -2; x <= 2; ++x) {
         for (int z = -2; z <= 2; ++z) {
            world.setBlockState(center.add(x, 0, z), platformMaterial);
         }
      }
      createReturnPortalFrame(world, center.add(4, 1, 0));
   }

   private static void createReturnPortalFrame(ServerWorld world, BlockPos bottomLeft) {
      Block frameBlock = ModBlocks.getFrameBlock(0);
      BlockState frameState = frameBlock.getDefaultState();
      for (int x = 0; x < 4; ++x) {
         world.setBlockState(bottomLeft.add(x, 0, 0), frameState);
         world.setBlockState(bottomLeft.add(x, 4, 0), frameState);
      }
      for (int y = 1; y < 4; ++y) {
         world.setBlockState(bottomLeft.add(0, y, 0), frameState);
         world.setBlockState(bottomLeft.add(3, y, 0), frameState);
      }
   }
}
