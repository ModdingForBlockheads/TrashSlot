package net.blay09.mods.trashslot.api;

import net.blay09.mods.balm.api.event.BalmEvent;
import net.minecraft.world.entity.player.Player;

public class TrashSlotEmptiedEvent extends BalmEvent {
    private final Player player;

    public TrashSlotEmptiedEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
}
