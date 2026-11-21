package mcjty.rftools.blocks.storagemonitor;

import mcjty.rftools.network.PacketGuard;
import net.minecraft.entity.player.EntityPlayerMP;
import mcjty.lib.typed.TypedMap;
import mcjty.lib.varia.WorldTools;
import mcjty.rftools.ClientCommandHandler;
import mcjty.rftools.network.RFToolsMessages;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class StorageScannerTools {

    public static void scannerSearch(EntityPlayer player, Integer dim, BlockPos pos, String text) {
        if (dim == null || text == null || !(player instanceof EntityPlayerMP)) {
            return;
        }
        World world = DimensionManager.getWorld(dim);
        {
            TileEntity te = PacketGuard.machine((EntityPlayerMP) player, world, pos);
            if (te instanceof StorageScannerTileEntity) {
                StorageScannerTileEntity scannerTileEntity = (StorageScannerTileEntity) te;
                Set<BlockPos> inventories = scannerTileEntity.performSearch(text);
                RFToolsMessages.sendToClient(player, ClientCommandHandler.CMD_RETURN_SCANNER_SEARCH,
                        TypedMap.builder()
                                .put(ClientCommandHandler.PARAM_INVENTORIES, new ArrayList<>(inventories)));
            }
        }
    }

    public static void requestContents(EntityPlayer player, Integer dim, BlockPos pos, BlockPos invpos) {
        if (dim == null || invpos == null || !(player instanceof EntityPlayerMP)) {
            return;
        }
        World world = DimensionManager.getWorld(dim);
        {
            // 守卫 + getInventoryForBlock 只给这台扫描器库存列表里、范围内的容器
            TileEntity te = PacketGuard.machine((EntityPlayerMP) player, world, pos);
            if (te instanceof StorageScannerTileEntity) {
                StorageScannerTileEntity scannerTileEntity = (StorageScannerTileEntity) te;
                List<ItemStack> inv = scannerTileEntity.getInventoryForBlock(invpos);
                RFToolsMessages.sendToClient(player, ClientCommandHandler.CMD_RETURN_SCANNER_CONTENTS,
                        TypedMap.builder()
                                .put(ClientCommandHandler.PARAM_STACKS, inv)
                );
            }
        }
    }
}
