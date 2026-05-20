package com.wickedsik.personalworlds.dimension;

import com.wickedsik.personalworlds.PersonalWorldsMod;
import com.wickedsik.personalworlds.compat.GameRulesCompat;
import com.wickedsik.personalworlds.compat.IdentifierCompat;
import com.wickedsik.personalworlds.config.ModConfig;
import com.wickedsik.personalworlds.dimension.generator.VoidIslandChunkGenerator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

/**
 * Управление персональными измерениями: создание, загрузка, выгрузка, удаление.
 */
public class DimensionManager {
   private static final Map<UUID, RuntimeWorldHandle> activeHandles = new HashMap<>();

   public static ServerWorld getOrCreatePlayerDimension(MinecraftServer server, UUID playerUuid, String playerName, WorldGenType genType, int portalTypeIndex) {
      Fantasy fantasy = Fantasy.get(server);
      Identifier dimId = IdentifierCompat.modId("pw_" + playerUuid.toString().replace("-", ""));

      if (activeHandles.containsKey(playerUuid)) {
         return activeHandles.get(playerUuid).asWorld();
      }

      RuntimeWorldConfig config = createWorldConfig(server, genType, playerUuid, portalTypeIndex);
      RuntimeWorldHandle handle = fantasy.getOrOpenPersistentWorld(dimId, config);
      activeHandles.put(playerUuid, handle);

      DimensionRegistry registry = DimensionRegistry.get(server);
      if (!registry.hasDimension(playerUuid)) {
         PlayerDimensionData data = new PlayerDimensionData(playerUuid, playerName, dimId, System.currentTimeMillis(), getSpawnPoint(genType), genType, portalTypeIndex);
         registry.registerDimension(data);
      }

      DimensionMetadataFile.write(server, registry.getDimensionData(playerUuid).orElseThrow());
      return handle.asWorld();
   }

   public static void loadExistingDimension(MinecraftServer server, PlayerDimensionData data) {
      Fantasy fantasy = Fantasy.get(server);
      if (!activeHandles.containsKey(data.ownerUuid())) {
         RuntimeWorldConfig config = createWorldConfig(server, data.generatorType(), data.ownerUuid(), data.portalTypeIndex());
         RuntimeWorldHandle handle = fantasy.getOrOpenPersistentWorld(data.dimensionId(), config);
         activeHandles.put(data.ownerUuid(), handle);
      }
   }

   public static boolean unloadIfEmpty(UUID playerUuid) {
      RuntimeWorldHandle handle = activeHandles.get(playerUuid);
      if (handle != null && handle.asWorld().getPlayers().isEmpty()) {
         handle.unload();
         activeHandles.remove(playerUuid);
         return true;
      }
      return false;
   }

   public static boolean deleteDimension(MinecraftServer server, UUID playerUuid) {
      RuntimeWorldHandle handle = activeHandles.get(playerUuid);
      if (handle != null) {
         handle.delete();
         activeHandles.remove(playerUuid);
         return true;
      }
      return DimensionMetadataFile.deleteDimensionFolder(server, playerUuid);
   }

   public static void unloadEmptyDimensions() {
      activeHandles.entrySet().removeIf(entry -> {
         if (entry.getValue().asWorld().getPlayers().isEmpty()) {
            entry.getValue().unload();
            return true;
         }
         return false;
      });
   }

   public static void unloadAll() {
      activeHandles.values().forEach(RuntimeWorldHandle::unload);
      activeHandles.clear();
   }

   public static boolean isDimensionLoaded(UUID playerUuid) {
      return activeHandles.containsKey(playerUuid);
   }

   public static ServerWorld getLoadedDimension(UUID playerUuid) {
      RuntimeWorldHandle handle = activeHandles.get(playerUuid);
      return handle != null ? handle.asWorld() : null;
   }

   public static int getLoadedDimensionCount() {
      return activeHandles.size();
   }

   private static RuntimeWorldConfig createWorldConfig(MinecraftServer server, WorldGenType genType, UUID playerUuid, int portalTypeIndex) {
      RuntimeWorldConfig config = new RuntimeWorldConfig()
         .setDimensionType(DimensionTypes.OVERWORLD)
         .setSeed((long) playerUuid.hashCode())
         .setDifficulty(server.getWorld(DimensionTypes.OVERWORLD).getDifficulty())
         .setShouldTickTime(true)
         .setTimeOfDay(server.getOverworld().getTimeOfDay());
      config.setGenerator(createChunkGenerator(server, genType, portalTypeIndex));
      GameRulesCompat.applyGameRules(config, server);
      return config;
   }

   private static ChunkGenerator createChunkGenerator(MinecraftServer server, WorldGenType genType, int portalTypeIndex) {
      RegistryEntryLookup<Biome> biomeRegistry = server.getRegistryManager().getWrapperOrThrow(RegistryKeys.BIOME);
      RegistryEntry<Biome> voidBiome = biomeRegistry.getOrThrow(BiomeKeys.PLAINS);

      switch (genType) {
         case VOID:
            BlockState[] islandLayers = convertIslandLayers(portalTypeIndex);
            return new VoidIslandChunkGenerator(voidBiome.value().getBiomeSource(), islandLayers);
         case OVERWORLD:
         case FLAT:
         default:
            return server.getOverworld().getChunkManager().getChunkGenerator();
      }
   }

   private static BlockState[] convertIslandLayers(int portalTypeIndex) {
      ModConfig.PortalConfig config = ModConfig.get().portalTypes.get(portalTypeIndex);
      String[] layerIds = config.islandLayers;
      int layerCount = Math.min(layerIds.length, 5);
      if (layerCount == 0) {
         return new BlockState[]{Blocks.GRASS_BLOCK.getDefaultState()};
      }
      BlockState[] islandLayers = new BlockState[layerCount];
      for (int i = 0; i < layerCount; ++i) {
         Identifier id = IdentifierCompat.tryParse(layerIds[i]);
         Block block = id != null ? net.minecraft.registry.Registries.BLOCK.get(id) : Blocks.AIR;
         if (block == Blocks.AIR && !layerIds[i].equals("minecraft:air")) {
            block = Blocks.GRASS_BLOCK;
         }
         islandLayers[i] = block.getDefaultState();
      }
      return islandLayers;
   }

   private static BlockPos getSpawnPoint(WorldGenType genType) {
      return switch (genType) {
         case VOID -> new BlockPos(0, 65, 0);
         case OVERWORLD, FLAT -> new BlockPos(0, 64, 0);
      };
   }
}
