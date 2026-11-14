package edu.wpi.first.math.util;

public final class Units {
  private Units() {}

  public static double degreesToRadians(double degrees) {
    return Math.toRadians(degrees);
  }

  public static double radiansToDegrees(double radians) {
    return Math.toDegrees(radians);
  }

  public static double inchesToMeters(double inches) {
    return inches * 0.0254;
  }

  public static double metersToInches(double meters) {
    return meters / 0.0254;
  }

  public static double feetToMeters(double feet) {
    return feet * 0.3048;
  }

  public static double metersToFeet(double meters) {
    return meters / 0.3048;
  }

  public static double rotationsToRadians(double rotations) {
    return rotations * 2.0 * Math.PI;
  }

  public static double radiansToRotations(double radians) {
    return radians / (2.0 * Math.PI);
  }

  public static double rotationsPerMinuteToRadiansPerSecond(double rpm) {
    return rpm * 2.0 * Math.PI / 60.0;
  }

  public static double radiansPerSecondToRotationsPerMinute(double radPerSec) {
    return radPerSec * 60.0 / (2.0 * Math.PI);
  }
}
