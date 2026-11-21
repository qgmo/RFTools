package mcjty.rftools.shapes;

import mcjty.rftools.blocks.shaper.ScannerConfiguration;
import mcjty.rftools.items.builder.ShapeCardItem;
import mcjty.rftools.network.RFToolsMessages;
import mcjty.rftools.varia.RLE;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

import java.util.*;

/// Server side handling for shape data
///
/// 形状数据由客户端提供的卡片计算，所以按“请求的玩家”计预算：每名玩家每刻最多做 planeSurfacePerTick 的工作量
/// （一层的花费 = 截面积 × 组合形状的子形状数，空形状也扣），每名玩家同时最多排 MAX_SHAPES_PER_PLAYER 个形状，
/// 超过时丢掉他最早排的那个。
public class ShapeDataManagerServer {

    /** 每名玩家同时排队的形状数上限。 */
    public static final int MAX_SHAPES_PER_PLAYER = 4;

    private static class WorkUnit {
        private final List<EntityPlayerMP> players = new ArrayList<>();
        private ItemStack stack;
        private int offsetY;
        private IFormula formula;

        public WorkUnit(ItemStack stack, int offsetY, IFormula formula, EntityPlayerMP player) {
            this.stack = stack;
            this.offsetY = offsetY;
            this.formula = formula;
            this.players.add(player);
        }

        public void update(ItemStack stack, int offsetY, IFormula formula, EntityPlayerMP player) {
            this.stack = stack;
            this.offsetY = offsetY;
            this.formula = formula;
            if (!players.contains(player)) {
                players.add(player);
            }
        }

        public List<EntityPlayerMP> getPlayers() {
            return players;
        }

        public ItemStack getStack() {
            return stack;
        }

        public int getOffsetY() {
            return offsetY;
        }

        public IFormula getFormula() {
            return formula;
        }
    }

    private static class WorkQueue {
        private final ArrayDeque<WorkUnit> workQueue = new ArrayDeque<>();
        private final Map<Integer, WorkUnit> workingOn = new HashMap<>();
        /** 最早请求这个形状的玩家：按他的预算做。 */
        private final UUID owner;

        private WorkQueue(UUID owner) {
            this.owner = owner;
        }
    }

    // Server-side
    private static final Map<ShapeID, WorkQueue> workQueues = new LinkedHashMap<>();
    private static final PlayerWorkBudget<UUID> budget = new PlayerWorkBudget<>(0);

    public static void clear() {
        workQueues.clear();
    }

    public static void pushWork(ShapeID shapeID, ItemStack stack, int offsetY, IFormula formula, EntityPlayerMP player) {
        WorkQueue queue = workQueues.get(shapeID);
        if (queue == null) {
            evictOldestIfFull(player.getUniqueID());
            queue = new WorkQueue(player.getUniqueID());
            workQueues.put(shapeID, queue);
        }
        if (queue.workingOn.containsKey(offsetY)) {
            queue.workingOn.get(offsetY).update(stack, offsetY, formula, player);
        } else {
            WorkUnit unit = new WorkUnit(stack, offsetY, formula, player);
            queue.workQueue.addLast(unit);
            queue.workingOn.put(offsetY, unit);
        }
    }

    private static void evictOldestIfFull(UUID owner) {
        ShapeID oldest = null;
        int count = 0;
        for (Map.Entry<ShapeID, WorkQueue> entry : workQueues.entrySet()) {
            if (entry.getValue().owner.equals(owner)) {
                if (oldest == null) {
                    oldest = entry.getKey();
                }
                count++;
            }
        }
        if (count >= MAX_SHAPES_PER_PLAYER && oldest != null) {
            workQueues.remove(oldest);
        }
    }

    /** 一层的花费：截面积 × 子形状数（至少 1）。 */
    public static long costOfLayer(BlockPos clampedDimension, int formulaParts) {
        return (long) Math.max(1, clampedDimension.getX()) * Math.max(1, clampedDimension.getZ()) * Math.max(1, formulaParts);
    }

    /** 卡片里组合形状的子形状总数（递归，最多数到 limit）。 */
    public static int countFormulaParts(NBTTagCompound tag, int limit) {
        if (tag == null) {
            return 1;
        }
        int[] count = {1};
        countChildren(tag, count, limit, 0);
        return count[0];
    }

    private static void countChildren(NBTTagCompound tag, int[] count, int limit, int depth) {
        if (depth > 8 || count[0] >= limit) {
            return;
        }
        NBTTagList children = tag.getTagList("children", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < children.tagCount() && count[0] < limit; i++) {
            count[0]++;
            countChildren(children.getCompoundTagAt(i), count, limit, depth + 1);
        }
    }

    public static void handleWork() {
        budget.reset(ScannerConfiguration.planeSurfacePerTick.get());
        Set<ShapeID> toRemove = new HashSet<>();
        for (Map.Entry<ShapeID, WorkQueue> entry : workQueues.entrySet()) {
            ShapeID shapeID = entry.getKey();
            WorkQueue queue = entry.getValue();

            while (!queue.workQueue.isEmpty() && budget.hasBudget(queue.owner)) {
                WorkUnit unit = queue.workQueue.removeFirst();
                queue.workingOn.remove(unit.getOffsetY());

                ItemStack card = unit.getStack();
                boolean solid = ShapeCardItem.isSolid(card);
                BlockPos dimension = ShapeCardItem.getDimension(card);
                BlockPos clamped = new BlockPos(Math.min(dimension.getX(), 512), Math.min(dimension.getY(), 256), Math.min(dimension.getZ(), 512));

                RLE positions = new RLE();
                StatePalette statePalette = new StatePalette();
                int cnt = ShapeCardItem.getRenderPositions(card, solid, positions, statePalette, unit.getFormula(), unit.getOffsetY());
                // 不论是否为空都扣
                budget.charge(queue.owner, costOfLayer(clamped, countFormulaParts(card.getTagCompound(), 1024)));

                for (EntityPlayerMP player : unit.getPlayers()) {
                    if (!player.hasDisconnected()) {
                        RFToolsMessages.INSTANCE.sendTo(new PacketReturnShapeData(shapeID, positions, statePalette, dimension, cnt, unit.getOffsetY(), ""), player);
                    }
                }
            }
            if (queue.workQueue.isEmpty()) {
                toRemove.add(shapeID);
            }
        }
        for (ShapeID id : toRemove) {
            workQueues.remove(id);
        }

    }

}
