package edu.wpi.first.wpilibj2.command;

public class RunCommand extends Command {
  private final Runnable m_toRun;

  public RunCommand(Runnable toRun, Subsystem... requirements) {
    m_toRun = toRun;
    addRequirements(requirements);
  }

  @Override
  public void execute() {
    m_toRun.run();
  }
}
