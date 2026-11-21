package com.sirens.visuals;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class EmoteScreen extends Screen {
    static final String[] N = {"Wave", "Love", "Party", "Angry", "Happy", "Music"};

    public EmoteScreen() { super(Component.literal("Emotes")); }

    @Override
    protected void init() {
        for (int i = 0; i < N.length; i++) {
            final int k = i;
            addRenderableWidget(Button.builder(Component.literal(N[i]), b -> { Visuals.playEmote(k); onClose(); })
                    .bounds(this.width / 2 - 100 + (i % 2) * 100, this.height / 2 - 40 + (i / 2) * 24, 96, 20).build());
        }
    }
}
