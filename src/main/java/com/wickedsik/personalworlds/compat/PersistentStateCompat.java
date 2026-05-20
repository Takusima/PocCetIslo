package com.wickedsik.personalworlds.compat;

import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

/**
 * Утилиты для работы с постоянными данными (сохраняются на диск).
 */
public final class PersistentStateCompat {
   private PersistentStateCompat() {
   }

   // Получает или создаёт объект постоянных данных
   public static <T extends PersistentState> T getOrCreate(
      PersistentStateManager stateManager,
      String name,
      Supplier<T> constructor,
      Function<NbtCompound, T> deserializer
   ) {
      return stateManager.getOrCreate(deserializer, constructor, name);
   }
}
