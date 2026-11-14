package frc.robot;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.commands.DriveDistance;
import frc.robot.commands.TurnToAngle;
import frc.robot.subsystems.DriveSubsystem;

/** Robotun tum parcalari ve komut baglantilari burada toplanir. */
public class RobotContainer {
  private final DriveSubsystem drive = new DriveSubsystem();
  private final CommandXboxController controller = new CommandXboxController(0);

  public RobotContainer() {
    // Teleop: baska komut calismiyorken joystick ile sur.
    drive.setDefaultCommand(
        drive.run(() -> drive.arcadeDrive(-controller.getLeftY(), -controller.getRightX())));

    // A tusuna basilinca olan her sey durur (komutu iptal eder).
    controller.a().onTrue(Commands.runOnce(drive::stop, drive));
  }

  public void resetSensors() {
    drive.resetSensors();
  }

  /** 2 m ileri, 90 derece sola don, 1 m ileri. */
  public Command routeAuto() {
    return new DriveDistance(drive, 2.0)
        .andThen(new TurnToAngle(drive, 90.0), new DriveDistance(drive, 1.0))
        .withName("Rota");
  }

  /** 5 m istiyoruz ama 1,5 saniye sonra zorla kes: zaman asimi (withTimeout) ornegi. */
  public Command timeoutAuto() {
    return new DriveDistance(drive, 5.0).withTimeout(1.5).withName("ZamanAsimli");
  }
}
