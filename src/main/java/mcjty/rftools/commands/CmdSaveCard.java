package mcjty.rftools.commands;

import mcjty.rftools.items.builder.ShapeCardItem;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TextComponentTranslation;

public class CmdSaveCard extends AbstractRfToolsCommand {
    @Override
    public String getHelp() {
        return "<filename>";
    }

    @Override
    public String getCommand() {
        return "save";
    }

    @Override
    public int getPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        if (args.length > 2) {
            sender.sendMessage(new TextComponentTranslation("msg.rftools.too_many_parameters"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(new TextComponentTranslation("msg.rftools.missing_filename"));
            return;
        }

        if (!(sender instanceof EntityPlayer)) {
            sender.sendMessage(new TextComponentTranslation("msg.rftools.this_command_only_works_as_a_player"));
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;
        ItemStack heldItem = player.getHeldItem(EnumHand.MAIN_HAND);
        if (heldItem.isEmpty() || !(heldItem.getItem() instanceof ShapeCardItem)) {
            sender.sendMessage(new TextComponentTranslation("msg.rftools.you_need_to_hold_a_shapecard_in_your_hand"));
            return;
        }

        ShapeCardItem.save(player, heldItem, args[1]);
    }
}
