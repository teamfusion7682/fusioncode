package frcsim.physics;

import frcsim.hal.SimHal;

/**
 * Diferansiyel (tank) surus fizigi.
 *
 * Her tarafta: motor gerilimi -> akim (geri-EMK dahil) -> tork -> tekerlek kuvveti.
 * Govde: dogrusal ve donme hareketi (kutle, atalet), yuvarlanma/donme surtunmesi,
 * cekis siniri (tekerlek kaymasi) ve akim siniri. Pozitif theta saat yonunun tersidir.
 */
public final class DrivetrainModel {
  private static final double GRAVITY = 9.80665;
  private static final double SUBSTEP = 0.001;

  private final RobotConfig c;
  private final MotorSpec motor;
  private final boolean coast;

  public double x;
  public double y;
  public double theta;
  public double vl;
  public double vr;
  public double leftDist;
  public double rightDist;
  /** Son adimda sola/saga uygulanan normalize komut (ileri pozitif). */
  public double cmdLeft;
  public double cmdRight;

  public DrivetrainModel(RobotConfig config) {
    this.c = config;
    this.motor = MotorSpec.byName(config.motor);
    this.coast = config.idleMode.equalsIgnoreCase("coast");
  }

  /** Bir robot dongusu (dt saniye) boyunca fizigi ilerletir; motor komutlari SimHal.pwm'den okunur. */
  public void step(double dt, boolean enabled) {
    cmdLeft = sideCommand(c.leftPwm, c.leftReversed, enabled);
    cmdRight = sideCommand(c.rightPwm, c.rightReversed, enabled);
    double volts = SimHal.batteryVolts;
    double voltsL = cmdLeft * volts;
    double voltsR = cmdRight * volts;
    boolean freeL = coast && Math.abs(cmdLeft) < 1e-3;
    boolean freeR = coast && Math.abs(cmdRight) < 1e-3;

    double mass = c.massKg;
    double rollForce = c.rollingFriction * mass * GRAVITY;
    double turnTorque = c.turnFriction * mass * GRAVITY * c.trackWidthMeters / 4.0;
    double tractionPerSide = c.tractionCoefficient * mass * GRAVITY / 2.0;

    int steps = Math.max(1, (int) Math.round(dt / SUBSTEP));
    double h = dt / steps;
    for (int i = 0; i < steps; i++) {
      double fl = sideForce(voltsL, vl, freeL, tractionPerSide);
      double fr = sideForce(voltsR, vr, freeR, tractionPerSide);
      double v = (vl + vr) / 2.0;
      double w = (vr - vl) / c.trackWidthMeters;

      double force = fl + fr - rollForce * Math.tanh(v / 0.01);
      double torque = (fr - fl) * c.trackWidthMeters / 2.0 - turnTorque * Math.tanh(w / 0.05);

      v += force / mass * h;
      w += torque / c.momentOfInertiaKgM2 * h;

      vl = v - w * c.trackWidthMeters / 2.0;
      vr = v + w * c.trackWidthMeters / 2.0;
      x += v * Math.cos(theta) * h;
      y += v * Math.sin(theta) * h;
      theta += w * h;
      leftDist += vl * h;
      rightDist += vr * h;
    }
  }

  /** Sensor degerlerini sanal donanima yazar (encoder sayaclari, gyro). */
  public void writeSensors() {
    for (RobotConfig.EncoderConfig e : c.encoders) {
      boolean left = e.side.equals("left");
      double pos = left ? leftDist : rightDist;
      double vel = left ? vl : vr;
      double perMeter = e.countsPerRev / (2.0 * Math.PI * c.wheelRadiusMeters);
      double sign = e.reversed ? -1.0 : 1.0;
      SimHal.setEncoder(e.a, sign * pos * perMeter, sign * vel * perMeter);
    }
    double omega = (vr - vl) / c.trackWidthMeters;
    SimHal.gyroAngleDeg[c.gyroChannel] = -Math.toDegrees(theta);
    SimHal.gyroRateDegPerSec[c.gyroChannel] = -Math.toDegrees(omega);
  }

  public double linearVelocity() {
    return (vl + vr) / 2.0;
  }

  public double angularVelocity() {
    return (vr - vl) / c.trackWidthMeters;
  }

  private double sideCommand(int[] channels, boolean reversed, boolean enabled) {
    if (!enabled) return 0.0;
    double sum = 0.0;
    for (int ch : channels) sum += SimHal.pwm[ch];
    double avg = sum / channels.length;
    return reversed ? -avg : avg;
  }

  private double sideForce(double volts, double wheelVel, boolean free, double tractionLimit) {
    if (free) return 0.0;
    double motorSpeed = wheelVel / c.wheelRadiusMeters * c.gearing;
    double current = (volts - motorSpeed / motor.kv) / motor.resistance;
    current = Math.max(-c.currentLimitAmps, Math.min(c.currentLimitAmps, current));
    double force = motor.kt * current * c.motorsPerSide * c.gearing * c.efficiency / c.wheelRadiusMeters;
    return Math.max(-tractionLimit, Math.min(tractionLimit, force));
  }
}
