package mcjty.rftools.keys;

import mcjty.lib.debugtools.DumpBlockNBT;
import mcjty.lib.debugtools.DumpItemNBT;
import mcjty.lib.typed.TypedMap;
import mcjty.rftools.setup.CommandHandler;
import mcjty.rftools.network.RFToolsMessages;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

public class KeyInputHandler {

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (KeyBindings.porterNextDestination.isPressed()) {
            RFToolsMessages.sendToServer(CommandHandler.CMD_CYCLE_DESTINATION, TypedMap.builder().put(CommandHandler.PARAM_NEXT, true));
        } else if (KeyBindings.porterPrevDestination.isPressed()) {
            RFToolsMessages.sendToServer(CommandHandler.CMD_CYCLE_DESTINATION, TypedMap.builder().put(CommandHandler.PARAM_NEXT, false));
        } else if (KeyBindings.debugDumpNBTItem.isPressed()) {
            // 服务端不再接收调试包，只在客户端日志里输出
            DumpItemNBT.dumpHeldItem(null, Minecraft.getMinecraft().player, false);
        } else if (KeyBindings.debugDumpNBTBlock.isPressed()) {
            DumpBlockNBT.dumpFocusedBlock(null, Minecraft.getMinecraft().player, true, false);
        }
    }
}
