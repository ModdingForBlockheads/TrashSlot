package net.blay09.mods.trashslot.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class TrashSlotSlot extends Slot {

    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("container/slot/trashslot");

    public static class TrashInventory implements Container {
        private ItemStack currentStack = ItemStack.EMPTY;

        @Override
        public int getContainerSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return currentStack.isEmpty();
        }

        @Override
        public ItemStack getItem(int index) {
            return currentStack;
        }

        @Override
        public ItemStack removeItem(int index, int count) {
            ItemStack itemStack = !currentStack.isEmpty() && count > 0 ? currentStack.split(count) : ItemStack.EMPTY;
            if (!itemStack.isEmpty()) {
                this.setChanged();
            }
            return itemStack;
        }

        @Override
        public ItemStack removeItemNoUpdate(int index) {
            ItemStack itemStack = currentStack;
            currentStack = ItemStack.EMPTY;
            return itemStack;
        }

        @Override
        public void setItem(int index, ItemStack stack) {
            currentStack = stack;
        }

        @Override
        public int getMaxStackSize() {
            return 64;
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void startOpen(Player player) {
        }

        @Override
        public void stopOpen(Player player) {
        }

        @Override
        public boolean canPlaceItem(int index, ItemStack stack) {
            return true;
        }

        @Override
        public void clearContent() {
            currentStack = ItemStack.EMPTY;
        }
    }

    public TrashSlotSlot() {
        super(new TrashInventory(), 0, 0, 0);
    }

    @Override
    public ResourceLocation getNoItemIcon() {
        return ICON;
    }
}
