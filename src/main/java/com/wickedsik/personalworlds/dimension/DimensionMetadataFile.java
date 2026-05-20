package com.wickedsik.personalworlds.dimension;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.wickedsik.personalworlds.PersonalWorldsMod;
import com.wickedsik.personalworlds.compat.IdentifierCompat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.level.storage.LevelStorage;

/**
 * Работа с файлом метаданных измерения (personalworlds_metadata.json).
 * Хранит информацию о владельце, точке спавна и типе генерации.
 */
public class DimensionMetadataFile {
   private static final String FILENAME = "personalworlds_metadata.json";
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

   // Записывает метаданные в файл
   public static void write(MinecraftServer server, PlayerDimensionData data) {
      Path metadataPath = getMetadataPath(server, data.ownerUuid());
      if (metadataPath != null) {
         try {
            Files.createDirectories(metadataPath.getParent());
            MetadataJson json = MetadataJson.fromPlayerData(data);
            String content = GSON.toJson(json);
            Files.writeString(metadataPath, content);
         } catch (IOException e) {
            PersonalWorldsMod.LOGGER.error("Failed to write dimension metadata: {}", e.getMessage());
         }
      }
   }

   // Читает метаданные из файла
   public static Optional<PlayerDimensionData> read(MinecraftServer server, UUID playerUuid) {
      Path metadataPath = getMetadataPath(server, playerUuid);
      if (metadataPath != null && Files.exists(metadataPath)) {
         try {
            String content = Files.readString(metadataPath);
            MetadataJson json = GSON.fromJson(content, MetadataJson.class);
            if (json != null && json.ownerUuid != null) {
               return Optional.of(json.toPlayerData());
            }
         } catch (IOException | JsonSyntaxException | IllegalArgumentException e) {
            PersonalWorldsMod.LOGGER.warn("Failed to read metadata for {}: {}", playerUuid, e.getMessage());
         }
      }
      return Optional.empty();
   }

   // Проверяет существование файла метаданных
   public static boolean exists(MinecraftServer server, UUID playerUuid) {
      Path metadataPath = getMetadataPath(server, playerUuid);
      return metadataPath != null && Files.exists(metadataPath);
   }

   // Удаляет файл метаданных
   public static void delete(MinecraftServer server, UUID playerUuid) {
      Path metadataPath = getMetadataPath(server, playerUuid);
      if (metadataPath != null) {
         try {
            Files.deleteIfExists(metadataPath);
         } catch (IOException e) {
            PersonalWorldsMod.LOGGER.error("Failed to delete metadata: {}", e.getMessage());
         }
      }
   }

   // Получает путь к файлу метаданных
   private static Path getMetadataPath(MinecraftServer server, UUID playerUuid) {
      try {
         Path worldRoot = server.getSavePath(LevelStorage.ACCESSOR);
         String folderName = "pw_" + playerUuid.toString().replace("-", "");
         return worldRoot.resolve("dimensions/personalworlds/" + folderName + "/" + FILENAME);
      } catch (Exception e) {
         PersonalWorldsMod.LOGGER.error("Failed to determine metadata path: {}", e.getMessage());
         return null;
      }
   }

   // Получает путь к папке измерения
   public static Path getDimensionFolderPath(MinecraftServer server, UUID playerUuid) {
      Path worldRoot = server.getSavePath(LevelStorage.ACCESSOR);
      String folderName = "pw_" + playerUuid.toString().replace("-", "");
      return worldRoot.resolve("dimensions/personalworlds/" + folderName);
   }

   // Получает путь к корневой папке измерений
   public static Path getDimensionsRootPath(MinecraftServer server) {
      Path worldRoot = server.getSavePath(LevelStorage.ACCESSOR);
      return worldRoot.resolve("dimensions/personalworlds");
   }

   // Удаляет папку измерения
   public static boolean deleteDimensionFolder(MinecraftServer server, UUID playerUuid) {
      Path dimensionFolder = getDimensionFolderPath(server, playerUuid);
      if (!Files.exists(dimensionFolder)) {
         return false;
      }
      try {
         deleteDirectoryRecursively(dimensionFolder);
         return true;
      } catch (IOException e) {
         PersonalWorldsMod.LOGGER.error("Failed to delete dimension folder: {}", e.getMessage());
         return false;
      }
   }

   // Рекурсивно удаляет папку
   private static void deleteDirectoryRecursively(Path directory) throws IOException {
      if (Files.exists(directory)) {
         try (Stream<Path> stream = Files.walk(directory)) {
            stream.sorted((a, b) -> b.compareTo(a))
                  .forEach(path -> {
                     try { Files.delete(path); }
                     catch (IOException e) { /* пропускаем */ }
                  });
         }
      }
   }

   // Внутренний класс для сериализации в JSON
   private static class MetadataJson {
      String ownerUuid;
      String ownerName;
      String dimensionId;
      long createdAt;
      int spawnX, spawnY, spawnZ;
      String generatorType;
      Integer portalTypeIndex;

      static MetadataJson fromPlayerData(PlayerDimensionData data) {
         MetadataJson json = new MetadataJson();
         json.ownerUuid = data.ownerUuid().toString();
         json.ownerName = data.ownerName();
         json.dimensionId = data.dimensionId().toString();
         json.createdAt = data.createdAt();
         json.spawnX = data.spawnPoint().getX();
         json.spawnY = data.spawnPoint().getY();
         json.spawnZ = data.spawnPoint().getZ();
         json.generatorType = data.generatorType().name();
         json.portalTypeIndex = data.portalTypeIndex();
         return json;
      }

      PlayerDimensionData toPlayerData() {
         int portalType = this.portalTypeIndex != null ? this.portalTypeIndex : 0;
         return new PlayerDimensionData(
            UUID.fromString(this.ownerUuid), this.ownerName,
            IdentifierCompat.fromNbtString(this.dimensionId),
            this.createdAt,
            new BlockPos(this.spawnX, this.spawnY, this.spawnZ),
            WorldGenType.fromString(this.generatorType), portalType
         );
      }
   }
}
