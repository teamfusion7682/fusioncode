package edu.wpi.first.math.controller;

import edu.wpi.first.math.MathUtil;

/** WPILib PIDController ile ayni algoritma (turev hatadan, integral sinirli). */
public class PIDController implements AutoCloseable {
  private double m_kp;
  private double m_ki;
  private double m_kd;
  private final double m_period;

  private double m_maximumIntegral = 1.0;
  private double m_minimumIntegral = -1.0;
  private double m_maximumInput;
  private double m_minimumInput;
  private boolean m_continuous;

  private double m_positionError;
  private double m_velocityError;
  private double m_prevError;
  private double m_totalError;

  private double m_positionTolerance = 0.05;
  private double m_velocityTolerance = Double.POSITIVE_INFINITY;
  private double m_iZone = Double.POSITIVE_INFINITY;

  private double m_setpoint;
  private double m_measurement;
  private boolean m_haveMeasurement;
  private boolean m_haveSetpoint;

  public PIDController(double kp, double ki, double kd) {
    this(kp, ki, kd, 0.02);
  }

  public PIDController(double kp, double ki, double kd, double period) {
    m_kp = kp;
    m_ki = ki;
    m_kd = kd;
    if (kp < 0.0) throw new IllegalArgumentException("Kp must be a non-negative number!");
    if (ki < 0.0) throw new IllegalArgumentException("Ki must be a non-negative number!");
    if (kd < 0.0) throw new IllegalArgumentException("Kd must be a non-negative number!");
    if (period <= 0.0) throw new IllegalArgumentException("Controller period must be a positive number!");
    m_period = period;
  }

  public void setPID(double kp, double ki, double kd) {
    m_kp = kp;
    m_ki = ki;
    m_kd = kd;
  }

  public void setP(double kp) {
    m_kp = kp;
  }

  public void setI(double ki) {
    m_ki = ki;
  }

  public void setD(double kd) {
    m_kd = kd;
  }

  public void setIZone(double iZone) {
    if (iZone < 0) throw new IllegalArgumentException("IZone must be a non-negative number!");
    m_iZone = iZone;
  }

  public double getP() {
    return m_kp;
  }

  public double getI() {
    return m_ki;
  }

  public double getD() {
    return m_kd;
  }

  public double getIZone() {
    return m_iZone;
  }

  public double getPeriod() {
    return m_period;
  }

  public double getPositionTolerance() {
    return m_positionTolerance;
  }

  public double getVelocityTolerance() {
    return m_velocityTolerance;
  }

  public void setSetpoint(double setpoint) {
    m_setpoint = setpoint;
    m_haveSetpoint = true;
    if (m_continuous) {
      double errorBound = (m_maximumInput - m_minimumInput) / 2.0;
      m_positionError = MathUtil.inputModulus(m_setpoint - m_measurement, -errorBound, errorBound);
    } else {
      m_positionError = m_setpoint - m_measurement;
    }
    m_velocityError = (m_positionError - m_prevError) / m_period;
  }

  public double getSetpoint() {
    return m_setpoint;
  }

  public boolean atSetpoint() {
    return m_haveMeasurement
        && m_haveSetpoint
        && Math.abs(m_positionError) < m_positionTolerance
        && Math.abs(m_velocityError) < m_velocityTolerance;
  }

  public void enableContinuousInput(double minimumInput, double maximumInput) {
    m_continuous = true;
    m_minimumInput = minimumInput;
    m_maximumInput = maximumInput;
  }

  public void disableContinuousInput() {
    m_continuous = false;
  }

  public boolean isContinuousInputEnabled() {
    return m_continuous;
  }

  public void setIntegratorRange(double minimumIntegral, double maximumIntegral) {
    m_minimumIntegral = minimumIntegral;
    m_maximumIntegral = maximumIntegral;
  }

  public void setTolerance(double positionTolerance) {
    setTolerance(positionTolerance, Double.POSITIVE_INFINITY);
  }

  public void setTolerance(double positionTolerance, double velocityTolerance) {
    m_positionTolerance = positionTolerance;
    m_velocityTolerance = velocityTolerance;
  }

  public double getPositionError() {
    return m_positionError;
  }

  public double getVelocityError() {
    return m_velocityError;
  }

  public double calculate(double measurement, double setpoint) {
    m_setpoint = setpoint;
    m_haveSetpoint = true;
    return calculate(measurement);
  }

  public double calculate(double measurement) {
    m_measurement = measurement;
    m_prevError = m_positionError;
    m_haveMeasurement = true;

    if (m_continuous) {
      double errorBound = (m_maximumInput - m_minimumInput) / 2.0;
      m_positionError = MathUtil.inputModulus(m_setpoint - m_measurement, -errorBound, errorBound);
    } else {
      m_positionError = m_setpoint - m_measurement;
    }

    m_velocityError = (m_positionError - m_prevError) / m_period;

    if (Math.abs(m_positionError) > m_iZone) {
      m_totalError = 0;
    } else if (m_ki != 0) {
      m_totalError =
          MathUtil.clamp(
              m_totalError + m_positionError * m_period,
              m_minimumIntegral / m_ki,
              m_maximumIntegral / m_ki);
    }

    return m_kp * m_positionError + m_ki * m_totalError + m_kd * m_velocityError;
  }

  public void reset() {
    m_positionError = 0;
    m_prevError = 0;
    m_totalError = 0;
    m_velocityError = 0;
    m_haveMeasurement = false;
  }

  @Override
  public void close() {}
}
