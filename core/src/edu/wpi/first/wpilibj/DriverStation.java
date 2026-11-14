package edu.wpi.first.wpilibj;

import frcsim.hal.SimHal;
import java.util.Optional;

public final class DriverStation {
  private DriverStation() {}

  public enum Alliance { Red, Blue }

  public static boolean isEnabled() {
    return SimHal.isEnabled();
  }

  public static boolean isDisabled() {
    return !SimHal.isEnabled();
  }

  public static boolean isAutonomous() {
    return SimHal.mode == SimHal.Mode.AUTONOMOUS;
  }

  public static boolean isAutonomousEnabled() {
    return isAutonomous();
  }

  public static boolean isTeleop() {
    return SimHal.mode == SimHal.Mode.TELEOP;
  }

  public static boolean isTeleopEnabled() {
    return isTeleop();
  }

  public static boolean isTest() {
    return SimHal.mode == SimHal.Mode.TEST;
  }

  public static boolean isDSAttached() {
    return true;
  }

  public static boolean isFMSAttached() {
    return false;
  }

  public static double getMatchTime() {
    return -1.0;
  }

  public static Optional<Alliance> getAlliance() {
    return Optional.of(Alliance.Blue);
  }

  public static void reportWarning(String warning, boolean printTrace) {
    System.err.println("Warning: " + warning);
  }

  public static void reportError(String error, boolean printTrace) {
    System.err.println("ERROR: " + error);
  }
}
