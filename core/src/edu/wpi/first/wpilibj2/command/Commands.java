package edu.wpi.first.wpilibj2.command;

import java.util.function.BooleanSupplier;

/** Komut olusturma kisayollari (WPILib Commands). */
public final class Commands {
  private Commands() {}

  public static Command none() {
    return new InstantCommand().withName("None");
  }

  public static Command runOnce(Runnable action, Subsystem... requirements) {
    return new InstantCommand(action, requirements);
  }

  public static Command run(Runnable action, Subsystem... requirements) {
    return new RunCommand(action, requirements);
  }

  public static Command startEnd(Runnable start, Runnable end, Subsystem... requirements) {
    return new StartEndCommand(start, end, requirements);
  }

  public static Command waitSeconds(double seconds) {
    return new WaitCommand(seconds);
  }

  public static Command waitUntil(BooleanSupplier condition) {
    return new WaitUntilCommand(condition);
  }

  public static Command sequence(Command... commands) {
    return new SequentialCommandGroup(commands);
  }

  public static Command parallel(Command... commands) {
    return new ParallelCommandGroup(commands);
  }

  public static Command race(Command... commands) {
    return new ParallelRaceGroup(commands);
  }

  public static Command deadline(Command deadline, Command... others) {
    return new ParallelDeadlineGroup(deadline, others);
  }
}
