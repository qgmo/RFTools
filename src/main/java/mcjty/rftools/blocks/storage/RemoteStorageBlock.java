package mcjty.rftools.blocks.storage;

import mcjty.lib.api.Infusable;
import mcjty.lib.gui.GenericGuiContainer;
import mcjty.rftools.blocks.GenericRFToolsBlock;
import mcjty.rftools.setup.GuiProxy;
import net.minecraft.block.material.Material;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.client.resources.I18n;
import mcjty.rftools.client.GuiLoc;
import java.util.Collections;

public class RemoteStorageBlock extends GenericRFToolsBlock<RemoteStorageTileEntity, RemoteStorageContainer> implements Infusable {

    public RemoteStorageBlock() {
        super(Material.IRON, RemoteStorageTileEntity.class, RemoteStorageContainer::new, "remote_storage", true);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BiFunction<RemoteStorageTileEntity, RemoteStorageContainer, GenericGuiContainer<? super RemoteStorageTileEntity>> getGuiFactory() {
        return GuiRemoteStorage::new;
    }

    @Override
    public int getGuiID() {
        return GuiProxy.GUI_REMOTE_STORAGE;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack itemStack, World player, List<String> list, ITooltipFlag whatIsThis) {
        super.addInformation(itemStack, player, list, whatIsThis);

        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            Collections.addAll(list, GuiLoc.lines("tooltip.rftools.make_storage_modules_remotely_available"));
            list.add(I18n.format("tooltip.rftools.infusing_bonus_reduced_power_consumption_3"));
        } else {
            list.add(TextFormatting.WHITE + I18n.format("message.rftools.shiftmessage"));
        }
    }
}
