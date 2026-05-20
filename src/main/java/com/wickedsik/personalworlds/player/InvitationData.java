package com.wickedsik.personalworlds.player;

import com.wickedsik.personalworlds.compat.NbtCompat;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;

/**
 * Данные приглашения: кто пригласил, кого, когда, и разрешён ли всегда вход.
 */
public record InvitationData(UUID ownerUuid, String ownerName, long invitedAt, boolean alwaysWelcome) {
   public InvitationData(UUID ownerUuid, String ownerName, long invitedAt) {
      this(ownerUuid, ownerName, invitedAt, false);
   }

   // Переключает флаг "всегда разрешён вход"
   public InvitationData withToggledAlwaysWelcome() {
      return new InvitationData(this.ownerUuid, this.ownerName, this.invitedAt, !this.alwaysWelcome);
   }

   public NbtCompound toNbt() {
      NbtCompound nbt = new NbtCompound();
      NbtCompat.putUuid(nbt, "OwnerUuid", this.ownerUuid);
      nbt.putString("OwnerName", this.ownerName);
      nbt.putLong("InvitedAt", this.invitedAt);
      nbt.putBoolean("AlwaysWelcome", this.alwaysWelcome);
      return nbt;
   }

   public static InvitationData fromNbt(NbtCompound nbt) {
      boolean alwaysWelcome = NbtCompat.getBoolean(nbt, "AlwaysWelcome", false);
      return new InvitationData(
         NbtCompat.getUuid(nbt, "OwnerUuid"),
         NbtCompat.getString(nbt, "OwnerName", "Unknown"),
         NbtCompat.getLong(nbt, "InvitedAt", 0L),
         alwaysWelcome
      );
   }
}
