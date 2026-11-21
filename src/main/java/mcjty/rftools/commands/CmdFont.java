package mcjty.rftools.commands;

import mcjty.lib.font.FontLoader;
import mcjty.lib.font.TrueTypeFont;
import mcjty.rftools.blocks.screens.ScreenConfiguration;
import mcjty.rftools.setup.ClientProxy;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TextComponentTranslation;

public class CmdFont extends AbstractRfToolsCommand {
    @Override
    public String getHelp() {
        return "<name>,<size>";
    }

    @Override
    public String getCommand() {
        return "font";
    }

    @Override
    public int getPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        if (args.length < 3) {
            ITextComponent component = new TextComponentTranslation("msg.rftools.several_parameters_are_missing");
            if (sender instanceof EntityPlayer) {
                ((EntityPlayer) sender).sendStatusMessage(component, false);
            } else {
                sender.sendMessage(component);
            }
            return;
        } else if (args.length > 3) {
            ITextComponent component = new TextComponentTranslation("msg.rftools.too_many_parameters");
            if (sender instanceof EntityPlayer) {
                ((EntityPlayer) sender).sendStatusMessage(component, false);
            } else {
                sender.sendMessage(component);
            }
            return;
        }

        // @todo, fix the 1.13 way
//        ScreenConfiguration.font = fetchString(sender, args, 1, "rftools:fonts/ubuntu.ttf");
//        ScreenConfiguration.fontSize = fetchFloat(sender, args, 2, 40);
        TrueTypeFont font = FontLoader.createFont(new ResourceLocation(ScreenConfiguration.font.get()), (float) ScreenConfiguration.fontSize.get(), false);
        if (font == null) {
            ITextComponent component = new TextComponentTranslation("msg.rftools.could_not_load_font");
            if (sender instanceof EntityPlayer) {
                ((EntityPlayer) sender).sendStatusMessage(component, false);
            } else {
                sender.sendMessage(component);
            }
            return;
        }
        ClientProxy.font = font;
    }

    @Override
    public boolean isClientSide() {
        return true;
    }
}
