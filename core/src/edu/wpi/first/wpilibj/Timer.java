package edu.wpi.first.wpilibj;

import frcsim.hal.SimHal;

/** Simulasyon saatine bagli zamanlayici (gercek saat degil). */
public class Timer {
  private double m_accumulated;
  private double m_startTime;
  private boolean m_running;

  public static double getFPGATimestamp() {
    return SimHal.timeSeconds;
  }

  public static double getMatchTime() {
    return -1.0;
  }

  public static void delay(double seconds) {
    // Simulatorde bloklayan bekleme desteklenmez; zaman yalnizca dongu ile ilerler.
  }

  public double get() {
    return m_running ? m_accumulated + (SimHal.timeSeconds - m_startTime) : m_accumulated;
  }

  public void reset() {
    m_accumulated = 0.0;
    m_startTime = SimHal.timeSeconds;
  }

  public void start() {
    if (!m_running) {
      m_startTime = SimHal.timeSeconds;
      m_running = true;
    }
  }

  public void restart() {
    reset();
    m_running = true;
  }

  public void stop() {
    m_accumulated = get();
    m_running = false;
  }

  public boolean hasElapsed(double seconds) {
    return get() >= seconds;
  }

  public boolean advanceIfElapsed(double seconds) {
    if (get() >= seconds) {
      m_accumulated -= seconds;
      return true;
    }
    return false;
  }

  public boolean isRunning() {
    return m_running;
  }
}
