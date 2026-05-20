package com.wickedsik.personalworlds.compat;

import java.util.UUID;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Утилиты для работы с Identifier (уникальными именами в Minecraft).
 * Identifier — это строка вида "modid:path", например "minecraft:stone".
 */
public final class IdentifierCompat {
   private IdentifierCompat() {
   }

   // Создаёт Identifier из двух частей: пространства имён и пути
   public static Identifier create(String namespace, String path) {
      return new Identifier(namespace, path);
   }

   // Создаёт Identifier для нашего мода (personalworlds:...)
   public static Identifier modId(String path) {
      return create("personalworlds", path);
   }

   // Создаёт Identifier для измерения игрока
   public static Identifier dimensionId(UUID playerUuid) {
      return modId("pw_" + playerUuid.toString());
   }

   // Пытается разобрать строку как Identifier, возвращает null если не получилось
   public static @Nullable Identifier tryParse(String id) {
      return id != null && !id.isEmpty() ? Identifier.tryParse(id) : null;
   }

   // Создаёт Identifier из строки (для чтения из NBT)
   public static Identifier fromNbtString(String value) {
      return new Identifier(value);
   }
}
