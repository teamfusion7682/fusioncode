package edu.wpi.first.wpilibj2.command;

import frcsim.hal.CommandEvent;
import frcsim.hal.SimHal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

/** WPILib Command: initialize / execute / isFinished / end yasam dongusu. */
public abstract class Command {
  private final Set<Subsystem> m_requirements = new LinkedHashSet<>();
  private String m_name;
  private CommandEvent m_event;
  private int m_depth;

  public void initialize() {}

  public void execute() {}

  public boolean isFinished() {
    return false;
  }

  public void end(boolean interrupted) {}

  public final void addRequirements(Subsystem... requirements) {
    for (Subsystem s : requirements) {
      if (s == null) throw new NullPointerException("Gereksinim null olamaz");
      m_requirements.add(s);
    }
  }

  public Set<Subsystem> getRequirements() {
    return Collections.unmodifiableSet(m_requirements);
  }

  public boolean hasRequirement(Subsystem requirement) {
    return m_requirements.contains(requirement);
  }

  public boolean runsWhenDisabled() {
    return false;
  }

  public String getName() {
    if (m_name != null) return m_name;
    String n = getClass().getSimpleName();
    return n.isEmpty() ? "Command" : n;
  }

  public void setName(String name) {
    m_name = name;
  }

  public Command withName(String name) {
    m_name = name;
    return this;
  }

  public void schedule() {
    CommandScheduler.getInstance().schedule(this);
  }

  public void cancel() {
    CommandScheduler.getInstance().cancel(this);
  }

  public boolean isScheduled() {
    return CommandScheduler.getInstance().isScheduled(this);
  }

  public Command withTimeout(double seconds) {
    return raceWith(new WaitCommand(seconds));
  }

  public Command until(BooleanSupplier condition) {
    return raceWith(new WaitUntilCommand(condition));
  }

  public Command andThen(Command... next) {
    List<Command> all = new ArrayList<>();
    all.add(this);
    Collections.addAll(all, next);
    return new SequentialCommandGroup(all.toArray(new Command[0]));
  }

  public Command beforeStarting(Command before) {
    return new SequentialCommandGroup(before, this);
  }

  public Command alongWith(Command... parallel) {
    List<Command> all = new ArrayList<>();
    all.add(this);
    Collections.addAll(all, parallel);
    return new ParallelCommandGroup(all.toArray(new Command[0]));
  }

  public Command raceWith(Command... parallel) {
    List<Command> all = new ArrayList<>();
    all.add(this);
    Collections.addAll(all, parallel);
    return new ParallelRaceGroup(all.toArray(new Command[0]));
  }

  public Command deadlineWith(Command... parallel) {
    return new ParallelDeadlineGroup(this, parallel);
  }

  // ---- simulator ici: zaman cizelgesi kaydi (kullanici kodu cagirmaz) ----

  final void simStart(int depth) {
    m_depth = depth;
    m_event = SimHal.commandStart(getName(), depth);
    initialize();
  }

  final void simEnd(boolean interrupted) {
    try {
      end(interrupted);
    } finally {
      SimHal.commandEnd(m_event, interrupted);
      m_event = null;
    }
  }

  final int simDepth() {
    return m_depth;
  }
}
