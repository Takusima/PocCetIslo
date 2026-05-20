package com.wickedsik.personalworlds.player;

import com.wickedsik.personalworlds.PersonalWorldsMod;
import com.wickedsik.personalworlds.compat.IdentifierCompat;
import com.wickedsik.personalworlds.compat.NbtCompat;
import com.wickedsik.personalworlds.compat.PersistentStateCompat;
import com.wickedsik.personalworlds.util.DataValidator;
import java.util.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

/**
 * Управляет данными игроков: позиции возврата, приглашения, текущие измерения.
 */
public class PlayerDataManager extends PersistentState {
   private static final String DATA_NAME = "personalworlds_player_data";
   private final Map<UUID, ReturnData> returnPositions = new HashMap<>();
   private final Map<UUID, List<InvitationData>> receivedInvitations = new HashMap<>();
   private final Map<UUID, Set<UUID>> sentInvitations = new HashMap<>();
   private final Map<UUID, RegistryKey<World>> currentPocketDimensions = new HashMap<>();

   // === Позиции возврата ===
   public void setReturnData(UUID playerUuid, ReturnData data) {
      if (data != null && DataValidator.isValidUuid(playerUuid)) {
         returnPositions.put(playerUuid, data);
         this.markDirty();
      }
   }

   public Optional<ReturnData> getReturnData(UUID playerUuid) {
      return Optional.ofNullable(returnPositions.get(playerUuid));
   }

   public void clearReturnData(UUID playerUuid) {
      if (returnPositions.remove(playerUuid) != null) this.markDirty();
   }

   public boolean hasReturnData(UUID playerUuid) {
      return returnPositions.containsKey(playerUuid);
   }

   // === Приглашения ===
   public boolean addInvitation(UUID ownerUuid, String ownerName, UUID guestUuid) {
      return addInvitation(ownerUuid, ownerName, guestUuid, false);
   }

   public boolean addInvitation(UUID ownerUuid, String ownerName, UUID guestUuid, boolean alwaysWelcome) {
      if (hasInvitationFrom(guestUuid, ownerUuid)) return false;
      receivedInvitations.computeIfAbsent(guestUuid, k -> new ArrayList<>())
         .add(new InvitationData(ownerUuid, ownerName, System.currentTimeMillis(), alwaysWelcome));
      sentInvitations.computeIfAbsent(ownerUuid, k -> new HashSet<>()).add(guestUuid);
      this.markDirty();
      return true;
   }

   public boolean removeInvitation(UUID ownerUuid, UUID guestUuid) {
      boolean removed = false;
      List<InvitationData> guestInvs = receivedInvitations.get(guestUuid);
      if (guestInvs != null) {
         removed = guestInvs.removeIf(inv -> inv.ownerUuid().equals(ownerUuid));
         if (guestInvs.isEmpty()) receivedInvitations.remove(guestUuid);
      }
      Set<UUID> ownerSent = sentInvitations.get(ownerUuid);
      if (ownerSent != null) {
         ownerSent.remove(guestUuid);
         if (ownerSent.isEmpty()) sentInvitations.remove(ownerUuid);
      }
      if (removed) this.markDirty();
      return removed;
   }

   public boolean hasInvitationFrom(UUID guestUuid, UUID ownerUuid) {
      return getInvitationFrom(guestUuid, ownerUuid).isPresent();
   }

   public Optional<InvitationData> getInvitationFrom(UUID guestUuid, UUID ownerUuid) {
      List<InvitationData> invs = receivedInvitations.get(guestUuid);
      return invs == null ? Optional.empty() : invs.stream().filter(i -> i.ownerUuid().equals(ownerUuid)).findFirst();
   }

   public boolean isAlwaysWelcome(UUID ownerUuid, UUID guestUuid) {
      return getInvitationFrom(guestUuid, ownerUuid).map(InvitationData::alwaysWelcome).orElse(false);
   }

   public List<InvitationData> getReceivedInvitations(UUID guestUuid) {
      return receivedInvitations.getOrDefault(guestUuid, Collections.emptyList());
   }

   public Set<UUID> getSentInvitations(UUID ownerUuid) {
      return sentInvitations.getOrDefault(ownerUuid, Collections.emptySet());
   }

   // === Текущее карманное измерение ===
   public void setCurrentPocketDimension(UUID playerUuid, RegistryKey<World> dimension) {
      currentPocketDimensions.put(playerUuid, dimension);
      this.markDirty();
   }

   public Optional<RegistryKey<World>> getCurrentPocketDimension(UUID playerUuid) {
      return Optional.ofNullable(currentPocketDimensions.get(playerUuid));
   }

   public void clearCurrentPocketDimension(UUID playerUuid) {
      if (currentPocketDimensions.remove(playerUuid) != null) this.markDirty();
   }

