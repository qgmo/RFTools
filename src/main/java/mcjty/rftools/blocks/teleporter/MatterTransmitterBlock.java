package mcjty.rftools.blocks.teleporter;

import mcjty.lib.api.Infusable;
import mcjty.lib.container.EmptyContainer;
import mcjty.lib.gui.GenericGuiContainer;
import mcjty.lib.typed.TypedMap;
import mcjty.lib.varia.BlockPosTools;
import mcjty.rftools.setup.CommandHandler;
import mcjty.rftools.blocks.GenericRFToolsBlock;
import mcjty.rftools.setup.GuiProxy;
import mcjty.rftools.network.RFToolsMessages;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.client.resources.I18n;
import mcjty.rftools.client.GuiLoc;
import java.util.Collections;

public class MatterTransmitterBlock extends GenericRFToolsBlock<MatterTransmitterTileEntity, EmptyContainer> implements Infusable {

    public static Integer clientSideId = null;
    public static String clientSideName = "?";

    public MatterTransmitterBlock() {
        super(Material.IRON, MatterTransmitterTileEntity.class, EmptyContainer::new, "matter_transmitter", false);
        setDefaultState(this.blockState.getBaseState());
    }

    @SideOnly(Side.CLIENT)
    public static void setDestinationInfo(Integer id, String name) {
        MatterTransmitterBlock.clientSideId = id;
        MatterTransmitterBlock.clientSideName = name;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BiFunction<MatterTransmitterTileEntity, EmptyContainer, GenericGuiContainer<? super MatterTransmitterTileEntity>> getGuiFactory() {
        return GuiMatterTransmitter::new;
    }

    @Override
    public void initModel() {
        super.initModel();
        BeamRenderer.register();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack itemStack, World player, List<String> list, ITooltipFlag whatIsThis) {
        super.addInformation(itemStack, player, list, whatIsThis);
        NBTTagCompound tagCompound = itemStack.getTagCompound();
        if (tagCompound != null) {
            String name = tagCompound.getString("tpName");
            list.add(I18n.format("tooltip.rftools.name_s", name));

            boolean dialed = false;
            BlockPos c = BlockPosTools.readFromNBT(tagCompound, "dest");
            if (c != null && c.getY() >= 0) {
                dialed = true;
            } else if (tagCompound.hasKey("destId")) {
                if (tagCompound.getInteger("destId") != -1) {
                    dialed = true;
                }
            }

            if (dialed) {
                int destId = tagCompound.getInteger("destId");
                if (System.currentTimeMillis() - lastTime > 500) {
                    lastTime = System.currentTimeMillis();
                    RFToolsMessages.sendToServer(CommandHandler.CMD_GET_DESTINATION_INFO, TypedMap.builder().put(CommandHandler.PARAM_ID, destId));
                }

                String destname = "?";
                if (clientSideId != null && clientSideId == destId) {
                    destname = clientSideName;
                }
                list.add(I18n.format("tooltip.rftools.dialed_to_s", destname));
            }

            boolean once = tagCompound.getBoolean("once");
            if (once) {
                list.add(I18n.format("tooltip.rftools.once"));
            }
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            Collections.addAll(list, GuiLoc.lines("tooltip.rftools.if_you_place_this_block_near_a_dialing"));
            Collections.addAll(list, GuiLoc.lines("tooltip.rftools.infusing_bonus_reduced_power_consumption_4"));
        } else {
            list.add(TextFormatting.WHITE + I18n.format("message.rftools.shiftmessage"));
        }
    }

    private static long lastTime = 0;

    @Override
    @Optional.Method(modid = "theoneprobe")
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
        super.addProbeInfo(mode, probeInfo, player, world, blockState, data);
        TileEntity te = world.getTileEntity(data.getPos());
        if (te instanceof MatterTransmitterTileEntity) {
            MatterTransmitterTileEntity matterTransmitterTileEntity = (MatterTransmitterTileEntity) te;
            probeInfo.text(TextFormatting.GREEN + "Name: " + matterTransmitterTileEntity.getName());
            if (matterTransmitterTileEntity.isDialed()) {
                Integer teleportId = matterTransmitterTileEntity.getTeleportId();
                TeleportDestinations destinations = TeleportDestinations.getDestinations(world);
                String name = "?";
                if (teleportId != null) {
                    name = TeleportDestinations.getDestinationName(destinations, teleportId);
                }
                probeInfo.text(TextFormatting.YELLOW + "[DIALED to " + name + "]");
            }
            if (matterTransmitterTileEntity.isOnce()) {
                probeInfo.text(TextFormatting.YELLOW + "[ONCE]");
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    @Optional.Method(modid = "waila")
    public List<String> getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        super.getWailaBody(itemStack, currenttip, accessor, config);
        TileEntity te = accessor.getTileEntity();
        if (te instanceof MatterTransmitterTileEntity) {
            MatterTransmitterTileEntity matterTransmitterTileEntity = (MatterTransmitterTileEntity) te;
            currenttip.add(TextFormatting.GREEN + "Name: " + matterTransmitterTileEntity.getName());
            if (matterTransmitterTileEntity.isDialed()) {
                if (System.currentTimeMillis() - lastTime > 500) {
                    lastTime = System.currentTimeMillis();
                    RFToolsMessages.sendToServer(CommandHandler.CMD_GET_DESTINATION_INFO,
                            TypedMap.builder().put(CommandHandler.PARAM_ID, matterTransmitterTileEntity.getTeleportId()));
                }

                String name = "?";
                if (clientSideId != null && clientSideId == matterTransmitterTileEntity.getTeleportId()) {
                    name = clientSideName;
                }
                currenttip.add(TextFormatting.YELLOW + "[DIALED to " + name + "]");
            }
            if (matterTransmitterTileEntity.isOnce()) {
                currenttip.add(TextFormatting.YELLOW + "[ONCE]");
            }
        }
        return currenttip;
    }

    @Override
    public int getGuiID() {
        return GuiProxy.GUI_MATTER_TRANSMITTER;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        // We don't want what GenericBlock does.
        restoreBlockFromNBT(world, pos, stack);
        setOwner(world, pos, placer);
    }

    @Override
    public RotationType getRotationType() {
        return RotationType.NONE;
    }

}
