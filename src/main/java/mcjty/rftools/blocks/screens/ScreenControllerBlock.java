package mcjty.rftools.blocks.screens;

import mcjty.lib.api.Infusable;
import mcjty.lib.gui.GenericGuiContainer;
import mcjty.rftools.blocks.GenericRFToolsBlock;
import mcjty.rftools.setup.GuiProxy;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
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

public class ScreenControllerBlock extends GenericRFToolsBlock<ScreenControllerTileEntity, ScreenControllerContainer> implements Infusable {

    public ScreenControllerBlock() {
        super(Material.IRON, ScreenControllerTileEntity.class, ScreenControllerContainer::new, "screen_controller", false);
    }

    @Override
    public int getGuiID() {
        return GuiProxy.GUI_SCREENCONTROLLER;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BiFunction<ScreenControllerTileEntity, ScreenControllerContainer, GenericGuiContainer<? super ScreenControllerTileEntity>> getGuiFactory() {
        return GuiScreenController::new;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack itemStack, World player, List<String> list, ITooltipFlag whatIsThis) {
        super.addInformation(itemStack, player, list, whatIsThis);

        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            Collections.addAll(list, GuiLoc.lines("tooltip.rftools.before_screens_can_work_they_need_to_get"));
            list.add(I18n.format("tooltip.rftools.infusing_bonus_increased_range_for_screens"));
        } else {
            list.add(TextFormatting.WHITE + I18n.format("message.rftools.shiftmessage"));
        }
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity instanceof ScreenControllerTileEntity) {
                ((ScreenControllerTileEntity) tileEntity).detach();
            }
        }
        super.breakBlock(world, pos, state);
    }
}
