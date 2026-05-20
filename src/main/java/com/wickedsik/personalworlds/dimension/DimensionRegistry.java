package com.wickedsik.personalworlds.dimension;

import com.wickedsik.personalworlds.PersonalWorldsMod;
import com.wickedsik.personalworlds.compat.NbtCompat;
import com.wickedsik.personalworlds.compat.PersistentStateCompat;
import com.wickedsik.personalworlds.util.DataValidator;
import java.util.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

/**
 * Реестр всех персональных измерений.
 */
public class DimensionRegistry extends PersistentState {
   private static final String DATA_NAME = "personalworlds_registry";
   private final Map<UUID, PlayerDimensionData> dimensions = new HashMap<>();

   public void registerDimension(PlayerDimensionData data) {
      if (!DataValidator.isValidDimensionData(data)) {
         data = DataValidator.sanitizeDimensionData(data, data.ownerUuid());
      }
      this.dimensions.put(data.ownerUuid(), data);
      this.markDirty();
      PersonalWorldsMod.LOGGER.info("Registered dimension for player: {} ({})", data.ownerName(), data.ownerUuid());
   }

   public boolean hasDimension(UUID playerUuid) {
      return this.dimensions.containsKey(playerUuid);
   }

   public Optional<PlayerDimensionData> getDimensionData(UUID playerUuid) {
      return Optional.ofNullable(this.dimensions.get(playerUuid));
   }

   public Map<UUID, PlayerDimensionData> getAllDimensions() {
      return Map.copyOf(this.dimensions);
   }

   public void removeDimension(UUID playerUuid) {
      if (this.dimensions.remove(playerUuid) != null) {
         this.markDirty();
         PersonalWorldsMod.LOGGER.info("Removed dimension for player: {}", playerUuid);
      }
   }

   // Получает измерение игрока (загруженное через DimensionManager)
   public Optional<ServerWorld> getPlayerDimension(MinecraftServer server, UUID playerUuid) {
      ServerWorld loaded = DimensionManager.getLoadedDimension(playerUuid);
      if (loaded != null) return Optional.of(loaded);
      // Пробуем загрузить
      Optional<PlayerDimensionData> dataOpt = getDimensionData(playerUuid);
      if (dataOpt.isPresent()) {
         DimensionManager.loadExistingDimension(server, dataOpt.get());
         loaded = DimensionManager.getLoadedDimension(playerUuid);
         return Optional.ofNullable(loaded);
      }
      return Optional.empty();
   }

   // Восстанавливает все измерения при запуске сервера
   public void restoreAllDimensions(MinecraftServer server) {
      PersonalWorldsMod.LOGGER.info("Restoring {} player dimensions...", this.dimensions.size());
      for (PlayerDimensionData data : this.dimensions.values()) {
         try {
            DimensionManager.loadExistingDimension(server, data);
         } catch (Exception e) {
            PersonalWorldsMod.LOGGER.error("Failed to restore dimension for {}: {}", data.ownerName(), e.getMessage());
         }
      }
      PersonalWorldsMod.LOGGER.info("Dimension restoration complete!");
   }

   @Override
   public NbtCompound writeNbt(NbtCompound nbt) {
      NbtList dimensionList = new NbtList();
      for (PlayerDimensionData data : this.dimensions.values()) {
         dimensionList.add(data.toNbt());
      }
      nbt.put("Dimensions", dimensionList);
      return nbt;
   }

   public static DimensionRegistry fromNbt(NbtCompound nbt) {
      DimensionRegistry registry = new DimensionRegistry();
      NbtList dimensionList = NbtCompat.getList(nbt, "Dimensions", NbtElement.COMPOUND_TYPE);
      int skipped = 0;
      for (int i = 0; i < dimensionList.size(); ++i) {
         try {
            PlayerDimensionData data = PlayerDimensionData.fromNbt(NbtCompat.getCompound(dimensionList, i));
            if (!DataValidator.isValidDimensionData(data)) {
               PersonalWorldsMod.LOGGER.warn("Skipping invalid dimension data for {}", data.ownerUuid());
               ++skipped;
            } else {
               registry.dimensions.put(data.ownerUuid(), data);
            }
         } catch (Exception e) {
            PersonalWorldsMod.LOGGER.error("Failed to load dimension data at index {}: {}", i, e.getMessage());
            ++skipped;
         }
      }
      PersonalWorldsMod.LOGGER.info("Loaded {} dimensions from registry ({} skipped)", registry.dimensions.size(), skipped);
      return registry;
   }

   public static DimensionRegistry get(MinecraftServer server) {
      PersistentStateManager stateManager = server.getOverworld().getPersistentStateManager();
      return PersistentStateCompat.getOrCreate(stateManager, "personalworlds_registry", DimensionRegistry::new, DimensionRegistry::fromNbt);
   }
}
