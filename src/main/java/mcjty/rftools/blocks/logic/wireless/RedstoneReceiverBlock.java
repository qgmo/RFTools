package mcjty.rftools.blocks.logic.wireless;

import mcjty.lib.blocks.GenericItemBlock;
import mcjty.lib.container.EmptyContainer;
import mcjty.lib.gui.GenericGuiContainer;
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

public class RedstoneReceiverBlock extends RedstoneChannelBlock<RedstoneReceiverTileEntity, EmptyContainer> {

    public RedstoneReceiverBlock() {
        super(Material.IRON, RedstoneReceiverTileEntity.class, EmptyContainer::new, GenericItemBlock::new, "redstone_receiver_block");
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BiFunction<RedstoneReceiverTileEntity, EmptyContainer, GenericGuiContainer<? super RedstoneReceiverTileEntity>> getGuiFactory() {
        return GuiRedstoneReceiver::new;
    }

    @Override
    public boolean needsRedstoneCheck() {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack itemStack, World player, List<String> list, ITooltipFlag whatIsThis) {
        super.addInformation(itemStack, player, list, whatIsThis);
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            Collections.addAll(list, GuiLoc.lines("tooltip.rftools.this_logic_block_sends_redstone_signals"));
        } else {
            list.add(TextFormatting.WHITE + I18n.format("message.rftools.shiftmessage"));
        }
    }

    @Override
    public int getGuiID() {
        return GuiProxy.GUI_REDSTONE_RECEIVER;
    }
}
