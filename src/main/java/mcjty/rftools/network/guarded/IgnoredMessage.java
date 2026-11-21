package mcjty.rftools.network.guarded;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * 占住 McJtyLib 的两个调试包（3：PacketDumpItemInfo、4：PacketDumpBlockInfo）的判别号：服务端不再注册这两个包，
 * 老客户端发来的内容直接丢弃，不处理。两个判别号各用一个子类（Forge 的编解码器按类区分判别号）。
 */
public abstract class IgnoredMessage implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {
        buf.skipBytes(buf.readableBytes());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        throw new UnsupportedOperationException("server side only");
    }

    public static class DumpItem extends IgnoredMessage {
    }

    public static class DumpBlock extends IgnoredMessage {
    }
}
