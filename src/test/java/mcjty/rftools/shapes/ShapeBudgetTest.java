package mcjty.rftools.shapes;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShapeBudgetTest {

    @Test
    void budgetIsPerPlayerAndResetsEachTick() {
        PlayerWorkBudget<String> budget = new PlayerWorkBudget<>(1000);
        assertTrue(budget.hasBudget("alice"));
        budget.charge("alice", 600);
        assertTrue(budget.hasBudget("alice"));
        budget.charge("alice", 600);
        assertFalse(budget.hasBudget("alice"));
        // 别的玩家不受影响
        assertTrue(budget.hasBudget("bob"));
        budget.reset(1000);
        assertTrue(budget.hasBudget("alice"));
    }

    @Test
    void emptyWorkStillCosts() {
        PlayerWorkBudget<String> budget = new PlayerWorkBudget<>(3);
        // 空形状的一层也至少扣 1，不会无限做下去
        int done = 0;
        while (budget.hasBudget("alice") && done < 1000) {
            budget.charge("alice", 0);
            done++;
        }
        assertEquals(3, done);
    }

    @Test
    void layerCostCountsAreaAndFormulaParts() {
        assertEquals(512L * 512L, ShapeDataManagerServer.costOfLayer(new BlockPos(512, 256, 512), 1));
        assertEquals(512L * 512L * 300, ShapeDataManagerServer.costOfLayer(new BlockPos(512, 256, 512), 300));
        assertEquals(1, ShapeDataManagerServer.costOfLayer(new BlockPos(0, 0, 0), 0));
    }

    @Test
    void countsNestedChildrenWithLimit() {
        NBTTagCompound root = new NBTTagCompound();
        NBTTagList children = new NBTTagList();
        for (int i = 0; i < 3; i++) {
            NBTTagCompound child = new NBTTagCompound();
            NBTTagList grand = new NBTTagList();
            grand.appendTag(new NBTTagCompound());
            grand.appendTag(new NBTTagCompound());
            child.setTag("children", grand);
            children.appendTag(child);
        }
        root.setTag("children", children);
        // 1（自己）+ 3 个子形状 + 6 个孙形状
        assertEquals(10, ShapeDataManagerServer.countFormulaParts(root, 1024));
        assertEquals(5, ShapeDataManagerServer.countFormulaParts(root, 5));
        assertEquals(1, ShapeDataManagerServer.countFormulaParts(new NBTTagCompound(), 1024));
        assertEquals(1, ShapeDataManagerServer.countFormulaParts(null, 1024));
    }
}
