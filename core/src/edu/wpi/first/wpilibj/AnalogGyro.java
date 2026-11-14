package edu.wpi.first.wpilibj;

import edu.wpi.first.math.geometry.Rotation2d;
import frcsim.hal.SimHal;

/** Analog gyro. Aci saat yonunde pozitiftir (WPILib kurali). */
public class AnalogGyro implements AutoCloseable {
  private final int m_channel;
  private double m_zero;

  public AnalogGyro(int channel) {
    if (channel < 0 || channel >= 8) throw new IllegalArgumentException("Analog kanal 0-7 arasinda olmali");
    m_channel = channel;
  }

  public double getAngle() {
    return SimHal.gyroAngleDeg[m_channel] - m_zero;
  }

  public double getRate() {
    return SimHal.gyroRateDegPerSec[m_channel];
  }

  public void reset() {
    m_zero = SimHal.gyroAngleDeg[m_channel];
  }

  public void calibrate() {}

  public Rotation2d getRotation2d() {
    return Rotation2d.fromDegrees(-getAngle());
  }

  public void setSensitivity(double voltsPerDegreePerSecond) {}

  public void setDeadband(double volts) {}

  @Override
  public void close() {}
}
