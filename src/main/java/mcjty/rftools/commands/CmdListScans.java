package mcjty.rftools.commands;

import mcjty.rftools.shapes.ScanDataManager;
import net.minecraft.command.ICommandSender;

public class CmdListScans extends AbstractRfToolsCommand {
    @Override
    public String getHelp() {
        return "";
    }

    @Override
    public String getCommand() {
        return "listscans";
    }

    @Override
    public int getPermissionLevel() {
        // 管理员命令（原来是 0：任何玩家都能用）
        return 2;
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        ScanDataManager.listScans(sender);
    }
}
