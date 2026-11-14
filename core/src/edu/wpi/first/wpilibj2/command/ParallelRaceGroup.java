package edu.wpi.first.wpilibj2.command;

import java.util.LinkedHashMap;
import java.util.Map;

/** Komutlar paralel calisir; biri biter bitmez grup biter, digerleri iptal edilir. */
public class ParallelRaceGroup extends Command {
  private final Map<Command, Boolean> m_commands = new LinkedHashMap<>(); // deger: bitti mi
  private boolean m_finished;

  public ParallelRaceGroup(Command... commands) {
    for (Command c : commands) {
      for (Subsystem s : c.getRequirements()) {
        if (getRequirements().contains(s)) {
          throw new IllegalArgumentException("Paralel komutlar ayni alt sistemi (" + s.getName() + ") kullanamaz");
        }
      }
      m_commands.put(c, Boolean.FALSE);
      addRequirements(c.getRequirements().toArray(new Subsystem[0]));
    }
  }

  @Override
  public void initialize() {
    m_finished = false;
    for (Map.Entry<Command, Boolean> e : m_commands.entrySet()) {
      e.setValue(Boolean.FALSE);
      e.getKey().simStart(simDepth() + 1);
    }
  }

  @Override
  public void execute() {
    for (Map.Entry<Command, Boolean> e : m_commands.entrySet()) {
      Command c = e.getKey();
      c.execute();
      if (c.isFinished()) {
        e.setValue(Boolean.TRUE);
        m_finished = true;
      }
    }
  }

  @Override
  public void end(boolean interrupted) {
    for (Map.Entry<Command, Boolean> e : m_commands.entrySet()) e.getKey().simEnd(!e.getValue());
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
