package edu.wpi.first.wpilibj2.command;

public interface Subsystem {
  default void periodic() {}

  default void simulationPeriodic() {}

  default void register() {
    CommandScheduler.getInstance().registerSubsystem(this);
  }

  default void setDefaultCommand(Command defaultCommand) {
    CommandScheduler.getInstance().setDefaultCommand(this, defaultCommand);
  }

  default Command getDefaultCommand() {
    return CommandScheduler.getInstance().getDefaultCommand(this);
  }

  default Command getCurrentCommand() {
    return CommandScheduler.getInstance().requiring(this);
  }

  default Command runOnce(Runnable action) {
    return Commands.runOnce(action, this);
  }

  default Command run(Runnable action) {
    return Commands.run(action, this);
  }

  default Command startEnd(Runnable start, Runnable end) {
    return Commands.startEnd(start, end, this);
  }

  default String getName() {
    return getClass().getSimpleName();
  }
}
