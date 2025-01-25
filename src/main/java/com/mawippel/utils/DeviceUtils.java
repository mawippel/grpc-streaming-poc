package com.mawippel.utils;

public class DeviceUtils {

    public static String getModelFromUdi(String udi) {
        String[] s = udi.split("_");
        if (s.length == 2) {
            return s[0];
        }
        return udi;
    }

    public static String getSerialNumberFromUdi(String udi) {
        String[] s = udi.split("_");
        if (s.length == 2) {
            return s[1];
        }
        return udi;
    }
}
