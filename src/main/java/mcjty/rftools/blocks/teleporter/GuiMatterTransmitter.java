package mcjty.rftools.blocks.teleporter;

import mcjty.lib.base.StyleConfig;
import mcjty.lib.container.EmptyContainer;
import mcjty.lib.gui.GenericGuiContainer;
import mcjty.lib.tileentity.GenericEnergyStorageTileEntity;
import mcjty.lib.gui.Window;
import mcjty.lib.gui.layout.HorizontalAlignment;
import mcjty.lib.gui.layout.HorizontalLayout;
import mcjty.lib.gui.layout.VerticalLayout;
import mcjty.lib.gui.widgets.Button;
import mcjty.lib.gui.widgets.*;
import mcjty.lib.gui.widgets.Label;
import mcjty.lib.gui.widgets.Panel;
import mcjty.lib.gui.widgets.TextField;
import mcjty.lib.typed.TypedMap;
import mcjty.rftools.RFTools;
import mcjty.rftools.setup.GuiProxy;
import mcjty.rftools.network.PacketGetPlayers;
import mcjty.rftools.network.RFToolsMessages;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.*;
import java.util.List;

import static mcjty.rftools.blocks.teleporter.MatterTransmitterTileEntity.PARAM_PLAYER;
import net.minecraft.client.resources.I18n;
import mcjty.rftools.client.GuiLoc;
import mcjty.rftools.client.LocChoiceLabel;

public class GuiMatterTransmitter extends GenericGuiContainer<MatterTransmitterTileEntity> {
    public static final int MATTER_WIDTH = 180;
    public static final int MATTER_HEIGHT = 160;
    public static final String ACCESS_PRIVATE = "Private";
    public static final String ACCESS_PUBLIC = "Public";

    private EnergyBar energyBar;
    private ChoiceLabel privateSetting;
    private WidgetList allowedPlayers;
    private Button addButton;
    private Button delButton;
    private TextField nameField;

    // A copy of the players we're currently showing.
    private List<String> players = null;
    private int listDirty = 0;

    private static Set<String> fromServer_allowedPlayers = new HashSet<>();
    public static void storeAllowedPlayersForClient(List<String> players) {
        fromServer_allowedPlayers = new HashSet<>(players);
    }


