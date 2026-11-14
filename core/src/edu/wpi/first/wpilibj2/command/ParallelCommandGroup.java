package edu.wpi.first.wpilibj2.command;

import java.util.LinkedHashMap;
import java.util.Map;

public class ParallelCommandGroup extends Command {
  private final Map<Command, Boolean> m_commands = new LinkedHashMap<>();

  public ParallelCommandGroup(Command... commands) {
    addCommands(commands);
  }

  public final void addCommands(Command... commands) {
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
    for (boolean running : m_commands.values()) if (running) return false;
    return true;
  }

  @Override
  public boolean runsWhenDisabled() {
    for (Command c : m_commands.keySet()) if (!c.runsWhenDisabled()) return false;
    return true;
  }
}
