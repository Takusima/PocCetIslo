package com.wickedsik.personalworlds.command;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/**
 * Результат выполнения команды.
 * Содержит статус (успех/ошибка), текст сообщения и флаг широковещания.
 */
public record CommandResult(boolean success, Text message, boolean broadcast) {
    public static final int SUCCESS = 1;
    public static final int FAILURE = 0;

    public static CommandResult success(Text message) {
        return new CommandResult(true, message, false);
    }

    public static CommandResult successBroadcast(Text message) {
        return new CommandResult(true, message, true);
    }

    public static CommandResult error(Text message) {
        return new CommandResult(false, message, false);
    }

    public static CommandResult silent() {
        return new CommandResult(true, null, false);
    }

    /**
     * Преобразует результат в код возврата для командного движка (1 = успех, 0 = ошибка).
     */
    public int toCommandReturn() {
        return this.success ? 1 : 0;
    }

    /**
     * Применяет результат к источнику команды: отправляет сообщение или ошибку.
     *
     * @param source Источник команды (админ/консоль)
     * @return 1 если успешно, 0 если ошибка
     */
    public int applyTo(ServerCommandSource source) {
        if (this.message != null) {
            if (this.success) {
                Text msg = this.message;
                source.sendFeedback(() -> msg, this.broadcast);
            } else {
                source.sendError(this.message);
            }
        }
        return this.toCommandReturn();
    }
}