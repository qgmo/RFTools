package mcjty.rftools.items.envmodules;

import mcjty.rftools.blocks.environmental.EnvModuleProvider;
import mcjty.rftools.blocks.environmental.EnvironmentalConfiguration;
import mcjty.rftools.blocks.environmental.modules.EnvironmentModule;
import mcjty.rftools.blocks.environmental.modules.WaterBreathingEModule;
import mcjty.rftools.items.GenericRFToolsItem;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import net.minecraft.client.resources.I18n;
import mcjty.rftools.client.GuiLoc;
import java.util.Collections;

public class WaterBreathingEModuleItem extends GenericRFToolsItem implements EnvModuleProvider {

    public WaterBreathingEModuleItem() {
        super("waterbreathing_module");
        setMaxStackSize(16);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack itemStack, World player, List<String> list, ITooltipFlag whatIsThis) {
        super.addInformation(itemStack, player, list, whatIsThis);
        Collections.addAll(list, GuiLoc.lines("tooltip.rftools.this_module_gives_water_breathing_bonus"));
        list.add(I18n.format("tooltip.rftools.uses_s_rf_tick_per_cubic_block", EnvironmentalConfiguration.WATERBREATHING_RFPERTICK.get()));
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 1;
    }

    @Override
    public Class<? extends EnvironmentModule> getServerEnvironmentModule() {
        return WaterBreathingEModule.class;
    }

    @Override
    public String getName() {
        return "Water";
    }
}