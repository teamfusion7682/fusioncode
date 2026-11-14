package edu.wpi.first.wpilibj2.command;

import edu.wpi.first.wpilibj.DriverStation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * WPILib CommandScheduler. robotPeriodic() icinde CommandScheduler.getInstance().run() cagirin.
 * Sira: alt sistem periodic -> tetikleyiciler -> komut execute/isFinished -> varsayilan komutlar.
 */
public final class CommandScheduler {
  private static CommandScheduler instance;

  public static synchronized CommandScheduler getInstance() {
    if (instance == null) instance = new CommandScheduler();
    return instance;
  }

  private final Map<Command, Boolean> m_scheduled = new LinkedHashMap<>();
  private final Map<Subsystem, Command> m_requirements = new LinkedHashMap<>();
  private final Map<Subsystem, Command> m_subsystems = new LinkedHashMap<>();
  private final List<Runnable> m_bindings = new ArrayList<>();

  private CommandScheduler() {}

  public void registerSubsystem(Subsystem... subsystems) {
    for (Subsystem s : subsystems) {
      if (!m_subsystems.containsKey(s)) m_subsystems.put(s, null);
    }
  }

  public void setDefaultCommand(Subsystem subsystem, Command command) {
    if (command == null) throw new IllegalArgumentException("Varsayilan komut null olamaz");
    if (!command.getRequirements().contains(subsystem)) {
      throw new IllegalArgumentException("Varsayilan komut, alt sistemi addRequirements ile gereksinim olarak eklemeli");
    }
    registerSubsystem(subsystem);
    m_subsystems.put(subsystem, command);
  }

  public Command getDefaultCommand(Subsystem subsystem) {
    return m_subsystems.get(subsystem);
  }

  public Command requiring(Subsystem subsystem) {
    return m_requirements.get(subsystem);
  }

  public void schedule(Command... commands) {
    for (Command c : commands) scheduleOne(c);
  }

  private void scheduleOne(Command command) {
    if (command == null) throw new IllegalArgumentException("Komut null olamaz");
    if (m_scheduled.containsKey(command)) return;
    if (DriverStation.isDisabled() && !command.runsWhenDisabled()) return;
    for (Subsystem r : command.getRequirements()) {
      Command current = m_requirements.get(r);
      if (current != null) cancelOne(current);
    }
    m_scheduled.put(command, Boolean.TRUE);
    for (Subsystem r : command.getRequirements()) m_requirements.put(r, command);
    command.simStart(0);
  }

  public void run() {
    for (Subsystem s : new ArrayList<>(m_subsystems.keySet())) {
      s.periodic();
      s.simulationPeriodic();
    }
    for (Runnable b : new ArrayList<>(m_bindings)) b.run();

    for (Command c : new ArrayList<>(m_scheduled.keySet())) {
      if (!m_scheduled.containsKey(c)) continue;
      if (DriverStation.isDisabled() && !c.runsWhenDisabled()) {
        cancelOne(c);
        continue;
      }
      c.execute();
      if (c.isFinished()) finish(c);
    }

    for (Map.Entry<Subsystem, Command> e : new ArrayList<>(m_subsystems.entrySet())) {
      Command def = e.getValue();
      if (def != null && !m_requirements.containsKey(e.getKey())) scheduleOne(def);
    }
  }

  private void release(Command c) {
    m_scheduled.remove(c);
    for (Subsystem r : c.getRequirements()) {
      if (m_requirements.get(r) == c) m_requirements.remove(r);
    }
  }

  private void finish(Command c) {
    release(c);
    c.simEnd(false);
  }

  private void cancelOne(Command c) {
    if (!m_scheduled.containsKey(c)) return;
    release(c);
    c.simEnd(true);
  }

  public void cancel(Command... commands) {
    for (Command c : commands) cancelOne(c);
  }

  public void cancelAll() {
    for (Command c : new ArrayList<>(m_scheduled.keySet())) cancelOne(c);
  }

  public boolean isScheduled(Command... commands) {
    for (Command c : commands) if (!m_scheduled.containsKey(c)) return false;
    return commands.length > 0;
  }

  /** Simulator: tetikleyici (Trigger) baglantilari her run() basinda yoklanir. */
  public void bindEvent(Runnable poll) {
    m_bindings.add(poll);
  }
}
