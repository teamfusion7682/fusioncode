package edu.wpi.first.wpilibj.drive;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.wpilibj.motorcontrol.MotorController;

/** WPILib DifferentialDrive ile ayni arcade/tank/curvature algoritmalari. */
public class DifferentialDrive implements AutoCloseable {
  private final MotorController m_left;
  private final MotorController m_right;
  private double m_deadband = 0.02;
  private double m_maxOutput = 1.0;

  public DifferentialDrive(MotorController leftMotor, MotorController rightMotor) {
    m_left = leftMotor;
    m_right = rightMotor;
  }

  public void arcadeDrive(double xSpeed, double zRotation) {
    arcadeDrive(xSpeed, zRotation, true);
  }

  /** xSpeed: ileri pozitif. zRotation: saat yonunun tersi (sola) pozitif. */
  public void arcadeDrive(double xSpeed, double zRotation, boolean squareInputs) {
    xSpeed = MathUtil.applyDeadband(MathUtil.clamp(xSpeed, -1.0, 1.0), m_deadband);
    zRotation = MathUtil.applyDeadband(MathUtil.clamp(zRotation, -1.0, 1.0), m_deadband);

    if (squareInputs) {
      xSpeed = Math.copySign(xSpeed * xSpeed, xSpeed);
      zRotation = Math.copySign(zRotation * zRotation, zRotation);
    }
    double left = xSpeed - zRotation;
    double right = xSpeed + zRotation;
    double[] s = desaturate(left, right);
    m_left.set(s[0] * m_maxOutput);
    m_right.set(s[1] * m_maxOutput);
  }

  public void curvatureDrive(double xSpeed, double zRotation, boolean allowTurnInPlace) {
    xSpeed = MathUtil.applyDeadband(MathUtil.clamp(xSpeed, -1.0, 1.0), m_deadband);
    zRotation = MathUtil.applyDeadband(MathUtil.clamp(zRotation, -1.0, 1.0), m_deadband);
    double left;
    double right;
    if (allowTurnInPlace) {
      left = xSpeed - zRotation;
      right = xSpeed + zRotation;
    } else {
      left = xSpeed - Math.abs(xSpeed) * zRotation;
      right = xSpeed + Math.abs(xSpeed) * zRotation;
    }
    double[] s = desaturate(left, right);
    m_left.set(s[0] * m_maxOutput);
    m_right.set(s[1] * m_maxOutput);
  }

  public void tankDrive(double leftSpeed, double rightSpeed) {
    tankDrive(leftSpeed, rightSpeed, true);
  }

  public void tankDrive(double leftSpeed, double rightSpeed, boolean squareInputs) {
    leftSpeed = MathUtil.applyDeadband(MathUtil.clamp(leftSpeed, -1.0, 1.0), m_deadband);
    rightSpeed = MathUtil.applyDeadband(MathUtil.clamp(rightSpeed, -1.0, 1.0), m_deadband);
    if (squareInputs) {
      leftSpeed = Math.copySign(leftSpeed * leftSpeed, leftSpeed);
      rightSpeed = Math.copySign(rightSpeed * rightSpeed, rightSpeed);
    }
    m_left.set(leftSpeed * m_maxOutput);
    m_right.set(rightSpeed * m_maxOutput);
  }

  public void setDeadband(double deadband) {
    m_deadband = deadband;
  }

  public void setMaxOutput(double maxOutput) {
    m_maxOutput = maxOutput;
  }

  public void stopMotor() {
    m_left.stopMotor();
    m_right.stopMotor();
  }

  public void setSafetyEnabled(boolean enabled) {}

  public void setExpiration(double expirationTime) {}

  public void feed() {}

  @Override
  public void close() {}

  private static double[] desaturate(double left, double right) {
    double max = Math.max(Math.abs(left), Math.abs(right));
    if (max > 1.0) {
      left /= max;
      right /= max;
    }
    return new double[] {left, right};
  }
}
