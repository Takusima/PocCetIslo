package com.wickedsik.personalworlds.portal;

import com.wickedsik.personalworlds.PersonalWorldsMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Блок персонального портала. Телепортирует игроков при входе.
 */
public class PersonalPortalBlock extends Block {
   // Свойство: ось портала (X или Z)
   public static final EnumProperty<Direction.Axis> AXIS;
   // Свойство: цвет портала
   public static final EnumProperty<PortalColor> COLOR;
   // Формы блока для разных осей
   protected static final VoxelShape X_SHAPE;
   protected static final VoxelShape Z_SHAPE;
   private static final int PORTAL_COOLDOWN = 100;

   public PersonalPortalBlock(Settings settings) {
      super(settings);
      this.setDefaultState(this.getDefaultState()
         .with(AXIS, Direction.Axis.X)
         .with(COLOR, PortalColor.RED));
   }

   @Override
   protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
      builder.add(AXIS, COLOR);
   }

   @Override
   public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      return state.get(AXIS) == Direction.Axis.Z ? Z_SHAPE : X_SHAPE;
   }

   // Вызывается когда сущность входит в блок портала
   @Override
   public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
      this.handleEntityCollision(state, world, pos, entity);
   }

   private void handleEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
      if (!world.isClient()) {
         if (entity instanceof ServerPlayerEntity player) {
            if (player.hasVehicle()) {
               player.sendMessage(Text.translatable("pocketislands.portal.dismount_required"), true);
            } else if (!player.isSpectator()) {
               PortalHelper.handlePortalEntry(player, pos);
               player.resetPortalCooldown();
            }
         }
      }
   }

   // Вызывается когда соседний блок обновляется
   @Override
   public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
      this.handleNeighborUpdate(state, world, pos);
   }

   private void handleNeighborUpdate(BlockState state, World world, BlockPos pos) {
      if (!world.isClient()) {
         Direction.Axis axis = state.get(AXIS);
         if (!PortalHelper.isFrameValidForPortal(world, pos, axis)) {
            world.breakBlock(pos, false);
            PersonalWorldsMod.LOGGER.debug("Portal block removed at {} - frame broken", pos);
         }
      }
   }

   // Вызывается когда блок заменяется другим
   @Override
   public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
      if (!state.isOf(newState.getBlock()) && world instanceof ServerWorld serverWorld) {
         PortalOwnershipManager ownershipManager = PortalOwnershipManager.get(serverWorld.getServer());
         ownershipManager.removePortal(world, pos);
      }
      super.onStateReplaced(state, world, pos, newState, moved);
   }

   @Override
   public boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
      return true;
   }

   // Возвращает ось портала из состояния блока
   public static Direction.Axis getAxis(BlockState state) {
      return state.get(AXIS);
   }

   static {
      AXIS = Properties.HORIZONTAL_AXIS;
      COLOR = EnumProperty.of("color", PortalColor.class);
      X_SHAPE = Block.createCuboidShape(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);
      Z_SHAPE = Block.createCuboidShape(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);
   }
}
