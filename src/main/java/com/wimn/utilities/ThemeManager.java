package com.wimn.utilities;

import javafx.scene.Scene;

public class ThemeManager {
    public enum Theme {
        DARK_NAVY,
        LIGHT_ENTERPRISE
    }

    private static Theme currentTheme = Theme.DARK_NAVY;

    public static Theme getCurrentTheme() {
        return currentTheme;
    }

    public static void applyTheme(Scene scene, Theme theme) {
        currentTheme = theme;
        scene.getStylesheets().clear();
        if (theme == Theme.DARK_NAVY) {
            String darkCss = ThemeManager.class.getResource("/css/dark-theme.css") != null ?
                    ThemeManager.class.getResource("/css/dark-theme.css").toExternalForm() : null;
            if (darkCss != null) scene.getStylesheets().add(darkCss);
        } else {
            String lightCss = ThemeManager.class.getResource("/css/light-theme.css") != null ?
                    ThemeManager.class.getResource("/css/light-theme.css").toExternalForm() : null;
            if (lightCss != null) scene.getStylesheets().add(lightCss);
        }
    }
}
