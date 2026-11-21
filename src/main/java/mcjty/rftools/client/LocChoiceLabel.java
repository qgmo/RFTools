package mcjty.rftools.client;

import mcjty.lib.gui.widgets.ChoiceLabel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * A choice label that displays a translation of the current choice. The choice itself (which is also sent to the server
 * and compared in code) is not changed.
 */
@SideOnly(Side.CLIENT)
public class LocChoiceLabel extends ChoiceLabel {

    public LocChoiceLabel(Minecraft mc, Gui gui) {
        super(mc, gui);
    }

    @Override
    public ChoiceLabel setText(String text) {
        return super.setText(GuiLoc.choice(text, getName()));
    }

    @Override
    public List<String> getTooltips() {
        return GuiLoc.trLines(super.getTooltips());
    }
}
