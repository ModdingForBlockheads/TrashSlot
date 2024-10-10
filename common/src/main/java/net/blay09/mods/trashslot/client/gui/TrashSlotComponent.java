package net.blay09.mods.trashslot.client.gui;

import net.blay09.mods.balm.mixin.AbstractContainerScreenAccessor;
import net.blay09.mods.balm.mixin.SlotAccessor;
import net.blay09.mods.trashslot.TrashSlot;
import net.blay09.mods.trashslot.config.TrashSlotConfig;
import net.blay09.mods.trashslot.TrashSlotSaveState;
import net.blay09.mods.trashslot.api.IGuiContainerLayout;
import net.blay09.mods.trashslot.api.SlotRenderStyle;
import net.blay09.mods.trashslot.api.Snap;
import net.blay09.mods.trashslot.client.ContainerSettings;
import net.blay09.mods.trashslot.client.TrashSlotSlot;
import net.blay09.mods.trashslot.client.TrashSlotGuiHandler;
import net.blay09.mods.trashslot.client.deletion.DeletionProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class TrashSlotComponent {

    private static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(TrashSlot.MOD_ID, "textures/gui/slot.png");
    private static final int SNAP_SIZE = 7;

    private final AbstractContainerScreen<?> screen;
    private final IGuiContainerLayout layout;
    private final ContainerSettings settings;
    private final TrashSlotSlot trashSlot;

    private SlotRenderStyle renderStyle = SlotRenderStyle.LONE;

    private boolean wasMouseDown;
    private boolean isDragging;
    private int dragStartX;
    private int dragStartY;

    public TrashSlotComponent(AbstractContainerScreen<?> screen, IGuiContainerLayout layout, ContainerSettings settings, TrashSlotSlot trashSlot) {
        this.screen = screen;
        this.layout = layout;
        this.settings = settings;
        this.trashSlot = trashSlot;
    }

    public boolean isInside(int mouseX, int mouseY) {
        int anchoredX = getAnchoredX();
        int anchoredY = getAnchoredY();
        int renderX = anchoredX + renderStyle.getRenderOffsetX() + layout.getSlotOffsetX(screen, renderStyle);
        int renderY = anchoredY + renderStyle.getRenderOffsetY() + layout.getSlotOffsetY(screen, renderStyle);
        return mouseX >= renderX && mouseY >= renderY && mouseX < renderX + renderStyle.getRenderWidth() && mouseY < renderY + renderStyle.getRenderHeight();
    }

    public void update(int mouseX, int mouseY) {
        int anchoredX = getAnchoredX();
        int anchoredY = getAnchoredY();
        int renderX = anchoredX + renderStyle.getRenderOffsetX() + layout.getSlotOffsetX(screen, renderStyle);
        int renderY = anchoredY + renderStyle.getRenderOffsetY() + layout.getSlotOffsetY(screen, renderStyle);
        boolean isMouseOver = mouseX >= renderX && mouseY >= renderY && mouseX < renderX + renderStyle.getRenderWidth() && mouseY < renderY + renderStyle.getRenderHeight();
        if (TrashSlotGuiHandler.isLeftMouseDown()) {
            if (!isDragging && isMouseOver && !wasMouseDown && !settings.isLocked()) {
                if (Minecraft.getInstance().player.containerMenu.getCarried()
                        .isEmpty() && (!trashSlot.hasItem() || !((AbstractContainerScreenAccessor) screen).callIsHovering(trashSlot, mouseX, mouseY))) {
                    dragStartX = renderX - mouseX;
                    dragStartY = renderY - mouseY;
                    isDragging = true;
                }
            }
            wasMouseDown = true;
        } else {
            if (isDragging) {
                TrashSlotSaveState.save();
                isDragging = false;
            }
            wasMouseDown = false;
        }
        if (isDragging) {
            int targetX = mouseX + dragStartX;
            int targetY = mouseY + dragStartY;
            for (Rect2i collisionArea : layout.getCollisionAreas(screen)) {
                int targetRight = targetX + renderStyle.getWidth();
                int targetBottom = targetY + renderStyle.getHeight();
                int rectRight = collisionArea.getX() + collisionArea.getWidth();
                int rectBottom = collisionArea.getY() + collisionArea.getHeight();
                if (targetRight >= collisionArea.getX() && targetX < rectRight && targetBottom >= collisionArea.getY() && targetY < rectBottom) {
                    int distLeft = targetRight - collisionArea.getX();
                    int distRight = rectRight - targetX;
                    int distTop = targetBottom - collisionArea.getY();
                    int distBottom = rectBottom - targetY;
                    if (anchoredX >= collisionArea.getX() && anchoredX < collisionArea.getX() + collisionArea.getWidth()) {
                        targetY = distTop < distBottom ? collisionArea.getY() - renderStyle.getHeight() : collisionArea.getY() + collisionArea.getHeight();
                    } else {
                        targetX = distLeft < distRight ? collisionArea.getX() - renderStyle.getWidth() : collisionArea.getX() + collisionArea.getWidth();
                    }
                }
            }

            if (!Screen.hasShiftDown()) {
                int bestSnapDist = Integer.MAX_VALUE;
                Snap bestSnap = null;
                for (Snap snap : layout.getSnaps(screen, renderStyle)) {
                    int dist = Integer.MAX_VALUE;
                    switch (snap.getType()) {
                        case HORIZONTAL -> dist = Math.abs(snap.getY() - targetY);
                        case VERTICAL -> dist = Math.abs(snap.getX() - targetX);
                        case FIXED -> {
                            int distX = snap.getX() - targetX;
                            int distY = snap.getY() - targetY;
                            dist = (int) Math.sqrt(distX * distX + distY * distY);
                        }
                    }
                    if (dist < SNAP_SIZE && dist < bestSnapDist) {
                        bestSnap = snap;
                        bestSnapDist = dist;
                    }
                }
                if (bestSnap != null) {
                    if (bestSnap.getType() == Snap.Type.VERTICAL || bestSnap.getType() == Snap.Type.FIXED) {
                        targetX = bestSnap.getX();
                    }
                    if (bestSnap.getType() == Snap.Type.HORIZONTAL || bestSnap.getType() == Snap.Type.FIXED) {
                        targetY = bestSnap.getY();
                    }
                }
            }
            targetX = Mth.clamp(targetX, 0, screen.width - renderStyle.getRenderWidth());
            targetY = Mth.clamp(targetY, 0, screen.height - renderStyle.getRenderHeight());
            settings.setSlotX(getUnanchoredX(targetX));
            settings.setSlotY(getUnanchoredY(targetY));
        }
    }

    public void drawBackground(GuiGraphics guiGraphics) {
        int renderX = getAnchoredX();
        int renderY = getAnchoredY();
        renderStyle = layout.getSlotRenderStyle(screen, renderX, renderY);
        AbstractContainerScreenAccessor screenAccessor = (AbstractContainerScreenAccessor) screen;
        ((SlotAccessor) trashSlot).setX(renderX - screenAccessor.getLeftPos() + renderStyle.getSlotOffsetX() + layout.getSlotOffsetX(screen, renderStyle));
        ((SlotAccessor) trashSlot).setY(renderY - screenAccessor.getTopPos() + renderStyle.getSlotOffsetY() + layout.getSlotOffsetY(screen, renderStyle));

        var poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 1);

        renderX += renderStyle.getRenderOffsetX() + layout.getSlotOffsetX(screen, renderStyle);
        renderY += renderStyle.getRenderOffsetY() + layout.getSlotOffsetY(screen, renderStyle);
        DeletionProvider deletionProvider = TrashSlotConfig.getDeletionProvider();
        int texOffsetX = 0;
        if (deletionProvider == null || !deletionProvider.canUndeleteLast()) {
            texOffsetX = 64;
        }
        switch (renderStyle) {
            case LONE -> guiGraphics.blit(RenderType::guiTextured, texture, renderX, renderY, texOffsetX, 56, renderStyle.getRenderWidth(), renderStyle.getRenderHeight(), 256, 256);
            case ATTACH_BOTTOM_CENTER -> {
                guiGraphics.blit(RenderType::guiTextured, texture, renderX, renderY, texOffsetX, 0, renderStyle.getRenderWidth(), renderStyle.getRenderHeight(), 256, 256);
                guiGraphics.blit(RenderType::guiTextured, texture, renderX, renderY, texOffsetX + 50, 29, 4, 4, 256, 256);
                guiGraphics.blit(RenderType::guiTextured, texture, renderX + renderStyle.getRenderWidth() - 4, renderY, texOffsetX + 54, 29, 4, 4, 256, 256);
            }
            case ATTACH_BOTTOM_LEFT -> {
                guiGraphics.blit(RenderType::guiTextured, texture, renderX, renderY, texOffsetX, 0, renderStyle.getRenderWidth(), renderStyle.getRenderHeight(), 256, 256);
                guiGraphics.blit(RenderType::guiTextured, texture, renderX + renderStyle.getRenderWidth() - 4, renderY, texOffsetX + 54, 29, 4, 4, 256, 256);
            }
            case ATTACH_BOTTOM_RIGHT -> {
                guiGraphics.blit(RenderType::guiTextured, texture, renderX, renderY, texOffsetX, 0, renderStyle.getRenderWidth(), renderStyle.getRenderHeight(), 256, 256);
                guiGraphics.blit(RenderType::guiTextured, texture, renderX, renderY, texOffsetX + 50, 29, 4, 4, 256, 256);
            }
            case ATTACH_TOP_CENTER -> {
                guiGraphics.blit(RenderType::guiTextured, texture, renderX, renderY, texOffsetX + 32, 0, renderStyle.getRenderWidth(), renderStyle.getRenderHeight(), 256, 256);
                guiGraphics.blit(RenderType::guiTextured, texture, renderX, renderY + renderStyle.getRenderHeight() - 4, texOffsetX + 50, 25, 4, 4, 256, 256);
                guiGraphics.blit(RenderType::guiTextured, texture, renderX + renderStyle.getRenderWidth() - 4, renderY + renderStyle.getRenderHeight() - 4, texOffsetX + 54, 25, 4, 4, 256, 256);
            }
            case ATTACH_TOP_LEFT -> {
                guiGraphics.blit(RenderType::guiTextured, texture, renderX, renderY, texOffsetX + 32, 0, renderStyle.getRenderWidth(), renderStyle.getRenderHeight(), 256, 256);
                guiGraphics.blit(RenderType::guiTextured, texture, renderX + renderStyle.getRenderWidth() - 4, renderY + renderStyle.getRenderHeight() - 4, texOffsetX + 54, 25, 4, 4, 256, 256);
            }
            case ATTACH_TOP_RIGHT -> {
                guiGraphics.blit(RenderType::guiTextured, texture, renderX, renderY, texOffsetX + 32, 0, renderStyle.getRenderWidth(), renderStyle.getRenderHeight(), 256, 256);
                guiGraphics.blit(RenderType::guiTextured, texture, renderX, renderY + renderStyle.getRenderHeight() - 4, texOffsetX + 50, 25, 4, 4, 256, 256);
            }
            case ATTACH_LEFT_CENTER -> {
                guiGraphics.blit(RenderType::guiTextured, texture, renderX, renderY, texOffsetX + 25, 25, renderStyle.getRenderWidth(), renderStyle.getRenderHeight(), 256, 256);
                guiGraphics.blit(RenderType::guiTextured, texture, renderX + renderStyle.getRenderWidth() - 4, renderY, texOffsetX + 50, 33, 4, 4, 256, 256);
                guiGraphics.blit(RenderType::guiTextured, texture, renderX + renderStyle.getRenderWidth() - 4, renderY + renderStyle.getRenderHeight() - 4, texOffsetX + 50, 37, 4, 4, 256, 256);
            }
            case ATTACH_LEFT_TOP -> {
                guiGraphics.blit(RenderType::guiTextured, texture, renderX, renderY, texOffsetX + 25, 25, renderStyle.getRenderWidth(), renderStyle.getRenderHeight(), 256, 256);
                guiGraphics.blit(RenderType::guiTextured, texture, renderX + renderStyle.getRenderWidth() - 4, renderY + renderStyle.getRenderHeight() - 4, texOffsetX + 50, 37, 4, 4, 256, 256);
            }
            case ATTACH_LEFT_BOTTOM -> {
                guiGraphics.blit(RenderType::guiTextured, texture, renderX, renderY, texOffsetX + 25, 25, renderStyle.getRenderWidth(), renderStyle.getRenderHeight(), 256, 256);
                guiGraphics.blit(RenderType::guiTextured, texture, renderX + renderStyle.getRenderWidth() - 4, renderY, texOffsetX + 50, 33, 4, 4, 256, 256);
            }
            case ATTACH_RIGHT_CENTER -> {
                guiGraphics.blit(RenderType::guiTextured, texture, renderX, renderY, texOffsetX, 25, renderStyle.getRenderWidth(), renderStyle.getRenderHeight(), 256, 256);
                guiGraphics.blit(RenderType::guiTextured, texture, renderX, renderY, texOffsetX + 54, 33, 4, 4, 256, 256);
                guiGraphics.blit(RenderType::guiTextured, texture, renderX, renderY + renderStyle.getRenderHeight() - 4, texOffsetX + 54, 37, 4, 4, 256, 256);
            }
            case ATTACH_RIGHT_TOP -> {
                guiGraphics.blit(RenderType::guiTextured, texture, renderX, renderY, texOffsetX, 25, renderStyle.getRenderWidth(), renderStyle.getRenderHeight(), 256, 256);
                guiGraphics.blit(RenderType::guiTextured, texture, renderX, renderY + renderStyle.getRenderHeight() - 4, texOffsetX + 54, 37, 4, 4, 256, 256);
            }
            case ATTACH_RIGHT_BOTTOM -> {
                guiGraphics.blit(RenderType::guiTextured, texture, renderX, renderY, texOffsetX, 25, renderStyle.getRenderWidth(), renderStyle.getRenderHeight(), 256, 256);
                guiGraphics.blit(RenderType::guiTextured, texture, renderX, renderY, texOffsetX + 54, 33, 4, 4, 256, 256);
            }
        }

        poseStack.popPose();
    }

    private int getAnchoredX() {
        AbstractContainerScreenAccessor screenAccessor = (AbstractContainerScreenAccessor) screen;
        return Mth.clamp(settings.getSlotX() + screenAccessor.getLeftPos() + (int) (screenAccessor.getImageWidth() * settings.getAnchorX()),
                0,
                screen.width - renderStyle.getRenderWidth());
    }

    private int getUnanchoredX(int x) {
        AbstractContainerScreenAccessor screenAccessor = (AbstractContainerScreenAccessor) screen;
        return x - screenAccessor.getLeftPos() - (int) (screenAccessor.getImageWidth() * settings.getAnchorX());
    }

    private int getAnchoredY() {
        AbstractContainerScreenAccessor screenAccessor = (AbstractContainerScreenAccessor) screen;
        return Mth.clamp(settings.getSlotY() + screenAccessor.getTopPos() + (int) (screenAccessor.getImageHeight() * settings.getAnchorY()),
                0,
                screen.width - renderStyle.getRenderWidth());
    }

    private int getUnanchoredY(int y) {
        AbstractContainerScreenAccessor screenAccessor = (AbstractContainerScreenAccessor) screen;
        return y - screenAccessor.getTopPos() - (int) (screenAccessor.getImageHeight() * settings.getAnchorY());
    }

    public boolean isVisible() {
        return settings.isEnabled();
    }

    public Rect2i getRectangle() {
        int anchoredX = getAnchoredX();
        int anchoredY = getAnchoredY();
        int renderX = anchoredX + renderStyle.getRenderOffsetX() + layout.getSlotOffsetX(screen, renderStyle);
        int renderY = anchoredY + renderStyle.getRenderOffsetY() + layout.getSlotOffsetY(screen, renderStyle);
        return new Rect2i(renderX, renderY, renderStyle.getRenderWidth(), renderStyle.getRenderHeight());
    }

}
