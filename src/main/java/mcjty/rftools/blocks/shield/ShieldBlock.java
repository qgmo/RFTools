package mcjty.rftools.blocks.shield;

import mcjty.lib.api.IModuleSupport;
import mcjty.lib.api.Infusable;
import mcjty.lib.crafting.INBTPreservingIngredient;
import mcjty.lib.gui.GenericGuiContainer;
import mcjty.lib.varia.GlobalCoordinate;
import mcjty.lib.varia.Logging;
import mcjty.lib.varia.ModuleSupport;
import mcjty.rftools.blocks.GenericRFToolsBlock;
import mcjty.rftools.blocks.builder.BuilderSetup;
import mcjty.rftools.setup.GuiProxy;
import mcjty.rftools.items.smartwrench.SmartWrenchItem;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
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
import mcjty.rftools.varia.Msg;

//@Optional.InterfaceList({
//        @Optional.Interface(iface = "crazypants.enderio.api.redstone.IRedstoneConnectable", modid = "EnderIO")})
public class ShieldBlock extends GenericRFToolsBlock<ShieldTEBase, ShieldContainer> implements Infusable, INBTPreservingIngredient
        /*, IRedstoneConnectable*/ {

    private final int max;

    public ShieldBlock(String blockName, Class<? extends ShieldTEBase> clazz, int max) {
        super(Material.IRON, clazz, ShieldContainer::new, blockName, true);
        this.max = max;
    }

    @Override
    public boolean needsRedstoneCheck() {
        return true;
    }

    @Override
    public RotationType getRotationType() {
        return RotationType.NONE;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BiFunction<ShieldTEBase, ShieldContainer, GenericGuiContainer<? super ShieldTEBase>> getGuiFactory() {
        return GuiShield::new;
    }

    @Override
    public int getGuiID() {
        return GuiProxy.GUI_SHIELD;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack itemStack, World player, List<String> list, ITooltipFlag whatIsThis) {
        super.addInformation(itemStack, player, list, whatIsThis);

        list.add(I18n.format("tooltip.rftools.supports_s_blocks", max));

        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            Collections.addAll(list, GuiLoc.lines("tooltip.rftools.this_machine_forms_a_shield_out_of_adjacent"));
            list.add(I18n.format("tooltip.rftools.note_block_mimic_is_not_implemented_yet"));
            Collections.addAll(list, GuiLoc.lines("tooltip.rftools.infusing_bonus_reduced_power_consumption_2"));
        } else {
            list.add(TextFormatting.WHITE + I18n.format("message.rftools.shiftmessage"));
        }
    }

    @Override
    protected IModuleSupport getModuleSupport() {
        return new ModuleSupport(ShieldContainer.SLOT_SHAPE) {
            @Override
            public boolean isModule(ItemStack itemStack) {
                return itemStack.getItem() == BuilderSetup.shapeCardItem;
            }
        };
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        restoreBlockFromNBT(world, pos, stack);
        setOwner(world, pos, placer);
    }

    @Override
    public void onBlockClicked(World world, BlockPos pos, EntityPlayer playerIn) {
        if (!world.isRemote) {
            composeDecomposeShield(world, pos, true);
            // @todo achievements
//            Achievements.trigger(playerIn, Achievements.shieldSafety);
        }
    }

    @Override
    protected boolean wrenchUse(World world, BlockPos pos, EnumFacing side, EntityPlayer player) {
        composeDecomposeShield(world, pos, false);
        // @todo achievements
//        Achievements.trigger(player, Achievements.shieldSafety);
        return true;
    }

    @Override
    protected boolean wrenchSneakSelect(World world, BlockPos pos, EntityPlayer player) {
        if (!world.isRemote) {
            GlobalCoordinate currentBlock = SmartWrenchItem.getCurrentBlock(player.getHeldItem(EnumHand.MAIN_HAND));
            if (currentBlock == null) {
                SmartWrenchItem.setCurrentBlock(player.getHeldItem(EnumHand.MAIN_HAND), new GlobalCoordinate(pos, world.provider.getDimension()));
                Msg.message(player, "msg.rftools.selected_block");
            } else {
                SmartWrenchItem.setCurrentBlock(player.getHeldItem(EnumHand.MAIN_HAND), null);
                Msg.message(player, "msg.rftools.cleared_selected_block");
            }
        }
        return true;
    }

    private void composeDecomposeShield(World world, BlockPos pos, boolean ctrl) {
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof ShieldTEBase) {
                ((ShieldTEBase)te).composeDecomposeShield(ctrl);
            }
        }
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof ShieldTEBase) {
            if (!world.isRemote) {
                ShieldTEBase shieldTileEntity = (ShieldTEBase) te;
                if (shieldTileEntity.isShieldComposed()) {
                    shieldTileEntity.decomposeShield();
                }
            }
        }

        super.breakBlock(world, pos, state);
    }

//
//    @Override
//    public boolean shouldRedstoneConduitConnect(World world, int x, int y, int z, EnumFacing from) {
//        return true;
//    }
}
