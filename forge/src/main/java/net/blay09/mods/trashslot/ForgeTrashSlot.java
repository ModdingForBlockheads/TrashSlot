package net.blay09.mods.trashslot;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.client.BalmClient;
import net.blay09.mods.balm.forge.ForgeLoadContext;
import net.blay09.mods.trashslot.client.TrashSlotClient;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(TrashSlot.MOD_ID)
public class ForgeTrashSlot {

    public ForgeTrashSlot(FMLJavaModLoadingContext context) {
        final var loadContext = new ForgeLoadContext(context.getModEventBus());
        PlatformBindings.INSTANCE = new PlatformBindings() {
            @Override
            public boolean supportsKeyModifiers() {
                return true;
            }
        };

        Balm.initialize(TrashSlot.MOD_ID, loadContext, TrashSlot::initialize);
        if (FMLEnvironment.dist.isClient()) {
            BalmClient.initialize(TrashSlot.MOD_ID, loadContext, TrashSlotClient::initialize);
        }
    }

}
