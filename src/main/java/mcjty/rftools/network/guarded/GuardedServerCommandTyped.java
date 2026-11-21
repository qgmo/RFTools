package mcjty.rftools.network.guarded;

import io.netty.buffer.ByteBuf;
import mcjty.lib.network.ICommandHandler;
import mcjty.lib.thirteen.Context;
import mcjty.lib.typed.TypedMap;
import mcjty.rftools.network.PacketGuard;
import mcjty.rftools.network.PacketLimits;
import mcjty.rftools.network.SafeNetworkTools;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.function.Supplier;

/**
 * 服务端收 McJtyLib 的 {@code PacketServerCommandTyped}（判别号 0，界面按钮与值绑定都走它）时用的类：字节格式相同，
 * 解码有长度上限，处理前先过 {@link PacketGuard}。客户端发的仍是 McJtyLib 的类。
 */
public class GuardedServerCommandTyped implements IMessage {

    private BlockPos pos;
    private Integer dimensionId;
    private String command;
    private TypedMap params;

    public GuardedServerCommandTyped() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = SafeNetworkTools.readPos(buf);
        command = SafeNetworkTools.readString(buf);
        params = SafeNetworkTools.readArguments(buf);
        if (buf.readBoolean()) {
            dimensionId = buf.readInt();
        } else {
            dimensionId = null;
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        throw new UnsupportedOperationException("server side only");
    }

    public void handle(Supplier<Context> supplier) {
        Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            EntityPlayerMP player = ctx.getSender();
            if (player == null || command == null || !PacketLimits.stringsWithin(params, PacketLimits.MAX_PARAM_STRING_LENGTH)) {
                return;
            }
            World world = dimensionId == null ? player.getEntityWorld() : DimensionManager.getWorld(dimensionId);
            TileEntity te = PacketGuard.machine(player, world, pos);
            if (!(te instanceof ICommandHandler)) {
                return;
            }
            ICommandHandler handler = (ICommandHandler) te;
            PacketGuard.runAs(player, () -> {
                handler.execute(player, command, params);
            });
        });
        ctx.setPacketHandled(true);
    }
}
