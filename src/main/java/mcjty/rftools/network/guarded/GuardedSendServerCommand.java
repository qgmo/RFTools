package mcjty.rftools.network.guarded;

import io.netty.buffer.ByteBuf;
import mcjty.lib.McJtyLib;
import mcjty.lib.thirteen.Context;
import mcjty.lib.typed.TypedMap;
import mcjty.rftools.RFTools;
import mcjty.rftools.network.PacketGuard;
import mcjty.rftools.network.PacketLimits;
import mcjty.rftools.network.SafeNetworkTools;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.function.Supplier;

/**
 * 服务端收 McJtyLib 的 {@code PacketSendServerCommand}（判别号 1，RFTools 的服务端命令，见 setup.CommandHandler）时用的类：
 * 字节格式相同，解码有上限；只接受 RFTools 自己的命令（这条通道是 RFTools 的）。各命令自己检查参数与权限。
 */
public class GuardedSendServerCommand implements IMessage {

    private String modid;
    private String command;
    private TypedMap arguments;

    public GuardedSendServerCommand() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        modid = SafeNetworkTools.readString(buf);
        command = SafeNetworkTools.readString(buf);
        arguments = SafeNetworkTools.readArguments(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        throw new UnsupportedOperationException("server side only");
    }

    public void handle(Supplier<Context> supplier) {
        Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            EntityPlayerMP player = ctx.getSender();
            if (player == null || command == null || !PacketLimits.stringsWithin(arguments, PacketLimits.MAX_PARAM_STRING_LENGTH) || !RFTools.MODID.equals(modid)) {
                return;
            }
            PacketGuard.runAs(player, () -> {
                McJtyLib.handleCommand(RFTools.MODID, command, player, arguments);
            });
        });
        ctx.setPacketHandled(true);
    }
}
