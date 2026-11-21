package mcjty.rftools.items.envmodules;

import mcjty.rftools.blocks.environmental.EnvModuleProvider;
import mcjty.rftools.blocks.environmental.EnvironmentalConfiguration;
import mcjty.rftools.blocks.environmental.modules.EnvironmentModule;
import mcjty.rftools.blocks.environmental.modules.SlownessEModule;
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

public class SlownessEModuleItem extends GenericRFToolsItem implements EnvModuleProvider {

    public SlownessEModuleItem() {
        super("slowness_module");
        setMaxStackSize(16);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack itemStack, World player, List<String> list, ITooltipFlag whatIsThis) {
        super.addInformation(itemStack, player, list, whatIsThis);
        Collections.addAll(list, GuiLoc.lines("tooltip.rftools.this_module_gives_slowness_when_used_in_the"));
        list.add(I18n.format("tooltip.rftools.uses_s_rf_tick_per_cubic_block", EnvironmentalConfiguration.SLOWNESS_RFPERTICK.get()));
        if (!EnvironmentalConfiguration.slownessAvailable.get()) {
            list.add(I18n.format("tooltip.rftools.this_module_only_works_on_mobs_see_config"));
        }
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 1;
    }

    @Override
    public Class<? extends EnvironmentModule> getServerEnvironmentModule() {
        return SlownessEModule.class;
    }

    @Override
    public String getName() {
        return "Slowness";
    }
}