package com.wickedsik.personalworlds.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wickedsik.personalworlds.PersonalWorldsMod;
import com.wickedsik.personalworlds.portal.PortalColor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

public class ModConfig {
   private static ModConfig INSTANCE;
   private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("pocketislands.json");
   private static final Path LEGACY_CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("personalworlds.json");
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
   public List<PortalConfig> portalTypes = new ArrayList();
   public boolean consumeActivationItem = false;
   public int maxInvitationsPerPlayer = 20;
   public boolean enableAlwaysWelcome = false;
   public boolean allowVisitWhenHostNotHome = false;
   public int unloadEmptyDimensionDelayTicks = 600;
   public int cleanupIntervalTicks = 600;
   public Map<String, Object> dimensionGameRules = new LinkedHashMap(Map.of("doMobSpawning", false));
   public boolean enableTeleportParticles = true;
   public boolean enableTeleportSounds = true;
   public boolean enablePortalActivationEffects = true;
   public boolean enableInvitationNotifications = true;

   public static ModConfig get() {
      if (INSTANCE == null) {
         load();
      }

      return INSTANCE;
   }

   public static void load() {
      if (Files.exists(CONFIG_PATH, new LinkOption[0])) {
         try {
            String json = Files.readString(CONFIG_PATH);
            INSTANCE = (ModConfig)GSON.fromJson(json, ModConfig.class);
            if (INSTANCE == null) {
               PersonalWorldsMod.LOGGER.warn("Config file was empty, using defaults");
               INSTANCE = new ModConfig();
               save();
            }

            if (INSTANCE.portalTypes.isEmpty()) {
               PersonalWorldsMod.LOGGER.info("No portal types defined, adding default");
               INSTANCE.portalTypes.add(new PortalConfig());
               save();
            }

            PersonalWorldsMod.LOGGER.info("Configuration loaded from {}", CONFIG_PATH);
         } catch (IOException e) {
            PersonalWorldsMod.LOGGER.error("Failed to load configuration, using defaults", e);
            INSTANCE = new ModConfig();
         } catch (Exception e) {
            PersonalWorldsMod.LOGGER.error("Configuration file malformed, using defaults", e);
            INSTANCE = new ModConfig();
         }
      } else if (Files.exists(LEGACY_CONFIG_PATH, new LinkOption[0])) {
         try {
            PersonalWorldsMod.LOGGER.info("Found legacy config at {}, migrating to {}", LEGACY_CONFIG_PATH, CONFIG_PATH);
            Files.copy(LEGACY_CONFIG_PATH, CONFIG_PATH);
            Files.delete(LEGACY_CONFIG_PATH);
            PersonalWorldsMod.LOGGER.info("Config migrated (old file removed)");
            load();
            return;
         } catch (IOException e) {
            PersonalWorldsMod.LOGGER.error("Failed to migrate legacy config, using defaults", e);
            INSTANCE = new ModConfig();
            INSTANCE.portalTypes.add(new PortalConfig());
            save();
         }
      } else {
         PersonalWorldsMod.LOGGER.info("No configuration file found, creating default at {}", CONFIG_PATH);
         INSTANCE = new ModConfig();
         INSTANCE.portalTypes.add(new PortalConfig());
         save();
      }

      INSTANCE.validate();
   }

   public static void save() {
      try {
         Files.createDirectories(CONFIG_PATH.getParent());
         String json = GSON.toJson(INSTANCE);
         Files.writeString(CONFIG_PATH, json);
         PersonalWorldsMod.LOGGER.debug("Configuration saved to {}", CONFIG_PATH);
      } catch (IOException e) {
         PersonalWorldsMod.LOGGER.error("Failed to save configuration", e);
      }

   }

   public static void reload() {
      INSTANCE = null;
      load();
      PersonalWorldsMod.LOGGER.info("Configuration reloaded");
   }

