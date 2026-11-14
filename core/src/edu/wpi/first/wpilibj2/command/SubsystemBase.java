package edu.wpi.first.wpilibj2.command;

public abstract class SubsystemBase implements Subsystem {
  private String m_name;

  public SubsystemBase() {
    CommandScheduler.getInstance().registerSubsystem(this);
  }

  @Override
  public String getName() {
    return m_name != null ? m_name : getClass().getSimpleName();
  }

  public void setName(String name) {
    m_name = name;
  }
}
