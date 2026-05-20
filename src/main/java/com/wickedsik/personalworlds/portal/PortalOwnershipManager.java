package com.wickedsik.personalworlds.portal;

import com.wickedsik.personalworlds.PersonalWorldsMod;
import com.wickedsik.personalworlds.compat.NbtCompat;
import com.wickedsik.personalworlds.compat.PersistentStateCompat;
import com.wickedsik.personalworlds.dimension.DimensionRegistry;
import com.wickedsik.personalworlds.dimension.PlayerDimensionData;
import java.util.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

/**
 * Хранит информацию о том, кому принадлежит каждый портал.
 */
public class PortalOwnershipManager extends PersistentState {
   private static final String DATA_NAME = "personalworlds_portal_ownership";
   private final Map<String, PortalOwnershipData> portalOwners = new HashMap<>();

   public void registerPortal(World world, BlockPos pos, UUID ownerUuid, int portalTypeIndex) {
      String key = makeKey(world, pos);
      portalOwners.put(key, new PortalOwnershipData(ownerUuid, portalTypeIndex));
      this.markDirty();
   }

   public Optional<UUID> getOwner(World world, BlockPos pos) {
      PortalOwnershipData data = portalOwners.get(makeKey(world, pos));
      return data != null ? Optional.of(data.ownerUuid) : Optional.empty();
   }

   public Optional<Integer> getPortalType(World world, BlockPos pos) {
      PortalOwnershipData data = portalOwners.get(makeKey(world, pos));
      return data != null ? Optional.of(data.portalTypeIndex) : Optional.empty();
   }

   public void removePortal(World world, BlockPos pos) {
      if (portalOwners.remove(makeKey(world, pos)) != null) {
         this.markDirty();
      }
   }

   public int clearPortalsOwnedBy(UUID ownerUuid) {
      int removed = 0;
      var iterator = portalOwners.entrySet().iterator();
      while (iterator.hasNext()) {
         if (iterator.next().getValue().ownerUuid.equals(ownerUuid)) {
            iterator.remove();
            ++removed;
         }
      }
      if (removed > 0) this.markDirty();
      return removed;
   }

   public String getOwnerName(MinecraftServer server, UUID ownerUuid) {
      ServerPlayerEntity player = server.getPlayerManager().getPlayer(ownerUuid);
      if (player != null) return player.getName().getString();
      DimensionRegistry registry = DimensionRegistry.get(server);
      return registry.getDimensionData(ownerUuid)
         .map(PlayerDimensionData::ownerName)
         .orElse(ownerUuid.toString().substring(0, 8));
   }

   private String makeKey(World world, BlockPos pos) {
      return world.getRegistryKey().getValue().toString() + ":" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
   }

   @Override
   public NbtCompound writeNbt(NbtCompound nbt) {
      NbtCompound portalsNbt = new NbtCompound();
      for (var entry : portalOwners.entrySet()) {
         NbtCompound portalData = new NbtCompound();
         portalData.putUuid("OwnerUuid", entry.getValue().ownerUuid);
         portalData.putInt("PortalTypeIndex", entry.getValue().portalTypeIndex);
         portalsNbt.put(entry.getKey(), portalData);
      }
      nbt.put("PortalOwners", portalsNbt);
      return nbt;
   }

   public static PortalOwnershipManager fromNbt(NbtCompound nbt) {
      PortalOwnershipManager manager = new PortalOwnershipManager();
      if (NbtCompat.contains(nbt, "PortalOwners", NbtElement.COMPOUND_TYPE)) {
         NbtCompound portalsNbt = NbtCompat.getCompound(nbt, "PortalOwners");
         for (String key : portalsNbt.getKeys()) {
            try {
               NbtElement element = portalsNbt.get(key);
               if (element instanceof NbtCompound portalData) {
                  UUID uuid = NbtCompat.getUuid(portalData, "OwnerUuid");
                  int portalTypeIndex = NbtCompat.getInt(portalData, "PortalTypeIndex", 0);
                  if (uuid != null) manager.portalOwners.put(key, new PortalOwnershipData(uuid, portalTypeIndex));
               }
            } catch (Exception e) {
               PersonalWorldsMod.LOGGER.warn("Invalid portal ownership data for key: {}", key);
            }
         }
      }
      return manager;
   }

   public static PortalOwnershipManager get(MinecraftServer server) {
      PersistentStateManager stateManager = server.getOverworld().getPersistentStateManager();
      return PersistentStateCompat.getOrCreate(stateManager, DATA_NAME, PortalOwnershipManager::new, PortalOwnershipManager::fromNbt);
   }

   private record PortalOwnershipData(UUID ownerUuid, int portalTypeIndex) {}
}
