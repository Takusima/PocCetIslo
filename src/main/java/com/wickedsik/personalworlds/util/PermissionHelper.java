package com.wickedsik.personalworlds.util;

import com.wickedsik.personalworlds.PersonalWorldsMod;
import com.wickedsik.personalworlds.compat.CommandCompat;
import java.util.function.Predicate;
import net.minecraft.server.command.ServerCommandSource;

/**
 * Проверка прав игроков (поддержка fabric-permissions-api).
 */
public final class PermissionHelper {
   public static final String ADMIN_LIST = "pocketislands.admin.list";
   public static final String ADMIN_INFO = "pocketislands.admin.info";
   public static final String ADMIN_DELETE = "pocketislands.admin.delete";
   public static final String ADMIN_TELEPORT = "pocketislands.admin.teleport";
   public static final String ADMIN_RELOAD = "pocketislands.admin.reload";
   public static final String PLAYER_CREATE = "pocketislands.player.create";
   public static final String PLAYER_INVITE = "pocketislands.player.invite";
   public static final String PLAYER_VISIT = "pocketislands.player.visit";

   private static Boolean permissionsApiAvailable = null;

   public static boolean check(ServerCommandSource source, String permission, int fallbackLevel) {
      if (isPermissionsApiAvailable()) {
         try {
            return me.lucko.fabric.api.permissions.v0.Permissions.check(source, permission, fallbackLevel);
         } catch (Exception e) {
            return CommandCompat.hasPermissionLevel(source, fallbackLevel);
         }
      }
      return CommandCompat.hasPermissionLevel(source, fallbackLevel);
   }

   public static Predicate<ServerCommandSource> require(String permission, int fallbackLevel) {
      return (source) -> check(source, permission, fallbackLevel);
   }

   public static boolean canAdminList(ServerCommandSource source) { return check(source, ADMIN_LIST, 2); }
   public static boolean canAdminInfo(ServerCommandSource source) { return check(source, ADMIN_INFO, 2); }
   public static boolean canAdminDelete(ServerCommandSource source) { return check(source, ADMIN_DELETE, 4); }
   public static boolean canAdminTeleport(ServerCommandSource source) { return check(source, ADMIN_TELEPORT, 2); }
   public static boolean canAdminReload(ServerCommandSource source) { return check(source, ADMIN_RELOAD, 3); }

   private static boolean isPermissionsApiAvailable() {
      if (permissionsApiAvailable == null) {
         try {
            Class.forName("me.lucko.fabric.api.permissions.v0.Permissions");
            permissionsApiAvailable = true;
            PersonalWorldsMod.LOGGER.info("fabric-permissions-api detected");
         } catch (ClassNotFoundException e) {
            permissionsApiAvailable = false;
            PersonalWorldsMod.LOGGER.info("fabric-permissions-api not found, using vanilla OP levels");
         }
      }
      return permissionsApiAvailable;
   }

   public static void resetCache() { permissionsApiAvailable = null; }

   private PermissionHelper() {}
}
