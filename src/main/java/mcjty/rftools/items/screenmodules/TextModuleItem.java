package mcjty.rftools.items.screenmodules;

import mcjty.rftools.api.screens.IModuleGuiBuilder;
import mcjty.rftools.api.screens.IModuleProvider;
import mcjty.rftools.blocks.screens.ScreenConfiguration;
import mcjty.rftools.blocks.screens.modules.TextScreenModule;
import mcjty.rftools.blocks.screens.modulesclient.TextClientScreenModule;
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

public class TextModuleItem extends GenericRFToolsItem implements IModuleProvider {

    public TextModuleItem() {
        super("text_module");
        setMaxStackSize(16);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 1;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack itemStack, World player, List<String> list, ITooltipFlag whatIsThis) {
        super.addInformation(itemStack, player, list, whatIsThis);
        list.add(I18n.format("tooltip.rftools.uses_s_rf_tick", ScreenConfiguration.TEXT_RFPERTICK.get()));
        NBTTagCompound tagCompound = itemStack.getTagCompound();
        if (tagCompound != null) {
            list.add(I18n.format("tooltip.rftools.text_s", tagCompound.getString("text")));
        }
    }

    @Override
    public Class<TextScreenModule> getServerScreenModule() {
        return TextScreenModule.class;
    }

    @Override
    public Class<TextClientScreenModule> getClientScreenModule() {
        return TextClientScreenModule.class;
    }

    @Override
    public String getName() {
        return "Text";
    }

    @Override
    public void createGui(IModuleGuiBuilder guiBuilder) {
        guiBuilder
                .label("Text:").text("text", "Text to show").color("color", "Color for the text").nl()
                .toggle("large", "Large", "Large or small font")
                .choices("align", "Text alignment", "Left", "Center", "Right").nl();

    }
}