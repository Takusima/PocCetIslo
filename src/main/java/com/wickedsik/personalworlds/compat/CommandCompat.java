package com.wickedsik.personalworlds.compat;

import java.util.function.Predicate;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Утилиты для работы с командами и правами.
 */
public final class CommandCompat {
   private CommandCompat() {
   }

   // Проверяет, есть ли у источника команды нужный уровень прав
   public static boolean hasPermissionLevel(ServerCommandSource source, int level) {
      return source.hasPermissionLevel(level);
   }

   // Проверяет, есть ли у игрока нужный уровень прав
   public static boolean hasPermissionLevel(ServerPlayerEntity player, int level) {
      return player.hasPermissionLevel(level);
   }

   // Возвращает проверку для команды: "требуется уровень прав X"
   public static Predicate<ServerCommandSource> requiresLevel(int level) {
      return (source) -> hasPermissionLevel(source, level);
   }
}
