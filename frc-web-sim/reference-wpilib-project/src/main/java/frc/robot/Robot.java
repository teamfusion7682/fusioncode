package frc.robot;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.wpilibj.DataLogManager;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * DEMO: "Zayif kod" ile "PID" otonomunu ayni robotta karsilastir.
 *
 * Hedef: robotu tam 3 metre ileri goturup durdurmak.
 *  - ZAYIF: Geri bildirim yok. Motoru sabit sure calistirir, robot hedefi asar.
 *  - PID  : Encoder okur, hedefe yaklastikca yavaslar ve durur.
 *
 * Sim GUI'de Autonomous'u secip calistirin; "Otonom/MesafeM" ve "Otonom/HedefM"
 * degerlerini AdvantageScope'ta cizdirerek yan yana grafik alabilirsiniz.
 */
public class Robot extends TimedRobot {
  private static final String AUTO_WEAK = "1 - Zayif kod (geri bildirimsiz)";
  private static final String AUTO_PID = "2 - PID (encoder ile)";

  private static final double TARGET_METERS = 3.0;
  private static final double WEAK_SPEED = 0.8;
  private static final double WEAK_SECONDS = 1.2;

  private final XboxController controller = new XboxController(0);
  private final SendableChooser<String> autoChooser = new SendableChooser<>();
  private final PIDController distancePid = new PIDController(1.2, 0.0, 0.1);
  private final Timer autoTimer = new Timer();

  private RobotConfig config;
  private Drivetrain drivetrain;
  private String selectedAuto = AUTO_WEAK;

  @Override
  public void robotInit() {
    // Log dosyasi: AdvantageScope ile acilip simdeki ve gercekteki davranis karsilastirilir
    DataLogManager.start();
    DriverStation.startDataLog(DataLogManager.getLog());

    config = RobotConfig.load();
    SmartDashboard.putString("Robot/Ad", config.robotName);
    drivetrain = new Drivetrain(config);

    distancePid.setTolerance(0.03);

    autoChooser.setDefaultOption(AUTO_WEAK, AUTO_WEAK);
    autoChooser.addOption(AUTO_PID, AUTO_PID);
    SmartDashboard.putData("Otonom/Secim", autoChooser);
  }

  @Override
  public void robotPeriodic() {
    drivetrain.periodic();
    SmartDashboard.putNumber("Otonom/MesafeM", drivetrain.getDistanceMeters());
    SmartDashboard.putNumber("Otonom/HedefM", TARGET_METERS);
  }

  @Override
  public void autonomousInit() {
    selectedAuto = autoChooser.getSelected();
    drivetrain.resetEncoders();
    distancePid.reset();
    autoTimer.restart();
  }

  @Override
  public void autonomousPeriodic() {
    if (AUTO_PID.equals(selectedAuto)) {
      double output = distancePid.calculate(drivetrain.getDistanceMeters(), TARGET_METERS);
      drivetrain.arcadeDrive(MathUtil.clamp(output, -0.8, 0.8), 0.0);
    } else {
      // Zayif kod: sure dolunca durur, nerede durdugu umurunda degil
      if (autoTimer.get() < WEAK_SECONDS) {
        drivetrain.arcadeDrive(WEAK_SPEED, 0.0);
      } else {
        drivetrain.stop();
      }
    }
  }

  @Override
  public void teleopPeriodic() {
    drivetrain.arcadeDrive(-controller.getLeftY(), -controller.getRightX());
  }

  @Override
  public void disabledInit() {
    drivetrain.stop();
  }

  @Override
  public void simulationPeriodic() {
    drivetrain.simulationUpdate();
  }
}
