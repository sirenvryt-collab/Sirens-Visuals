package com.sirens.visuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

public class SettingsScreen extends Screen {
    static final int PER = 12;
    final Screen parent;
    int page = 0;

    public SettingsScreen(Screen parent) {
        super(Component.literal("Sirens Visuals"));
        this.parent = parent;
    }

    static Component label(Opts.Opt o) {
        return Component.literal(o.label() + ": " + (Config.getB(o.f()) ? "ON" : "OFF"));
    }

    @Override
    protected void init() {
        List<Opts.Opt> all = Opts.ALL;
        int cx = this.width / 2, start = page * PER;
        for (int i = 0; i < PER && start + i < all.size(); i++) {
            Opts.Opt o = all.get(start + i);
            int x = cx - 155 + (i % 2) * 160, y = 34 + (i / 2) * 24;
            if (o.isBool()) {
                addRenderableWidget(Button.builder(label(o), b -> {
                    Config.setB(o.f(), !Config.getB(o.f()));
                    b.setMessage(label(o));
                }).bounds(x, y, 150, 20).build());
            } else {
                addRenderableWidget(new FSlider(x, y, 150, 20, o));
            }
        }
        int pages = (all.size() + PER - 1) / PER;
        addRenderableWidget(Button.builder(Component.literal("< Prev"), b -> { if (page > 0) { page--; rebuildWidgets(); } })
                .bounds(cx - 155, this.height - 28, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(cx - 40, this.height - 28, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Next >"), b -> { if (page < pages - 1) { page++; rebuildWidgets(); } })
                .bounds(cx + 85, this.height - 28, 70, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        int pages = (Opts.ALL.size() + PER - 1) / PER;
        g.drawCenteredString(this.font, "Sirens Visuals  (page " + (page + 1) + "/" + pages + ")", this.width / 2, 12, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        Config.save();
        Minecraft.getInstance().setScreen(parent);
    }

    static class FSlider extends AbstractSliderButton {
        final Opts.Opt o;
        FSlider(int x, int y, int w, int h, Opts.Opt o) {
            super(x, y, w, h, Component.empty(), (Config.getF(o.f()) - o.min()) / (o.max() - o.min()));
            this.o = o;
            updateMessage();
        }
        @Override protected void updateMessage() {
            setMessage(Component.literal(o.label() + ": " + String.format("%.2f", Config.getF(o.f()))));
        }
        @Override protected void applyValue() {
            Config.setF(o.f(), (float) (o.min() + this.value * (o.max() - o.min())));
        }
    }
}
