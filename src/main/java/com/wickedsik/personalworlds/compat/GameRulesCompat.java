package com.wickedsik.personalworlds.compat;

import com.wickedsik.personalworlds.PersonalWorldsMod;
import com.wickedsik.personalworlds.config.ModConfig;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameRules;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;

/**
 * Утилиты для копирования игровых правил (gamrules) в новые измерения.
 */
public final class GameRulesCompat {
   private static Map<String, GameRules.Key<?>> ruleKeysByName;

   private GameRulesCompat() {
   }

   // Копирует все игровые правила из основного мира в конфиг нового измерения
   public static void applyGameRules(RuntimeWorldConfig config, MinecraftServer server) {
      GameRules overworldRules = server.getOverworld().getGameRules();
      copyAllRules(config, overworldRules);
      Map<String, Object> overrides = ModConfig.get().dimensionGameRules;
      if (overrides != null && !overrides.isEmpty()) {
         applyOverrides(config, overworldRules, overrides);
      }
   }

   // Копирует все правила из исходного мира
   private static void copyAllRules(final RuntimeWorldConfig config, final GameRules overworldRules) {
      overworldRules.accept(new GameRules.Visitor() {
         public <T extends GameRules.Rule<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
            config.setGameRule(key, overworldRules.get(key).getValue());
         }
      });
   }

   // Применяет переопределения правил из конфига
   private static void applyOverrides(RuntimeWorldConfig config, GameRules overworldRules, Map<String, Object> overrides) {
      Map<String, GameRules.Key<?>> keyMap = getOrBuildKeyMap(overworldRules);

      for (Map.Entry<String, Object> entry : overrides.entrySet()) {
         String ruleName = entry.getKey();
         Object value = entry.getValue();
         GameRules.Key<?> key = keyMap.get(ruleName);
         if (key == null) {
            PersonalWorldsMod.LOGGER.warn("Unknown game rule '{}' in dimensionGameRules config, skipping", ruleName);
         } else if (value instanceof Boolean) {
            Boolean boolVal = (Boolean) value;
            config.setGameRule(key, boolVal);
         } else if (value instanceof Number) {
            Number numVal = (Number) value;
            config.setGameRule(key, numVal.intValue());
         } else {
            PersonalWorldsMod.LOGGER.warn("Game rule '{}' has unsupported value type: {}", ruleName, value.getClass().getSimpleName());
         }
      }
   }

   // Строит карту "имя правила → ключ правила" для быстрого поиска
   private static Map<String, GameRules.Key<?>> getOrBuildKeyMap(GameRules rules) {
      if (ruleKeysByName != null) {
         return ruleKeysByName;
      } else {
         final Map<String, GameRules.Key<?>> map = new HashMap<>();
         rules.accept(new GameRules.Visitor() {
            public <T extends GameRules.Rule<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
               map.put(key.getName(), key);
            }
         });
         ruleKeysByName = map;
         PersonalWorldsMod.LOGGER.debug("Built game rule key map with {} entries", map.size());
         return map;
      }
   }
}
