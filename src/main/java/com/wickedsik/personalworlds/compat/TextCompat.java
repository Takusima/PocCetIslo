package com.wickedsik.personalworlds.compat;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;

/**
 * Утилиты для работы с текстовыми сообщениями.
 */
public final class TextCompat {
   private TextCompat() {
   }

   // Создаёт событие "выполнить команду" при клике на текст
   public static ClickEvent runCommand(String command) {
      return new ClickEvent(ClickEvent.Action.RUN_COMMAND, command);
   }

   // Создаёт событие "предложить команду" при клике на текст
   public static ClickEvent suggestCommand(String command) {
      return new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command);
   }

   // Создаёт событие "открыть URL" при клике на текст
   public static ClickEvent openUrl(String url) {
      return new ClickEvent(ClickEvent.Action.OPEN_URL, url);
   }

   // Создаёт событие "показать текст" при наведении мыши
   public static HoverEvent showText(Text text) {
      return new HoverEvent(HoverEvent.Action.SHOW_TEXT, text);
   }
}
