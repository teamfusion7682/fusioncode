package edu.wpi.first.wpilibj2.command;

import java.util.LinkedHashMap;
import java.util.Map;

/** Ilk komut (deadline) bitince grup biter; digerleri o ana kadar calisir. */
public class ParallelDeadlineGroup extends Command {
  private final Map<Command, Boolean> m_commands = new LinkedHashMap<>(); // deger: calisiyor mu
  private final Command m_deadline;
  private boolean m_finished;

  public ParallelDeadlineGroup(Command deadline, Command... others) {
    m_deadline = deadline;
    add(deadline);
    for (Command c : others) add(c);
  }

  private void add(Command c) {
    for (Subsystem s : c.getRequirements()) {
      if (getRequirements().contains(s)) {
        throw new IllegalArgumentException("Paralel komutlar ayni alt sistemi (" + s.getName() + ") kullanamaz");
      }
    }
    m_commands.put(c, Boolean.FALSE);
    addRequirements(c.getRequirements().toArray(new Subsystem[0]));
  }

  @Override
  public void initialize() {
    m_finished = false;
    for (Map.Entry<Command, Boolean> e : m_commands.entrySet()) {
      e.getKey().simStart(simDepth() + 1);
      e.setValue(Boolean.TRUE);
    }
  }

  @Override
  public void execute() {
    for (Map.Entry<Command, Boolean> e : m_commands.entrySet()) {
      if (!e.getValue()) continue;
      Command c = e.getKey();
      c.execute();
      if (c.isFinished()) {
        c.simEnd(false);
        e.setValue(Boolean.FALSE);
        if (c == m_deadline) m_finished = true;
      }
    }
  }

  @Override
  public void end(boolean interrupted) {
    for (Map.Entry<Command, Boolean> e : m_commands.entrySet()) {
      if (e.getValue()) e.getKey().simEnd(true);
      e.setValue(Boolean.FALSE);
    }
  }

  @Override
  public boolean isFinished() {
    return m_finished;
  }

  @Override
  public boolean runsWhenDisabled() {
    for (Command c : m_commands.keySet()) if (!c.runsWhenDisabled()) return false;
    return true;
  }
}
