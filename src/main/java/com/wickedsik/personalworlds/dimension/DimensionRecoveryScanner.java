package com.wickedsik.personalworlds.dimension;

import com.wickedsik.personalworlds.PersonalWorldsMod;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Сканирует папку измерений и восстанавливает "осиротевшие" измерения
 * (те, которые есть на диске, но отсутствуют в реестре).
 */
public class DimensionRecoveryScanner {
   private static final String FOLDER_PREFIX = "pw_";

   public static ScanResult scanAndRecover(MinecraftServer server) {
      Path dimensionsRoot = DimensionMetadataFile.getDimensionsRootPath(server);
      if (!Files.exists(dimensionsRoot) || !Files.isDirectory(dimensionsRoot)) {
         return new ScanResult(0, 0, 0, 0, 0);
      }
      List<UUID> discoveredUuids = discoverDimensionFolders(dimensionsRoot);
      if (discoveredUuids.isEmpty()) {
         return new ScanResult(0, 0, 0, 0, 0);
      }
      DimensionRegistry registry = DimensionRegistry.get(server);
      int alreadyRegistered = 0, recoveredFromMetadata = 0, recoveredMinimal = 0, failedRecovery = 0;
      for (UUID playerUuid : discoveredUuids) {
         if (registry.hasDimension(playerUuid)) {
            ++alreadyRegistered;
         } else {
            switch (recoverDimension(server, registry, playerUuid)) {
               case FROM_METADATA -> ++recoveredFromMetadata;
               case MINIMAL -> ++recoveredMinimal;
               case FAILED -> ++failedRecovery;
            }
         }
      }
      ScanResult result = new ScanResult(discoveredUuids.size(), alreadyRegistered, recoveredFromMetadata, recoveredMinimal, failedRecovery);
      if (result.totalFoldersFound() != 0 && result.hasRecoveries()) {
         PersonalWorldsMod.LOGGER.info("Dimension recovery complete: {} recovered, {} already registered, {} failed",
            result.totalRecovered(), result.alreadyRegistered(), result.failedRecovery());
      }
      return result;
   }

   private static List<UUID> discoverDimensionFolders(Path dimensionsRoot) {
      List<UUID> uuids = new ArrayList<>();
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(dimensionsRoot)) {
         for (Path folder : stream) {
            if (Files.isDirectory(folder)) {
               String folderName = folder.getFileName().toString();
               if (folderName.startsWith(FOLDER_PREFIX)) {
                  parseFolderUuid(folderName).ifPresent(uuids::add);
               }
            }
         }
      } catch (IOException e) {
         PersonalWorldsMod.LOGGER.error("Failed to scan dimensions folder: {}", e.getMessage());
      }
      return uuids;
   }

   private static Optional<UUID> parseFolderUuid(String folderName) {
      String hexPart = folderName.substring(FOLDER_PREFIX.length());
      if (hexPart.length() != 32 || !hexPart.matches("[0-9a-fA-F]+")) return Optional.empty();
      try {
         String uuidString = hexPart.substring(0, 8) + "-" + hexPart.substring(8, 12) + "-" + hexPart.substring(12, 16) + "-" + hexPart.substring(16, 20) + "-" + hexPart.substring(20);
         return Optional.of(UUID.fromString(uuidString));
      } catch (IllegalArgumentException e) {
         return Optional.empty();
      }
   }

   private static RecoveryResult recoverDimension(MinecraftServer server, DimensionRegistry registry, UUID playerUuid) {
      Optional<PlayerDimensionData> metadataOpt = DimensionMetadataFile.read(server, playerUuid);
      if (metadataOpt.isPresent()) {
         registry.registerDimension(metadataOpt.get());
         return RecoveryResult.FROM_METADATA;
      }
      try {
         PlayerDimensionData minimalData = createMinimalRecoveryData(server, playerUuid);
         registry.registerDimension(minimalData);
         return RecoveryResult.MINIMAL;
      } catch (Exception e) {
         PersonalWorldsMod.LOGGER.error("Failed to create recovery entry for {}: {}", playerUuid, e.getMessage());
         return RecoveryResult.FAILED;
      }
   }

   private static PlayerDimensionData createMinimalRecoveryData(MinecraftServer server, UUID playerUuid) {
      long createdAt = System.currentTimeMillis();
      String shortUuid = playerUuid.toString().substring(0, 8);
      String dimIdPath = "pw_" + playerUuid.toString().replace("-", "");
      return new PlayerDimensionData(playerUuid, "Unknown (" + shortUuid + ")", IdentifierCompat.modId(dimIdPath), createdAt, new BlockPos(0, 65, 0), WorldGenType.VOID, 0);
   }

   public record ScanResult(int totalFoldersFound, int alreadyRegistered, int recoveredFromMetadata, int recoveredMinimal, int failedRecovery) {
      public int totalRecovered() { return recoveredFromMetadata + recoveredMinimal; }
      public boolean hasRecoveries() { return totalRecovered() > 0; }
   }

   private enum RecoveryResult { FROM_METADATA, MINIMAL, FAILED }
}
