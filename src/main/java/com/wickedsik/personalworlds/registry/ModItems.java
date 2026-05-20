package com.wickedsik.personalworlds.registry;

import com.wickedsik.personalworlds.PersonalWorldsMod;
import com.wickedsik.personalworlds.compat.IdentifierCompat;
import com.wickedsik.personalworlds.config.ModConfig;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Регистрация предметов мода.
 */
public class ModItems {
   private static Item[] cachedActivationItems = null;

   public static void register() {
      PersonalWorldsMod.LOGGER.info("Registered items");
   }

   // Получает предмет активации портала по типу
   public static Item getActivationItem(int portalTypeIndex) {
      if (cachedActivationItems == null) {
         List<ModConfig.PortalConfig> configs = ModConfig.get().portalTypes;
         cachedActivationItems = new Item[configs.size()];
         for (int i = 0; i < configs.size(); ++i) {
            String itemId = configs.get(i).activationItem;
            Identifier id = IdentifierCompat.tryParse(itemId);
            Item item = id != null ? Registries.ITEM.get(id) : Items.AIR;
            if (item == Items.AIR && !itemId.equals("minecraft:air")) {
               PersonalWorldsMod.LOGGER.warn("Invalid activation item '{}' for portal type {}, using emerald", itemId, i);
               item = Items.EMERALD;
            }
            cachedActivationItems[i] = item;
            PersonalWorldsMod.LOGGER.debug("Portal type {} activation item set to: {}", i, Registries.ITEM.getId(item));
         }
      }
      if (portalTypeIndex >= 0 && portalTypeIndex < cachedActivationItems.length) {
         return cachedActivationItems[portalTypeIndex];
      }
      return cachedActivationItems[0];
   }

   public static void clearCache() {
      cachedActivationItems = null;
   }
}
