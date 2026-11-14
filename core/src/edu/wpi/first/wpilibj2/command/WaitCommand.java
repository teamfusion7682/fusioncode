package edu.wpi.first.wpilibj2.command;

import edu.wpi.first.wpilibj.Timer;

public class WaitCommand extends Command {
  private final Timer m_timer = new Timer();
  private final double m_duration;

  public WaitCommand(double seconds) {
    m_duration = seconds;
    setName("WaitCommand(" + seconds + " s)");
  }

  @Override
  public void initialize() {
    m_timer.restart();
  }

  @Override
  public void end(boolean interrupted) {
    m_timer.stop();
  }

  @Override
  public boolean isFinished() {
    return m_timer.hasElapsed(m_duration);
  }

  @Override
  public boolean runsWhenDisabled() {
    return true;
  }
}
