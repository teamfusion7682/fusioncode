package edu.wpi.first.wpilibj2.command;

public class StartEndCommand extends Command {
  private final Runnable m_onInit;
  private final Runnable m_onEnd;

  public StartEndCommand(Runnable onInit, Runnable onEnd, Subsystem... requirements) {
    m_onInit = onInit;
    m_onEnd = onEnd;
    addRequirements(requirements);
  }

  @Override
  public void initialize() {
    m_onInit.run();
  }

  @Override
  public void end(boolean interrupted) {
    m_onEnd.run();
  }
}
