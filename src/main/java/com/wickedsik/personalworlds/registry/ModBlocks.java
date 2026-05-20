package com.wickedsik.personalworlds.registry;

import com.wickedsik.personalworlds.PersonalWorldsMod;
import com.wickedsik.personalworlds.compat.BlockSettingsCompat;
import com.wickedsik.personalworlds.compat.IdentifierCompat;
import com.wickedsik.personalworlds.config.ModConfig;
import com.wickedsik.personalworlds.portal.PersonalPortalBlock;
import com.wickedsik.personalworlds.portal.PortalColor;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ModBlocks {
   // Уникальный ID блока портала
   private static final Identifier PERSONAL_PORTAL_ID = IdentifierCompat.modId("personal_portal");
   public static final Block PERSONAL_PORTAL;
   private static Block[] cachedFrameBlocks;
   private static PortalColor[] cachedPortalColors;

   public static void register() {
      // Регистрируем блок портала в реестре
      Registry.register(Registries.BLOCK, PERSONAL_PORTAL_ID, PERSONAL_PORTAL);
      PersonalWorldsMod.LOGGER.info("Registered blocks");
   }

   // Получаем блок рамки портала по типу портала
   public static Block getFrameBlock(int portalTypeIndex) {
      if (cachedFrameBlocks == null) {
         List<ModConfig.PortalConfig> configs = ModConfig.get().portalTypes;
         cachedFrameBlocks = new Block[configs.size()];

         for (int i = 0; i < configs.size(); ++i) {
            String blockId = configs.get(i).frameBlock;
            Identifier id = IdentifierCompat.tryParse(blockId);
            // Ищем блок по ID в реестре, если не нашли — используем воздух
            Block block = id != null ? Registries.BLOCK.get(id) : Blocks.AIR;
            if (block == Blocks.AIR && !blockId.equals("minecraft:air")) {
               PersonalWorldsMod.LOGGER.warn("Invalid frame block '{}' for portal type {}, using nether_bricks", blockId, i);
               block = Blocks.NETHER_BRICKS;
            }

            cachedFrameBlocks[i] = block;
            PersonalWorldsMod.LOGGER.debug("Portal type {} frame block set to: {}", i, Registries.BLOCK.getId(block));
         }
      }

      if (portalTypeIndex >= 0 && portalTypeIndex < cachedFrameBlocks.length) {
         return cachedFrameBlocks[portalTypeIndex];
      } else {
         PersonalWorldsMod.LOGGER.warn("Portal type index {} out of bounds (0-{}), using 0", portalTypeIndex, cachedFrameBlocks.length - 1);
         return cachedFrameBlocks[0];
      }
   }

   // Получаем цвет портала по типу
   public static PortalColor getPortalColor(int portalTypeIndex) {
      if (cachedPortalColors == null) {
         List<ModConfig.PortalConfig> configs = ModConfig.get().portalTypes;
         cachedPortalColors = new PortalColor[configs.size()];

         for (int i = 0; i < configs.size(); ++i) {
            String colorStr = configs.get(i).portalColor;
            cachedPortalColors[i] = PortalColor.fromString(colorStr);
            PersonalWorldsMod.LOGGER.debug("Portal type {} color set to: {}", i, cachedPortalColors[i].getName());
         }
      }

      if (portalTypeIndex >= 0 && portalTypeIndex < cachedPortalColors.length) {
         return cachedPortalColors[portalTypeIndex];
      } else {
         PersonalWorldsMod.LOGGER.warn("Portal type index {} out of bounds (0-{}), using RED", portalTypeIndex, cachedPortalColors.length - 1);
         return PortalColor.RED;
      }
   }

   // Очищаем кеш (при перезагрузке конфига)
   public static void clearCache() {
      cachedFrameBlocks = null;
      cachedPortalColors = null;
      PersonalWorldsMod.LOGGER.debug("Block cache cleared");
   }

   static {
      // Создаём блок портала с настройками: фиолетовый цвет, нет коллизий, неразрушимый, светится
      PERSONAL_PORTAL = new PersonalPortalBlock(
         BlockSettingsCompat.create(PERSONAL_PORTAL_ID)
            .mapColor(MapColor.PURPLE)
            .noCollision()
            .strength(-1.0F)
            .sounds(BlockSoundGroup.GLASS)
            .luminance((state) -> 11)
            .dropsNothing()
      );
      cachedFrameBlocks = null;
      cachedPortalColors = null;
   }
}
