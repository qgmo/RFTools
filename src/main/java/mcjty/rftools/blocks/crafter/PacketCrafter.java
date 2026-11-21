package mcjty.rftools.blocks.crafter;

import mcjty.rftools.network.PacketGuard;
import mcjty.rftools.network.PacketLimits;
import mcjty.rftools.network.SafeNetworkTools;
import io.netty.buffer.ByteBuf;
import mcjty.lib.network.NetworkTools;
import mcjty.lib.thirteen.Context;
import mcjty.lib.varia.Logging;
import mcjty.rftools.craftinggrid.CraftingRecipe;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.function.Supplier;

public class PacketCrafter implements IMessage {
    private BlockPos pos;

    private int recipeIndex;
    private ItemStack items[];
    private boolean keepOne;
    private CraftingRecipe.CraftMode craftInternal;


    @Override
    public void fromBytes(ByteBuf buf) {
        pos = NetworkTools.readPos(buf);
        keepOne = buf.readBoolean();
        craftInternal = SafeNetworkTools.readEnumByte(buf, CraftingRecipe.CraftMode.values());

        recipeIndex = buf.readByte();
        int l = buf.readByte();
        if (l == 0) {
            items = null;
        } else {
            // 合成器的配方固定是 9 格 + 1 个结果
            if (l != 10) {
                throw new IllegalArgumentException("bad recipe size " + l);
            }
            items = new ItemStack[l];
            for (int i = 0 ; i < l ; i++) {
                boolean b = buf.readBoolean();
                if (b) {
                    items[i] = SafeNetworkTools.readItemStack(buf);
                } else {
                    items[i] = ItemStack.EMPTY;
                }
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        NetworkTools.writePos(buf, pos);
        buf.writeBoolean(keepOne);
        buf.writeByte(craftInternal.ordinal());

        buf.writeByte(recipeIndex);
        if (items != null) {
            buf.writeByte(items.length);
            for (ItemStack item : items) {
                if (item.isEmpty()) {
                    buf.writeBoolean(false);
                } else {
                    buf.writeBoolean(true);
                    NetworkTools.writeItemStack(buf, item);
                }
            }
        } else {
            buf.writeByte(0);
        }
    }

    public PacketCrafter() {
    }

    public PacketCrafter(ByteBuf buf) {
        fromBytes(buf);
    }

    public PacketCrafter(BlockPos pos, int recipeIndex, InventoryCrafting inv, ItemStack result, boolean keepOne, CraftingRecipe.CraftMode craftInternal) {
        this.pos = pos;
        this.recipeIndex = recipeIndex;
        this.items = new ItemStack[10];
        if (inv != null) {
            for (int i = 0 ; i < 9 ; i++) {
                ItemStack slot = inv.getStackInSlot(i);
                if (!slot.isEmpty()) {
                    items[i] = slot.copy();
                } else {
                    items[i] = ItemStack.EMPTY;
                }
            }
        } else {
            for (int i = 0 ; i < 9 ; i++) {
                items[i] = ItemStack.EMPTY;
            }
        }
        items[9] = result.isEmpty() ? ItemStack.EMPTY : result.copy();
        this.keepOne = keepOne;
        this.craftInternal = craftInternal;
    }

    public void handle(Supplier<Context> supplier) {
        Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            CrafterBaseTE crafterBlockTileEntity = PacketGuard.machine(ctx.getSender(), ctx.getSender().getEntityWorld(), pos, CrafterBaseTE.class);
            if (crafterBlockTileEntity == null) {
                return;
            }
            crafterBlockTileEntity.noRecipesWork = false;
            if (recipeIndex != -1) {
                if (items == null || !PacketLimits.validIndex(recipeIndex, crafterBlockTileEntity.getSupportedRecipes())) {
                    return;
                }
                updateRecipe(crafterBlockTileEntity);
            }
        });
        ctx.setPacketHandled(true);
    }

    private void updateRecipe(CrafterBaseTE crafterBlockTileEntity) {
        CraftingRecipe recipe = crafterBlockTileEntity.getRecipe(recipeIndex);
        recipe.setRecipe(items, items[9]);
        recipe.setKeepOne(keepOne);
        recipe.setCraftMode(craftInternal);
        crafterBlockTileEntity.selectRecipe(recipeIndex);
        crafterBlockTileEntity.markDirtyClient();
    }
}
