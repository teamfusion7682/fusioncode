package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;

/**
 * Command-Based robot: Robot sadece sirayi yonetir, is komutlarda ve alt sistemlerdedir.
 * Zaman cizelgesi (altta) hangi komutun ne zaman calistigini gosterir.
 */
public class Robot extends TimedRobot {
  private static final String AUTO_ROUTE = "1 - Rota (komut zinciri)";
  private static final String AUTO_TIMEOUT = "2 - Zaman asimli (withTimeout)";

  private final RobotContainer container = new RobotContainer();
  private final SendableChooser<String> chooser = new SendableChooser<>();
  private Command autoCommand;

  @Override
  public void robotInit() {
    chooser.setDefaultOption(AUTO_ROUTE, AUTO_ROUTE);
    chooser.addOption(AUTO_TIMEOUT, AUTO_TIMEOUT);
    SmartDashboard.putData("Otonom", chooser);
  }

  @Override
  public void robotPeriodic() {
    CommandScheduler.getInstance().run();
  }

  @Override
  public void autonomousInit() {
    container.resetSensors();
    autoCommand = AUTO_TIMEOUT.equals(chooser.getSelected()) ? container.timeoutAuto() : container.routeAuto();
    autoCommand.schedule();
  }

  @Override
  public void teleopInit() {
    if (autoCommand != null) autoCommand.cancel();
  }
}
