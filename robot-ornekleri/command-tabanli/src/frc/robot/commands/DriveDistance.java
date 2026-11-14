package frc.robot.commands;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.subsystems.DriveSubsystem;

/** PID ile verilen mesafe kadar ileri git (bulundugun yerden olculur). */
public class DriveDistance extends Command {
  private final DriveSubsystem drive;
  private final double meters;
  private final PIDController pid = new PIDController(1.2, 0.0, 0.1);
  private double start;

  public DriveDistance(DriveSubsystem drive, double meters) {
    this.drive = drive;
    this.meters = meters;
    pid.setTolerance(0.03, 0.15);
    addRequirements(drive);
    setName("DriveDistance(" + meters + " m)");
  }

  @Override
  public void initialize() {
    start = drive.getDistanceMeters();
    pid.reset();
    SmartDashboard.putNumber("Hedef (m)", start + meters);
  }

  @Override
  public void execute() {
    double out = pid.calculate(drive.getDistanceMeters() - start, meters);
    drive.arcadeDrive(MathUtil.clamp(out, -0.7, 0.7), 0.0);
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
