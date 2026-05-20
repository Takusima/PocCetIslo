package com.wickedsik.personalworlds;

import com.wickedsik.personalworlds.chat.ChatCommandHandler;
import com.wickedsik.personalworlds.command.ModCommands;
import com.wickedsik.personalworlds.compat.VoiceChatCompatibility;
import com.wickedsik.personalworlds.config.ModConfig;
import com.wickedsik.personalworlds.dimension.generator.ModChunkGenerators;
import com.wickedsik.personalworlds.event.ModEventHandlers;
import com.wickedsik.personalworlds.network.ModNetworking;
import com.wickedsik.personalworlds.registry.ModBlocks;
import com.wickedsik.personalworlds.registry.ModItems;
import com.wickedsik.personalworlds.server.WorldBorderManager;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Главный класс мода Pocket Islands.
 * Инициализирует все компоненты мода.
 */
public class PersonalWorldsMod implements ModInitializer {
    public static final String MOD_ID = "personalworlds";
    public static final Logger LOGGER = LoggerFactory.getLogger("PocketIslands");

    @Override
    public void onInitialize() {
        LOGGER.info("Pocket Islands initializing...");

        // Загрузка конфигурации
        ModConfig.load();

        // Регистрация блоков и предметов
        ModBlocks.register();
        ModItems.register();

        // Регистрация генераторов чанков
        ModChunkGenerators.register();

        // Регистрация обработчиков событий
        ModEventHandlers.register();

        // Регистрация команд
        ModCommands.register();

        // Регистрация сетевых пакетов (серверная часть)
        ModNetworking.registerServerReceivers();

        // Регистрация обработчика чат-команд (Prunus/Malus)
        ChatCommandHandler.register();

        // Регистрация менеджера границ мира
        WorldBorderManager.register();

        // Инициализация интеграции с Simple Voice Chat
        VoiceChatCompatibility.initialize();

        LOGGER.info("Pocket Islands initialized!");
    }
}