   private void validate() {
      if (this.portalTypes.isEmpty()) {
         PersonalWorldsMod.LOGGER.warn("No portal types defined, adding default");
         this.portalTypes.add(new PortalConfig());
      }

      for(int i = 0; i < this.portalTypes.size(); ++i) {
         PortalConfig portal = (PortalConfig)this.portalTypes.get(i);
         if (portal.frameBlock == null || !portal.frameBlock.contains(":")) {
            PersonalWorldsMod.LOGGER.warn("Portal type {} has invalid frameBlock '{}', using minecraft:nether_bricks", i, portal.frameBlock);
            portal.frameBlock = "minecraft:nether_bricks";
         }

         if (portal.activationItem == null || !portal.activationItem.contains(":")) {
            PersonalWorldsMod.LOGGER.warn("Portal type {} has invalid activationItem '{}', using minecraft:emerald", i, portal.activationItem);
            portal.activationItem = "minecraft:emerald";
         }

         if (portal.islandLayers == null || portal.islandLayers.length == 0) {
            PersonalWorldsMod.LOGGER.warn("Portal type {} has no island layers, using default", i);
            portal.islandLayers = new String[]{"minecraft:grass_block", "minecraft:dirt", "minecraft:stone"};
         }

         if (portal.islandLayers.length > 5) {
            PersonalWorldsMod.LOGGER.warn("Portal type {} has {} island layers (max 5), truncating", i, portal.islandLayers.length);
            String[] truncated = new String[5];
            System.arraycopy(portal.islandLayers, 0, truncated, 0, 5);
            portal.islandLayers = truncated;
         }

         for(int j = 0; j < portal.islandLayers.length; ++j) {
            if (portal.islandLayers[j] == null || !portal.islandLayers[j].contains(":")) {
               PersonalWorldsMod.LOGGER.warn("Portal type {} layer {} has invalid block ID '{}', using minecraft:grass_block", new Object[]{i, j, portal.islandLayers[j]});
               portal.islandLayers[j] = "minecraft:grass_block";
            }
         }

         if (portal.portalColor != null && !portal.portalColor.isEmpty()) {
            PortalColor parsedColor = PortalColor.fromString(portal.portalColor);
            if (!parsedColor.method_15434().equalsIgnoreCase(portal.portalColor)) {
               PersonalWorldsMod.LOGGER.warn("Portal type {} has invalid color '{}', using '{}' instead", new Object[]{i, portal.portalColor, parsedColor.method_15434()});
            }
         } else {
            PersonalWorldsMod.LOGGER.info("Portal type {} has no color specified, using 'red'", i);
            portal.portalColor = "red";
         }
      }

      for(int i = 0; i < this.portalTypes.size(); ++i) {
         for(int j = i + 1; j < this.portalTypes.size(); ++j) {
            if (((PortalConfig)this.portalTypes.get(i)).frameBlock.equals(((PortalConfig)this.portalTypes.get(j)).frameBlock)) {
               PersonalWorldsMod.LOGGER.warn("Portal types {} and {} both use frame block '{}' - first match will be used", new Object[]{i, j, ((PortalConfig)this.portalTypes.get(i)).frameBlock});
            }
         }
      }

      if (this.maxInvitationsPerPlayer < -1) {
         PersonalWorldsMod.LOGGER.warn("Invalid maxInvitationsPerPlayer {}, using 20", this.maxInvitationsPerPlayer);
         this.maxInvitationsPerPlayer = 20;
      }

      if (this.unloadEmptyDimensionDelayTicks < 0) {
         PersonalWorldsMod.LOGGER.warn("Invalid unloadEmptyDimensionDelayTicks {}, using 600", this.unloadEmptyDimensionDelayTicks);
         this.unloadEmptyDimensionDelayTicks = 600;
      }

      if (this.cleanupIntervalTicks < 20) {
         PersonalWorldsMod.LOGGER.warn("cleanupIntervalTicks {} too low, using minimum 20", this.cleanupIntervalTicks);
         this.cleanupIntervalTicks = 20;
      }

      if (this.dimensionGameRules == null) {
         PersonalWorldsMod.LOGGER.warn("dimensionGameRules is null, using default (doMobSpawning=false)");
         this.dimensionGameRules = new LinkedHashMap(Map.of("doMobSpawning", false));
      } else {
         Iterator<Map.Entry<String, Object>> iterator = this.dimensionGameRules.entrySet().iterator();

         while(iterator.hasNext()) {
            Map.Entry<String, Object> entry = (Map.Entry)iterator.next();
            if (entry.getKey() == null) {
               PersonalWorldsMod.LOGGER.warn("Removing null key from dimensionGameRules");
               iterator.remove();
            } else if (entry.getValue() == null) {
               PersonalWorldsMod.LOGGER.warn("Removing game rule '{}' with null value from dimensionGameRules", entry.getKey());
               iterator.remove();
            } else if (!(entry.getValue() instanceof Boolean) && !(entry.getValue() instanceof Number)) {
               PersonalWorldsMod.LOGGER.warn("Game rule '{}' has invalid value type '{}' (expected Boolean or Number), removing", entry.getKey(), entry.getValue().getClass().getSimpleName());
               iterator.remove();
            }
         }
      }

   }

   public static Path getConfigPath() {
      return CONFIG_PATH;
   }

   public static class PortalConfig {
      public String frameBlock;
      public String activationItem;
      public String[] islandLayers;
      public String portalColor;

      public PortalConfig() {
         this.frameBlock = "minecraft:nether_bricks";
         this.activationItem = "minecraft:emerald";
         this.islandLayers = new String[]{"minecraft:grass_block", "minecraft:dirt", "minecraft:stone"};
         this.portalColor = "red";
      }

      public PortalConfig(String frameBlock, String activationItem, String[] islandLayers, String portalColor) {
         this.frameBlock = frameBlock;
         this.activationItem = activationItem;
         this.islandLayers = islandLayers;
         this.portalColor = portalColor;
      }
   }
}
