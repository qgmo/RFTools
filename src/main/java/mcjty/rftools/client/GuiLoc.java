package mcjty.rftools.client;

import mcjty.lib.gui.Window;
import mcjty.lib.varia.Logging;
import mcjty.lib.gui.widgets.*;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Client-side helpers for the GUI texts. McJtyLib widgets do not translate anything themselves, so
 * the texts are translated here: Java code calls {@link #lines}/{@code I18n.format}, and texts that come from
 * the .gui files (which contain lang keys) are translated after the window has been built.
 */
@SideOnly(Side.CLIENT)
public class GuiLoc {

    /** Prefix of the keys used for the ids of text choice labels (the id itself is also sent to the server and compared in code) */
    public static final String ENG_PREFIX = "gui.rftools.en.";
    public static final String CHOICE_PREFIX = "gui.rftools.choice.";

    /**
     * Translate a key and split the result in lines (tooltips)
     */
    public static String[] lines(String key, Object... args) {
        return StringUtils.split(I18n.format(key, args), '\n');
    }

    /**
     * If the string is a lang key, return the translation, otherwise return the string itself
     */
    public static String tr(String s) {
        if (s != null && s.indexOf(' ') < 0 && s.indexOf('.') > 0 && I18n.hasKey(s)) {
            return I18n.format(s);
        }
        return s;
    }

    /**
     * Same as {@link #tr} but for tooltips: one lang key can expand to several lines
     */
    public static List<String> trLines(List<String> list) {
        if (list == null) {
            return null;
        }
        List<String> result = null;
        for (int i = 0; i < list.size(); i++) {
            String s = list.get(i);
            String t = tr(s);
            if (t != s && result == null) {
                result = new ArrayList<>(list.subList(0, i));
            }
            if (result != null) {
                if (t != s) {
                    for (String l : StringUtils.split(t, '\n')) {
                        result.add(l);
                    }
                } else {
                    result.add(s);
                }
            }
        }
        return result == null ? list : result;
    }

    /**
     * Key of the translation of a text that is given in English by the screen modules (these use the
     * module gui builder, the English text is the identification there)
     */
    public static String engKey(String english) {
        return ENG_PREFIX + english.replaceAll("§.", "").replace("+", "_plus").replace("-", "_minus").replaceAll("[^A-Za-z0-9]+", "_").replaceAll("^_+|_+$", "").toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Translation of an English text for which a key engKey() exists. If there is none the text itself is returned.
     */
    public static String eng(String english) {
        if (english == null || english.isEmpty()) {
            return english;
        }
        String key = engKey(english);
        return I18n.hasKey(key) ? I18n.format(key) : english;
    }

    /**
     * Same as eng() for a tooltip of several lines
     */
    public static String[] engLines(String... english) {
        if (english == null || english.length == 0) {
            return english;
        }
        String key = engKey(StringUtils.join(english, '\n'));
        return I18n.hasKey(key) ? StringUtils.split(I18n.format(key), '\n') : english;
    }

    /**
     * Displayed text of a choice id (ChoiceLabel). If there is no translation the id is displayed
     */
    public static String choice(String id) {
        return choice(id, null);
    }

    /**
     * Same as choice(id) but a translation that is specific for the label with the given name is preferred
     */
    public static String choice(String id, String labelName) {
        if (id == null || id.isEmpty()) {
            return id;
        }
        String key = choiceKey(id);
        if (labelName != null && !labelName.isEmpty()) {
            String specific = CHOICE_PREFIX + choiceKey(labelName) + "." + key;
            if (I18n.hasKey(specific)) {
                return I18n.format(specific);
            }
        }
        key = CHOICE_PREFIX + key;
        return I18n.hasKey(key) ? I18n.format(key) : id;
    }

    public static String choiceKey(String id) {
        return id.replace("+", "_plus").replace("-", "_minus").replaceAll("[^A-Za-z0-9]+", "_").replaceAll("^_+|_+$", "").toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Translate all the lang keys in the labels, buttons and tooltips in this window (as read from a .gui file)
     */
    public static void localize(Window window) {
        localize(window.getToplevel());
    }

    @SuppressWarnings("unchecked")
    public static void localize(Widget<?> widget) {
        if (widget == null) {
            return;
        }
        if (widget instanceof AbstractLabel && !(widget instanceof ChoiceLabel)) {
            AbstractLabel<?> label = (AbstractLabel<?>) widget;
            String text = label.getText();
            if (text != null) {
                label.setText(tr(text));
            }
        }
        if (!(widget instanceof ChoiceLabel) && !(widget instanceof ImageChoiceLabel)) {
            List<String> tooltips = widget.getTooltips();
            List<String> translated = trLines(tooltips);
            if (translated != tooltips) {
                ((Widget) widget).setTooltips(translated.toArray(new String[0]));
            }
        }
        if (widget instanceof AbstractContainerWidget) {
            for (Widget<?> child : ((AbstractContainerWidget<?>) widget).getChildren()) {
                localize(child);
            }
        }
    }

    /**
     * Make the choice labels read from .gui files translate their choices and tooltips
     */
    @SuppressWarnings("unchecked")
    public static void registerWidgets() {
        try {
            Field field = WidgetRepository.class.getDeclaredField("FACTORIES");
            field.setAccessible(true);
            Map<String, BiFunction<net.minecraft.client.Minecraft, net.minecraft.client.gui.Gui, Widget<?>>> factories =
                    (Map<String, BiFunction<net.minecraft.client.Minecraft, net.minecraft.client.gui.Gui, Widget<?>>>) field.get(null);
            factories.put(ChoiceLabel.TYPE_CHOICELABEL, LocChoiceLabel::new);
            factories.put(ImageChoiceLabel.TYPE_IMAGECHOICELABEL, LocImageChoiceLabel::new);
        } catch (Exception e) {
            // Without this the choices are displayed in English
            Logging.getLogger().warn("Could not register the translated choice labels: " + e);
        }
    }
}
