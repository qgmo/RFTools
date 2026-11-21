package mcjty.rftools.blocks.logic.wireless;

import mcjty.lib.blocks.GenericItemBlock;
import mcjty.lib.container.EmptyContainer;
import mcjty.rftools.setup.GuiProxy;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.List;
import net.minecraft.client.resources.I18n;
import mcjty.rftools.client.GuiLoc;
import java.util.Collections;

public class RedstoneTransmitterBlock extends RedstoneChannelBlock<RedstoneTransmitterTileEntity, EmptyContainer> {

    public RedstoneTransmitterBlock() {
        super(Material.IRON, RedstoneTransmitterTileEntity.class, EmptyContainer::new, GenericItemBlock::new, "redstone_transmitter_block");
    }

    @Override
    public boolean hasRedstoneOutput() {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack itemStack, World player, List<String> list, ITooltipFlag whatIsThis) {
        super.addInformation(itemStack, player, list, whatIsThis);
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            Collections.addAll(list, GuiLoc.lines("tooltip.rftools.this_logic_block_accepts_redstone_signals"));
        } else {
            list.add(TextFormatting.WHITE + I18n.format("message.rftools.shiftmessage"));
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        super.neighborChanged(state, worldIn, pos, blockIn, fromPos);
        RedstoneTransmitterTileEntity te = (RedstoneTransmitterTileEntity) worldIn.getTileEntity(pos);
        te.update();
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (!world.isRemote) {
            // @todo double check
            ((RedstoneTransmitterTileEntity)world.getTileEntity(pos)).update();
        }
    }

    @Override
    public int getGuiID() {
        return -1;
    }
}
