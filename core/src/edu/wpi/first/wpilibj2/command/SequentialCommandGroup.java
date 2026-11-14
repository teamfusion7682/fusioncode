package edu.wpi.first.wpilibj2.command;

import java.util.ArrayList;
import java.util.List;

public class SequentialCommandGroup extends Command {
  private final List<Command> m_commands = new ArrayList<>();
  private int m_index = -1;

  public SequentialCommandGroup(Command... commands) {
    addCommands(commands);
  }

  public final void addCommands(Command... commands) {
    if (m_index >= 0) throw new IllegalStateException("Komut grubu calisirken komut eklenemez");
    for (Command c : commands) {
      m_commands.add(c);
      addRequirements(c.getRequirements().toArray(new Subsystem[0]));
    }
  }

  @Override
  public void initialize() {
    m_index = 0;
    if (!m_commands.isEmpty()) m_commands.get(0).simStart(simDepth() + 1);
  }

  @Override
  public void execute() {
    if (m_commands.isEmpty() || m_index >= m_commands.size()) return;
    Command current = m_commands.get(m_index);
    current.execute();
    if (current.isFinished()) {
      current.simEnd(false);
      m_index++;
      if (m_index < m_commands.size()) m_commands.get(m_index).simStart(simDepth() + 1);
    }
  }

  @Override
  public void end(boolean interrupted) {
    if (interrupted && m_index >= 0 && m_index < m_commands.size()) m_commands.get(m_index).simEnd(true);
    m_index = -1;
  }

  @Override
  public boolean isFinished() {
    return m_index >= m_commands.size();
  }

  @Override
  public boolean runsWhenDisabled() {
    for (Command c : m_commands) if (!c.runsWhenDisabled()) return false;
    return true;
  }
}
