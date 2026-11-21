package mcjty.rftools.craftinggrid;

import mcjty.rftools.network.PacketGuard;
import mcjty.rftools.network.PacketLimits;
import mcjty.rftools.network.SafeNetworkTools;
import net.minecraft.entity.player.EntityPlayerMP;
import io.netty.buffer.ByteBuf;
import mcjty.lib.network.NetworkTools;
import mcjty.rftools.blocks.storage.ModularStorageItemContainer;
import mcjty.rftools.blocks.storage.ModularStorageSetup;
import mcjty.rftools.blocks.storage.RemoteStorageItemContainer;
import mcjty.rftools.blocks.storagemonitor.StorageScannerContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class PacketGridSync {

    public static final int RECIPE_COUNT = 6;
    public static final int RECIPE_SIZE = 10;

    private BlockPos pos;
    private List<ItemStack[]> recipes;

    public void convertFromBytes(ByteBuf buf) {
        if (buf.readBoolean()) {
            pos = NetworkTools.readPos(buf);
        } else {
            pos = null;
        }
        // 网格固定 6 个配方、每个 10 格（1 个结果 + 9 格材料）；先封顶再分配
        int s = PacketLimits.checkDeclaredLength(buf.readInt(), buf.readableBytes(), RECIPE_COUNT, 4);
        recipes = new ArrayList<>(s);
        for (int i = 0 ; i < s ; i++) {
            int ss = PacketLimits.checkDeclaredLength(buf.readInt(), buf.readableBytes(), RECIPE_SIZE, 1);
            if (ss != RECIPE_SIZE) {
                throw new IllegalArgumentException("bad recipe size " + ss);
            }
            ItemStack[] stacks = new ItemStack[ss];
            for (int j = 0 ; j < ss ; j++) {
                if (buf.readBoolean()) {
                    stacks[j] = SafeNetworkTools.readItemStack(buf);
                } else {
                    stacks[j] = ItemStack.EMPTY;
                }
            }
            recipes.add(stacks);
        }
    }

    public void convertToBytes(ByteBuf buf) {
        if (pos != null) {
            buf.writeBoolean(true);
            NetworkTools.writePos(buf, pos);
        } else {
            buf.writeBoolean(false);
        }
        buf.writeInt(recipes.size());
        for (ItemStack[] recipe : recipes) {
            buf.writeInt(recipe.length);
            for (ItemStack stack : recipe) {
                if (!stack.isEmpty()) {
                    buf.writeBoolean(true);
                    NetworkTools.writeItemStack(buf, stack);
                } else {
                    buf.writeBoolean(false);
                }
            }
        }
    }

    protected void init(BlockPos pos, CraftingGrid grid) {
        this.pos = pos;
        recipes = new ArrayList<>();
        for (int i = 0 ; i < 6 ; i++) {
            CraftingRecipe recipe = grid.getRecipe(i);
            InventoryCrafting inventory = recipe.getInventory();
            ItemStack[] stacks = new ItemStack[10];
            stacks[0] = recipe.getResult();
            for (int j = 0 ; j < 9 ; j++) {
                stacks[j+1] = inventory.getStackInSlot(j);
            }
            recipes.add(stacks);
        }
    }

    protected CraftingGridProvider handleMessage(World world, EntityPlayer player) {
        CraftingGridProvider provider = null;
        if (pos == null) {
            // Handle tablet version
            ItemStack mainhand = player.getHeldItemMainhand();
            if (!mainhand.isEmpty() && mainhand.getItem() == ModularStorageSetup.storageModuleTabletItem) {
                if (player.openContainer instanceof ModularStorageItemContainer) {
                    ModularStorageItemContainer storageItemContainer = (ModularStorageItemContainer) player.openContainer;
                    provider = storageItemContainer.getCraftingGridProvider();
                } else if (player.openContainer instanceof RemoteStorageItemContainer) {
                    RemoteStorageItemContainer storageItemContainer = (RemoteStorageItemContainer) player.openContainer;
                    provider = storageItemContainer.getCraftingGridProvider();
                } else if (player.openContainer instanceof StorageScannerContainer) {
                    StorageScannerContainer storageItemContainer = (StorageScannerContainer) player.openContainer;
                    provider = storageItemContainer.getStorageScannerTileEntity();
                }
            }
        } else {
            // 方块上的网格：要过封包守卫（开着这台机器的界面等）
            TileEntity te = player instanceof EntityPlayerMP ? PacketGuard.machine((EntityPlayerMP) player, world, pos) : null;
            if (te instanceof CraftingGridProvider) {
                provider = ((CraftingGridProvider) te);
            }
        }
        if (provider != null) {
            for (int i = 0; i < recipes.size(); i++) {
                provider.setRecipe(i, recipes.get(i));
            }
        }
        return provider;
    }
}
