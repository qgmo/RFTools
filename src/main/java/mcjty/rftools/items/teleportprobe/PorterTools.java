package mcjty.rftools.items.teleportprobe;

import mcjty.rftools.network.PacketLimits;
import mcjty.rftools.varia.Msg;
import net.minecraftforge.common.DimensionManager;
import mcjty.lib.typed.TypedMap;
import mcjty.lib.varia.GlobalCoordinate;
import mcjty.rftools.ClientCommandHandler;
import mcjty.rftools.blocks.teleporter.TeleportDestination;
import mcjty.rftools.blocks.teleporter.TeleportDestinations;
import mcjty.rftools.network.RFToolsMessages;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.util.text.TextComponentTranslation;

public class PorterTools {

    public static void clearTarget(EntityPlayer player, int index) {
        ItemStack heldItem = player.getHeldItem(EnumHand.MAIN_HAND);
        if (heldItem.isEmpty() || !(heldItem.getItem() instanceof ChargedPorterItem) || !PacketLimits.validIndex(index, AdvancedChargedPorterItem.MAXTARGETS)) {
            return;
        }
        NBTTagCompound tagCompound = heldItem.getTagCompound();
        if (tagCompound == null) {
            return;
        }
        if (tagCompound.hasKey("target"+ index)) {
            int id = tagCompound.getInteger("target"+ index);
            if (tagCompound.hasKey("target") && tagCompound.getInteger("target") == id) {
                tagCompound.removeTag("target");
            }
            tagCompound.removeTag("target"+ index);
        }
    }

    public static void forceTeleport(EntityPlayer player, int dimension, BlockPos pos) {
        boolean probeInMainHand = !player.getHeldItemMainhand().isEmpty() && player.getHeldItemMainhand().getItem() instanceof TeleportProbeItem;
        boolean probeInOffHand = !player.getHeldItemOffhand().isEmpty() && player.getHeldItemOffhand().getItem() instanceof TeleportProbeItem;
        if ((!probeInMainHand) && (!probeInOffHand)) {
            return;
        }
        // 传送探针是创造物品，这里再要求是管理员，以防它流出；目标维度必须已加载
        if (!player.canUseCommand(2, "") || pos == null || DimensionManager.getWorld(dimension) == null) {
            return;
        }

        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        int currentId = player.getEntityWorld().provider.getDimension();
        if (currentId != dimension) {
            mcjty.lib.varia.TeleportationTools.teleportToDimension(player, dimension, x + .5, y + 1, z + .5);
        } else {
            player.setPositionAndUpdate(x+.5, y + 1.5, z+.5);
        }
    }

    public static void cycleDestination(EntityPlayer player, boolean next) {
        ItemStack stack = player.getHeldItemMainhand();
        if (!stack.isEmpty() && stack.getItem() instanceof AdvancedChargedPorterItem) {
            NBTTagCompound tagCompound = stack.getTagCompound();
            if (tagCompound == null) {
                return;
            }
            TeleportDestinations destinations = TeleportDestinations.getDestinations(player.getEntityWorld());

            int curtarget = tagCompound.getInteger("target");

            int donext = 0;
            // To wrap around we cycle through the list twice
            for (int i = 0; i < AdvancedChargedPorterItem.MAXTARGETS * 2; i++) {
                int tgt;
                if (next) {
                    tgt = i % AdvancedChargedPorterItem.MAXTARGETS;
                } else {
                    tgt = (AdvancedChargedPorterItem.MAXTARGETS * 2 - i) % AdvancedChargedPorterItem.MAXTARGETS;
                }
                donext = checkTarget(player, tagCompound, destinations, curtarget, donext, tgt);
                if (donext == 2) {
                    break;
                }
            }
        }
    }

    private static int checkTarget(EntityPlayer playerEntity, NBTTagCompound tagCompound, TeleportDestinations destinations, int curtarget, int donext, int tgt) {
        if (tagCompound.hasKey("target" + tgt)) {
            int target = tagCompound.getInteger("target" + tgt);
            GlobalCoordinate gc = destinations.getCoordinateForId(target);
            if (gc != null) {
                TeleportDestination destination = destinations.getDestination(gc);
                if (destination != null) {
                    if (donext == 1) {
                        String name = destination.getName() + " (dimension " + destination.getDimension() + ")";
                        tagCompound.setInteger("target", target);
                        ITextComponent component = new TextComponentTranslation("msg.rftools.target_s", name);
                        if (playerEntity != null) {
                            playerEntity.sendStatusMessage(component, false);
                        }
                        donext = 2;
                    } else if (target == curtarget) {
                        donext = 1;
                    }
                }
            }
        }
        return donext;
    }

