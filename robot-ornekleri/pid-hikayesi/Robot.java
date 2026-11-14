package frc.robot;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.wpilibj.AnalogGyro;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.motorcontrol.PWMSparkMax;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * PID ayarının hikayesi: aynı hedef (3 m), dört farklı otonom.
 *  1) Zayif kod : geri bildirim yok, sabit sure surer -> hedefi asar
 *  2) Kotu PID  : kP cok yuksek, turev yok -> hedefin etrafinda salinir
 *  3) Ayarli PID: kP ve kD dengeli -> hedefte durur
 *  4) Rota      : 2 m ileri, gyro ile 90 derece sola don, 1 m ileri
 */
public class Robot extends TimedRobot {
  private static final String AUTO_WEAK = "1 - Zayif kod (geri bildirimsiz)";
  private static final String AUTO_BAD_PID = "2 - Kotu PID (kP cok yuksek)";
  private static final String AUTO_PID = "3 - Ayarli PID";
  private static final String AUTO_ROUTE = "4 - Rota: 2 m ileri, 90 derece don, 1 m ileri";

  private static final double TARGET_METERS = 3.0;

  private final PWMSparkMax leftMotor = new PWMSparkMax(0);
  private final PWMSparkMax rightMotor = new PWMSparkMax(1);
  private final DifferentialDrive drive = new DifferentialDrive(leftMotor, rightMotor);
  private final Encoder leftEncoder = new Encoder(0, 1);
  private final Encoder rightEncoder = new Encoder(2, 3);
  private final AnalogGyro gyro = new AnalogGyro(0);
  private final XboxController controller = new XboxController(0);
  private final SendableChooser<String> chooser = new SendableChooser<>();

  private final PIDController badPid = new PIDController(6.0, 0.0, 0.0);
  private final PIDController distancePid = new PIDController(1.2, 0.0, 0.1);
  private final PIDController turnPid = new PIDController(0.02, 0.0, 0.002);
  private final Timer autoTimer = new Timer();

  private String selected = AUTO_WEAK;
  private int step;
  private double stepStartDistance;

  @Override
  public void robotInit() {
    rightMotor.setInverted(true);
    double perPulse = 2.0 * Math.PI * 0.0762 / 2048.0;
    leftEncoder.setDistancePerPulse(perPulse);
    rightEncoder.setDistancePerPulse(perPulse);
    distancePid.setTolerance(0.03, 0.15);
    turnPid.setTolerance(2.0, 15.0);
    turnPid.enableContinuousInput(-180.0, 180.0);

    chooser.setDefaultOption(AUTO_WEAK, AUTO_WEAK);
    chooser.addOption(AUTO_BAD_PID, AUTO_BAD_PID);
    chooser.addOption(AUTO_PID, AUTO_PID);
    chooser.addOption(AUTO_ROUTE, AUTO_ROUTE);
    SmartDashboard.putData("Otonom", chooser);
  }

  private double distance() {
    return (leftEncoder.getDistance() + rightEncoder.getDistance()) / 2.0;
  }

  /** Saat yonunun tersi (sola) pozitif, arcadeDrive ile ayni isaret. */
  private double heading() {
    return gyro.getRotation2d().getDegrees();
  }

  @Override
  public void robotPeriodic() {
    SmartDashboard.putNumber("Mesafe (m)", distance());
    SmartDashboard.putNumber("Yön (derece)", heading());
  }

  @Override
  public void autonomousInit() {
    selected = chooser.getSelected();
    leftEncoder.reset();
    rightEncoder.reset();
    gyro.reset();
    badPid.reset();
    distancePid.reset();
    turnPid.reset();
    autoTimer.restart();
    step = 0;
    stepStartDistance = 0.0;
  }

  @Override
  public void autonomousPeriodic() {
    switch (selected) {
      case AUTO_BAD_PID:
        SmartDashboard.putNumber("Hedef (m)", TARGET_METERS);
        double bad = badPid.calculate(distance(), TARGET_METERS);
        drive.arcadeDrive(MathUtil.clamp(bad, -0.8, 0.8), 0.0, false);
        break;

      case AUTO_PID:
        SmartDashboard.putNumber("Hedef (m)", TARGET_METERS);
        double good = distancePid.calculate(distance(), TARGET_METERS);
        drive.arcadeDrive(MathUtil.clamp(good, -0.8, 0.8), 0.0, false);
        break;

      case AUTO_ROUTE:
        route();
        break;

      default: // AUTO_WEAK: sabit sure surer, nerede durdugu umurunda degil
        SmartDashboard.putNumber("Hedef (m)", TARGET_METERS);
        if (autoTimer.get() < 1.2) {
          drive.arcadeDrive(0.8, 0.0, false);
        } else {
          drive.stopMotor();
        }
    }
  }

  private void route() {
    switch (step) {
      case 0: // 2 m ileri
        driveStraight(2.0, 0.7);
        break;
      case 1: // gyro ile 90 derece sola don
        SmartDashboard.putNumber("Hedef yön (derece)", 90.0);
        double turn = turnPid.calculate(heading(), 90.0);
        drive.arcadeDrive(0.0, MathUtil.clamp(turn, -0.6, 0.6), false);
        if (turnPid.atSetpoint()) {
          step = 2;
          stepStartDistance = distance();
          distancePid.reset();
        }
        break;
      case 2: // 1 m ileri
        driveStraight(1.0, 0.7);
        break;
      default:
        drive.stopMotor();
    }
  }

  private void driveStraight(double meters, double maxOutput) {
    double out = distancePid.calculate(distance() - stepStartDistance, meters);
    drive.arcadeDrive(MathUtil.clamp(out, -maxOutput, maxOutput), 0.0, false);
    if (distancePid.atSetpoint()) {
      if (step == 0) {
        step = 1;
        turnPid.reset();
      } else {
        step = 3;
      }
    }
  }

  @Override
  public void teleopPeriodic() {
    drive.arcadeDrive(-controller.getLeftY(), -controller.getRightX());
  }

  @Override
  public void disabledInit() {
    drive.stopMotor();
  }
}
