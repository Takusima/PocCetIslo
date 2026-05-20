package com.wickedsik.personalworlds.client.screen;

import com.wickedsik.personalworlds.network.ModNetworking;
import com.wickedsik.personalworlds.network.NetworkHandlerClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class VisitRequestScreen extends Screen {

    private static final int BACKGROUND_WIDTH = 300;
    private static final int BACKGROUND_HEIGHT = 120;

    private final String requesterName;
    private int x;
    private int y;

    public VisitRequestScreen(String requesterName) {
        super(Text.translatable("pocketislands.gui.visit_request.title"));
        this.requesterName = requesterName;
    }

    @Override
    protected void init() {
        super.init();
        this.x = (this.width - BACKGROUND_WIDTH) / 2;
        this.y = (this.height - BACKGROUND_HEIGHT) / 2;

        ButtonWidget acceptButton = ButtonWidget.builder(
                Text.translatable("pocketislands.gui.visit_request.accept"),
                button -> handleAccept()
            )
            .dimensions(this.x + 20, this.y + 80, 120, 20)
            .build();

        ButtonWidget denyButton = ButtonWidget.builder(
                Text.translatable("pocketislands.gui.visit_request.deny"),
                button -> handleDeny()
            )
            .dimensions(this.x + 160, this.y + 80, 120, 20)
            .build();

        this.addDrawableChild(acceptButton);
        this.addDrawableChild(denyButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        context.fill(this.x, this.y, this.x + BACKGROUND_WIDTH, this.y + BACKGROUND_HEIGHT, 0xFF1A1A2E);
        context.drawBorder(this.x, this.y, BACKGROUND_WIDTH, BACKGROUND_HEIGHT, 0xFF4A4A6A);

        Text title = Text.translatable("pocketislands.gui.visit_request.title").formatted(Formatting.WHITE);
        context.drawTextWithShadow(this.textRenderer, title,
            this.x + BACKGROUND_WIDTH / 2 - this.textRenderer.getWidth(title.getString()) / 2,
            this.y + 10, 0xFFFFFF);

        Text requestText = Text.translatable("pocketislands.gui.visit_request.message", this.requesterName)
            .formatted(Formatting.GRAY);
        context.drawTextWithShadow(this.textRenderer, requestText,
            this.x + BACKGROUND_WIDTH / 2 - this.textRenderer.getWidth(requestText.getString()) / 2,
            this.y + 35, 0xFFFFFF);

        Text hint = Text.translatable("pocketislands.gui.visit_request.hint").formatted(Formatting.DARK_GRAY);
        context.drawTextWithShadow(this.textRenderer, hint,
            this.x + BACKGROUND_WIDTH / 2 - this.textRenderer.getWidth(hint.getString()) / 2,
            this.y + 58, 0xAAAAAA);

        super.render(context, mouseX, mouseY, delta);
    }

    private void handleAccept() {
        PacketByteBuf buf = ModNetworking.createBuf();
        buf.writeString(requesterName);
        buf.writeBoolean(true);
        ModNetworking.sendToServer(ModNetworking.INVITATION_RESPONSE, buf);
        NetworkHandlerClient.clearCurrentVisitRequest();
        if (this.client != null) {
            this.client.setScreen(null);
        }
    }

    private void handleDeny() {
        PacketByteBuf buf = ModNetworking.createBuf();
        buf.writeString(requesterName);
        buf.writeBoolean(false);
        ModNetworking.sendToServer(ModNetworking.INVITATION_RESPONSE, buf);
        NetworkHandlerClient.clearVisitRequest();
        if (this.client != null) {
            this.client.setScreen(null);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            handleDeny();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}