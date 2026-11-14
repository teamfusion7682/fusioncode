package edu.wpi.first.wpilibj2.command.button;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import java.util.function.BooleanSupplier;

/** Bir kosul (ornegin dugme) ile komut baglama. CommandScheduler.run() her cagrildiginda yoklanir. */
public class Trigger implements BooleanSupplier {
  private final BooleanSupplier m_condition;

  public Trigger(BooleanSupplier condition) {
    m_condition = condition;
  }

  @Override
  public boolean getAsBoolean() {
    return m_condition.getAsBoolean();
  }

  private interface Edge {
    void on(boolean previous, boolean current);
  }

  private Trigger bind(Edge edge) {
    final boolean[] prev = {m_condition.getAsBoolean()};
    CommandScheduler.getInstance().bindEvent(() -> {
      boolean cur = m_condition.getAsBoolean();
      edge.on(prev[0], cur);
      prev[0] = cur;
    });
    return this;
  }

  public Trigger onTrue(Command command) {
    return bind((p, c) -> { if (!p && c) command.schedule(); });
  }

  public Trigger onFalse(Command command) {
    return bind((p, c) -> { if (p && !c) command.schedule(); });
  }

  public Trigger whileTrue(Command command) {
    return bind((p, c) -> {
      if (!p && c) command.schedule();
      else if (p && !c) command.cancel();
    });
  }

  public Trigger whileFalse(Command command) {
    return bind((p, c) -> {
      if (p && !c) command.schedule();
      else if (!p && c) command.cancel();
    });
  }

  public Trigger toggleOnTrue(Command command) {
    return bind((p, c) -> {
      if (!p && c) {
        if (command.isScheduled()) command.cancel(); else command.schedule();
      }
    });
  }

  public Trigger and(BooleanSupplier other) {
    return new Trigger(() -> m_condition.getAsBoolean() && other.getAsBoolean());
  }

  public Trigger or(BooleanSupplier other) {
    return new Trigger(() -> m_condition.getAsBoolean() || other.getAsBoolean());
  }

  public Trigger negate() {
    return new Trigger(() -> !m_condition.getAsBoolean());
  }
}
