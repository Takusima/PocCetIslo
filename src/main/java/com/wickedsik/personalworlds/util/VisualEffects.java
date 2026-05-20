package com.wickedsik.personalworlds.util;

import com.wickedsik.personalworlds.compat.EntityCompat;
import com.wickedsik.personalworlds.config.ModConfig;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Визуальные эффекты: частицы, звуки, эффекты статуса.
 */
public final class VisualEffects {
   private static final int NAUSEA_DURATION = 200;
   private static final int BLINDNESS_AMPLIFIER = 2;
   private static final int BLINDNESS_DURATION = 60;
   private static final int REDSTONE_PARTICLE_COUNT = 150;

   private VisualEffects() {}

   // Эффекты отправления при телепортации
   public static void playTeleportDepartureEffects(ServerPlayerEntity player) {
      ModConfig config = ModConfig.get();
      ServerWorld world = EntityCompat.getServerWorld(player);
      Vec3d pos = EntityCompat.getPos(player);
      if (config.enableTeleportParticles) spawnRedstoneParticleFog(world, pos);
      if (config.enableTeleportSounds) world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
   }

   // Эффекты прибытия при телепортации
   public static void playTeleportArrivalEffects(ServerPlayerEntity player) {
      ModConfig config = ModConfig.get();
      ServerWorld world = EntityCompat.getServerWorld(player);
      Vec3d pos = EntityCompat.getPos(player);
      if (config.enableTeleportParticles) spawnRedstoneParticleFog(world, pos);
      if (config.enableTeleportSounds) world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.8F, 1.2F);
   }

   // Создаёт туман из красных частиц
   public static void spawnRedstoneParticleFog(ServerWorld world, Vec3d pos) {
      world.spawnParticles(ParticleTypes.DUST, pos.x, pos.y + 1.0, pos.z, REDSTONE_PARTICLE_COUNT, 0.8, 1.5, 0.8, 0.05);
      world.spawnParticles(ParticleTypes.DUST, pos.x, pos.y + 0.5, pos.z, REDSTONE_PARTICLE_COUNT / 2, 0.4, 0.75, 0.4, 0.025);
   }

   // Применяет эффект слепоты
   public static void applyBlindnessEffect(ServerPlayerEntity player) {
      player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, BLINDNESS_DURATION, BLINDNESS_AMPLIFIER, false, true, true));
   }

   // Удаляет эффект слепоты
   public static void removeBlindnessEffect(ServerPlayerEntity player) {
      if (player.hasStatusEffect(StatusEffects.BLINDNESS)) player.removeStatusEffect(StatusEffects.BLINDNESS);
   }

   // Применяет эффект тошноты
   public static void applyNauseaEffect(ServerPlayerEntity player) {
      player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, NAUSEA_DURATION, 0, false, true, true));
   }

   // Полная последовательность начала телепортации
   public static void playTeleportSequenceStart(ServerPlayerEntity player) {
      ModConfig config = ModConfig.get();
      ServerWorld world = EntityCompat.getServerWorld(player);
      Vec3d pos = EntityCompat.getPos(player);
      if (config.enableTeleportParticles) spawnRedstoneParticleFog(world, pos);
      applyBlindnessEffect(player);
      if (config.enableTeleportSounds) world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
   }

   // Полная последовательность завершения телепортации
   public static void playTeleportSequenceEnd(ServerPlayerEntity player) {
      ModConfig config = ModConfig.get();
      ServerWorld world = EntityCompat.getServerWorld(player);
      Vec3d pos = EntityCompat.getPos(player);
      if (config.enableTeleportParticles) spawnRedstoneParticleFog(world, pos);
      removeBlindnessEffect(player);
      applyNauseaEffect(player);
      if (config.enableTeleportSounds) world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.8F, 1.2F);
   }

   // Эффекты активации портала
   public static void playPortalActivationEffects(World world, BlockPos center) {
      if (ModConfig.get().enablePortalActivationEffects && world instanceof ServerWorld serverWorld) {
         serverWorld.spawnParticles(ParticleTypes.DUST, center.getX() + 0.5, center.getY() + 1.5, center.getZ() + 0.5, 100, 1.0, 2.0, 1.0, 0.1);
         serverWorld.spawnParticles(ParticleTypes.PORTAL, center.getX() + 0.5, center.getY() + 2.0, center.getZ() + 0.5, 50, 1.0, 0.5, 1.0, 0.5);
      }
   }

   // Эффект получения приглашения
   public static void playInvitationReceivedEffect(ServerPlayerEntity guest) {
      if (ModConfig.get().enableInvitationNotifications) {
         guest.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.5F, 1.2F);
      }
   }

   // Эффект отзыва приглашения
   public static void playInvitationRevokedEffect(ServerPlayerEntity guest) {
      if (ModConfig.get().enableInvitationNotifications) {
         guest.playSound(SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 0.7F, 0.5F);
      }
   }

   // Эффект отправки приглашения
   public static void playInvitationSentEffect(ServerPlayerEntity owner) {
      if (ModConfig.get().enableInvitationNotifications) {
         owner.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.3F, 1.5F);
      }
   }

   // Эффект входа в измерение
   public static void playDimensionEntryEffect(ServerPlayerEntity player) {
      if (ModConfig.get().enableTeleportSounds) {
         player.playSound(SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.PLAYERS, 0.5F, 1.5F);
      }
   }

   // Эффект выхода из измерения
   public static void playDimensionExitEffect(ServerPlayerEntity player) {
      if (ModConfig.get().enableTeleportSounds) {
         player.playSound(SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.PLAYERS, 0.3F, 1.2F);
      }
   }

   // Эффект предупреждения для администратора
   public static void playAdminWarningEffect(ServerPlayerEntity admin) {
      admin.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.PLAYERS, 1.0F, 0.5F);
   }

   // Эффект успешного действия администратора
   public static void playAdminSuccessEffect(ServerPlayerEntity admin) {
      admin.playSound(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.PLAYERS, 0.3F, 2.0F);
   }
}
