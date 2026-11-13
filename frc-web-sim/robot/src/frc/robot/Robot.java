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
 * Normal bir FRC takiminin yazacagi tarzda robot kodu (simulatore ozel hicbir sey yok).
 * Hedef: 3 metre ileri gidip durmak.
 */
public class Robot extends TimedRobot {
  private static final String AUTO_WEAK = "1 - Zayif kod (geri bildirimsiz)";
  private static final String AUTO_PID = "2 - PID (encoder ile)";
  private static final double TARGET_METERS = 3.0;

  private final PWMSparkMax leftMotor = new PWMSparkMax(0);
  private final PWMSparkMax rightMotor = new PWMSparkMax(1);
  private final DifferentialDrive drive = new DifferentialDrive(leftMotor, rightMotor);
  private final Encoder leftEncoder = new Encoder(0, 1);
  private final Encoder rightEncoder = new Encoder(2, 3);
  private final AnalogGyro gyro = new AnalogGyro(0);
  private final XboxController controller = new XboxController(0);
  private final SendableChooser<String> chooser = new SendableChooser<>();
  private final PIDController distancePid = new PIDController(1.2, 0.0, 0.1);
  private final Timer autoTimer = new Timer();
  private String selected = AUTO_WEAK;

  @Override
  public void robotInit() {
    rightMotor.setInverted(true);
    double perPulse = 2.0 * Math.PI * 0.0762 / 2048.0;
    leftEncoder.setDistancePerPulse(perPulse);
    rightEncoder.setDistancePerPulse(perPulse);
    distancePid.setTolerance(0.03);

    chooser.setDefaultOption(AUTO_WEAK, AUTO_WEAK);
    chooser.addOption(AUTO_PID, AUTO_PID);
    SmartDashboard.putData("Otonom", chooser);
  }

  private double distance() {
    return (leftEncoder.getDistance() + rightEncoder.getDistance()) / 2.0;
  }

  @Override
  public void robotPeriodic() {
    SmartDashboard.putNumber("Mesafe (m)", distance());
    SmartDashboard.putNumber("Hedef (m)", TARGET_METERS);
    SmartDashboard.putNumber("Gyro (derece)", gyro.getAngle());
  }

  @Override
  public void autonomousInit() {
    selected = chooser.getSelected();
    leftEncoder.reset();
    rightEncoder.reset();
    distancePid.reset();
    autoTimer.restart();
  }

  @Override
  public void autonomousPeriodic() {
    if (AUTO_PID.equals(selected)) {
      double out = distancePid.calculate(distance(), TARGET_METERS);
      drive.arcadeDrive(MathUtil.clamp(out, -0.8, 0.8), 0.0, false);
    } else {
      // Zayif kod: sabit sure surer, nerede durdugu umurunda degil
      if (autoTimer.get() < 1.2) {
        drive.arcadeDrive(0.8, 0.0, false);
      } else {
        drive.stopMotor();
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
