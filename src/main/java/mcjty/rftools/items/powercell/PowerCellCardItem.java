package mcjty.rftools.items.powercell;

import mcjty.rftools.items.GenericRFToolsItem;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import net.minecraft.client.resources.I18n;

public class PowerCellCardItem extends GenericRFToolsItem {

    public PowerCellCardItem() {
        super("powercell_card");
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack itemStack, World player, List<String> list, ITooltipFlag whatIsThis) {
        super.addInformation(itemStack, player, list, whatIsThis);
        list.add(I18n.format("tooltip.rftools.use_to_connect_a_powercell_multiblock"));
        int id = getId(itemStack);
        if (id == -1) {
            list.add(I18n.format("tooltip.rftools.unlinked"));
        } else {
            list.add(I18n.format("tooltip.rftools.link_id_s", id));
        }
    }

    public static int getId(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            return -1;
        }
        if (!stack.getTagCompound().hasKey("id")) {
            return -1;
        }
        return stack.getTagCompound().getInteger("id");
    }

    public static void setId(ItemStack stack, int id) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setInteger("id", id);
    }
}
