package frc.robot.commands;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.subsystems.DriveSubsystem;

/** Gyro ile verilen yone don (derece, sola pozitif). */
public class TurnToAngle extends Command {
  private final DriveSubsystem drive;
  private final double degrees;
  private final PIDController pid = new PIDController(0.02, 0.0, 0.002);

  public TurnToAngle(DriveSubsystem drive, double degrees) {
    this.drive = drive;
    this.degrees = degrees;
    pid.setTolerance(2.0, 15.0);
    pid.enableContinuousInput(-180.0, 180.0);
    addRequirements(drive);
    setName("TurnToAngle(" + degrees + " derece)");
  }

  @Override
  public void initialize() {
    pid.reset();
    SmartDashboard.putNumber("Hedef yön (derece)", degrees);
  }

  @Override
  public void execute() {
    double out = pid.calculate(drive.getHeadingDegrees(), degrees);
    drive.arcadeDrive(0.0, MathUtil.clamp(out, -0.6, 0.6));
  }

  @Override
  public boolean isFinished() {
    return pid.atSetpoint();
  }

  @Override
  public void end(boolean interrupted) {
    drive.stop();
  }
}
