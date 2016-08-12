package net.blay09.mods.trashslot;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class CommonProxy {

    private final HashSet<String> modInstalled = new HashSet<>();

    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void addScheduledTask(Runnable runnable) {
        FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(runnable);
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (modInstalled.contains(event.player.getName())) {
            patchContainer(event.player, event.player.inventoryContainer);
        }
    }

    @SubscribeEvent
    public void onOpenContainer(PlayerContainerEvent.Open event) {
        if (event.getEntityPlayer().openContainer == event.getEntityPlayer().inventoryContainer && modInstalled.contains(event.getEntityPlayer().getName())) {
            if (findSlotTrash(event.getEntityPlayer().inventoryContainer) == null) {
                patchContainer(event.getEntityPlayer(), event.getEntityPlayer().inventoryContainer);
            }
        }
    }

    protected SlotTrash patchContainer(EntityPlayer entityPlayer, Container container) {
        SlotTrash slot = new SlotTrash(entityPlayer, 152, 165);
        slot.slotNumber = container.inventorySlots.size();
        container.inventorySlots.add(slot);
        container.inventoryItemStacks.add(null);
        return slot;
    }

    protected SlotTrash unpatchContainer(Container container) {
        for (int i = container.inventorySlots.size() - 1; i >= 0; i--) {
            if (container.inventorySlots.get(i).getClass() == SlotTrash.class) {
                return (SlotTrash) container.inventorySlots.remove(i);
            }
        }
        return null;
    }

    public Slot findSlotTrash(Container container) {
        List slots = container.inventorySlots;
        for (int i = slots.size() - 1; i >= 0; i--) {
            if (slots.get(i).getClass() == SlotTrash.class) {
                return (Slot) slots.get(i);
            }
        }
        return null;
    }

    public void checkNetwork(Map<String, String> map, Side side) {}

    public void receivedHello(EntityPlayer playerEntity) {
        modInstalled.add(playerEntity.getName());
        if (findSlotTrash(playerEntity.inventoryContainer) == null) {
            patchContainer(playerEntity, playerEntity.inventoryContainer);
        }
    }
}
