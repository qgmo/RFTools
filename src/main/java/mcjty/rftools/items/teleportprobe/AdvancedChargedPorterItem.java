package mcjty.rftools.items.teleportprobe;

import mcjty.lib.varia.Logging;
import mcjty.rftools.RFTools;
import mcjty.rftools.blocks.teleporter.TeleportConfiguration;
import mcjty.rftools.setup.GuiProxy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import mcjty.rftools.varia.Msg;

public class AdvancedChargedPorterItem extends ChargedPorterItem {
    public static final int MAXTARGETS = 8;

    public AdvancedChargedPorterItem() {
        super("advanced_charged_porter", TeleportConfiguration.ADVANCED_CHARGEDPORTER_MAXENERGY.get());
    }

    @Override
    protected int getSpeedBonus() {
        return TeleportConfiguration.advancedSpeedBonus.get();
    }

    @Override
    protected void selectOnReceiver(EntityPlayer player, World world, NBTTagCompound tagCompound, int id) {
        for (int i = 0 ; i < MAXTARGETS ; i++) {
            if (tagCompound.hasKey("target"+i) && tagCompound.getInteger("target"+i) == id) {
                // Id is already there.
                Msg.message(player, "msg.rftools.receiver_s_was_already_added_to_the_charged", id);
                return;
            }
        }

        for (int i = 0 ; i < MAXTARGETS ; i++) {
            if (!tagCompound.hasKey("target"+i)) {
                tagCompound.setInteger("target"+i, id);
                if (world.isRemote) {
                    Msg.message(player, "msg.rftools.receiver_s_is_added_to_the_charged_porter", id);
                }
                if (!tagCompound.hasKey("target")) {
                    tagCompound.setInteger("target", id);
                }
                return;
            }
        }

        if (world.isRemote) {
            Msg.message(player, "msg.rftools.charged_porter_has_no_free_targets");
        }
    }

    @Override
    protected void selectReceiver(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) {
            player.openGui(RFTools.instance, GuiProxy.GUI_ADVANCEDPORTER, player.getEntityWorld(), (int) player.posX, (int) player.posY, (int) player.posZ);
        }
    }

    @Override
    protected void selectOnThinAir(EntityPlayer player, World world, NBTTagCompound tagCompound, ItemStack stack) {
        selectReceiver(stack, world, player);
    }
}
