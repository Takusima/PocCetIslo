package com.wickedsik.personalworlds.util;

import com.wickedsik.personalworlds.PersonalWorldsMod;
import com.wickedsik.personalworlds.compat.IdentifierCompat;
import com.wickedsik.personalworlds.dimension.PlayerDimensionData;
import com.wickedsik.personalworlds.dimension.WorldGenType;
import com.wickedsik.personalworlds.player.InvitationData;
import com.wickedsik.personalworlds.player.ReturnData;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Проверка и очистка данных.
 */
public final class DataValidator {
   private DataValidator() {}

   public static Optional<UUID> validateUuid(String uuidString) {
      if (uuidString != null && !uuidString.isEmpty()) {
         try { return Optional.of(UUID.fromString(uuidString)); }
         catch (IllegalArgumentException e) { return Optional.empty(); }
      }
      return Optional.empty();
   }

   public static boolean isValidUuid(UUID uuid) { return uuid != null; }

   public static Optional<Identifier> validateIdentifier(String namespace, String path) {
      if (namespace != null && path != null) {
         try { return Optional.of(IdentifierCompat.create(namespace, path)); }
         catch (Exception e) { return Optional.empty(); }
      }
      return Optional.empty();
   }

   public static boolean isValidBlockPos(BlockPos pos, MinecraftServer server) {
      if (pos == null) return false;
      return pos.getX() >= -30000000 && pos.getX() <= 30000000
         && pos.getY() >= -64 && pos.getY() <= 320
         && pos.getZ() >= -30000000 && pos.getZ() <= 30000000;
   }

   public static BlockPos sanitizeBlockPos(BlockPos pos) {
      if (pos == null) return new BlockPos(0, 64, 0);
      return new BlockPos(
         Math.max(-30000000, Math.min(30000000, pos.getX())),
         Math.max(-64, Math.min(320, pos.getY())),
         Math.max(-30000000, Math.min(30000000, pos.getZ()))
      );
   }

   public static boolean isValidDimensionData(PlayerDimensionData data) {
      if (data == null) return false;
      if (!isValidUuid(data.ownerUuid())) return false;
      if (data.ownerName() == null || data.ownerName().isEmpty()) return false;
      if (data.dimensionId() == null) return false;
      if (data.spawnPoint() == null) return false;
      if (data.generatorType() == null) return false;
      return data.createdAt() >= 0L;
   }

   public static PlayerDimensionData sanitizeDimensionData(PlayerDimensionData data, UUID ownerUuid) {
      if (data == null) {
         String dimPath = "pw_" + ownerUuid.toString().replace("-", "");
         return new PlayerDimensionData(ownerUuid, "Unknown", IdentifierCompat.modId(dimPath), System.currentTimeMillis(), new BlockPos(0, 65, 0), WorldGenType.VOID, 0);
      }
      UUID uuid = data.ownerUuid() != null ? data.ownerUuid() : ownerUuid;
      String name = (data.ownerName() != null && !data.ownerName().isEmpty()) ? data.ownerName() : "Unknown";
      Identifier dimId = data.dimensionId() != null ? data.dimensionId() : IdentifierCompat.modId("pw_" + uuid.toString().replace("-", ""));
      long createdAt = data.createdAt() > 0L ? data.createdAt() : System.currentTimeMillis();
      BlockPos spawn = sanitizeBlockPos(data.spawnPoint());
      WorldGenType genType = data.generatorType() != null ? data.generatorType() : WorldGenType.VOID;
      return new PlayerDimensionData(uuid, name, dimId, createdAt, spawn, genType, data.portalTypeIndex());
   }

   public static boolean isValidReturnData(ReturnData data, MinecraftServer server) {
      if (data == null) return false;
      if (data.dimension() == null) return false;
      return server.getWorld(data.dimension()) != null;
   }

   public static boolean isValidInvitationData(InvitationData data) {
      if (data == null) return false;
      if (!isValidUuid(data.ownerUuid())) return false;
      if (data.ownerName() == null || data.ownerName().isEmpty()) return false;
      return data.invitedAt() > 0L;
   }

   public static String sanitizePlayerName(String name) {
      if (name != null && !name.isEmpty()) {
         String sanitized = name.replaceAll("[^\\p{Print}]", "");
         return sanitized.substring(0, Math.min(sanitized.length(), 32)).trim();
      }
      return "Unknown";
   }
}
