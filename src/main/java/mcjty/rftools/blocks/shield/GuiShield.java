package mcjty.rftools.blocks.shield;

import mcjty.lib.base.StyleConfig;
import mcjty.lib.gui.GenericGuiContainer;
import mcjty.lib.tileentity.GenericEnergyStorageTileEntity;
import mcjty.lib.tileentity.GenericTileEntity;
import mcjty.lib.gui.Window;
import mcjty.lib.gui.events.DefaultSelectionEvent;
import mcjty.lib.gui.layout.HorizontalAlignment;
import mcjty.lib.gui.layout.HorizontalLayout;
import mcjty.lib.gui.layout.PositionalLayout;
import mcjty.lib.gui.widgets.Button;
import mcjty.lib.gui.widgets.*;
import mcjty.lib.gui.widgets.Label;
import mcjty.lib.gui.widgets.Panel;
import mcjty.lib.gui.widgets.TextField;
import mcjty.lib.typed.TypedMap;
import mcjty.lib.varia.RedstoneMode;
import mcjty.rftools.RFTools;
import mcjty.rftools.blocks.shield.filters.*;
import mcjty.rftools.setup.GuiProxy;
import mcjty.rftools.network.RFToolsMessages;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static mcjty.rftools.blocks.shield.ShieldTEBase.*;
import net.minecraft.client.resources.I18n;
import mcjty.rftools.client.GuiLoc;
import mcjty.rftools.client.LocChoiceLabel;
import mcjty.rftools.client.LocImageChoiceLabel;

public class GuiShield extends GenericGuiContainer<ShieldTEBase> {
    public static final int SHIELD_WIDTH = 256;
    public static final int SHIELD_HEIGHT = 224;

    public static final String ACTION_PASS = "Pass";
    public static final String ACTION_SOLID = "Solid";
    public static final String ACTION_DAMAGE = "Damage";
    public static final String ACTION_SOLIDDAMAGE = "SolDmg";

    public static final String DAMAGETYPE_GENERIC = DamageTypeMode.DAMAGETYPE_GENERIC.getDescription();
    public static final String DAMAGETYPE_PLAYER = DamageTypeMode.DAMAGETYPE_PLAYER.getDescription();

    private EnergyBar energyBar;
    private ChoiceLabel visibilityOptions;
    private ChoiceLabel actionOptions;
    private ChoiceLabel typeOptions;
    private ChoiceLabel damageType;
    private WidgetList filterList;
    private TextField player;
    private Button addFilter;
    private Button delFilter;
    private Button upFilter;
    private Button downFilter;
    private ColorSelector colorSelector;

    // A copy of the filterList we're currently showing.
    private List<ShieldFilter> filters = null;
    private int listDirty = 0;

    private static List<ShieldFilter> fromServer_filters = new ArrayList<>();
    public static void storeFiltersForClient(List<ShieldFilter> filters) {
        fromServer_filters = new ArrayList<>(filters);
    }

    private static final ResourceLocation iconLocation = new ResourceLocation(RFTools.MODID, "textures/gui/shieldprojector.png");
    private static final ResourceLocation iconGuiElements = new ResourceLocation(RFTools.MODID, "textures/gui/guielements.png");

    public GuiShield(ShieldTileEntity shieldTileEntity, ShieldContainer container) {
        this((ShieldTEBase) shieldTileEntity, container);
    }

    public GuiShield(ShieldTileEntity2 shieldTileEntity, ShieldContainer container) {
        this((ShieldTEBase) shieldTileEntity, container);
    }

    public GuiShield(ShieldTileEntity3 shieldTileEntity, ShieldContainer container) {
        this((ShieldTEBase) shieldTileEntity, container);
    }

    public GuiShield(ShieldTileEntity4 shieldTileEntity, ShieldContainer container) {
        this((ShieldTEBase) shieldTileEntity, container);
    }

    public GuiShield(ShieldTEBase shieldTileEntity, ShieldContainer container) {
        super(RFTools.instance, RFToolsMessages.INSTANCE, shieldTileEntity, container, GuiProxy.GUI_MANUAL_SHAPE, "shield");
        GenericEnergyStorageTileEntity.setCurrentRF(shieldTileEntity.getStoredPower());

        xSize = SHIELD_WIDTH;
        ySize = SHIELD_HEIGHT;
    }

