package edu.wpi.first.math.geometry;

public class Rotation2d {
  private final double radians;
  private final double cos;
  private final double sin;

  public Rotation2d() {
    this(0.0);
  }

  public Rotation2d(double radians) {
    this.radians = radians;
    this.cos = Math.cos(radians);
    this.sin = Math.sin(radians);
  }

  public static Rotation2d fromRadians(double radians) {
    return new Rotation2d(radians);
  }

  public static Rotation2d fromDegrees(double degrees) {
    return new Rotation2d(Math.toRadians(degrees));
  }

  public static Rotation2d fromRotations(double rotations) {
    return new Rotation2d(rotations * 2.0 * Math.PI);
  }

  public double getRadians() {
    return radians;
  }

  public double getDegrees() {
    return Math.toDegrees(radians);
  }

  public double getRotations() {
    return radians / (2.0 * Math.PI);
  }

  public double getCos() {
    return cos;
  }

  public double getSin() {
    return sin;
  }

  public double getTan() {
    return sin / cos;
  }

  public Rotation2d plus(Rotation2d other) {
    return new Rotation2d(radians + other.radians);
  }

  public Rotation2d minus(Rotation2d other) {
    return new Rotation2d(radians - other.radians);
  }

  public Rotation2d unaryMinus() {
    return new Rotation2d(-radians);
  }

  public Rotation2d times(double scalar) {
    return new Rotation2d(radians * scalar);
  }

  public Rotation2d rotateBy(Rotation2d other) {
    return plus(other);
  }

  @Override
  public String toString() {
    return String.format("Rotation2d(Rads: %.2f, Deg: %.2f)", radians, getDegrees());
  }
}
