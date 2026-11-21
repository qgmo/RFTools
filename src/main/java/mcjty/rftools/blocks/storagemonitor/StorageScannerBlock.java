package mcjty.rftools.blocks.storagemonitor;

import mcjty.lib.api.Infusable;
import mcjty.lib.gui.GenericGuiContainer;
import mcjty.rftools.blocks.GenericRFToolsBlock;
import mcjty.rftools.setup.GuiProxy;
import net.minecraft.block.material.Material;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.client.resources.I18n;
import mcjty.rftools.client.GuiLoc;
import java.util.Collections;

public class StorageScannerBlock extends GenericRFToolsBlock<StorageScannerTileEntity, StorageScannerContainer> implements Infusable {

    public StorageScannerBlock() {
        super(Material.IRON, StorageScannerTileEntity.class, StorageScannerContainer::new, "storage_scanner", true);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BiFunction<StorageScannerTileEntity, StorageScannerContainer, GenericGuiContainer<? super StorageScannerTileEntity>> getGuiFactory() {
        return GuiStorageScanner::new;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack itemStack, World player, List<String> list, ITooltipFlag advancedToolTip) {
        super.addInformation(itemStack, player, list, advancedToolTip);

        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            Collections.addAll(list, GuiLoc.lines("tooltip.rftools.this_machine_will_scan_all_nearby"));
            list.add(I18n.format("tooltip.rftools.infusing_bonus_reduced_power_consumption"));
        } else {
            list.add(TextFormatting.WHITE + I18n.format("message.rftools.shiftmessage"));
        }
    }

    @Override
    public int getGuiID() {
        return GuiProxy.GUI_STORAGE_SCANNER;
    }
}
