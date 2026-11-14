package edu.wpi.first.wpilibj;

import frcsim.hal.SimHal;

public final class RobotController {
  private RobotController() {}

  public static double getBatteryVoltage() {
    return SimHal.batteryVolts;
  }

  public static double getInputVoltage() {
    return SimHal.batteryVolts;
  }

  public static long getFPGATime() {
    return (long) (SimHal.timeSeconds * 1.0e6);
  }

  public static boolean isBrownedOut() {
    return false;
  }
}
