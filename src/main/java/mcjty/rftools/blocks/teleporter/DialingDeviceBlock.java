package mcjty.rftools.blocks.teleporter;

import mcjty.lib.api.Infusable;
import mcjty.lib.container.EmptyContainer;
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

public class DialingDeviceBlock extends GenericRFToolsBlock<DialingDeviceTileEntity, EmptyContainer> implements Infusable {

    public DialingDeviceBlock() {
        super(Material.IRON, DialingDeviceTileEntity.class, EmptyContainer::new, "dialing_device", false);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BiFunction<DialingDeviceTileEntity, EmptyContainer, GenericGuiContainer<? super DialingDeviceTileEntity>> getGuiFactory() {
        return GuiDialingDevice::new;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack itemStack, World player, List<String> list, ITooltipFlag whatIsThis) {
        super.addInformation(itemStack, player, list, whatIsThis);

        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            Collections.addAll(list, GuiLoc.lines("tooltip.rftools.with_the_dialing_device_you_can_dial_up_any"));
            list.add(I18n.format("tooltip.rftools.infusing_bonus_reduced_power_consumption"));
        } else {
            list.add(TextFormatting.WHITE + I18n.format("message.rftools.shiftmessage"));
        }
    }


    @Override
    public int getGuiID() {
        return GuiProxy.GUI_DIALING_DEVICE;
    }
}