    @Override
    public void initGui() {
        super.initGui();

        long maxEnergyStored = tileEntity.getCapacity();
        energyBar = new EnergyBar(mc, this).setVertical().setMaxValue(maxEnergyStored).setLayoutHint(12, 141, 10, 76).setShowText(false);
        energyBar.setValue(GenericEnergyStorageTileEntity.getCurrentRF());

        initVisibilityMode();
        initActionOptions();
        initTypeOptions();
        ImageChoiceLabel redstoneMode = initRedstoneMode();
        initDamageType();

        filterList = new WidgetList(mc, this).setName("filters").setDesiredHeight(120).
                addSelectionEvent(new DefaultSelectionEvent() {
                    @Override
                    public void select(Widget<?> parent, int index) {
                        selectFilter();
                    }
                });
        Slider filterSlider = new Slider(mc, this).setVertical().setScrollableName("filters").setDesiredWidth(11).setDesiredHeight(120);
        Panel filterPanel = new Panel(mc, this).setLayout(new HorizontalLayout().setSpacing(1).setHorizontalMargin(3))
                .setLayoutHint(12, 10, 154, 124).addChildren(filterList, filterSlider)
                .setFilledBackground(0xff9e9e9e);

        Button applyCamo = new Button(mc, this).setChannel("camo").setText(I18n.format("gui.rftools.common.set")).setTooltips(GuiLoc.lines("gui.rftools.common.set_the_camouflage_block")).
                setLayoutHint(46, 142, 30, 16);
        colorSelector = new ColorSelector(mc, this)
                .setName("color")
                .setTooltips(GuiLoc.lines("gui.rftools.common.color_for_the_shield"))
                .setLayoutHint(25, 177, 30, 16);

        ToggleButton light = new ToggleButton(mc, this).setName("light").setCheckMarker(true).setText("L").setTooltips(GuiLoc.lines("gui.rftools.common.if_pressed_light_is_blocked_by_the_shield"))
                .setLayoutHint(56, 177, 23, 16);

        player = new TextField(mc, this).setTooltips(GuiLoc.lines("gui.rftools.common.optional_player_name")).setLayoutHint(170, 44, 80, 14);

        addFilter = new Button(mc, this).setChannel("addfilter").setText(I18n.format("gui.rftools.common.add")).setTooltips(GuiLoc.lines("gui.rftools.common.add_selected_filter")).setLayoutHint(4, 6, 36, 14);
        delFilter = new Button(mc, this).setChannel("delfilter").setText(I18n.format("gui.rftools.common.del")).setTooltips(GuiLoc.lines("gui.rftools.common.delete_selected_filter")).setLayoutHint(39, 6, 36, 14);
        upFilter = new Button(mc, this).setChannel("upfilter").setText(I18n.format("gui.rftools.common.up")).setTooltips(GuiLoc.lines("gui.rftools.common.move_filter_up")).setLayoutHint(4, 22, 36, 14);
        downFilter = new Button(mc, this).setChannel("downfilter").setText(I18n.format("gui.rftools.common.down")).setTooltips(GuiLoc.lines("gui.rftools.common.move_filter_down")).setLayoutHint(39, 22, 36, 14);

        Panel controlPanel = new Panel(mc, this).setLayout(new PositionalLayout()).setLayoutHint(170, 58, 80, 43)
                .addChildren(addFilter, delFilter, upFilter, downFilter)
                .setFilledRectThickness(-2)
                .setFilledBackground(StyleConfig.colorListBackground);

        Label lootingBonus = new Label(mc, this).setHorizontalAlignment(HorizontalAlignment.ALIGN_RIGHT)
                .setText(I18n.format("gui.rftools.common.looting"));
        lootingBonus.setTooltips(GuiLoc.lines("gui.rftools.common.insert_dimensional_shards_for_looting_bonus"))
                .setLayoutHint(160, 118, 60, 18);

        Panel toplevel = new Panel(mc, this).setBackground(iconLocation).setLayout(new PositionalLayout()).addChildren(energyBar,
                visibilityOptions, applyCamo, redstoneMode, filterPanel, actionOptions,
                typeOptions, player, controlPanel, damageType,
                colorSelector, lootingBonus, light);
        toplevel.setBounds(new Rectangle(guiLeft, guiTop, xSize, ySize));

        window = new Window(this, toplevel);

        window.bind(RFToolsMessages.INSTANCE, "redstone", tileEntity, GenericTileEntity.VALUE_RSMODE.getName());
        window.bind(RFToolsMessages.INSTANCE, "visibility", tileEntity, ShieldTEBase.VALUE_SHIELDVISMODE.getName());
        window.bind(RFToolsMessages.INSTANCE, "damage", tileEntity, ShieldTEBase.VALUE_DAMAGEMODE.getName());
        window.bind(RFToolsMessages.INSTANCE, "color", tileEntity, ShieldTEBase.VALUE_COLOR.getName());
        window.bind(RFToolsMessages.INSTANCE, "light", tileEntity, ShieldTEBase.VALUE_LIGHT.getName());
        window.event("camo", (source, params) -> applyCamoToShield());
        window.event("addfilter", (source, params) -> addNewFilter());
        window.event("delfilter", (source, params) -> removeSelectedFilter());
        window.event("upfilter", (source, params) -> moveFilterUp());
        window.event("downfilter", (source, params) -> moveFilterDown());

        listDirty = 0;
        requestFilters();
        tileEntity.requestRfFromServer(RFTools.MODID);
    }

