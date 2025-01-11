package net.blay09.mods.trashslot.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.client.screen.*;
import net.blay09.mods.balm.mixin.AbstractContainerScreenAccessor;
import net.blay09.mods.balm.mixin.SlotAccessor;
import net.blay09.mods.trashslot.Hints;
import net.blay09.mods.trashslot.TrashSlot;
import net.blay09.mods.trashslot.config.TrashSlotConfig;
import net.blay09.mods.trashslot.TrashSlotSaveState;
import net.blay09.mods.trashslot.api.IGuiContainerLayout;
import net.blay09.mods.trashslot.client.deletion.DeletionProvider;
import net.blay09.mods.trashslot.client.gui.TrashSlotComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class TrashSlotGuiHandler {

    private static final TrashSlotSlot trashSlot = new TrashSlotSlot();
    private static TrashSlotComponent trashSlotComponent;
    private static ContainerSettings currentContainerSettings = ContainerSettings.NONE;
    private static boolean ignoreMouseUp;

    private static boolean sentMissingMessage;
    private static boolean isLeftMouseDown;

    private static Hint currentHint;

    public static void initialize() {
        Balm.getEvents().onEvent(ScreenInitEvent.Post.class, TrashSlotGuiHandler::onScreenInit);
        Balm.getEvents().onEvent(ScreenMouseEvent.Release.Pre.class, TrashSlotGuiHandler::onMouseRelease);
        Balm.getEvents().onEvent(ScreenMouseEvent.Click.Pre.class, TrashSlotGuiHandler::onMouseClick);
        Balm.getEvents().onEvent(ScreenKeyEvent.Press.Post.class, TrashSlotGuiHandler::onKeyPress);
        Balm.getEvents().onEvent(ContainerScreenDrawEvent.Background.class, TrashSlotGuiHandler::onBackgroundDrawn);
        Balm.getEvents().onEvent(ContainerScreenDrawEvent.Background.class, TrashSlotGuiHandler::onScreenDrawn);
    }

    private static void onScreenInit(ScreenInitEvent.Post event) {
        // Ignore screens from ReplayMod because they wrap every screen with their own class for some reason
        if (event.getScreen().getClass().getName().startsWith("com.replaymod")) {
            return;
        }

        Player player = Minecraft.getInstance().player;
        if (player != null && player.isSpectator()) {
            return;
        }

        if (event.getScreen() instanceof CreativeModeInventoryScreen) {
            currentContainerSettings = ContainerSettings.NONE;
            trashSlotComponent = null;
            return;
        }

        if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
            if (!TrashSlot.isServerSideInstalled && !sentMissingMessage) {
                TrashSlot.logger.info("TrashSlot is not installed on the server and thus will be unavailable.");
                MutableComponent noHabloEspanol = Component.translatable("trashslot.serverNotInstalled");
                noHabloEspanol.withStyle(ChatFormatting.RED);
                showHint(Hints.SERVER_NOT_INSTALLED, noHabloEspanol, 5000, true);
                sentMissingMessage = true;
                return;
            }

            // For some reason this event gets fired with GuiInventory right after opening the creative menu, AFTER it got fired for GuiContainerCreative
            if (screen instanceof InventoryScreen && player != null && player.getAbilities().instabuild) {
                return;
            }

            IGuiContainerLayout layout = LayoutManager.getLayout(screen);
            currentContainerSettings = TrashSlotSaveState.getSettings(screen, layout);
            if (currentContainerSettings != ContainerSettings.NONE) {
                trashSlotComponent = new TrashSlotComponent(screen, layout, currentContainerSettings, trashSlot);

                if (!currentContainerSettings.isEnabled() && !layout.isEnabledByDefault() && !ModKeyMappings.keyBindToggleSlot.getBinding()
                        .key()
                        .equals(InputConstants.UNKNOWN)) {
                    var hintMessage = Component.translatable("trashslot.hint.toggleOn", ModKeyMappings.keyBindToggleSlot.getBinding().key().getDisplayName());
                    showHint(Hints.TOGGLE_ON, hintMessage, 5000);
                }
            } else {
                trashSlotComponent = null;
            }
        } else {
            currentContainerSettings = ContainerSettings.NONE;
            trashSlotComponent = null;
        }
    }

    private static void onMouseRelease(ScreenMouseEvent.Release.Pre event) {
        if (event.getButton() == 0) {
            isLeftMouseDown = false;
        }

        if (ignoreMouseUp) {
            event.setCanceled(true);
            ignoreMouseUp = false;
        }
    }

    private static void onMouseClick(ScreenMouseEvent.Click.Pre event) {
        if (event.getButton() == 0) {
            isLeftMouseDown = true;
        }

        DeletionProvider deletionProvider = TrashSlotConfig.getDeletionProvider();
        if (deletionProvider == null || !currentContainerSettings.isEnabled()) {
            return;
        }

        int mouseButton = event.getButton();
        if (runKeyBindings(event.getScreen(), mouseButton, 0, 0)) {
            event.setCanceled(true);
            return;
        }

        if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
            double mouseX = event.getMouseX();
            double mouseY = event.getMouseY();
            if (((AbstractContainerScreenAccessor) screen).callIsHovering(trashSlot, mouseX, mouseY)) {
                Player player = Minecraft.getInstance().player;
                if (player != null) {
                    ItemStack mouseItem = screen.getMenu().getCarried();
                    boolean isRightClick = mouseButton == 1;
                    if (mouseItem.isEmpty()) {
                        deletionProvider.undeleteLast(player, trashSlot, isRightClick);
                    } else {
                        // check deny list first
                        var registryName = Balm.getRegistries().getKey(mouseItem.getItem());
                        if (registryName == null || !TrashSlotConfig.getActive().deletionDenyList.contains(registryName.toString())) {
                            deletionProvider.deleteMouseItem(player, mouseItem, trashSlot, isRightClick);
                        } else {
                            var hintMessage = Component.translatable("trashslot.hint.deletionDenied");
                            hintMessage.withStyle(ChatFormatting.RED);
                            showHint(Hints.DELETION_DENIED, hintMessage, 1000, true);
                        }
                    }

                    event.setCanceled(true);
                    ignoreMouseUp = true;
                }
            } else if (trashSlotComponent.isInside((int) mouseX, (int) mouseY)) {
                // Prevent click-through on the background and border of the slot
                event.setCanceled(true);
                ignoreMouseUp = true;
            }
        }
    }

    private static void onKeyPress(ScreenKeyEvent.Press.Post event) {
        DeletionProvider deletionProvider = TrashSlotConfig.getDeletionProvider();
        if (deletionProvider == null) {
            return;
        }

        int keyCode = event.getKey();
        int scanCode = event.getScanCode();
        if (runKeyBindings(event.getScreen(), keyCode, scanCode, event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    private static boolean runKeyBindings(Screen screen, int keyCode, int scanCode, int modifiers) {
        DeletionProvider deletionProvider = TrashSlotConfig.getDeletionProvider();
        if (deletionProvider == null) {
            return false;
        }

        boolean isDelete = ModKeyMappings.keyBindDelete.isActiveAndMatchesKey(keyCode, scanCode, modifiers);
        boolean isDeleteAll = ModKeyMappings.keyBindDeleteAll.isActiveAndMatchesKey(keyCode, scanCode, modifiers);

        LocalPlayer player = Minecraft.getInstance().player;

        // Special handling for creative inventory. We don't have a TrashSlot here, but we still allow deleting via DELETE key
        if ((isDelete || isDeleteAll) && TrashSlotConfig.getActive().enableDeleteKeysInCreative && screen instanceof CreativeModeInventoryScreen containerScreen && player != null) {
            Slot mouseSlot = ((AbstractContainerScreenAccessor) containerScreen).getHoveredSlot();
            DeletionProvider creativeDeletionProvider = TrashSlotConfig.getCreativeDeletionProvider();
            if (mouseSlot != null && mouseSlot.getClass() == Slot.class) {
                creativeDeletionProvider.deleteContainerItem(containerScreen.getMenu(), mouseSlot.index - 9, isDeleteAll, trashSlot);
            } else if (mouseSlot != null && mouseSlot.getClass().getSimpleName().equals("SlotWrapper")) {
                creativeDeletionProvider.deleteContainerItem(containerScreen.getMenu(), mouseSlot.getContainerSlot(), isDeleteAll, trashSlot);
            }
        }

        // For all other screens, respect the normal settings
        if ((currentContainerSettings.isEnabled() || TrashSlotConfig.getActive().allowDeletionWhileTrashSlotIsInvisible) && (isDelete || isDeleteAll)) {
            if (player != null && screen instanceof AbstractContainerScreen<?> containerScreen) {
                Slot mouseSlot = ((AbstractContainerScreenAccessor) containerScreen).getHoveredSlot();
                if (mouseSlot != null && mouseSlot.hasItem()) {
                    var registryName = Balm.getRegistries().getKey(mouseSlot.getItem().getItem());
                    if (registryName == null || !TrashSlotConfig.getActive().deletionDenyList.contains(registryName.toString())) {
                        deletionProvider.deleteContainerItem(containerScreen.getMenu(), mouseSlot.index, isDeleteAll, trashSlot);
                        if (!currentContainerSettings.isEnabled()) {
                            var hintMessage = Component.translatable("trashslot.hint.deletedWhileHidden");
                            hintMessage.withStyle(ChatFormatting.GOLD);
                            showHint(Hints.DELETED_WHILE_HIDDEN, hintMessage, 800, true);
                        }
                    } else {
                        var hintMessage = Component.translatable("trashslot.hint.deletionDenied");
                        hintMessage.withStyle(ChatFormatting.RED);
                        showHint(Hints.DELETION_DENIED, hintMessage, 1000, true);
                    }
                } else {
                    Window mainWindow = Minecraft.getInstance().getWindow();
                    double rawMouseX = Minecraft.getInstance().mouseHandler.xpos();
                    double rawMouseY = Minecraft.getInstance().mouseHandler.ypos();
                    double mouseX = rawMouseX * (double) mainWindow.getGuiScaledWidth() / (double) mainWindow.getWidth();
                    double mouseY = rawMouseY * (double) mainWindow.getGuiScaledHeight() / (double) mainWindow.getHeight();

                    if (((AbstractContainerScreenAccessor) containerScreen).callIsHovering(trashSlot, mouseX, mouseY)) {
                        deletionProvider.emptyTrashSlot(trashSlot);
                    }
                }
                return true;
            }
        }

        // Toggling of trashslot
        if (screen instanceof AbstractContainerScreen<?> && currentContainerSettings != ContainerSettings.NONE) {
            if (ModKeyMappings.keyBindToggleSlot.isActiveAndMatchesKey(keyCode, scanCode, modifiers)) {
                currentContainerSettings.setEnabled(!currentContainerSettings.isEnabled());
                if (!currentContainerSettings.isEnabled() && !ModKeyMappings.keyBindToggleSlot.getBinding().key().equals(InputConstants.UNKNOWN)) {
                    var hintMessage = Component.translatable("trashslot.hint.toggledOff", ModKeyMappings.keyBindToggleSlot.getBinding().key().getDisplayName());
                    showHint(Hints.TOGGLED_OFF, hintMessage, 5000);
                }
                TrashSlotSaveState.save();
                return true;
            } else if (ModKeyMappings.keyBindToggleSlotLock.isActiveAndMatchesKey(keyCode, scanCode, modifiers)) {
                currentContainerSettings.setLocked(!currentContainerSettings.isLocked());
                if (currentContainerSettings.isLocked()) {
                    var hintMessage = Component.translatable("trashslot.hint.locked", ModKeyMappings.keyBindToggleSlotLock.getBinding().key().getDisplayName());
                    hintMessage.withStyle(ChatFormatting.GOLD);
                    showHint(Hints.LOCKED, hintMessage, 5000, true);
                } else {
                    var hintMessage = Component.translatable("trashslot.hint.unlocked", ModKeyMappings.keyBindToggleSlotLock.getBinding().key().getDisplayName());
                    hintMessage.withStyle(ChatFormatting.GOLD);
                    showHint(Hints.UNLOCKED, hintMessage, 5000, true);
                }
                TrashSlotSaveState.save();
                return true;
            }
        }

        return false;
    }

    private static void showHint(String id, MutableComponent message, int timeToDisplay) {
        showHint(id, message, timeToDisplay, false);
    }

    private static void showHint(String id, MutableComponent message, int timeToDisplay, boolean force) {
        var saveState = TrashSlotSaveState.getInstance();
        if (force || (!saveState.hasSeenHint(id) && TrashSlotConfig.getActive().enableHints)) {
            currentHint = new Hint(id, message, timeToDisplay);
        }
    }

    public static void onBackgroundDrawn(ContainerScreenDrawEvent.Background event) {
        DeletionProvider deletionProvider = TrashSlotConfig.getDeletionProvider();
        if (deletionProvider == null || !currentContainerSettings.isEnabled()) {
            return;
        }

        if (event.getScreen() instanceof AbstractContainerScreen<?> screen && trashSlotComponent != null) {
            trashSlotComponent.update(event.getMouseX(), event.getMouseY());
            trashSlotComponent.drawBackground(event.getGuiGraphics());

            if (((AbstractContainerScreenAccessor) screen).callIsHovering(trashSlot, event.getMouseX(), event.getMouseY())) {
                var poseStack = event.getGuiGraphics().pose();
                poseStack.pushPose();
                poseStack.translate(((AbstractContainerScreenAccessor) screen).getLeftPos(), ((AbstractContainerScreenAccessor) screen).getTopPos(), 0);
                AbstractContainerScreen.renderSlotHighlight(event.getGuiGraphics(), trashSlot.x, trashSlot.y, 0);
                poseStack.popPose();
            }
        }
    }

    public static void onScreenDrawn(ContainerScreenDrawEvent.Background event) {
        if (currentHint != null) {
            currentHint.render(event.getScreen(), event.getGuiGraphics());
            if (currentHint.isComplete()) {
                TrashSlotSaveState.getInstance().markHintAsSeen(currentHint.getId());
                TrashSlotSaveState.save();
                currentHint = null;
            }
        }

        DeletionProvider deletionProvider = TrashSlotConfig.getDeletionProvider();
        if (deletionProvider == null || !currentContainerSettings.isEnabled()) {
            return;
        }

        if (event.getScreen() instanceof AbstractContainerScreen<?> screen && trashSlotComponent != null) {
            // TODO bit ugly for now since renderSlot ignores the pose stack translation
            TrashSlotSlot trashSlot = TrashSlotGuiHandler.trashSlot;
            SlotAccessor slotAccessor = (SlotAccessor) trashSlot;
            slotAccessor.setX(trashSlot.x + ((AbstractContainerScreenAccessor) screen).getLeftPos());
            slotAccessor.setY(trashSlot.y + ((AbstractContainerScreenAccessor) screen).getTopPos());
            ((AbstractContainerScreenAccessor) screen).callRenderSlot(event.getGuiGraphics(), trashSlot);
            slotAccessor.setX(trashSlot.x - ((AbstractContainerScreenAccessor) screen).getLeftPos());
            slotAccessor.setY(trashSlot.y - ((AbstractContainerScreenAccessor) screen).getTopPos());

            boolean isMouseSlot = ((AbstractContainerScreenAccessor) screen).callIsHovering(trashSlot, event.getMouseX(), event.getMouseY());
            if (isMouseSlot && screen.getMenu().getCarried().isEmpty() && trashSlot.hasItem()) {
                event.getGuiGraphics().renderTooltip(Minecraft.getInstance().font, trashSlot.getItem(), event.getMouseX(), event.getMouseY());
            }
        }
    }

    public static TrashSlotComponent getTrashSlotComponent() {
        return trashSlotComponent;
    }

    public static TrashSlotSlot getTrashSlot() {
        return trashSlot;
    }

    public static boolean isLeftMouseDown() {
        return isLeftMouseDown;
    }
}
