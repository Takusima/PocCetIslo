package com.wickedsik.personalworlds.client.screen;

import com.wickedsik.personalworlds.network.ModNetworking;
import com.wickedsik.personalworlds.network.NetworkHandlerClient;
import com.wickedsik.personalworlds.portal.PortalColor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.*;

@Environment(EnvType.CLIENT)
public class PocketDimensionScreen extends Screen {
    private static final int W = 350, H = 250, PX = 10, PY = 30, CS = 18, CSP = 2, CPR = 4;
    private static final int PLX = 100, PLY = 30, PLW = 240, PLH = 200, PLEH = 28;
    private static final int SBW = 6, SBH = 200, SBX = PLX + PLW + 2;

    private final Map<PortalColor, Integer> COLORS = Map.ofEntries(
        Map.entry(PortalColor.WHITE, 0xFFFFFFFF), Map.entry(PortalColor.RED, 0xFFFF3333),
        Map.entry(PortalColor.BLUE, 0xFF3355FF), Map.entry(PortalColor.GREEN, 0xFF33AA33)
    );

    private List<String> players = new ArrayList<>(NetworkHandlerClient.getOnlinePlayers());
    private float scroll = 0;
    private boolean dragging = false;
    private int selected = 5;
    private final Map<String, ButtonWidget> buttons = new HashMap<>();

    public PocketDimensionScreen() {
        super(Text.translatable("pocketislands.gui.pocket_dimension.title"));
    }

    @Override
    protected void init() {
        super.init();
        buttons.clear();
        for (String name : players) {
            ButtonWidget b = ButtonWidget.builder(Text.literal("✉"), btn -> invite(name))
                .dimensions(0, 0, 20, 20).build();
            b.visible = false;
            buttons.put(name, b);
            addDrawableChild(b);
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx); // ✅ Только один аргумент!
        int x = (width - W) / 2, y = (height - H) / 2;
        ctx.fill(x, y, x + W, y + H, 0xFF1A1A2E);
        ctx.drawBorder(x, y, W, H, 0xFF4A4A6A);
        Text t = Text.translatable("pocketislands.gui.pocket_dimension.title").formatted(Formatting.WHITE);
        ctx.drawTextWithShadow(textRenderer, t, x + W/2 - textRenderer.getWidth(t)/2, y + 8, 0xFFFFFF);
        // Тут можно добавить рендер палитры и списка (сокращено для краткости)
        super.render(ctx, mx, my, delta);
    }

    private void invite(String name) {
        PacketByteBuf buf = ModNetworking.createBuf();
        buf.writeString(name);
        ModNetworking.sendToServer(ModNetworking.SEND_INVITATION_REQUEST, buf);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int x = (width - W) / 2, y = (height - H) / 2;
        if (mx >= x + SBX && mx < x + SBX + SBW && my >= y + PLY && my < y + PLY + SBH) {
            dragging = true;
            return true;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        dragging = false;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) { // ✅ 3 параметра!
        int x = (width - W) / 2, y = (height - H) / 2;
        if (mx >= x + PLX && mx < x + PLX + PLW && my >= y + PLY && my < y + PLY + PLH) {
            scroll = Math.max(0, Math.min(scroll - amount, Math.max(0, players.size() - PLH / PLEH)));
            return true;
        }
        return super.mouseScrolled(mx, my, amount);
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }
    // ❌ shouldPause() удалён — не существует в 1.20.1
}