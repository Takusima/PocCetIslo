package com.wickedsik.personalworlds.dimension;

import com.wickedsik.personalworlds.compat.IdentifierCompat;
import com.wickedsik.personalworlds.compat.NbtCompat;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Данные о персональном измерении игрока.
 */
public record PlayerDimensionData(UUID ownerUuid, String ownerName, Identifier dimensionId, long createdAt, BlockPos spawnPoint, WorldGenType generatorType, int portalTypeIndex) {
   public NbtCompound toNbt() {
      NbtCompound nbt = new NbtCompound();
      NbtCompat.putUuid(nbt, "OwnerUuid", this.ownerUuid);
      nbt.putString("OwnerName", this.ownerName);
      nbt.putString("DimensionId", this.dimensionId.toString());
      nbt.putLong("CreatedAt", this.createdAt);
      nbt.putInt("SpawnX", this.spawnPoint.getX());
      nbt.putInt("SpawnY", this.spawnPoint.getY());
      nbt.putInt("SpawnZ", this.spawnPoint.getZ());
      nbt.putString("GeneratorType", this.generatorType.name());
      nbt.putInt("PortalTypeIndex", this.portalTypeIndex);
      return nbt;
   }

   public static PlayerDimensionData fromNbt(NbtCompound nbt) {
      int portalTypeIndex = NbtCompat.getInt(nbt, "PortalTypeIndex", 0);
      return new PlayerDimensionData(
         NbtCompat.getUuid(nbt, "OwnerUuid"),
         NbtCompat.getString(nbt, "OwnerName", "Unknown"),
         IdentifierCompat.fromNbtString(NbtCompat.getString(nbt, "DimensionId", "")),
         NbtCompat.getLong(nbt, "CreatedAt", 0L),
         new BlockPos(
            NbtCompat.getInt(nbt, "SpawnX", 0),
            NbtCompat.getInt(nbt, "SpawnY", 64),
            NbtCompat.getInt(nbt, "SpawnZ", 0)
         ),
         WorldGenType.fromString(NbtCompat.getString(nbt, "GeneratorType", "VOID")),
         portalTypeIndex
      );
   }
}
