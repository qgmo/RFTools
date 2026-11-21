package mcjty.rftools.blocks.security;

import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import mcjty.lib.typed.TypedMap;
import mcjty.rftools.ClientCommandHandler;
import mcjty.rftools.network.RFToolsMessages;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class SecurityTools {

    /**
     * 玩家能不能查看这个安全频道：手上拿着、或开着的界面里（包括自己的背包、安全管理器的卡槽）有一张这个频道的安全卡；管理员都可以。
     */
    public static boolean canSeeChannel(EntityPlayer player, int id) {
        if (player.canUseCommand(2, "")) {
            return true;
        }
        if (isCardForChannel(player.getHeldItemMainhand(), id) || isCardForChannel(player.getHeldItemOffhand(), id)) {
            return true;
        }
        if (player.openContainer != null) {
            for (Slot slot : player.openContainer.inventorySlots) {
                if (isCardForChannel(slot.getStack(), id)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isCardForChannel(ItemStack stack, int id) {
        if (stack.isEmpty() || !(stack.getItem() instanceof SecurityCardItem)) {
            return false;
        }
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasKey("channel") && tag.getInteger("channel") == id;
    }

    public static void returnSecurityInfo(EntityPlayer player, int id) {
        SecurityChannels channels = SecurityChannels.getChannels(player.getEntityWorld());
        SecurityChannels.SecurityChannel channel = channels.getChannel(id);
        if (channel == null) {
            return;
        }
        RFToolsMessages.INSTANCE.sendTo(new PacketSecurityInfoReady(channel), (EntityPlayerMP) player);
    }

    public static void returnSecurityName(EntityPlayer player, int id) {
        SecurityChannels channels = SecurityChannels.getChannels(player.getEntityWorld());
        SecurityChannels.SecurityChannel channel = channels.getChannel(id);
        if (channel == null) {
            return;
        }
        RFToolsMessages.sendToClient(player, ClientCommandHandler.CMD_RETURN_SECURITY_NAME,
                TypedMap.builder().put(ClientCommandHandler.PARAM_NAME, channel.getName()));
    }
}
