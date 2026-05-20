package com.wickedsik.personalworlds.compat;

import com.wickedsik.personalworlds.PersonalWorldsMod;
import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Класс интеграции с модом Simple Voice Chat.
 * Обеспечивает автоматическое объединение игроков в аудио-группы
 * при принятии приглашения на посещение карманного измерения.
 *
 * Использует рефлексию для безопасного доступа к API Simple Voice Chat,
 * чтобы мод не падал при отсутствии голосового чата на сервере.
 */
public final class VoiceChatCompatibility {

   // Флаг наличия Simple Voice Chat на сервере
   private static boolean voiceChatPresent = false;

   // Рефлексивные ссылки на классы и методы Simple Voice Chat API
   private static Class<?> voiceChatApiClass = null;
   private static Class<?> groupClass = null;
   private static Method getGroupMethod = null;
   private static Method createGroupMethod = null;
   private static Method addPlayerToGroupMethod = null;
   private static Method removePlayerFromGroupMethod = null;
   private static Method getApiInstanceMethod = null;

   // Префикс для групп голосового чата Pocket Islands
   private static final String GROUP_PREFIX = "pocketislands_";

   private VoiceChatCompatibility() {
   }

   // Инициализация интеграции с Simple Voice Chat
   public static void initialize() {
      try {
         voiceChatApiClass = Class.forName("de.maxhenricg.voicechat.api.VoiceChatApi");
         groupClass = Class.forName("de.maxhenricg.voicechat.api.Group");

         getApiInstanceMethod = voiceChatApiClass.getMethod("getInstance");
         getGroupMethod = voiceChatApiClass.getMethod("getGroup", String.class);
         createGroupMethod = voiceChatApiClass.getMethod("createGroup", String.class, boolean.class);
         addPlayerToGroupMethod = groupClass.getMethod("addPlayer", UUID.class);
         removePlayerFromGroupMethod = groupClass.getMethod("removePlayer", UUID.class);

         voiceChatPresent = true;
         PersonalWorldsMod.LOGGER.info("Simple Voice Chat integration initialized successfully!");
      } catch (ClassNotFoundException e) {
         voiceChatPresent = false;
         PersonalWorldsMod.LOGGER.info("Simple Voice Chat not found. Voice chat integration disabled.");
      } catch (NoSuchMethodException e) {
         voiceChatPresent = false;
         PersonalWorldsMod.LOGGER.warn("Simple Voice Chat API methods not found. Voice chat integration disabled.", e);
      } catch (Exception e) {
         voiceChatPresent = false;
         PersonalWorldsMod.LOGGER.error("Failed to initialize Simple Voice Chat integration", e);
      }
   }

   // Проверяет, доступен ли Simple Voice Chat
   public static boolean isVoiceChatPresent() {
      return voiceChatPresent;
   }

   // Создаёт скрытую аудио-группу для двух игроков по UUID
   public static boolean createVoiceGroup(UUID player1Uuid, UUID player2Uuid, String groupName) {
      if (!voiceChatPresent) {
         return false;
      }
      try {
         Object apiInstance = getApiInstanceMethod.invoke(null);
         if (apiInstance == null) {
            return false;
         }
         String fullGroupName = GROUP_PREFIX + groupName;
         Object existingGroup = getGroupMethod.invoke(apiInstance, fullGroupName);
         if (existingGroup != null) {
            addPlayerToGroupMethod.invoke(existingGroup, player1Uuid);
            addPlayerToGroupMethod.invoke(existingGroup, player2Uuid);
            return true;
         }
         Object newGroup = createGroupMethod.invoke(apiInstance, fullGroupName, true);
         if (newGroup == null) {
            return false;
         }
         addPlayerToGroupMethod.invoke(newGroup, player1Uuid);
         addPlayerToGroupMethod.invoke(newGroup, player2Uuid);
         return true;
      } catch (Exception e) {
         PersonalWorldsMod.LOGGER.error("Failed to create voice group", e);
         return false;
      }
   }

