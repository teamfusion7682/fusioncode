package edu.wpi.first.wpilibj2.command;

import java.util.function.BooleanSupplier;

public class WaitUntilCommand extends Command {
  private final BooleanSupplier m_condition;

  public WaitUntilCommand(BooleanSupplier condition) {
    m_condition = condition;
    setName("WaitUntilCommand");
  }

  @Override
  public boolean isFinished() {
    return m_condition.getAsBoolean();
  }

  @Override
  public boolean runsWhenDisabled() {
    return true;
  }
}
