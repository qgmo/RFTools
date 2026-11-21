package mcjty.rftools.blocks.security;

import mcjty.lib.tileentity.GenericTileEntity;
import mcjty.lib.varia.Logging;
import mcjty.rftools.setup.GuiProxy;
import mcjty.rftools.items.GenericRFToolsItem;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
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

public class OrphaningCardItem extends GenericRFToolsItem {

    public OrphaningCardItem() {
        super("orphaning_card");
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
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            Collections.addAll(list, GuiLoc.lines("tooltip.rftools.sneak_right_click_on_an_rftools_machine_to"));
        } else {
            list.add(TextFormatting.WHITE + I18n.format("message.rftools.shiftmessage"));
        }
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof GenericTileEntity) {
                // Generalize with a security API interface @todo
                GenericTileEntity genericTileEntity = (GenericTileEntity) te;
                if (genericTileEntity.getOwnerUUID() == null) {
                    Msg.message(player, "msg.rftools.this_block_has_no_owner");
                } else {
                    if (isPrivileged(player, world)) {
                        genericTileEntity.clearOwner();
                        Msg.message(player, "msg.rftools.cleared_owner");
                    } else if (genericTileEntity.getOwnerUUID().equals(player.getPersistentID())) {
                        genericTileEntity.clearOwner();
                        Msg.message(player, "msg.rftools.cleared_owner");
                    } else {
                        Msg.message(player, "msg.rftools.you_cannot_clear_ownership_of_a_block_you");
                    }
                }
            } else {
                Msg.message(player, "msg.rftools.onwership_is_not_supported_on_this_block");
            }
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.SUCCESS;
    }

    public static boolean isPrivileged(EntityPlayer player, World world) {
//        return false;
        return player.capabilities.isCreativeMode || world.getMinecraftServer().getPlayerList().canSendCommands(player.getGameProfile());
    }
}