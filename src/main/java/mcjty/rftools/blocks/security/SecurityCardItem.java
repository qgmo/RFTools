package mcjty.rftools.blocks.security;

import mcjty.lib.tileentity.GenericTileEntity;
import mcjty.lib.typed.TypedMap;
import mcjty.lib.varia.Logging;
import mcjty.rftools.setup.CommandHandler;
import mcjty.rftools.setup.GuiProxy;
import mcjty.rftools.items.GenericRFToolsItem;
import mcjty.rftools.network.RFToolsMessages;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
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

public class SecurityCardItem extends GenericRFToolsItem {

    public static String channelNameFromServer = "";
    private static long lastTime = 0;

    public SecurityCardItem() {
        super("security_card");
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
        if (tagCompound != null && tagCompound.hasKey("channel")) {
            channel = tagCompound.getInteger("channel");
        }
        if (channel != -1) {
            if (System.currentTimeMillis() - lastTime > 250) {
                lastTime = System.currentTimeMillis();
                RFToolsMessages.sendToServer(CommandHandler.CMD_GET_SECURITY_NAME, TypedMap.builder().put(CommandHandler.PARAM_ID, channel));
            }
            list.add(I18n.format("tooltip.rftools.channel_s_s", channel, channelNameFromServer));
        } else {
            list.add(I18n.format("tooltip.rftools.channel_is_not_set"));
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            Collections.addAll(list, GuiLoc.lines("tooltip.rftools.manage_security_channels_in_the_security"));
        } else {
            list.add(TextFormatting.WHITE + I18n.format("message.rftools.shiftmessage"));
        }
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof GenericTileEntity) {
                // @tod security api
                GenericTileEntity genericTileEntity = (GenericTileEntity) te;
                if (genericTileEntity.getOwnerUUID() == null) {
                    Msg.message(player, "msg.rftools.this_block_has_no_owner");
                } else {
                    if (OrphaningCardItem.isPrivileged(player, world) || isOwner(player, genericTileEntity)) {
                        NBTTagCompound tagCompound = stack.getTagCompound();
                        if (tagCompound == null || !tagCompound.hasKey("channel")) {
                            int blockSecurity = genericTileEntity.getSecurityChannel();
                            if (blockSecurity == -1) {
                                Msg.message(player, "msg.rftools.this_security_card_is_not_setup_correctly");
                            } else {
                                if (tagCompound == null) {
                                    tagCompound = new NBTTagCompound();
                                    stack.setTagCompound(tagCompound);
                                }
                                tagCompound.setInteger("channel", blockSecurity);
                                Msg.message(player, "msg.rftools.copied_security_channel_from_block_to_card");
                            }
                        } else {
                            int channel = tagCompound.getInteger("channel");
                            toggleSecuritySettings(player, genericTileEntity, channel);
                        }
                    } else {
                        Msg.message(player, "msg.rftools.you_cannot_change_security_settings_of_a");
                    }
                }
            } else {
                Msg.message(player, "msg.rftools.security_is_not_supported_on_this_block");
            }
            return EnumActionResult.SUCCESS;
        }
       return EnumActionResult.SUCCESS;
    }

    private boolean isOwner(EntityPlayer player, GenericTileEntity genericTileEntity) {
        return genericTileEntity.getOwnerUUID().equals(player.getPersistentID());
    }

//    @SideOnly(Side.CLIENT)
//    @Override
//    public IIcon getIconIndex(ItemStack stack) {
//        NBTTagCompound tagCompound = stack.getTagCompound();
//        if (tagCompound != null && tagCompound.hasKey("channel")) {
//            return activeIcon;
//        } else {
//            return itemIcon;
//        }
//    }


    private void toggleSecuritySettings(EntityPlayer player, GenericTileEntity genericTileEntity, int channel) {
        int sec = genericTileEntity.getSecurityChannel();
        if (sec == channel) {
            genericTileEntity.setSecurityChannel(-1);
            Msg.message(player, "msg.rftools.security_settings_cleared_from_block");
        } else {
            genericTileEntity.setSecurityChannel(channel);
            Msg.message(player, "msg.rftools.security_settings_applied_on_block");
        }
    }
}