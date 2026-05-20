package com.wickedsik.personalworlds.player;

import com.wickedsik.personalworlds.compat.IdentifierCompat;
import com.wickedsik.personalworlds.compat.NbtCompat;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Данные для возврата игрока: измерение, позиция, поворот.
 */
public record ReturnData(RegistryKey<World> dimension, BlockPos position, float yaw, float pitch) {
   public NbtCompound toNbt() {
      NbtCompound nbt = new NbtCompound();
      nbt.putString("Dimension", this.dimension.getValue().toString());
      nbt.putInt("X", this.position.getX());
      nbt.putInt("Y", this.position.getY());
      nbt.putInt("Z", this.position.getZ());
      nbt.putFloat("Yaw", this.yaw);
      nbt.putFloat("Pitch", this.pitch);
      return nbt;
   }

   public static ReturnData fromNbt(NbtCompound nbt) {
      Identifier dimId = IdentifierCompat.fromNbtString(NbtCompat.getString(nbt, "Dimension", "minecraft:overworld"));
      RegistryKey<World> dimension = RegistryKey.of(RegistryKeys.WORLD, dimId);
      BlockPos position = new BlockPos(
         NbtCompat.getInt(nbt, "X", 0),
         NbtCompat.getInt(nbt, "Y", 64),
         NbtCompat.getInt(nbt, "Z", 0)
      );
      float yaw = NbtCompat.getFloat(nbt, "Yaw", 0.0F);
      float pitch = NbtCompat.getFloat(nbt, "Pitch", 0.0F);
      return new ReturnData(dimension, position, yaw, pitch);
   }
}
