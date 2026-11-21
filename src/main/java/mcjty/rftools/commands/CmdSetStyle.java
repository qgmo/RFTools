package mcjty.rftools.commands;

import mcjty.lib.McJtyLib;
import mcjty.lib.gui.GuiStyle;
import mcjty.lib.preferences.PreferencesProperties;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TextComponentTranslation;

public class CmdSetStyle extends AbstractRfToolsCommand {
    @Override
    public String getHelp() {
        return "[<style>]";
    }

    @Override
    public String getCommand() {
        return "setstyle";
    }

    @Override
    public int getPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        if (args.length > 2) {
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

        if (args.length < 2) {
            GuiStyle style = properties.getStyle();
            ITextComponent component = new TextComponentTranslation("msg.rftools.current_gui_style_s", style.getStyle());
            if (sender instanceof EntityPlayer) {
                ((EntityPlayer) sender).sendStatusMessage(component, false);
            } else {
                sender.sendMessage(component);
            }
            return;
        }

        String s = fetchString(sender, args, 1, "");
        boolean b = properties.setStyle(s);
        if (!b) {
            String buf = "";
            for (GuiStyle style : GuiStyle.values()) {
                buf = buf + " " + style.getStyle();
            }

            ITextComponent component = new TextComponentTranslation("msg.rftools.unknown_style_options_s", buf);
            if (sender instanceof EntityPlayer) {
                ((EntityPlayer) sender).sendStatusMessage(component, false);
            } else {
                sender.sendMessage(component);
            }
        }
    }
}
