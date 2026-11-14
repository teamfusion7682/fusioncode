package edu.wpi.first.wpilibj2.command;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class FunctionalCommand extends Command {
  private final Runnable m_onInit;
  private final Runnable m_onExecute;
  private final Consumer<Boolean> m_onEnd;
  private final BooleanSupplier m_isFinished;

  public FunctionalCommand(Runnable onInit, Runnable onExecute, Consumer<Boolean> onEnd,
      BooleanSupplier isFinished, Subsystem... requirements) {
    m_onInit = onInit;
    m_onExecute = onExecute;
    m_onEnd = onEnd;
    m_isFinished = isFinished;
    addRequirements(requirements);
  }

  @Override
  public void initialize() {
    m_onInit.run();
  }

  @Override
  public void execute() {
    m_onExecute.run();
  }

  @Override
  public void end(boolean interrupted) {
    m_onEnd.accept(interrupted);
  }

  @Override
  public boolean isFinished() {
    return m_isFinished.getAsBoolean();
  }
}
