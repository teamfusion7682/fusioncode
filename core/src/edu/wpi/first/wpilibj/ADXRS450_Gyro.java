package edu.wpi.first.wpilibj;

import edu.wpi.first.math.geometry.Rotation2d;
import frcsim.hal.SimHal;

/** SPI gyro. Simulatorde, config'deki gyro kanalini kullanir. */
public class ADXRS450_Gyro implements AutoCloseable {
  private static final int CHANNEL = 0;
  private double m_zero;

  public ADXRS450_Gyro() {}

  public double getAngle() {
    return SimHal.gyroAngleDeg[CHANNEL] - m_zero;
  }

  public double getRate() {
    return SimHal.gyroRateDegPerSec[CHANNEL];
  }

  public void reset() {
    m_zero = SimHal.gyroAngleDeg[CHANNEL];
  }

  public void calibrate() {}

  public Rotation2d getRotation2d() {
    return Rotation2d.fromDegrees(-getAngle());
  }

  @Override
  public void close() {}
}