    public static void returnDestinationInfo(EntityPlayer player, int receiverId) {
        World world = player.getEntityWorld();
        TeleportDestinations destinations = TeleportDestinations.getDestinations(world);
        String name = TeleportDestinations.getDestinationName(destinations, receiverId);
        RFToolsMessages.sendToClient(player, ClientCommandHandler.CMD_RETURN_DESTINATION_INFO,
                TypedMap.builder().put(ClientCommandHandler.PARAM_ID, receiverId).put(ClientCommandHandler.PARAM_NAME, name));
    }

    public static void setTarget(EntityPlayer player, int target) {
        ItemStack heldItem = player.getHeldItem(EnumHand.MAIN_HAND);
        if (heldItem.isEmpty() || !(heldItem.getItem() instanceof ChargedPorterItem)) {
            return;
        }
        NBTTagCompound tagCompound = heldItem.getTagCompound();
        if (tagCompound == null) {
            return;
        }
        // 只能设成这个传送器已经记下的目标之一（界面里列出的那些），并且现在仍有权限
        if (!isStoredTarget(tagCompound, target)) {
            return;
        }
        TeleportDestinations destinations = TeleportDestinations.getDestinations(player.getEntityWorld());
        GlobalCoordinate gc = destinations.getCoordinateForId(target);
        TeleportDestination destination = gc == null ? null : destinations.getDestination(gc);
        if (destination == null) {
            return;
        }
        World world = DimensionManager.getWorld(destination.getDimension());
        if (world != null && !TeleportDestinations.hasAccess(world, destination, player.getName())) {
            Msg.message(player, "msg.rftools.you_have_no_access_to_target_this_receiver");
            return;
        }
        if (world == null && Boolean.FALSE.equals(destination.checkCachedAccess(player.getName()))) {
            Msg.message(player, "msg.rftools.you_have_no_access_to_target_this_receiver");
            return;
        }
        tagCompound.setInteger("target", target);
    }

    /** {@code target} 是否是这个传送器记下的目标（target0 … target{MAXTARGETS-1}）之一。 */
    public static boolean isStoredTarget(NBTTagCompound tagCompound, int target) {
        for (int i = 0; i < AdvancedChargedPorterItem.MAXTARGETS; i++) {
            if (tagCompound.hasKey("target" + i) && tagCompound.getInteger("target" + i) == target) {
                return true;
            }
        }
        return false;
    }

    public static void returnTargets(EntityPlayer player) {
        ItemStack heldItem = player.getHeldItem(EnumHand.MAIN_HAND);
        if (heldItem.isEmpty()) {
            return;
        }
        NBTTagCompound tagCompound = heldItem.getTagCompound();

        int target = -1;
        int targets[] = new int[AdvancedChargedPorterItem.MAXTARGETS];
        String names[] = new String[AdvancedChargedPorterItem.MAXTARGETS];
        TeleportDestinations destinations = TeleportDestinations.getDestinations(player.getEntityWorld());

        if (tagCompound != null) {
            if (tagCompound.hasKey("target")) {
                target = tagCompound.getInteger("target");
            } else {
                target = -1;
            }
            for (int i = 0 ; i < AdvancedChargedPorterItem.MAXTARGETS ; i++) {
                names[i] = "";
                if (tagCompound.hasKey("target" + i)) {
                    targets[i] = tagCompound.getInteger("target" + i);
                    GlobalCoordinate gc = destinations.getCoordinateForId(targets[i]);
                    if (gc != null) {
                        TeleportDestination destination = destinations.getDestination(gc);
                        if (destination != null) {
                            names[i] = destination.getName() + " (dimension " + destination.getDimension() + ")";
                        }
                    }
                } else {
                    targets[i] = -1;
                }
            }
        } else {
            for (int i = 0 ; i < AdvancedChargedPorterItem.MAXTARGETS ; i++) {
                targets[i] = -1;
                names[i] = "";
            }
        }

        PacketTargetsReady msg = new PacketTargetsReady(target, targets, names);
        RFToolsMessages.INSTANCE.sendTo(msg, (EntityPlayerMP) player);
    }
}
