package mcjty.rftools.items.builder;

import mcjty.rftools.network.PacketLimits;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 形状卡界面（GuiShapeCard）改卡片 NBT 时，服务端只接受界面实际会写的那些键，并检查类型与范围：
 * <ul>
 *     <li>布尔：{@code solid}、{@code oredict} 与 7 个 {@code void*}（采石卡的“销毁这些方块”）；</li>
 *     <li>整数：{@code dimX/dimY/dimZ}（截到 [0, maxDimension]，Y 截到 [0, 256]）、{@code offsetX/Y/Z}（截到 ±maxOffset）；</li>
 *     <li>字符串：{@code shapenew}，必须是认识的形状名。</li>
 * </ul>
 * 其余键（包括组合形状的 {@code children}、扫描编号等由服务端维护的数据）一律不收。
 */
public final class ShapeCardNbtSanitizer {

    public static final Set<String> BOOLEAN_KEYS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "solid", "oredict", "voidstone", "voidcobble", "voiddirt", "voidgravel", "voidsand", "voidnetherrack", "voidendstone")));
    public static final Set<String> DIMENSION_KEYS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("dimX", "dimY", "dimZ")));
    public static final Set<String> OFFSET_KEYS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("offsetX", "offsetY", "offsetZ")));
    public static final String SHAPE_KEY = "shapenew";
    public static final int MAX_DIM_Y = 256;

    private final Predicate<String> validShape;
    private final int maxDimension;
    private final int maxOffset;

    public ShapeCardNbtSanitizer(Predicate<String> validShape, int maxDimension, int maxOffset) {
        this.validShape = validShape;
        this.maxDimension = Math.max(0, maxDimension);
        this.maxOffset = Math.max(0, maxOffset);
    }

    /** 用当前配置（建造者与扫描器两者中较大的上限，与界面一致）。 */
    public static ShapeCardNbtSanitizer fromConfig() {
        int maxDim = Math.max(mcjty.rftools.blocks.shaper.ScannerConfiguration.maxScannerDimension.get(),
                mcjty.rftools.blocks.builder.BuilderConfiguration.maxBuilderDimension.get());
        int maxOff = Math.max(mcjty.rftools.blocks.shaper.ScannerConfiguration.maxScannerOffset.get(),
                mcjty.rftools.blocks.builder.BuilderConfiguration.maxBuilderOffset.get());
        return new ShapeCardNbtSanitizer(s -> mcjty.rftools.shapes.Shape.getShape(s) != null, maxDim, maxOff);
    }

    /**
     * 把一个客户端给的值写进 {@code target}。值的类型不对、键不在白名单里、形状名不认识时不写，返回 false。
     */
    public boolean apply(NBTTagCompound target, String key, Object value) {
        if (key == null || value == null) {
            return false;
        }
        if (BOOLEAN_KEYS.contains(key)) {
            if (!(value instanceof Boolean)) {
                return false;
            }
            target.setBoolean(key, (Boolean) value);
            return true;
        }
        if (DIMENSION_KEYS.contains(key)) {
            if (!(value instanceof Integer)) {
                return false;
            }
            int max = "dimY".equals(key) ? Math.min(MAX_DIM_Y, maxDimension) : maxDimension;
            target.setInteger(key, PacketLimits.clamp((Integer) value, 0, max));
            return true;
        }
        if (OFFSET_KEYS.contains(key)) {
            if (!(value instanceof Integer)) {
                return false;
            }
            target.setInteger(key, PacketLimits.clamp((Integer) value, -maxOffset, maxOffset));
            return true;
        }
        if (SHAPE_KEY.equals(key)) {
            if (!(value instanceof String) || !validShape.test((String) value)) {
                return false;
            }
            target.setString(key, (String) value);
            return true;
        }
        return false;
    }

    /**
     * 整个 NBT 由客户端提供的情况（从机器槽位里打开的形状卡界面）：从服务端现有的 {@code current} 复制一份，
     * 只把客户端 NBT 里白名单内、类型正确的键套上去。
     */
    public NBTTagCompound merge(NBTTagCompound current, NBTTagCompound fromClient) {
        NBTTagCompound result = current == null ? new NBTTagCompound() : current.copy();
        if (fromClient == null) {
            return result;
        }
        for (String key : BOOLEAN_KEYS) {
            if (fromClient.hasKey(key, Constants.NBT.TAG_BYTE)) {
                apply(result, key, fromClient.getBoolean(key));
            }
        }
        for (String key : DIMENSION_KEYS) {
            if (fromClient.hasKey(key, Constants.NBT.TAG_INT)) {
                apply(result, key, fromClient.getInteger(key));
            }
        }
        for (String key : OFFSET_KEYS) {
            if (fromClient.hasKey(key, Constants.NBT.TAG_INT)) {
                apply(result, key, fromClient.getInteger(key));
            }
        }
        if (fromClient.hasKey(SHAPE_KEY, Constants.NBT.TAG_STRING)) {
            apply(result, SHAPE_KEY, fromClient.getString(SHAPE_KEY));
        }
        return result;
    }
}