    private void selectFilter() {
        int selected = filterList.getSelected();
        if (selected != -1) {
            ShieldFilter shieldFilter = filters.get(selected);
            boolean solid = (shieldFilter.getAction() & ShieldFilter.ACTION_SOLID) != 0;
            boolean damage = (shieldFilter.getAction() & ShieldFilter.ACTION_DAMAGE) != 0;
            if (solid && damage) {
                actionOptions.setChoice(ACTION_SOLIDDAMAGE);
            } else if (solid) {
                actionOptions.setChoice(ACTION_SOLID);
            } else if (damage) {
                actionOptions.setChoice(ACTION_DAMAGE);
            } else {
                actionOptions.setChoice(ACTION_PASS);
            }
            String type = shieldFilter.getFilterName();
            if (DefaultFilter.DEFAULT.equals(type)) {
                typeOptions.setChoice("All");
            } else if (AnimalFilter.ANIMAL.equals(type)) {
                typeOptions.setChoice("Passive");
            } else if (HostileFilter.HOSTILE.equals(type)) {
                typeOptions.setChoice("Hostile");
            } else if (PlayerFilter.PLAYER.equals(type)) {
                typeOptions.setChoice("Player");
            } else if (ItemFilter.ITEM.equals(type)) {
                typeOptions.setChoice("Item");
            }
            if (shieldFilter instanceof PlayerFilter) {
                player.setText(((PlayerFilter)shieldFilter).getName());
            } else {
                player.setText("");
            }
        }
    }

    private void requestFilters() {
        RFToolsMessages.INSTANCE.sendToServer(new PacketGetFilters(tileEntity.getPos()));
    }

    private void requestListsIfNeeded() {
        listDirty--;
        if (listDirty <= 0) {
            requestFilters();
            listDirty = 20;
        }
    }

    private void populateFilters() {
        List<ShieldFilter> newFilters = new ArrayList<>(fromServer_filters);
        if (newFilters.equals(filters)) {
            return;
        }

        filters = new ArrayList<>(newFilters);
        filterList.removeChildren();
        for (ShieldFilter filter : filters) {
            String n;
            if ("player".equals(filter.getFilterName())) {
                PlayerFilter playerFilter = (PlayerFilter) filter;
                if (playerFilter.getName() == null || playerFilter.getName().isEmpty()) {
                    n = I18n.format("gui.rftools.shield.players");
                } else {
                    n = I18n.format("gui.rftools.shield.player_s", playerFilter.getName());
                }
            } else {
                n = filter.getFilterName();
            }
            Panel panel = new Panel(mc, this).setLayout(new HorizontalLayout());
            panel.addChild(new Label(mc, this).setText(n).setColor(StyleConfig.colorTextInListNormal).setHorizontalAlignment(HorizontalAlignment.ALIGN_LEFT).setDesiredWidth(85));
            String actionName;
            boolean solid = (filter.getAction() & ShieldFilter.ACTION_SOLID) != 0;
            boolean damage = (filter.getAction() & ShieldFilter.ACTION_DAMAGE) != 0;
            if (solid && damage) {
                actionName = ACTION_SOLIDDAMAGE;
            } else if (solid) {
                actionName = ACTION_SOLID;
            } else if (damage) {
                actionName = ACTION_DAMAGE;
            } else {
                actionName = ACTION_PASS;
            }
            panel.addChild(new Label(mc, this).setText(actionName).setColor(StyleConfig.colorTextInListNormal).setHorizontalAlignment(HorizontalAlignment.ALIGN_LEFT));
            filterList.addChild(panel);
        }
    }

