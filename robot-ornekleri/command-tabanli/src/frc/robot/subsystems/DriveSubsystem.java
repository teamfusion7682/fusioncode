package frc.robot.subsystems;

import edu.wpi.first.wpilibj.AnalogGyro;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.motorcontrol.PWMSparkMax;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

/** Surus mekanizmasi: motorlar, enkoderler ve gyro tek yerde. */
public class DriveSubsystem extends SubsystemBase {
  private final PWMSparkMax leftMotor = new PWMSparkMax(0);
  private final PWMSparkMax rightMotor = new PWMSparkMax(1);
  private final DifferentialDrive drive = new DifferentialDrive(leftMotor, rightMotor);
  private final Encoder leftEncoder = new Encoder(0, 1);
  private final Encoder rightEncoder = new Encoder(2, 3);
  private final AnalogGyro gyro = new AnalogGyro(0);

  public DriveSubsystem() {
    rightMotor.setInverted(true);
    double perPulse = 2.0 * Math.PI * 0.0762 / 2048.0;
    leftEncoder.setDistancePerPulse(perPulse);
    rightEncoder.setDistancePerPulse(perPulse);
  }

  public void arcadeDrive(double forward, double rotation) {
    drive.arcadeDrive(forward, rotation, false);
  }

  public void stop() {
    drive.stopMotor();
  }

  public double getDistanceMeters() {
    return (leftEncoder.getDistance() + rightEncoder.getDistance()) / 2.0;
  }

  /** Saat yonunun tersi (sola) pozitif. */
  public double getHeadingDegrees() {
    return gyro.getRotation2d().getDegrees();
  }

  public void resetSensors() {
    leftEncoder.reset();
    rightEncoder.reset();
    gyro.reset();
  }

  @Override
  public void periodic() {
    SmartDashboard.putNumber("Mesafe (m)", getDistanceMeters());
    SmartDashboard.putNumber("Yön (derece)", getHeadingDegrees());
  }
}
