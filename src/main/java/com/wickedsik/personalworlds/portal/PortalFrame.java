package com.wickedsik.personalworlds.portal;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Описывает рамку портала: позицию нижнего левого угла, размеры и ось.
 */
public record PortalFrame(BlockPos bottomLeft, int width, int height, Direction.Axis axis) {

   // Возвращает позиции внутренних блоков портала (воздух внутри рамки)
   public List<BlockPos> getInteriorPositions() {
      List<BlockPos> positions = new ArrayList<>();
      Direction horizontal = this.axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
      BlockPos start = this.bottomLeft.up().offset(horizontal);

      for (int h = 0; h < this.height; ++h) {
         for (int w = 0; w < this.width; ++w) {
            BlockPos pos = start.up(h).offset(horizontal, w);
            positions.add(pos);
         }
      }
      return positions;
   }

   // Возвращает позиции блоков рамки (контур портала)
   public List<BlockPos> getFramePositions() {
      List<BlockPos> positions = new ArrayList<>();
      Direction horizontal = this.axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
      int frameWidth = this.width + 2;
      int frameHeight = this.height + 2;

      for (int w = 0; w < frameWidth; ++w) {
         positions.add(this.bottomLeft.offset(horizontal, w));
      }
      for (int w = 0; w < frameWidth; ++w) {
         positions.add(this.bottomLeft.up(frameHeight - 1).offset(horizontal, w));
      }
      for (int h = 1; h < frameHeight - 1; ++h) {
         positions.add(this.bottomLeft.up(h));
      }
      for (int h = 1; h < frameHeight - 1; ++h) {
         positions.add(this.bottomLeft.offset(horizontal, frameWidth - 1).up(h));
      }
      return positions;
   }

   // Возвращает центр портала
   public BlockPos getCenter() {
      Direction horizontal = this.axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
      return this.bottomLeft.up(1 + this.height / 2).offset(horizontal, 1 + this.width / 2);
   }
}
