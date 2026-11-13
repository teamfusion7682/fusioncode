package frc.robot;

import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.kinematics.DifferentialDriveOdometry;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.wpilibj.AnalogGyro;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.motorcontrol.PWMSparkMax;
import edu.wpi.first.wpilibj.simulation.AnalogGyroSim;
import edu.wpi.first.wpilibj.simulation.DifferentialDrivetrainSim;
import edu.wpi.first.wpilibj.simulation.EncoderSim;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * Tank surus alt sistemi.
 *
 * ONEMLI: Robot kodu (surus, sensor okuma) simulasyonla ayni. Gercek robotta calisirken
 * sadece simulationUpdate() cagrilmaz; geri kalan her sey ayni kalir.
 * Gercek robotta farkli motor surucusu kullaniyorsaniz sadece motor siniflarini
 * (PWMSparkMax) degistirmeniz yeterli.
 */
public class Drivetrain {
  private static final double LOOP_SECONDS = 0.02;
  private static final int ENCODER_CPR = 4096;

  private final PWMSparkMax leftMotor;
  private final PWMSparkMax rightMotor;
  private final Encoder leftEncoder;
  private final Encoder rightEncoder;
  private final AnalogGyro gyro;
  private final DifferentialDrive drive;
  private final DifferentialDriveOdometry odometry;
  private final Field2d field = new Field2d();

  // Simulasyon nesneleri
  private final EncoderSim leftEncoderSim;
  private final EncoderSim rightEncoderSim;
  private final AnalogGyroSim gyroSim;
  private final DifferentialDrivetrainSim driveSim;

  public Drivetrain(RobotConfig config) {
    RobotConfig.PortsConfig ports = config.ports;
    RobotConfig.DrivetrainConfig dt = config.drivetrain;

    leftMotor = new PWMSparkMax(ports.leftMotorPwm);
    rightMotor = new PWMSparkMax(ports.rightMotorPwm);
    rightMotor.setInverted(true);
    drive = new DifferentialDrive(leftMotor, rightMotor);

    leftEncoder = new Encoder(ports.leftEncoder[0], ports.leftEncoder[1]);
    rightEncoder = new Encoder(ports.rightEncoder[0], ports.rightEncoder[1]);
    double distancePerPulse = 2.0 * Math.PI * dt.wheelRadiusMeters / ENCODER_CPR;
    leftEncoder.setDistancePerPulse(distancePerPulse);
    rightEncoder.setDistancePerPulse(distancePerPulse);

    gyro = new AnalogGyro(ports.gyroAnalog);

    odometry =
        new DifferentialDriveOdometry(
            gyro.getRotation2d(), leftEncoder.getDistance(), rightEncoder.getDistance());

    // Simulasyon: parametreler robot-config.json'dan gelir
    driveSim =
        new DifferentialDrivetrainSim(
            motorFromName(dt.motor, dt.motorsPerSide),
            dt.gearing,
            dt.momentOfInertiaKgM2,
            dt.massKg,
            dt.wheelRadiusMeters,
            dt.trackWidthMeters,
            null);
    leftEncoderSim = new EncoderSim(leftEncoder);
    rightEncoderSim = new EncoderSim(rightEncoder);
    gyroSim = new AnalogGyroSim(gyro);

    SmartDashboard.putData("Saha", field);
  }

  private static DCMotor motorFromName(String name, int count) {
    switch (name) {
      case "CIM":
        return DCMotor.getCIM(count);
      case "MiniCIM":
        return DCMotor.getMiniCIM(count);
      case "Falcon500":
        return DCMotor.getFalcon500(count);
      case "NEO":
      default:
        return DCMotor.getNEO(count);
    }
  }

  // ---- Gercek robot ve simulasyonda ortak kisim ----

  public void arcadeDrive(double speed, double rotation) {
    drive.arcadeDrive(speed, rotation);
  }

  public void stop() {
    drive.stopMotor();
  }

  public double getDistanceMeters() {
    return (leftEncoder.getDistance() + rightEncoder.getDistance()) / 2.0;
  }

  public void resetEncoders() {
    leftEncoder.reset();
    rightEncoder.reset();
  }

  public Pose2d getPose() {
    return odometry.getPoseMeters();
  }

  /** Her dongude Robot.robotPeriodic() icinden cagrilir. */
  public void periodic() {
    odometry.update(
        gyro.getRotation2d(), leftEncoder.getDistance(), rightEncoder.getDistance());
  }

  // ---- Sadece simulasyonda calisan kisim ----

  /** Robot.simulationPeriodic() icinden cagrilir; gercek robotta hic cagrilmaz. */
  public void simulationUpdate() {
    double batteryVolts = RobotController.getBatteryVoltage();
    driveSim.setInputs(leftMotor.get() * batteryVolts, rightMotor.get() * batteryVolts);
    driveSim.update(LOOP_SECONDS);

    leftEncoderSim.setDistance(driveSim.getLeftPositionMeters());
    leftEncoderSim.setRate(driveSim.getLeftVelocityMetersPerSecond());
    rightEncoderSim.setDistance(driveSim.getRightPositionMeters());
    rightEncoderSim.setRate(driveSim.getRightVelocityMetersPerSecond());
    gyroSim.setAngle(-driveSim.getHeading().getDegrees());

    field.setRobotPose(driveSim.getPose());
  }
}
