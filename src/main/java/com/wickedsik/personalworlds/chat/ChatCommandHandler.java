package com.wickedsik.personalworlds.chat;

import com.wickedsik.personalworlds.PersonalWorldsMod;
import com.wickedsik.personalworlds.network.NetworkHandler;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ChatCommandHandler {
    private static final String PRUNUS = "Prunus";
    private static final String MALUS = "Malus";

    private ChatCommandHandler() {}

    public static void register() {
        PersonalWorldsMod.LOGGER.info("Registering chat handler...");
        
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String text = message.getContent().getString().trim();
            if (text.equalsIgnoreCase(PRUNUS) || text.equalsIgnoreCase(MALUS)) {
                if (text.equalsIgnoreCase(PRUNUS)) {
                    NetworkHandler.handlePrunusCommand(sender);
                } else {
                    NetworkHandler.handleMalusCommand(sender);
                }
                return true; // ✅ Отменяем отображение в чате
            }
            return false; // Показываем как обычно
        });
    }

    public static boolean isCommand(String msg) {
        if (msg == null) return false;
        String t = msg.trim();
        return t.equalsIgnoreCase(PRUNUS) || t.equalsIgnoreCase(MALUS);
    }

    public static String getCommand(String msg) {
        if (msg == null) return null;
        String t = msg.trim();
        if (t.equalsIgnoreCase(PRUNUS)) return PRUNUS;
        if (t.equalsIgnoreCase(MALUS)) return MALUS;
        return null;
    }
}