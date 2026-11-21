package mcjty.rftools.shapes;

import mcjty.rftools.network.PacketGuard;
import mcjty.rftools.blocks.shaper.ComposerTileEntity;
import mcjty.rftools.network.SafeNetworkTools;
import io.netty.buffer.ByteBuf;
import mcjty.lib.network.NetworkTools;
import mcjty.lib.thirteen.Context;
import mcjty.rftools.blocks.shaper.ComposerTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.function.Supplier;

public class PacketSendComposerData implements IMessage {
    private BlockPos pos;
    private ShapeModifier modifiers[];

    @Override
    public void fromBytes(ByteBuf buf) {
        int s = buf.readInt();
        // 合成器固定 9 个槽位；数量不对、名字不认识都不收（数组短了会在机器每刻更新时越界）
        if (s != ComposerTileEntity.SLOT_COUNT) {
            throw new IllegalArgumentException("bad modifier count " + s);
        }
        modifiers = new ShapeModifier[s];
        for (int i = 0 ; i < s ; i++) {
            String code = SafeNetworkTools.readString(buf);
            ShapeOperation op = ShapeOperation.getByName(code);
            boolean flip = buf.readBoolean();
            code = SafeNetworkTools.readString(buf);
            ShapeRotation rot = ShapeRotation.getByName(code);
            if (op == null || rot == null) {
                throw new IllegalArgumentException("bad modifier");
            }
            modifiers[i] = new ShapeModifier(op, flip, rot);
        }
        pos = NetworkTools.readPos(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(modifiers.length);
        for (ShapeModifier modifier : modifiers) {
            NetworkTools.writeString(buf, modifier.getOperation().getCode());
            buf.writeBoolean(modifier.isFlipY());
            NetworkTools.writeString(buf, modifier.getRotation().getCode());
        }
        NetworkTools.writePos(buf, pos);
    }

    public PacketSendComposerData() {
    }

    public PacketSendComposerData(ByteBuf buf) {
        fromBytes(buf);
    }

    public PacketSendComposerData(BlockPos pos, ShapeModifier[] modifiers) {
        this.pos = pos;
        this.modifiers = modifiers;
    }

    public void handle(Supplier<Context> supplier) {
        Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            TileEntity te = PacketGuard.machine(ctx.getSender(), ctx.getSender().getEntityWorld(), pos);
            if (te instanceof ComposerTileEntity) {
                ((ComposerTileEntity) te).setModifiers(modifiers);
            }
        });
        ctx.setPacketHandled(true);
    }
}
