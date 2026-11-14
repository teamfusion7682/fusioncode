package edu.wpi.first.wpilibj2.command;

public class InstantCommand extends Command {
  private final Runnable m_toRun;

  public InstantCommand(Runnable toRun, Subsystem... requirements) {
    m_toRun = toRun;
    addRequirements(requirements);
  }

  public InstantCommand() {
    this(() -> {});
  }

  @Override
  public void initialize() {
    m_toRun.run();
  }

  @Override
  public final boolean isFinished() {
    return true;
  }
}
