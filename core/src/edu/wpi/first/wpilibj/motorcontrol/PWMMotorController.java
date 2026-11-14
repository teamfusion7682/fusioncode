package edu.wpi.first.wpilibj.motorcontrol;

import frcsim.hal.SimHal;

/** PWM ile surulen motor kontrolcusu. Ham PWM degeri SimHal.pwm'e yazilir. */
public abstract class PWMMotorController implements MotorController, AutoCloseable {
  private final int m_channel;
  private boolean m_inverted;
  private double m_speed;

  protected PWMMotorController(String name, int channel) {
    if (channel < 0 || channel >= SimHal.PWM_CHANNELS) {
      throw new IllegalArgumentException("PWM kanali 0-" + (SimHal.PWM_CHANNELS - 1) + " arasinda olmali: " + channel);
    }
    m_channel = channel;
  }

  @Override
  public void set(double speed) {
    if (Double.isNaN(speed)) speed = 0.0;
    m_speed = Math.max(-1.0, Math.min(1.0, speed));
    SimHal.pwm[m_channel] = m_inverted ? -m_speed : m_speed;
  }

  @Override
  public double get() {
    return m_speed;
  }

  @Override
  public void setInverted(boolean isInverted) {
    m_inverted = isInverted;
    SimHal.pwm[m_channel] = m_inverted ? -m_speed : m_speed;
  }

  @Override
  public boolean getInverted() {
    return m_inverted;
  }

  @Override
  public void disable() {
    SimHal.pwm[m_channel] = 0.0;
  }

  @Override
  public void stopMotor() {
    set(0.0);
  }

  public int getChannel() {
    return m_channel;
  }

  @Override
  public void close() {}
}
