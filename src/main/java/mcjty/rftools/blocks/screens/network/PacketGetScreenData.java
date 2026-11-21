package mcjty.rftools.blocks.screens.network;

import mcjty.rftools.network.PacketGuard;
import mcjty.rftools.network.RFToolsMessages;
import mcjty.rftools.network.SafeNetworkTools;
import io.netty.buffer.ByteBuf;
import mcjty.lib.network.NetworkTools;
import mcjty.lib.network.PacketHandler;
import mcjty.lib.thirteen.Context;
import mcjty.lib.varia.GlobalCoordinate;
import mcjty.lib.varia.Logging;
import mcjty.rftools.api.screens.data.IModuleData;
import mcjty.rftools.blocks.screens.ScreenTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

import java.util.Map;
import java.util.function.Supplier;

public class PacketGetScreenData implements IMessage {
    private String modid;
    private GlobalCoordinate pos;
    private long millis;

    @Override
    public void fromBytes(ByteBuf buf) {
        modid = SafeNetworkTools.readString(buf);
        pos = new GlobalCoordinate(NetworkTools.readPos(buf), buf.readInt());
        millis = buf.readLong();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        NetworkTools.writeString(buf, modid);
        NetworkTools.writePos(buf, pos.getCoordinate());
        buf.writeInt(pos.getDimension());
        buf.writeLong(millis);
    }

    public PacketGetScreenData() {
    }

    public PacketGetScreenData(ByteBuf buf) {
        fromBytes(buf);
    }

    public PacketGetScreenData(String modid, GlobalCoordinate pos, long millis) {
        this.modid = modid;
        this.pos = pos;
        this.millis = millis;
    }

    public void handle(Supplier<Context> supplier) {
        Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            World world = ctx.getSender().getEntityWorld();
            if (pos.getDimension() != world.provider.getDimension()) {
                return;
            }
            // 屏幕内容是谁都看得见的显示数据：只检查区块已加载、同一世界、在渲染距离（64 格）附近
            TileEntity te = PacketGuard.viewable(ctx.getSender(), world, pos.getCoordinate(), 80);
            if(!(te instanceof ScreenTileEntity)) {
                return;
            }
            Map<Integer, IModuleData> screenData = ((ScreenTileEntity) te).getScreenData(millis);

            // 回包一律走 RFTools 的通道（modid 由客户端提供，原来查不到时会空指针）
            PacketReturnScreenData msg = new PacketReturnScreenData(pos, screenData);
            RFToolsMessages.INSTANCE.sendTo(msg, ctx.getSender());
        });
        ctx.setPacketHandled(true);
    }

}