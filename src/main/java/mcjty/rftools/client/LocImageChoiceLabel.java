package mcjty.rftools.client;

import mcjty.lib.gui.widgets.ImageChoiceLabel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * An image choice label of which the tooltips can be lang keys (as read from a .gui file)
 */
@SideOnly(Side.CLIENT)
public class LocImageChoiceLabel extends ImageChoiceLabel {

    public LocImageChoiceLabel(Minecraft mc, Gui gui) {
        super(mc, gui);
    }

    @Override
    public List<String> getTooltips() {
        return GuiLoc.trLines(super.getTooltips());
    }
}