   // === NBT ===
   @Override
   public NbtCompound writeNbt(NbtCompound nbt) {
      NbtCompound returnDataNbt = new NbtCompound();
      for (var entry : returnPositions.entrySet()) {
         returnDataNbt.put(entry.getKey().toString(), entry.getValue().toNbt());
      }
      nbt.put("ReturnPositions", returnDataNbt);

      NbtCompound receivedNbt = new NbtCompound();
      for (var entry : receivedInvitations.entrySet()) {
         NbtList invList = new NbtList();
         for (InvitationData inv : entry.getValue()) invList.add(inv.toNbt());
         receivedNbt.put(entry.getKey().toString(), invList);
      }
      nbt.put("ReceivedInvitations", receivedNbt);

      NbtCompound sentNbt = new NbtCompound();
      for (var entry : sentInvitations.entrySet()) {
         NbtList guestList = new NbtList();
         for (UUID guestUuid : entry.getValue()) {
            NbtCompound guestNbt = new NbtCompound();
            NbtCompat.putUuid(guestNbt, "Uuid", guestUuid);
            guestList.add(guestNbt);
         }
         sentNbt.put(entry.getKey().toString(), guestList);
      }
      nbt.put("SentInvitations", sentNbt);

      NbtCompound pocketDimNbt = new NbtCompound();
      for (var entry : currentPocketDimensions.entrySet()) {
         pocketDimNbt.putString(entry.getKey().toString(), entry.getValue().getValue().toString());
      }
      nbt.put("CurrentPocketDimensions", pocketDimNbt);
      return nbt;
   }

   public static PlayerDataManager fromNbt(NbtCompound nbt) {
      PlayerDataManager manager = new PlayerDataManager();

      if (NbtCompat.contains(nbt, "ReturnPositions", NbtElement.COMPOUND_TYPE)) {
         NbtCompound returnDataNbt = NbtCompat.getCompound(nbt, "ReturnPositions");
         for (String key : returnDataNbt.getKeys()) {
            try {
               UUID uuid = UUID.fromString(key);
               manager.returnPositions.put(uuid, ReturnData.fromNbt(NbtCompat.getCompound(returnDataNbt, key)));
            } catch (IllegalArgumentException e) { /* пропускаем */ }
         }
      }

      if (NbtCompat.contains(nbt, "ReceivedInvitations", NbtElement.COMPOUND_TYPE)) {
         NbtCompound receivedNbt = NbtCompat.getCompound(nbt, "ReceivedInvitations");
         for (String key : receivedNbt.getKeys()) {
            try {
               UUID guestUuid = UUID.fromString(key);
               NbtList invList = NbtCompat.getList(receivedNbt, key, NbtElement.COMPOUND_TYPE);
               List<InvitationData> invitations = new ArrayList<>();
               for (int i = 0; i < invList.size(); i++) {
                  invitations.add(InvitationData.fromNbt(invList.getCompound(i)));
               }
               if (!invitations.isEmpty()) manager.receivedInvitations.put(guestUuid, invitations);
            } catch (IllegalArgumentException e) { /* пропускаем */ }
         }
      }

      if (NbtCompat.contains(nbt, "SentInvitations", NbtElement.COMPOUND_TYPE)) {
         NbtCompound sentNbt = NbtCompat.getCompound(nbt, "SentInvitations");
         for (String key : sentNbt.getKeys()) {
            try {
               UUID ownerUuid = UUID.fromString(key);
               NbtList guestList = NbtCompat.getList(sentNbt, key, NbtElement.COMPOUND_TYPE);
               Set<UUID> guests = new HashSet<>();
               for (int i = 0; i < guestList.size(); i++) {
                  UUID guestUuid = NbtCompat.getUuid(guestList.getCompound(i), "Uuid");
                  if (guestUuid != null) guests.add(guestUuid);
               }
               if (!guests.isEmpty()) manager.sentInvitations.put(ownerUuid, guests);
            } catch (IllegalArgumentException e) { /* пропускаем */ }
         }
      }

      if (NbtCompat.contains(nbt, "CurrentPocketDimensions", NbtElement.COMPOUND_TYPE)) {
         NbtCompound pocketDimNbt = NbtCompat.getCompound(nbt, "CurrentPocketDimensions");
         for (String key : pocketDimNbt.getKeys()) {
            try {
               UUID playerUuid = UUID.fromString(key);
               Identifier dimId = IdentifierCompat.fromNbtString(NbtCompat.getString(pocketDimNbt, key, ""));
               if (dimId != null) {
                  manager.currentPocketDimensions.put(playerUuid, RegistryKey.of(RegistryKeys.WORLD, dimId));
               }
            } catch (IllegalArgumentException e) { /* пропускаем */ }
         }
      }

      return manager;
   }

   public static PlayerDataManager get(MinecraftServer server) {
      PersistentStateManager stateManager = server.getOverworld().getPersistentStateManager();
      return PersistentStateCompat.getOrCreate(stateManager, DATA_NAME, PlayerDataManager::new, PlayerDataManager::fromNbt);
   }
}
