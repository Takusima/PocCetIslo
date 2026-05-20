package com.wickedsik.personalworlds.portal;

import net.minecraft.util.StringIdentifiable;

public enum PortalColor implements StringIdentifiable {
    WHITE("white", "White"), RED("red", "Red"), BLUE("blue", "Blue"), GREEN("green", "Green");
    // Добавь остальные цвета по аналогии

    private final String name, display;
    PortalColor(String name, String display) { this.name = name; this.display = display; }

    @Override
    public String asString() { return name; } // ✅ Обязательно!

    public String getName() { return name; }
    public String getDisplayName() { return display; }

    public static PortalColor byName(String name) {
        for (PortalColor c : values()) if (c.name.equals(name)) return c;
        return WHITE;
    }
}