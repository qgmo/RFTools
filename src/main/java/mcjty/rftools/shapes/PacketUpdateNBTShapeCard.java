package mcjty.rftools.shapes;

import mcjty.rftools.items.builder.ShapeCardItem;
import mcjty.rftools.items.builder.ShapeCardNbtSanitizer;
import mcjty.rftools.network.SafeNetworkTools;
import io.netty.buffer.ByteBuf;
import mcjty.lib.network.TypedMapTools;
import mcjty.lib.thirteen.Context;
import mcjty.lib.typed.Key;
import mcjty.lib.typed.Type;
import mcjty.lib.typed.TypedMap;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.function.Supplier;

/**
 * This is a packet that can be used to update the NBT on the held item of a player.
 */
public class PacketUpdateNBTShapeCard implements IMessage {
    private TypedMap args;

    @Override
    public void fromBytes(ByteBuf buf) {
        args = SafeNetworkTools.readArguments(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        TypedMapTools.writeArguments(buf, args);
    }

    public PacketUpdateNBTShapeCard() {
    }

    public PacketUpdateNBTShapeCard(ByteBuf buf) {
        fromBytes(buf);
    }

    public PacketUpdateNBTShapeCard(TypedMap arguments) {
        this.args = arguments;
    }

    public void handle(Supplier<Context> supplier) {
        Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            EntityPlayerMP playerEntity = ctx.getSender();
            ItemStack heldItem = playerEntity.getHeldItem(EnumHand.MAIN_HAND);
            // 只改手上的形状卡，只收形状卡界面会写的键（类型、范围都检查）
            if (heldItem.isEmpty() || !(heldItem.getItem() instanceof ShapeCardItem)) {
                return;
            }
            NBTTagCompound tagCompound = heldItem.getTagCompound();
            if (tagCompound == null) {
                tagCompound = new NBTTagCompound();
                heldItem.setTagCompound(tagCompound);
            }
            ShapeCardNbtSanitizer sanitizer = ShapeCardNbtSanitizer.fromConfig();
            for (Key<?> akey : args.getKeys()) {
                sanitizer.apply(tagCompound, akey.getName(), args.get(akey));
            }
        });
        ctx.setPacketHandled(true);
    }
}