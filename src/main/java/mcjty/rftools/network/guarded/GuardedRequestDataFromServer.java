package mcjty.rftools.network.guarded;

import io.netty.buffer.ByteBuf;
import mcjty.lib.network.ICommandHandler;
import mcjty.lib.network.PacketDataFromServer;
import mcjty.lib.thirteen.Context;
import mcjty.lib.typed.TypedMap;
import mcjty.rftools.network.PacketGuard;
import mcjty.rftools.network.PacketLimits;
import mcjty.rftools.network.RFToolsMessages;
import mcjty.rftools.network.SafeNetworkTools;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.function.Supplier;

/**
 * 服务端收 McJtyLib 的 {@code PacketRequestDataFromServer}（判别号 2）时用的类：字节格式相同，解码有上限，
 * 处理前先过 {@link PacketGuard}，并以发包玩家的身份执行（{@link PacketGuard#currentPlayer()}），回包走 RFTools 的通道。
 */
public class GuardedRequestDataFromServer implements IMessage {

    private BlockPos pos;
    private String command;
    private TypedMap params;

    public GuardedRequestDataFromServer() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = SafeNetworkTools.readPos(buf);
        command = SafeNetworkTools.readString(buf);
        params = SafeNetworkTools.readArguments(buf);
        // modid：回包一律走 RFTools 的通道，读掉即可
        SafeNetworkTools.readString(buf);
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
            TileEntity te = PacketGuard.machine(player, player.getEntityWorld(), pos);
            if (!(te instanceof ICommandHandler)) {
                return;
            }
            ICommandHandler handler = (ICommandHandler) te;
            TypedMap result = PacketGuard.runAs(player, () -> handler.executeWithResult(command, params));
            if (result == null) {
                return;
            }
            RFToolsMessages.INSTANCE.sendTo(new PacketDataFromServer(pos, command, result), player);
        });
        ctx.setPacketHandled(true);
    }
}
