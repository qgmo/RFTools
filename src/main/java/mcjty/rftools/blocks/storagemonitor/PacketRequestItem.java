package mcjty.rftools.blocks.storagemonitor;

import mcjty.rftools.network.PacketGuard;
import mcjty.rftools.network.SafeNetworkTools;
import io.netty.buffer.ByteBuf;
import mcjty.lib.network.NetworkTools;
import mcjty.lib.thirteen.Context;
import mcjty.lib.varia.WorldTools;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.function.Supplier;

public class PacketRequestItem implements IMessage {

    private int dimensionId;
    private BlockPos pos;
    private BlockPos inventoryPos;
    private ItemStack item;
    private int amount;


    @Override
    public void fromBytes(ByteBuf buf) {
        dimensionId = buf.readInt();
        pos = NetworkTools.readPos(buf);
        inventoryPos = NetworkTools.readPos(buf);
        amount = buf.readInt();
        item = SafeNetworkTools.readItemStack(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dimensionId);
        NetworkTools.writePos(buf, pos);
        NetworkTools.writePos(buf, inventoryPos);
        buf.writeInt(amount);
        NetworkTools.writeItemStack(buf, item);
    }

    public PacketRequestItem() {
    }

    public PacketRequestItem(ByteBuf buf) {
        fromBytes(buf);
    }

    public PacketRequestItem(int dimensionId, BlockPos pos, BlockPos inventoryPos, ItemStack item, int amount) {
        this.dimensionId = dimensionId;
        this.pos = pos;
        this.inventoryPos = inventoryPos;
        this.item = item;
        this.amount = amount;
    }

    public void handle(Supplier<Context> supplier) {
        Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            WorldServer world = DimensionManager.getWorld(dimensionId);
            // 守卫：区块已加载、开着这台扫描器的界面（方块或平板）或在附近、安全卡、领地
            StorageScannerTileEntity tileEntity = PacketGuard.machine(ctx.getSender(), world, pos, StorageScannerTileEntity.class);
            if (tileEntity != null && inventoryPos != null && item != null && !item.isEmpty()) {
                tileEntity.requestStack(inventoryPos, item, amount, ctx.getSender());
            }
        });
        ctx.setPacketHandled(true);
    }
}
