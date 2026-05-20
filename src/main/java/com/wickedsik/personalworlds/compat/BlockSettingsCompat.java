package com.wickedsik.personalworlds.compat;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.util.Identifier;

public final class BlockSettingsCompat {
   private BlockSettingsCompat() {
   }

   // Создаём настройки блока с привязкой к ID (уникальному имени блока)
   public static FabricBlockSettings create(Identifier id) {
      return FabricBlockSettings.create();
   }

   /** @deprecated */
   @Deprecated
   public static FabricBlockSettings create() {
      return FabricBlockSettings.create();
   }
}
