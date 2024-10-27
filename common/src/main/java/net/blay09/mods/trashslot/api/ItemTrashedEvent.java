package net.blay09.mods.trashslot.api;

import net.blay09.mods.balm.api.event.BalmEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public abstract class ItemTrashedEvent extends BalmEvent {
    private final Player player;
    private final ItemStack itemStack;

    public ItemTrashedEvent(Player player, ItemStack itemStack) {
        this.player = player;
        this.itemStack = itemStack;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public static class Pre extends ItemTrashedEvent {
        public Pre(Player player, ItemStack itemStack) {
            super(player, itemStack);
        }
    }

    public static class Post extends ItemTrashedEvent {
        public Post(Player player, ItemStack itemStack) {
            super(player, itemStack);
        }
    }
}