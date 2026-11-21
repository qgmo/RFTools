package mcjty.rftools.blocks.shaper;

import mcjty.lib.gui.layout.PositionalLayout;
import mcjty.lib.gui.widgets.Panel;
import mcjty.lib.gui.widgets.ToggleButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import mcjty.rftools.client.GuiLoc;

public class ShapeGuiTools {

    public static ToggleButton createAxisButton(Gui gui, Panel toplevel, int x, int y) {
        ToggleButton showAxis = new ToggleButton(Minecraft.getMinecraft(), gui).setCheckMarker(true)
                .setTooltips(GuiLoc.lines("gui.rftools.enable_axis_rendering_in_the_preview"))
                .setText("A").setLayoutHint(new PositionalLayout.PositionalHint(x, y, 24, 16));
        showAxis.setPressed(true);
        toplevel.addChild(showAxis);
        return showAxis;
    }

    public static ToggleButton createBoxButton(Gui gui, Panel toplevel, int x, int y) {
        ToggleButton showAxis = new ToggleButton(Minecraft.getMinecraft(), gui).setCheckMarker(true)
                .setTooltips(GuiLoc.lines("gui.rftools.enable_preview_of_the_outer_bounds"))
                .setText("B").setLayoutHint(new PositionalLayout.PositionalHint(x, y, 24, 16));
        showAxis.setPressed(true);
        toplevel.addChild(showAxis);
        return showAxis;
    }

    public static ToggleButton createScanButton(Gui gui, Panel toplevel, int x, int y) {
        ToggleButton showAxis = new ToggleButton(Minecraft.getMinecraft(), gui).setCheckMarker(true)
                .setTooltips(GuiLoc.lines("gui.rftools.show_a_visual_scanline_wherever_the_preview"))
                .setText("S").setLayoutHint(new PositionalLayout.PositionalHint(x, y, 24, 16));
        showAxis.setPressed(true);
        toplevel.addChild(showAxis);
        return showAxis;
    }
}
