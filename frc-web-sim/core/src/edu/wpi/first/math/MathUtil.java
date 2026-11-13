package edu.wpi.first.math;

public final class MathUtil {
  private MathUtil() {}

  public static double clamp(double value, double low, double high) {
    return Math.max(low, Math.min(value, high));
  }

  public static int clamp(int value, int low, int high) {
    return Math.max(low, Math.min(value, high));
  }

  public static double applyDeadband(double value, double deadband, double maxMagnitude) {
    if (Math.abs(value) > deadband) {
      if (maxMagnitude / deadband > 1.0e12) {
        return value > 0.0 ? value - deadband : value + deadband;
      }
      if (value > 0.0) {
        return maxMagnitude * (value - deadband) / (maxMagnitude - deadband);
      } else {
        return maxMagnitude * (value + deadband) / (maxMagnitude - deadband);
      }
    }
    return 0.0;
  }

  public static double applyDeadband(double value, double deadband) {
    return applyDeadband(value, deadband, 1.0);
  }

  public static double inputModulus(double input, double minimumInput, double maximumInput) {
    double modulus = maximumInput - minimumInput;
    int numMax = (int) ((input - minimumInput) / modulus);
    input -= numMax * modulus;
    int numMin = (int) ((input - maximumInput) / modulus);
    input -= numMin * modulus;
    return input;
  }

  public static double angleModulus(double angleRadians) {
    return inputModulus(angleRadians, -Math.PI, Math.PI);
  }

  public static double interpolate(double startValue, double endValue, double t) {
    return startValue + (endValue - startValue) * clamp(t, 0, 1);
  }

  public static boolean isNear(double expected, double actual, double tolerance) {
    return Math.abs(expected - actual) < tolerance;
  }
}
