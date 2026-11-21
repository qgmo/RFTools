package mcjty.rftools.items.builder;

import mcjty.lib.varia.Logging;
import mcjty.rftools.RFTools;
import mcjty.rftools.blocks.builder.BuilderConfiguration;
import mcjty.rftools.blocks.builder.SpaceChamberControllerTileEntity;
import mcjty.rftools.setup.GuiProxy;
import mcjty.rftools.items.GenericRFToolsItem;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
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
import mcjty.rftools.varia.Msg;

public class SpaceChamberCardItem extends GenericRFToolsItem {

    public SpaceChamberCardItem() {
        super("space_chamber_card");
        setMaxStackSize(1);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 1;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack itemStack, World player, List<String> list, ITooltipFlag whatIsThis) {
        super.addInformation(itemStack, player, list, whatIsThis);
        NBTTagCompound tagCompound = itemStack.getTagCompound();
        int channel = -1;
        if (tagCompound != null) {
            channel = tagCompound.getInteger("channel");
        }
        if (channel != -1) {
            list.add(I18n.format("tooltip.rftools.channel_s_2", channel));
        } else {
            list.add(I18n.format("tooltip.rftools.channel_is_not_set"));
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            Collections.addAll(list, GuiLoc.lines("tooltip.rftools.sneak_right_click_on_a_space_chamber"));
            list.add(I18n.format("tooltip.rftools.base_cost_s_rf_t_per_block", BuilderConfiguration.builderRfPerOperation.get()));
            list.add(I18n.format("tooltip.rftools.final_cost_depends_on_infusion_level"));
        } else {
            list.add(TextFormatting.WHITE + I18n.format("message.rftools.shiftmessage"));
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (!player.isSneaking()) {
            showDetails(world, player, player.getHeldItem(hand));
        }
        return super.onItemRightClick(world, player, hand);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        TileEntity te = world.getTileEntity(pos);
        NBTTagCompound tagCompound = stack.getTagCompound();
        if (tagCompound == null) {
            tagCompound = new NBTTagCompound();
            stack.setTagCompound(tagCompound);
        }

        int channel = -1;
        if (te instanceof SpaceChamberControllerTileEntity) {
            channel = ((SpaceChamberControllerTileEntity) te).getChannel();
        }

        if (channel == -1) {
            showDetails(world, player, stack);
        } else {
            tagCompound.setInteger("channel", channel);
            if (world.isRemote) {
                Msg.message(player, "msg.rftools.card_is_set_to_channel_s", channel);
            }
        }
        return EnumActionResult.SUCCESS;
    }

    private void showDetails(World world, EntityPlayer player, ItemStack stack) {
        if (stack.getTagCompound() != null && stack.getTagCompound().hasKey("channel")) {
            int channel = stack.getTagCompound().getInteger("channel");
            if (channel != -1) {
                showDetailsGui(world, player);
            } else {
                Msg.message(player, "msg.rftools.card_is_not_linked");
            }
        }
    }

    private void showDetailsGui(World world, EntityPlayer player) {
        if (world.isRemote) {
            player.openGui(RFTools.instance, GuiProxy.GUI_CHAMBER_DETAILS, player.getEntityWorld(), (int) player.posX, (int) player.posY, (int) player.posZ);
        }
    }

}