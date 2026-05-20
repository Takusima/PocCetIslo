package com.wickedsik.personalworlds.client;

import com.wickedsik.personalworlds.PersonalWorldsMod;
import com.wickedsik.personalworlds.client.screen.PocketDimensionScreen;
import com.wickedsik.personalworlds.network.ModNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import org.lwjgl.glfw.GLFW;

/**
 * Клиентский обработчик событий.
 * Регистрирует клавишу F7 для открытия GUI карманного измерения.
 */
@Environment(EnvType.CLIENT)
public class ClientEventHandler implements ClientModInitializer {

    // Клавиша F7 для открытия GUI
    private static KeyBinding guiKeyBinding;

    // Флаг для предотвращения повторных срабатываний
    private static boolean wasKeyPressed = false;

    @Override
    public void onInitializeClient() {
        PersonalWorldsMod.LOGGER.info("Initializing client event handler...");

        // Регистрация клавиши F7
        guiKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.personalworlds.open_gui",           // Идентификатор клавиши
            InputUtil.Type.KEYSYM,                   // Тип ввода - клавиатура
            GLFW.GLFW_KEY_F7,                        // Код клавиши F7
            "category.personalworlds.general"        // Категория в настройках управления
        ));

        // Регистрация обработчика тиков клиента
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

        // Регистрация клиентских сетевых обработчиков
        ModNetworking.registerClientReceivers();

        PersonalWorldsMod.LOGGER.info("Client event handler initialized");
    }

    /**
     * Обработка тика клиента.
     * Проверяет нажатие клавиши F7.
     */
    private void onClientTick(MinecraftClient client) {
        if (client.player == null) {
            return;
        }

        boolean isKeyPressed = guiKeyBinding.isPressed();

        // Проверяем нажатие (не удержание) — срабатывает только один раз при нажатии
        if (isKeyPressed && !wasKeyPressed) {
            onGuiKeyPressed(client);
        }

        wasKeyPressed = isKeyPressed;
    }

    /**
     * Обработка нажатия клавиши GUI.
     * Отправляет запрос на сервер и открывает экран.
     */
    private void onGuiKeyPressed(MinecraftClient client) {
        PersonalWorldsMod.LOGGER.info("GUI key pressed - requesting player list from server");

        // Отправляем запрос на сервер для получения списка игроков
        PacketByteBuf buf = ModNetworking.createBuf();
        ModNetworking.sendToServer(ModNetworking.OPEN_GUI, buf);

        // Открываем GUI (список игроков обновится, когда придёт ответ от сервера)
        client.setScreen(new PocketDimensionScreen());
    }

    /**
     * Получение привязки клавиши GUI.
     * Используется другими классами для отображения подсказки.
     */
    public static KeyBinding getGuiKeyBinding() {
        return guiKeyBinding;
    }
}