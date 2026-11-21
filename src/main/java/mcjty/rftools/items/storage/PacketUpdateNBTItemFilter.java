package mcjty.rftools.items.storage;

import mcjty.rftools.network.ItemNbtWhitelist;
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

public class PacketUpdateNBTItemFilter implements IMessage {

    public TypedMap args;

    public PacketUpdateNBTItemFilter() {
    }

    public PacketUpdateNBTItemFilter(ByteBuf buf) {
        fromBytes(buf);
    }

    public PacketUpdateNBTItemFilter(TypedMap arguments) {
        this.args = arguments;
    }

    private static final ItemNbtWhitelist WHITELIST = ItemNbtWhitelist.builder()
            .string("blacklistMode", 32).bool("oredictMode").bool("damageMode").bool("modMode").bool("nbtMode")
            .build();

    protected boolean isValidItem(ItemStack itemStack) {
        return itemStack.getItem() instanceof StorageFilterItem;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        args = SafeNetworkTools.readArguments(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        TypedMapTools.writeArguments(buf, args);
    }

    public void handle(Supplier<Context> supplier) {
        Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            EntityPlayerMP playerEntity = ctx.getSender();
            ItemStack heldItem = playerEntity.getHeldItem(EnumHand.MAIN_HAND);
            if (heldItem.isEmpty()) {
                return;
            }
            // To avoid people messing with packets
            if (!isValidItem(heldItem)) {
                return;
            }
            NBTTagCompound tagCompound = heldItem.getTagCompound();
            if (tagCompound == null) {
                tagCompound = new NBTTagCompound();
                heldItem.setTagCompound(tagCompound);
            }
            // 只收界面会写的键（类型、长度都检查）；以前任意键都能写（远程存储编号、扫描器坐标、能量等）
            for (Key<?> akey : args.getKeys()) {
                WHITELIST.apply(tagCompound, akey.getName(), akey.getType(), args.get(akey));
            }
        });
        ctx.setPacketHandled(true);
    }
}