    private void moveFilterUp() {
        sendServerCommand(RFToolsMessages.INSTANCE, ShieldTEBase.CMD_UPFILTER,
                TypedMap.builder()
                        .put(PARAM_SELECTED, filterList.getSelected())
                        .build());
        listDirty = 0;
    }

    private void moveFilterDown() {
        sendServerCommand(RFToolsMessages.INSTANCE, ShieldTEBase.CMD_DOWNFILTER,
                TypedMap.builder()
                        .put(PARAM_SELECTED, filterList.getSelected())
                        .build());
        listDirty = 0;
    }

    private void addNewFilter() {
        String actionName = actionOptions.getCurrentChoice();
        int action;
        if (ACTION_PASS.equals(actionName)) {
            action = ShieldFilter.ACTION_PASS;
        } else if (ACTION_SOLID.equals(actionName)) {
            action = ShieldFilter.ACTION_SOLID;
        } else if (ACTION_SOLIDDAMAGE.equals(actionName)) {
            action = ShieldFilter.ACTION_DAMAGE + ShieldFilter.ACTION_SOLID;
        } else {
            action = ShieldFilter.ACTION_DAMAGE;
        }

        String filterName = typeOptions.getCurrentChoice();
        String type;
        if ("All".equals(filterName)) {
            type = DefaultFilter.DEFAULT;
        } else if ("Passive".equals(filterName)) {
            type = AnimalFilter.ANIMAL;
        } else if ("Hostile".equals(filterName)) {
            type = HostileFilter.HOSTILE;
        } else if ("Item".equals(filterName)) {
            type = ItemFilter.ITEM;
        } else {
            type = PlayerFilter.PLAYER;
        }

        String playerName = player.getText();
        int selected = filterList.getSelected();

        sendServerCommand(RFToolsMessages.INSTANCE, ShieldTEBase.CMD_ADDFILTER,
                TypedMap.builder()
                        .put(PARAM_ACTION, action)
                        .put(PARAM_TYPE, type)
                        .put(PARAM_PLAYER, playerName)
                        .put(PARAM_SELECTED, filterList.getSelected())
                        .build());
        listDirty = 0;
    }

    private void removeSelectedFilter() {
        sendServerCommand(RFToolsMessages.INSTANCE, ShieldTEBase.CMD_DELFILTER,
                TypedMap.builder()
                        .put(PARAM_SELECTED, filterList.getSelected())
                        .build());
        listDirty = 0;
    }

    private ImageChoiceLabel initRedstoneMode() {
        ImageChoiceLabel redstoneMode = new LocImageChoiceLabel(mc, this).
                setName("redstone").
                addChoice(RedstoneMode.REDSTONE_IGNORED.getDescription(), I18n.format("gui.rftools.common.redstone_mode_ignored"), iconGuiElements, 0, 0).
                addChoice(RedstoneMode.REDSTONE_OFFREQUIRED.getDescription(), I18n.format("gui.rftools.common.redstone_mode_off_to_activate"), iconGuiElements, 16, 0).
                addChoice(RedstoneMode.REDSTONE_ONREQUIRED.getDescription(), I18n.format("gui.rftools.common.redstone_mode_on_to_activate"), iconGuiElements, 32, 0);
        redstoneMode.setLayoutHint(62, 200, 16, 16);
        redstoneMode.setCurrentChoice(tileEntity.getRSMode().ordinal());
        return redstoneMode;
    }

    private void initVisibilityMode() {
        visibilityOptions = new LocChoiceLabel(mc, this)
                .setName("visibility")
                .setLayoutHint(25, 161, 54, 14);
        for (ShieldRenderingMode m : ShieldRenderingMode.values()) {
            if ((!ShieldConfiguration.allowInvisibleShield.get()) && m == ShieldRenderingMode.MODE_INVISIBLE) {
                continue;
            }
            visibilityOptions.addChoices(m.getDescription());
        }
        if (ShieldConfiguration.allowInvisibleShield.get()) {
            visibilityOptions.setChoiceTooltip(ShieldRenderingMode.MODE_INVISIBLE.getDescription(), GuiLoc.lines("gui.rftools.common.shield_is_completely_invisible"));
        }
        visibilityOptions.setChoiceTooltip(ShieldRenderingMode.MODE_SHIELD.getDescription(), GuiLoc.lines("gui.rftools.common.default_shield_texture"));
        visibilityOptions.setChoiceTooltip(ShieldRenderingMode.MODE_TRANSP.getDescription(), GuiLoc.lines("gui.rftools.common.transparent_shield_texture"));
        visibilityOptions.setChoiceTooltip(ShieldRenderingMode.MODE_SOLID.getDescription(), GuiLoc.lines("gui.rftools.common.solid_shield_texture"));
        visibilityOptions.setChoiceTooltip(ShieldRenderingMode.MODE_MIMIC.getDescription(), GuiLoc.lines("gui.rftools.common.use_the_texture_from_the_supplied_block"));
    }

