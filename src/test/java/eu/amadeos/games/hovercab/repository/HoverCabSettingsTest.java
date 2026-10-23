package eu.amadeos.games.hovercab.repository;

import eu.amadeos.gameengine.swing.RenderMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HoverCabSettingsTest {
    @TempDir
    Path dir;

    @Test
    void settingsAreStoredAndReadBack() {
        HoverCabSettings settings = new HoverCabSettings(dir.resolve("hovercab.properties"));
        assertEquals(RenderMode.DIRECT, settings.renderMode());
        assertTrue(settings.sound());
        settings.setRenderMode(RenderMode.PIXEL);
        settings.setSound(false);
        HoverCabSettings again = new HoverCabSettings(dir.resolve("hovercab.properties"));
        assertEquals(RenderMode.PIXEL, again.renderMode());
        assertFalse(again.sound());
    }

    @Test
    void keysComeFromTheFileOrTheDefaults() throws Exception {
        Path file = dir.resolve("hovercab.properties");
        Files.writeString(file, "keys.gear=L SHIFT\nkeys.up=NOT_A_KEY\n");
        HoverCabSettings settings = new HoverCabSettings(file);
        assertArrayEquals(new int[]{KeyEvent.VK_L, KeyEvent.VK_SHIFT}, settings.keys("gear", KeyEvent.VK_SPACE));
        assertArrayEquals(new int[]{KeyEvent.VK_UP}, settings.keys("up", KeyEvent.VK_UP));
    }
}
