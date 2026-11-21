package mcjty.rftools.varia;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * Send translated messages to a player. The translation is done by the client so
 * the message follows the language of the player and not the one of the server.
 */
public class Msg {

    public static void message(@Nonnull EntityPlayer player, String key, Object... args) {
        player.sendStatusMessage(new TextComponentTranslation(key, args), false);
    }

    public static void warn(@Nonnull EntityPlayer player, String key, Object... args) {
        player.sendStatusMessage(new TextComponentTranslation(key, args).setStyle(new Style().setColor(TextFormatting.RED)), false);
    }

    private static final Map<String, Long> broadcasts = new HashMap<>();

    /**
     * Send a warning to all players near a block. The same message is not repeated within two seconds.
     */
    public static void broadcast(World world, int x, int y, int z, float radius, String key, Object... args) {
        long time = System.currentTimeMillis();
        Long last = broadcasts.get(key);
        if (last != null && (time - last) <= 2000) {
            return;
        }
        broadcasts.put(key, time);
        for (EntityPlayer player : world.playerEntities) {
            if (player.getDistanceSq(x + .5, y + .5, z + .5) < radius) {
                warn(player, key, args);
            }
        }
    }
}
