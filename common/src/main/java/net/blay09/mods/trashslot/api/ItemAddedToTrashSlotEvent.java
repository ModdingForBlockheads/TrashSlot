package net.blay09.mods.trashslot.api;

import net.blay09.mods.balm.api.event.BalmEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ItemAddedToTrashSlotEvent extends BalmEvent {
    private final Player player;
    private final ItemStack itemStack;

    public ItemAddedToTrashSlotEvent(Player player, ItemStack itemStack) {
        this.player = player;
        this.itemStack = itemStack;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}