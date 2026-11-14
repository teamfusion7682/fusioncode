package edu.wpi.first.wpilibj;

import frcsim.hal.SimHal;

/** Kuadratur encoder. Sayaclar fizik motorundan (robot-config.json) gelir. */
public class Encoder implements AutoCloseable {
  private final int m_channelA;
  private final int m_channelB;
  private boolean m_reverse;
  private double m_distancePerPulse = 1.0;
  private double m_zero;

  public Encoder(int channelA, int channelB) {
    this(channelA, channelB, false);
  }

  public Encoder(int channelA, int channelB, boolean reverseDirection) {
    if (channelA < 0 || channelB < 0) throw new IllegalArgumentException("Encoder kanallari negatif olamaz");
    m_channelA = channelA;
    m_channelB = channelB;
    m_reverse = reverseDirection;
  }

  private int rawSigned() {
    int counts = (int) (SimHal.getEncoder(m_channelA)[0] - m_zero);
    return m_reverse ? -counts : counts;
  }

  public int get() {
    return rawSigned();
  }

  public int getRaw() {
    return rawSigned();
  }

  public void reset() {
    m_zero = SimHal.getEncoder(m_channelA)[0];
  }

  public double getDistance() {
    return rawSigned() * m_distancePerPulse;
  }

  public double getRate() {
    double countsPerSec = SimHal.getEncoder(m_channelA)[1] * (m_reverse ? -1.0 : 1.0);
    return countsPerSec * m_distancePerPulse;
  }

  public boolean getDirection() {
    return getRate() >= 0.0;
  }

  public boolean getStopped() {
    return Math.abs(SimHal.getEncoder(m_channelA)[1]) < 1e-6;
  }

  public void setDistancePerPulse(double distancePerPulse) {
    m_distancePerPulse = distancePerPulse;
  }

  public double getDistancePerPulse() {
    return m_distancePerPulse;
  }

  public void setReverseDirection(boolean reverseDirection) {
    m_reverse = reverseDirection;
  }

  public void setSamplesToAverage(int samples) {}

  public void setMinRate(double minRate) {}

  @Override
  public void close() {}
}
