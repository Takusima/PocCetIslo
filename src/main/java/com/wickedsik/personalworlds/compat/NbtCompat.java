package com.wickedsik.personalworlds.compat;

import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import org.jetbrains.annotations.Nullable;

/**
 * Утилиты для работы с NBT (способ хранения данных в Minecraft).
 */
public final class NbtCompat {
   private NbtCompat() {
   }

   // Читает число из NBT, если ключ отсутствует — возвращает значение по умолчанию
   public static int getInt(NbtCompound nbt, String key, int defaultValue) {
      return nbt.contains(key, NbtCompound.INT_TYPE) ? nbt.getInt(key) : defaultValue;
   }

   // Читает строку из NBT
   public static String getString(NbtCompound nbt, String key, String defaultValue) {
      return nbt.contains(key, NbtCompound.STRING_TYPE) ? nbt.getString(key) : defaultValue;
   }

   // Читает дробное число из NBT
   public static float getFloat(NbtCompound nbt, String key, float defaultValue) {
      return nbt.contains(key, NbtCompound.FLOAT_TYPE) ? nbt.getFloat(key) : defaultValue;
   }

   // Читает логическое значение (true/false) из NBT
   public static boolean getBoolean(NbtCompound nbt, String key, boolean defaultValue) {
      return nbt.contains(key, NbtCompound.BYTE_TYPE) ? nbt.getBoolean(key) : defaultValue;
   }

   // Записывает UUID в NBT
   public static void putUuid(NbtCompound nbt, String key, UUID uuid) {
      nbt.putUuid(key, uuid);
   }

   // Читает UUID из NBT
   public static @Nullable UUID getUuid(NbtCompound nbt, String key) {
      return nbt.containsUuid(key) ? nbt.getUuid(key) : null;
   }

   // Проверяет, есть ли UUID в NBT
   public static boolean containsUuid(NbtCompound nbt, String key) {
      return nbt.containsUuid(key);
   }

   // Проверяет, есть ли ключ определённого типа в NBT
   public static boolean contains(NbtCompound nbt, String key, int type) {
      return nbt.contains(key, type);
   }

   // Читает вложенный NBT-объект
   public static NbtCompound getCompound(NbtCompound nbt, String key) {
      return nbt.getCompound(key);
   }

   // Читает длинное число из NBT
   public static long getLong(NbtCompound nbt, String key, long defaultValue) {
      return nbt.contains(key, NbtCompound.LONG_TYPE) ? nbt.getLong(key) : defaultValue;
   }

   // Читает список из NBT
   public static NbtList getList(NbtCompound nbt, String key, int type) {
      return nbt.getList(key, type);
   }

   // Читает NBT-объект из списка по индексу
   public static NbtCompound getCompound(NbtList list, int index) {
      return list.getCompound(index);
   }
}