   // Создаёт аудио-группу для двух игроков по их объектам
   public static boolean createVoiceGroup(ServerPlayerEntity player1, ServerPlayerEntity player2) {
      if (player1 == null || player2 == null) {
         return false;
      }
      String groupName = player1.getUuid().toString().substring(0, 8) + "_" +
                         player2.getUuid().toString().substring(0, 8);
      return createVoiceGroup(player1.getUuid(), player2.getUuid(), groupName);
   }

   // Удаляет игрока из аудио-группы
   public static boolean removePlayerFromVoiceGroup(UUID playerUuid, String groupName) {
      if (!voiceChatPresent) {
         return false;
      }
      try {
         Object apiInstance = getApiInstanceMethod.invoke(null);
         if (apiInstance == null) {
            return false;
         }
         String fullGroupName = GROUP_PREFIX + groupName;
         Object group = getGroupMethod.invoke(apiInstance, fullGroupName);
         if (group == null) {
            return false;
         }
         removePlayerFromGroupMethod.invoke(group, playerUuid);
         return true;
      } catch (Exception e) {
         PersonalWorldsMod.LOGGER.error("Failed to remove player from voice group", e);
         return false;
      }
   }

   // Удаляет игрока из всех аудио-групп Pocket Islands
   public static boolean removePlayerFromAllVoiceGroups(UUID playerUuid) {
      if (!voiceChatPresent) {
         return false;
      }
      try {
         Object apiInstance = getApiInstanceMethod.invoke(null);
         if (apiInstance == null) {
            return false;
         }
         try {
            Method getAllGroupsMethod = voiceChatApiClass.getMethod("getAllGroups");
            Object allGroups = getAllGroupsMethod.invoke(apiInstance);
            if (allGroups instanceof Iterable) {
               for (Object group : (Iterable<?>) allGroups) {
                  try {
                     Method getNameMethod = groupClass.getMethod("getName");
                     String groupName = (String) getNameMethod.invoke(group);
                     if (groupName != null && groupName.startsWith(GROUP_PREFIX)) {
                        removePlayerFromGroupMethod.invoke(group, playerUuid);
                     }
                  } catch (Exception e) {
                     // Пропускаем ошибки отдельных групп
                  }
               }
            }
         } catch (NoSuchMethodException e) {
            // Метод getAllGroups может не существовать
         }
         return true;
      } catch (Exception e) {
         PersonalWorldsMod.LOGGER.error("Failed to remove player from all voice groups", e);
         return false;
      }
   }

   // Проверяет, находится ли игрок в аудио-группе Pocket Islands
   public static boolean isInVoiceGroup(UUID playerUuid) {
      if (!voiceChatPresent) {
         return false;
      }
      try {
         Object apiInstance = getApiInstanceMethod.invoke(null);
         if (apiInstance == null) {
            return false;
         }
         try {
            Method getAllGroupsMethod = voiceChatApiClass.getMethod("getAllGroups");
            Object allGroups = getAllGroupsMethod.invoke(apiInstance);
            if (allGroups instanceof Iterable) {
               for (Object group : (Iterable<?>) allGroups) {
                  try {
                     Method getNameMethod = groupClass.getMethod("getName");
                     String groupName = (String) getNameMethod.invoke(group);
                     if (groupName != null && groupName.startsWith(GROUP_PREFIX)) {
                        Method getPlayersMethod = groupClass.getMethod("getPlayers");
                        Object players = getPlayersMethod.invoke(group);
                        if (players instanceof Iterable) {
                           for (Object player : (Iterable<?>) players) {
                              if (player instanceof UUID && player.equals(playerUuid)) {
                                 return true;
                              }
                           }
                        }
                     }
                  } catch (Exception e) {
                     // Пропускаем
                  }
               }
            }
         } catch (NoSuchMethodException e) {
            // Метод не найден
         }
         return false;
      } catch (Exception e) {
         return false;
      }
   }

   // Получает имя группы голосового чата для двух игроков
   public static String getVoiceGroupName(UUID player1Uuid, UUID player2Uuid) {
      return GROUP_PREFIX +
             player1Uuid.toString().substring(0, 8) + "_" +
             player2Uuid.toString().substring(0, 8);
   }
}
