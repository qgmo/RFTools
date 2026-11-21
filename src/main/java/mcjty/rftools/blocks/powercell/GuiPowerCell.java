package mcjty.rftools.blocks.powercell;

import mcjty.lib.gui.GenericGuiContainer;
import mcjty.lib.gui.Window;
import mcjty.lib.gui.layout.PositionalLayout;
import mcjty.lib.gui.widgets.Button;
import mcjty.lib.gui.widgets.EnergyBar;
import mcjty.lib.gui.widgets.Label;
import mcjty.lib.gui.widgets.Panel;
import mcjty.lib.typed.TypedMap;
import mcjty.rftools.RFTools;
import mcjty.rftools.setup.GuiProxy;
import mcjty.rftools.network.RFToolsMessages;
import net.minecraft.util.ResourceLocation;

import java.awt.Rectangle;
import net.minecraft.client.resources.I18n;
import mcjty.rftools.client.GuiLoc;

public class GuiPowerCell extends GenericGuiContainer<PowerCellTileEntity> {
    public static final int POWERCELL_WIDTH = 180;
    public static final int POWERCELL_HEIGHT = 152;

    private EnergyBar energyBar;
    private Button stats;

    private static long lastTime = 0;

    private static final ResourceLocation iconLocation = new ResourceLocation(RFTools.MODID, "textures/gui/powercell.png");

    public GuiPowerCell(PowerCellTileEntity te, PowerCellContainer container) {
        super(RFTools.instance, RFToolsMessages.INSTANCE, te, container, GuiProxy.GUI_MANUAL_MAIN, "powercell");

        xSize = POWERCELL_WIDTH;
        ySize = POWERCELL_HEIGHT;
    }

    @Override
    public void initGui() {
        super.initGui();

        energyBar = new EnergyBar(mc, this).setVertical().setMaxValue(1000).setLayoutHint(10, 7, 8, 54).setShowText(false);
        energyBar.setValue(0);

        Button allNone = new Button(mc, this)
                .setName("allnone")
                .setText(I18n.format("gui.rftools.none")).setTooltips(GuiLoc.lines("gui.rftools.set_all_sides_to_none"))
                .setLayoutHint(140, 10, 32, 15);
        Button allInput = new Button(mc, this)
                .setName("allinput")
                .setText(I18n.format("gui.rftools.in")).setTooltips(GuiLoc.lines("gui.rftools.set_all_sides_to_accept_energy"))
                .setLayoutHint(140, 27, 32, 15);
        Button allOutput = new Button(mc, this)
                .setName("alloutput")
                .setText(I18n.format("gui.rftools.out")).setTooltips(GuiLoc.lines("gui.rftools.set_all_sides_to_send_energy"))
                .setLayoutHint(140, 44, 32, 15);

        stats = new Button(mc, this)
                .setName("clearstats")
                .setText(I18n.format("gui.rftools.stats")).setTooltips(GuiLoc.lines("gui.rftools.power_statistics_press_to_clear"))
                .setLayoutHint(100, 10, 32, 15);

        Label label = new Label(mc, this);
        label.setText(I18n.format("gui.rftools.link")).setTooltips(GuiLoc.lines("gui.rftools.link_a_powercard_to_card_on_the_left")).setLayoutHint(26, 30, 40, 18);

        Panel toplevel = new Panel(mc, this).setBackground(iconLocation).setLayout(new PositionalLayout())
                .addChildren(energyBar, allNone, allInput, allOutput, label, stats);
        toplevel.setBounds(new Rectangle(guiLeft, guiTop, xSize, ySize));

        window = new Window(this, toplevel);

        window.action(RFToolsMessages.INSTANCE, "allnone", tileEntity, PowerCellTileEntity.ACTION_SETNONE);
        window.action(RFToolsMessages.INSTANCE, "allinput", tileEntity, PowerCellTileEntity.ACTION_SETINPUT);
        window.action(RFToolsMessages.INSTANCE, "alloutput", tileEntity, PowerCellTileEntity.ACTION_SETOUTPUT);
        window.action(RFToolsMessages.INSTANCE, "clearstats", tileEntity, PowerCellTileEntity.ACTION_CLEARSTATS);

        requestRF();
    }

    private void requestRF() {
        if (System.currentTimeMillis() - lastTime > 250) {
            lastTime = System.currentTimeMillis();
            tileEntity.requestDataFromServer(RFTools.MODID, PowerCellTileEntity.CMD_GET_INFO, TypedMap.EMPTY);
        }
    }


    @Override
    protected void drawGuiContainerBackgroundLayer(float v, int i, int i2) {
        drawWindow();

        requestRF();

        stats.setTooltips(GuiLoc.lines("gui.rftools.power_statistics_press_to_clear_inserted_s", PowerCellTileEntity.tooltipInserted, PowerCellTileEntity.tooltipExtracted));

        int maxValue = (PowerCellTileEntity.tooltipBlocks - PowerCellTileEntity.tooltipAdvancedBlocks - PowerCellTileEntity.tooltipSimpleBlocks) * PowerCellConfiguration.rfPerNormalCell.get();
        maxValue += PowerCellTileEntity.tooltipAdvancedBlocks * PowerCellConfiguration.rfPerNormalCell.get() * PowerCellConfiguration.advancedFactor.get();
        maxValue += PowerCellTileEntity.tooltipSimpleBlocks * PowerCellConfiguration.rfPerNormalCell.get() / PowerCellConfiguration.simpleFactor.get();
        energyBar.setMaxValue(maxValue);
        energyBar.setValue(PowerCellTileEntity.tooltipEnergy);
    }
}