    public GuiMatterTransmitter(MatterTransmitterTileEntity transmitterTileEntity, EmptyContainer container) {
        super(RFTools.instance, RFToolsMessages.INSTANCE, transmitterTileEntity, container, GuiProxy.GUI_MANUAL_MAIN, "tptransmitter");
        GenericEnergyStorageTileEntity.setCurrentRF(transmitterTileEntity.getStoredPower());

        xSize = MATTER_WIDTH;
        ySize = MATTER_HEIGHT;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void initGui() {
        super.initGui();

        long maxEnergyStored = tileEntity.getCapacity();
        energyBar = new EnergyBar(mc, this).setFilledRectThickness(1).setHorizontal().setDesiredHeight(12).setDesiredWidth(80).setMaxValue(maxEnergyStored).setShowText(false);
        energyBar.setValue(GenericEnergyStorageTileEntity.getCurrentRF());

        TextField textField = new TextField(mc, this)
                .setName("name")
                .setTooltips(GuiLoc.lines("gui.rftools.use_this_name_to_identify_this_transmitter"));
        Panel namePanel = new Panel(mc, this).setLayout(new HorizontalLayout()).addChild(new Label(mc, this).setText(I18n.format("gui.rftools.common.name"))).addChild(textField).setDesiredHeight(16);

        privateSetting = new LocChoiceLabel(mc, this)
                .setName("private")
                .addChoices(ACCESS_PUBLIC, ACCESS_PRIVATE).setDesiredHeight(14).setDesiredWidth(60).
            setChoiceTooltip(ACCESS_PUBLIC, GuiLoc.lines("gui.rftools.everyone_can_access_this_transmitter_and")).
            setChoiceTooltip(ACCESS_PRIVATE, GuiLoc.lines("gui.rftools.only_people_in_the_access_list_below_can_2"));
        ToggleButton beamToggle = new ToggleButton(mc, this)
                .setName("beam")
                .setText(I18n.format("gui.rftools.hide")).setCheckMarker(true).setDesiredHeight(14).setDesiredWidth(49).setTooltips(GuiLoc.lines("gui.rftools.hide_the_teleportation_beam"));
        Panel privatePanel = new Panel(mc, this).setLayout(new HorizontalLayout()).addChild(new Label(mc, this).setText(I18n.format("gui.rftools.common.access"))).addChild(privateSetting).addChild(beamToggle).setDesiredHeight(16);

        allowedPlayers = new WidgetList(mc, this).setName("allowedplayers");
        Slider allowedPlayerSlider = new Slider(mc, this).setDesiredWidth(10).setVertical().setScrollableName("allowedplayers");
        Panel allowedPlayersPanel = new Panel(mc, this).setLayout(new HorizontalLayout().setHorizontalMargin(3).setSpacing(1)).addChild(allowedPlayers).addChild(allowedPlayerSlider)
                .setFilledBackground(0xff9e9e9e);

        nameField = new TextField(mc, this);
        addButton = new Button(mc, this).setChannel("addplayer").setText(I18n.format("gui.rftools.common.add")).setDesiredHeight(13).setDesiredWidth(34).setTooltips(GuiLoc.lines("gui.rftools.common.add_a_player_to_the_access_list"));
        delButton = new Button(mc, this).setChannel("delplayer").setText(I18n.format("gui.rftools.common.del")).setDesiredHeight(13).setDesiredWidth(34).setTooltips(GuiLoc.lines("gui.rftools.common.remove_the_selected_player_from_the_access"));
        Panel buttonPanel = new Panel(mc, this).setLayout(new HorizontalLayout()).addChild(nameField).addChild(addButton).addChild(delButton).setDesiredHeight(16);

        Panel toplevel = new Panel(mc, this).setFilledRectThickness(2).setLayout(new VerticalLayout().setHorizontalMargin(3).setVerticalMargin(3).setSpacing(1)).
                addChildren(energyBar, namePanel, privatePanel, allowedPlayersPanel, buttonPanel);
        toplevel.setBounds(new Rectangle(guiLeft, guiTop, MATTER_WIDTH, MATTER_HEIGHT));
        window = new Window(this, toplevel);
        Keyboard.enableRepeatEvents(true);

        listDirty = 0;
        requestPlayers();
        tileEntity.requestRfFromServer(RFTools.MODID);

        window.bind(RFToolsMessages.INSTANCE, "name", tileEntity, MatterTransmitterTileEntity.VALUE_NAME.getName());
        window.bind(RFToolsMessages.INSTANCE, "private", tileEntity, MatterTransmitterTileEntity.VALUE_PRIVATE.getName());
        window.bind(RFToolsMessages.INSTANCE, "beam", tileEntity, MatterTransmitterTileEntity.VALUE_BEAM.getName());
        window.event("addplayer", (source, params) -> addPlayer());
        window.event("delplayer", (source, params) -> delPlayer());
    }

    private void addPlayer() {
        sendServerCommand(RFToolsMessages.INSTANCE, MatterTransmitterTileEntity.CMD_ADDPLAYER,
                TypedMap.builder()
                        .put(PARAM_PLAYER, nameField.getText())
                        .build());
        listDirty = 0;
    }

    private void delPlayer() {
        sendServerCommand(RFToolsMessages.INSTANCE, MatterTransmitterTileEntity.CMD_DELPLAYER,
                TypedMap.builder()
                        .put(PARAM_PLAYER, nameField.getText())
                        .build());
        listDirty = 0;
    }


    private void requestPlayers() {
        RFToolsMessages.INSTANCE.sendToServer(new PacketGetPlayers(tileEntity.getPos(), MatterTransmitterTileEntity.CMD_GETPLAYERS, MatterTransmitterTileEntity.CLIENTCMD_GETPLAYERS));
    }

    private void populatePlayers() {
        List<String> newPlayers = new ArrayList<>(fromServer_allowedPlayers);
        Collections.sort(newPlayers);
        if (newPlayers.equals(players)) {
            return;
        }

        players = new ArrayList<>(newPlayers);
        allowedPlayers.removeChildren();
        for (String player : players) {
            allowedPlayers.addChild(new Label(mc, this).setColor(StyleConfig.colorTextInListNormal).setText(player).setHorizontalAlignment(HorizontalAlignment.ALIGN_LEFT));
        }
    }


    private void requestListsIfNeeded() {
        listDirty--;
        if (listDirty <= 0) {
            requestPlayers();
            listDirty = 20;
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float v, int i, int i2) {
        requestListsIfNeeded();
        populatePlayers();
        enableButtons();

        drawWindow();
        long currentRF = GenericEnergyStorageTileEntity.getCurrentRF();
        energyBar.setValue(currentRF);
        tileEntity.requestRfFromServer(RFTools.MODID);
    }

    private void enableButtons() {
        boolean isPrivate = ACCESS_PRIVATE.equals(privateSetting.getCurrentChoice());
        allowedPlayers.setEnabled(isPrivate);
        nameField.setEnabled(isPrivate);

        int isPlayerSelected = allowedPlayers.getSelected();
        delButton.setEnabled(isPrivate && (isPlayerSelected != -1));
        String name = nameField.getText();
        addButton.setEnabled(isPrivate && name != null && !name.isEmpty());
    }
}
