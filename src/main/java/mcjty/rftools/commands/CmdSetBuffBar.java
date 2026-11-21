package mcjty.rftools.commands;

import mcjty.lib.McJtyLib;
import mcjty.lib.preferences.PreferencesProperties;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TextComponentTranslation;

public class CmdSetBuffBar extends AbstractRfToolsCommand {
    @Override
    public String getHelp() {
        return "[<x> <y>]";
    }

    @Override
    public String getCommand() {
        return "buffs";
    }

    @Override
    public int getPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        if (args.length > 3) {
            ITextComponent component = new TextComponentTranslation("msg.rftools.too_many_parameters");
            if (sender instanceof EntityPlayer) {
                ((EntityPlayer) sender).sendStatusMessage(component, false);
            } else {
                sender.sendMessage(component);
            }
            return;
        }

        if (!(sender instanceof EntityPlayer)) {
            ITextComponent component = new TextComponentTranslation("msg.rftools.this_command_only_works_as_a_player");
            if (sender instanceof EntityPlayer) {
                ((EntityPlayer) sender).sendStatusMessage(component, false);
            } else {
                sender.sendMessage(component);
            }
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;
        PreferencesProperties properties = McJtyLib.getPreferencesProperties(player);

        if (args.length < 3) {
            int buffX = properties.getBuffX();
            int buffY = properties.getBuffY();
            ITextComponent component = new TextComponentTranslation("msg.rftools.current_buffbar_location_s_s", buffX, buffY);
            if (sender instanceof EntityPlayer) {
                ((EntityPlayer) sender).sendStatusMessage(component, false);
            } else {
                sender.sendMessage(component);
            }
            return;
        }

        int x = fetchInt(sender, args, 1, 0);
        int y = fetchInt(sender, args, 2, 0);
        properties.setBuffXY(x, y);
    }
}
