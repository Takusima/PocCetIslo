package com.wickedsik.personalworlds.server;

import com.wickedsik.personalworlds.PersonalWorldsMod;
import com.wickedsik.personalworlds.portal.PortalHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.border.WorldBorder;

/**
 * Менеджер границ мира для карманных измерений.
 */
public final class WorldBorderManager {
   private static final double BORDER_SIZE = 96.0;
   private static final double BORDER_RADIUS = BORDER_SIZE / 2.0;

   private WorldBorderManager() {}

   public static void register() {
      PersonalWorldsMod.LOGGER.info("Registering World Border Manager...");
      ServerWorldEvents.LOAD.register((server, world) -> {
         if (isPersonalDimension(world)) setupBorder(world);
      });
      PersonalWorldsMod.LOGGER.info("World Border Manager registered");
   }

   public static void setupBorder(ServerWorld world) {
      if (!isPersonalDimension(world)) return;
      WorldBorder border = world.getWorldBorder();
      border.setCenter(0.0, 0.0);
      border.setSize(BORDER_SIZE);
      border.setWarningBlocks(5);
      border.setWarningTime(10);
      border.setDamagePerBlock(0.5);
   }

   private static boolean isPersonalDimension(ServerWorld world) {
      return PortalHelper.isInPersonalDimension(world);
   }

   public static boolean isInsideBorder(double x, double z) {
      return Math.abs(x) <= BORDER_RADIUS && Math.abs(z) <= BORDER_RADIUS;
   }

   public static double getBorderSize() { return BORDER_SIZE; }
   public static double getBorderRadius() { return BORDER_RADIUS; }
   public static double getMinX() { return -BORDER_RADIUS; }
   public static double getMaxX() { return BORDER_RADIUS; }
   public static double getMinZ() { return -BORDER_RADIUS; }
   public static double getMaxZ() { return BORDER_RADIUS; }
}
