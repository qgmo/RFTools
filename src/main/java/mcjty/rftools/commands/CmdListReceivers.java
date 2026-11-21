package mcjty.rftools.commands;

import mcjty.lib.varia.BlockPosTools;
import mcjty.rftools.blocks.teleporter.TeleportDestinationClientInfo;
import mcjty.rftools.blocks.teleporter.TeleportDestinations;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import java.util.Collection;
import net.minecraft.util.text.TextComponentTranslation;

public class CmdListReceivers extends AbstractRfToolsCommand {
    @Override
    public String getHelp() {
        return "";
    }

    @Override
    public String getCommand() {
        return "list";
    }

    @Override
    public int getPermissionLevel() {
        // 管理员命令（原来是 0：任何玩家都能用）
        return 2;
    }

    @Override
    public boolean isClientSide() {
        return false;
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        TeleportDestinations destinations = TeleportDestinations.getDestinations(sender.getEntityWorld());

        Collection<TeleportDestinationClientInfo> validDestinations = destinations.getValidDestinations(sender.getEntityWorld(), null);
        for (TeleportDestinationClientInfo clientInfo : validDestinations) {
            int id = clientInfo.getDimension();
            ITextComponent component = new TextComponentTranslation("msg.rftools.receiver_dimension_s_location_s", id, BlockPosTools.toString(clientInfo.getCoordinate()));
            if (sender instanceof EntityPlayer) {
                ((EntityPlayer) sender).sendStatusMessage(component, false);
            } else {
                sender.sendMessage(component);
            }
        }
    }
}
