package mcjty.rftools.items.builder;

import mcjty.rftools.network.PacketGuard;
import io.netty.buffer.ByteBuf;
import mcjty.lib.network.NetworkTools;
import mcjty.lib.thirteen.Context;
import mcjty.rftools.blocks.builder.BuilderTileEntity;
import mcjty.rftools.blocks.shaper.ComposerTileEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.function.Supplier;

public class PacketUpdateNBTItemInventoryShape implements IMessage {

    public BlockPos pos;
    public int slotIndex;
    public NBTTagCompound tagCompound;

    public PacketUpdateNBTItemInventoryShape() {
    }

    public PacketUpdateNBTItemInventoryShape(ByteBuf buf) {
        fromBytes(buf);
    }

    public PacketUpdateNBTItemInventoryShape(BlockPos pos, int slotIndex, NBTTagCompound tagCompound) {
        this.pos = pos;
        this.slotIndex = slotIndex;
        this.tagCompound = tagCompound;
    }

    protected boolean isValidBlock(World world, BlockPos blockPos, TileEntity tileEntity) {
        return tileEntity instanceof ComposerTileEntity || tileEntity instanceof BuilderTileEntity;
    }

    /** 能放形状卡的槽位：建造者的卡片槽；合成器的输出槽与 9 个输入槽（不含幽灵槽）。 */
    protected static boolean isCardSlot(TileEntity te, int slot) {
        if (te instanceof BuilderTileEntity) {
            return slot == BuilderTileEntity.SLOT_TAB;
        }
        if (te instanceof ComposerTileEntity) {
            return slot == ComposerTileEntity.SLOT_OUT
                    || (slot >= ComposerTileEntity.SLOT_TABS && slot < ComposerTileEntity.SLOT_TABS + ComposerTileEntity.SLOT_COUNT);
        }
        return false;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = NetworkTools.readPos(buf);
        slotIndex = buf.readInt();
        tagCompound = NetworkTools.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        NetworkTools.writePos(buf, pos);
        buf.writeInt(slotIndex);
        NetworkTools.writeTag(buf, tagCompound);
    }

    public void handle(Supplier<Context> supplier) {
        Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            World world = ctx.getSender().getEntityWorld();
            // 守卫：区块已加载、开着这台建造者 / 合成器的界面或在附近、安全卡、领地
            TileEntity te = PacketGuard.machine(ctx.getSender(), world, pos);
            if (te instanceof IInventory) {
                if (!isValidBlock(world, pos, te) || !isCardSlot(te, slotIndex)) {
                    return;
                }
                IInventory inv = (IInventory) te;
                ItemStack stack = inv.getStackInSlot(slotIndex);
                if (!stack.isEmpty() && stack.getItem() instanceof ShapeCardItem) {
                    // 只把形状卡界面会改的键套到服务端现有的 NBT 上，其余（组合形状、扫描编号等）保持不变
                    stack.setTagCompound(ShapeCardNbtSanitizer.fromConfig().merge(stack.getTagCompound(), tagCompound));
                    te.markDirty();
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
