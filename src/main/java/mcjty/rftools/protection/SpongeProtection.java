package mcjty.rftools.protection;

import com.flowpowered.math.vector.Vector3d;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

/**
 * 有 SpongeForge 时的领地检查（只由 {@link Protection} 调用；没装 Sponge 时这个类不会被加载）。
 * 照玩家右键方块时 SpongeForge 发的 {@link InteractBlockEvent.Secondary.MainHand} 构造：原因里第一个是玩家，用主手，
 * 点在方块顶面中心。被取消、或“使用方块”被定为 FALSE，就是“不能”。
 */
final class SpongeProtection {

    private SpongeProtection() {
    }

    static boolean canInteract(WorldServer world, BlockPos pos, EntityPlayerMP entity) {
        Player player = (Player) (Object) entity;
        World spongeWorld = (World) (Object) world;
        Location<World> location = new Location<>(spongeWorld, pos.getX(), pos.getY(), pos.getZ());
        BlockSnapshot target = location.createSnapshot();
        try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(player);
            frame.addContext(EventContextKeys.OWNER, player);
            frame.addContext(EventContextKeys.NOTIFIER, player);
            frame.addContext(EventContextKeys.USED_HAND, HandTypes.MAIN_HAND);
            InteractBlockEvent.Secondary.MainHand event = SpongeEventFactory.createInteractBlockEventSecondaryMainHand(
                    frame.getCurrentCause(), Tristate.UNDEFINED, Tristate.UNDEFINED, Tristate.UNDEFINED, Tristate.UNDEFINED,
                    HandTypes.MAIN_HAND, Optional.of(new Vector3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5)),
                    target, Direction.UP);
            if (Sponge.getEventManager().post(event)) {
                return false;
            }
            return event.getUseBlockResult() != Tristate.FALSE;
        }
    }
}
