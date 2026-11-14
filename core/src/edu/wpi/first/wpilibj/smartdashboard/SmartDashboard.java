package edu.wpi.first.wpilibj.smartdashboard;

import edu.wpi.first.util.sendable.Sendable;
import frcsim.hal.SimHal;

/** Sayilar/yazilar simulatorde kaydedilir ve web arayuzunde grafik olarak gosterilir. */
public final class SmartDashboard {
  private SmartDashboard() {}

  public static boolean putNumber(String key, double value) {
    SimHal.dashboardNumbers.put(key, value);
    return true;
  }

  public static double getNumber(String key, double defaultValue) {
    Double v = SimHal.dashboardNumbers.get(key);
    return v == null ? defaultValue : v;
  }

  public static boolean putBoolean(String key, boolean value) {
    SimHal.dashboardNumbers.put(key, value ? 1.0 : 0.0);
    return true;
  }

  public static boolean getBoolean(String key, boolean defaultValue) {
    Double v = SimHal.dashboardNumbers.get(key);
    return v == null ? defaultValue : v != 0.0;
  }

  public static boolean putString(String key, String value) {
    SimHal.dashboardStrings.put(key, value);
    return true;
  }

  public static String getString(String key, String defaultValue) {
    String v = SimHal.dashboardStrings.get(key);
    return v == null ? defaultValue : v;
  }

  public static void putData(String key, Sendable data) {
    if (data instanceof SendableChooser) {
      SendableChooser<?> chooser = (SendableChooser<?>) data;
      chooser.registerKey(key);
      SimHal.choosers.put(key, chooser);
    }
  }

  public static void updateValues() {}
}
