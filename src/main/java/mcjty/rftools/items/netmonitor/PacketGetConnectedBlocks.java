package mcjty.rftools.items.netmonitor;

import mcjty.rftools.network.PacketGuard;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumFacing;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import io.netty.buffer.ByteBuf;
import mcjty.lib.network.NetworkTools;
import mcjty.lib.thirteen.Context;
import mcjty.lib.varia.EnergyTools;
import mcjty.rftools.varia.BlockInfo;
import mcjty.rftools.network.RFToolsMessages;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.*;
import java.util.function.Supplier;

public class PacketGetConnectedBlocks implements IMessage {
    private BlockPos pos;

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = NetworkTools.readPos(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        NetworkTools.writePos(buf, pos);
    }

    public PacketGetConnectedBlocks() {
    }

    public PacketGetConnectedBlocks(ByteBuf buf) {
        fromBytes(buf);
    }

    public PacketGetConnectedBlocks(BlockPos pos) {
        this.pos = pos;
    }

    public void handle(Supplier<Context> supplier) {
        Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            EntityPlayerMP player = ctx.getSender();
            // 要拿着网络监视器，并且能碰这个方块（守卫：区块已加载、在附近、安全卡、领地）
            if (!(player.getHeldItemMainhand().getItem() instanceof NetworkMonitorItem) && !(player.getHeldItemOffhand().getItem() instanceof NetworkMonitorItem)) {
                return;
            }
            if (PacketGuard.machine(player, player.getEntityWorld(), pos) == null) {
                return;
            }
            Map<BlockPos,BlockInfo> connectedBlocks = new HashMap<>();
            findConnectedBlocks(connectedBlocks, player.getEntityWorld(), pos, NetworkMonitorConfiguration.maximumBlocks.get() * 4);

            if (connectedBlocks.size() > NetworkMonitorConfiguration.maximumBlocks.get()) {
                connectedBlocks = compactConnectedBlocks(connectedBlocks, pos, NetworkMonitorConfiguration.maximumBlocks.get());
            }

            int minx = 300000000;
            int miny = 300000000;
            int minz = 300000000;
            for (BlockPos coordinate : connectedBlocks.keySet()) {
                minx = Math.min(minx, coordinate.getX());
                miny = Math.min(miny, coordinate.getY());
                minz = Math.min(minz, coordinate.getZ());
            }
            RFToolsMessages.INSTANCE.sendTo(new PacketConnectedBlocksReady(connectedBlocks, minx, miny, minz), ctx.getSender());
        });
        ctx.setPacketHandled(true);
    }

    private Map<BlockPos,BlockInfo> compactConnectedBlocks(Map<BlockPos,BlockInfo> old, final BlockPos pos, int max) {
        List<BlockPos> list = new ArrayList<>(old.keySet());
        Collections.sort(list, Comparator.comparingInt(o1 -> (o1.getX()-pos.getX()) * (o1.getX()-pos.getX()) + (o1.getY()-pos.getY()) * (o1.getY()-pos.getY()) + (o1.getZ()-pos.getZ()) * (o1.getZ()-pos.getZ())));

        Map<BlockPos,BlockInfo> connectedBlocks = new HashMap<>();
        for (BlockPos coordinate : list) {
            connectedBlocks.put(coordinate, old.get(coordinate));
            max--;
            if (max <= 0) {
                break;
            }
        }

        return connectedBlocks;
    }


    /** 广度优先，只走已加载的区块，最多找 {@code limit} 个（原来是递归深搜，大网络会栈溢出，而且沿途加载区块）。 */
    private void findConnectedBlocks(Map<BlockPos,BlockInfo> connectedBlocks, World world, BlockPos start, int limit) {
        ArrayDeque<BlockPos> todo = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        todo.add(start);
        seen.add(start);
        while (!todo.isEmpty() && connectedBlocks.size() < limit) {
            BlockPos pos = todo.removeFirst();
            if (pos.getY() < 0 || pos.getY() >= world.getHeight() || !world.isBlockLoaded(pos)) {
                continue;
            }
            TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity != null && EnergyTools.isEnergyTE(tileEntity, null)) {
                connectedBlocks.put(pos, new BlockInfo(tileEntity, null, pos));
                for (EnumFacing facing : EnumFacing.VALUES) {
                    BlockPos p = pos.offset(facing);
                    if (seen.add(p)) {
                        todo.addLast(p);
                    }
                }
            }
        }
    }
}