    private void initActionOptions() {
        actionOptions = new LocChoiceLabel(mc, this).setLayoutHint(170, 12, 80, 14);
        actionOptions.addChoices(ACTION_PASS, ACTION_SOLID, ACTION_DAMAGE, ACTION_SOLIDDAMAGE);
        actionOptions.setChoiceTooltip(ACTION_PASS, GuiLoc.lines("gui.rftools.common.entity_that_matches_this_filter_can_pass_2"));
        actionOptions.setChoiceTooltip(ACTION_SOLID, GuiLoc.lines("gui.rftools.common.entity_that_matches_this_filter_cannot_pass_2"));
        actionOptions.setChoiceTooltip(ACTION_DAMAGE, GuiLoc.lines("gui.rftools.common.entity_that_matches_this_filter_can_pass"));
        actionOptions.setChoiceTooltip(ACTION_SOLIDDAMAGE, GuiLoc.lines("gui.rftools.common.entity_that_matches_this_filter_cannot_pass"));
    }

    private void initTypeOptions() {
        typeOptions = new LocChoiceLabel(mc, this).setLayoutHint(170, 28, 80, 14);
        typeOptions.addChoices("All", "Passive", "Hostile", "Item", "Player");
        typeOptions.setChoiceTooltip("All", GuiLoc.lines("gui.rftools.common.matches_everything"));
        typeOptions.setChoiceTooltip("Passive", GuiLoc.lines("gui.rftools.common.matches_passive_mobs"));
        typeOptions.setChoiceTooltip("Hostile", GuiLoc.lines("gui.rftools.common.matches_hostile_mobs"));
        typeOptions.setChoiceTooltip("Item", GuiLoc.lines("gui.rftools.common.matches_items"));
        typeOptions.setChoiceTooltip("Player", GuiLoc.lines("gui.rftools.common.matches_players_optionally_named"));
    }

    private void initDamageType() {
        damageType = new LocChoiceLabel(mc, this)
                .setName("damage")
                .setLayoutHint(170, 102, 80, 14);
        damageType.addChoices(DAMAGETYPE_GENERIC, DAMAGETYPE_PLAYER);
        damageType.setChoiceTooltip(DAMAGETYPE_GENERIC, GuiLoc.lines("gui.rftools.common.generic_damage_type"));
        damageType.setChoiceTooltip(DAMAGETYPE_PLAYER, GuiLoc.lines("gui.rftools.common.damage_as_done_by_a_player"));
    }

    private void applyCamoToShield() {
        ItemStack stack = tileEntity.getStackInSlot(0);

        int pass = 0;
        if (!stack.isEmpty()) {
            Block block = Block.getBlockFromItem(stack.getItem());
            if (block != null) {
                pass = block.getBlockLayer().ordinal();
            }
        }
        sendServerCommand(RFToolsMessages.INSTANCE, ShieldTEBase.CMD_APPLYCAMO,
                TypedMap.builder()
                        .put(PARAM_PASS, pass)
                        .build());
    }

    private void enableButtons() {
        int sel = filterList.getSelected();
        int cnt = filterList.getMaximum();
        delFilter.setEnabled(sel != -1 && cnt > 0);
        upFilter.setEnabled(sel > 0 && cnt > 0);
        downFilter.setEnabled(sel < cnt-1 && sel != -1 && cnt > 0);
        if (sel == -1) {
            addFilter.setText(I18n.format("gui.rftools.common.add"));
        } else {
            addFilter.setText(I18n.format("gui.rftools.ins"));
        }
        player.setEnabled("Player".equals(typeOptions.getCurrentChoice()));
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float v, int i, int i2) {
        requestListsIfNeeded();
        populateFilters();
        enableButtons();
        drawWindow();
        long currentRF = GenericEnergyStorageTileEntity.getCurrentRF();
        energyBar.setValue(currentRF);
        colorSelector.setCurrentColor(tileEntity.getShieldColor());

        tileEntity.requestRfFromServer(RFTools.MODID);
    }
}