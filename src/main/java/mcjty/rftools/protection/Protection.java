package mcjty.rftools.protection;

import mcjty.lib.varia.Logging;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * 领地保护检查（仅服务端）：回答“这名玩家能不能像右键那样使用这一格的方块”。
 * 封包操作机器时不经过方块交互事件，领地保护插件看不到，所以封包守卫在这里替它问一次。
 * <ul>
 *     <li>有 SpongeForge 时：发 Sponge 的主手右键方块事件（{@link SpongeProtection}），由 Sponge 的保护插件判断。</li>
 *     <li>没有 Sponge 时：发 Forge 的 {@link PlayerInteractEvent.RightClickBlock}，被取消或“使用方块”被拒绝都算不能。
 *     Forge 的这个事件取的是玩家所在的世界，所以方块不在玩家所在的世界时问不了，按“不能”回答。</li>
 * </ul>
 * 只问，不调用方块的 {@code onBlockActivated}。区块没加载时回答“不能”（不会因此加载区块）。出错时按“不能”处理。
 * Sponge 是可选依赖：没有 Sponge 时 {@link SpongeProtection} 不会被加载。
 */
public final class Protection {

    private static boolean warned = false;

    private Protection() {
    }

    public static boolean canUse(EntityPlayerMP player, World world, BlockPos pos) {
        if (!(world instanceof WorldServer) || !world.isBlockLoaded(pos)) {
            return false;
        }
        if (player instanceof FakePlayer) {
            return false;
        }
        try {
            if (SpongeHolder.LOADED) {
                return SpongeProtection.canInteract((WorldServer) world, pos, player);
            }
            if (player.world != world) {
                return false;
            }
            PlayerInteractEvent.RightClickBlock event = new PlayerInteractEvent.RightClickBlock(player, EnumHand.MAIN_HAND, pos,
                    EnumFacing.UP, new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
            return !MinecraftForge.EVENT_BUS.post(event) && event.getUseBlock() != Event.Result.DENY;
        } catch (RuntimeException | LinkageError e) {
            if (!warned) {
                warned = true;
                Logging.logError("RFTools: protection check failed, treating as denied (logged once)", e);
            }
            return false;
        }
    }

    private static final class SpongeHolder {
        static final boolean LOADED = Loader.isModLoaded("sponge");
    }
}
