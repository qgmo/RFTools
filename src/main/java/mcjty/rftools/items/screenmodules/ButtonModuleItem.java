package mcjty.rftools.items.screenmodules;

import mcjty.rftools.api.screens.IModuleGuiBuilder;
import mcjty.rftools.api.screens.IModuleProvider;
import mcjty.rftools.blocks.screens.ScreenConfiguration;
import mcjty.rftools.blocks.screens.modules.ButtonScreenModule;
import mcjty.rftools.blocks.screens.modulesclient.ButtonClientScreenModule;
import mcjty.rftools.items.GenericRFToolsItem;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import net.minecraft.client.resources.I18n;
import mcjty.rftools.client.GuiLoc;
import java.util.Collections;

public class ButtonModuleItem extends GenericRFToolsItem implements IModuleProvider {

    public ButtonModuleItem() {
        super("button_module");
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 1;
    }

    @Override
    public Class<ButtonScreenModule> getServerScreenModule() {
        return ButtonScreenModule.class;
    }

    @Override
    public Class<ButtonClientScreenModule> getClientScreenModule() {
        return ButtonClientScreenModule.class;
    }

    @Override
    public String getName() {
        return "Button";
    }

    @Override
    public void createGui(IModuleGuiBuilder guiBuilder) {
        guiBuilder
                .label("Label:").text("text", "Label text").color("color", "Label color").nl()
                .label("Button:").text("button", "Button text").color("buttonColor", "Button color").nl()
                .toggle("toggle", "Toggle", "Toggle button mode")
                .choices("align", "Label alignment", "Left", "Center", "Right").nl();

    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack itemStack, @Nullable World player, List<String> list, ITooltipFlag advanced) {
        super.addInformation(itemStack, player, list, advanced);
        list.add(I18n.format("tooltip.rftools.uses_s_rf_tick", ScreenConfiguration.BUTTON_RFPERTICK.get()));
        NBTTagCompound tagCompound = itemStack.getTagCompound();
        if (tagCompound != null) {
            list.add(I18n.format("tooltip.rftools.label_s", tagCompound.getString("text")));
            int channel = tagCompound.getInteger("channel");
            if (channel != -1) {
                list.add(I18n.format("tooltip.rftools.channel_s_2", channel));
            }
        }
        Collections.addAll(list, GuiLoc.lines("tooltip.rftools.sneak_right_click_on_a_redstone_receiver_to"));
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, IBlockAccess world, BlockPos pos, EntityPlayer player) {
        return true;
    }
}