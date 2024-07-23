package com.project.andrew;

public final class Utils {
    public static boolean showAdvancedInfo = false;

    public static void showText(String string) {
        if (showAdvancedInfo) {
            System.out.println(string);
        }
    }
}
