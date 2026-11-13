package edu.wpi.first.wpilibj;

import frcsim.hal.SimHal;

/** WPILib IterativeRobotBase: mod gecislerinde init/exit, her dongude periodic cagirir. */
public abstract class IterativeRobotBase extends RobotBase {
  private enum RunMode { NONE, DISABLED, AUTONOMOUS, TELEOP, TEST }

  private final double m_period;
  private RunMode m_lastMode = RunMode.NONE;

  protected IterativeRobotBase(double period) {
    m_period = period;
  }

  public void robotInit() {}

  public void simulationInit() {}

  public void disabledInit() {}

  public void autonomousInit() {}

  public void teleopInit() {}

  public void testInit() {}

  public void robotPeriodic() {}

  public void simulationPeriodic() {}

  public void disabledPeriodic() {}

  public void autonomousPeriodic() {}

  public void teleopPeriodic() {}

  public void testPeriodic() {}

  public void disabledExit() {}

  public void autonomousExit() {}

  public void teleopExit() {}

  public void testExit() {}

  /** Simulator icin: robot baslatma (robotInit + simulationInit). */
  public void simStart() {
    robotInit();
    simulationInit();
  }

  public final double simGetPeriod() {
    return m_period;
  }

  /** Bir robot dongusu. Simulator her adimda bir kez cagirir. */
  public void loopFunc() {
    RunMode mode = currentMode();
    if (m_lastMode != mode) {
      switch (m_lastMode) {
        case DISABLED: disabledExit(); break;
        case AUTONOMOUS: autonomousExit(); break;
        case TELEOP: teleopExit(); break;
        case TEST: testExit(); break;
        default: break;
      }
      switch (mode) {
        case DISABLED: disabledInit(); break;
        case AUTONOMOUS: autonomousInit(); break;
        case TELEOP: teleopInit(); break;
        case TEST: testInit(); break;
        default: break;
      }
      m_lastMode = mode;
    }
    switch (mode) {
      case DISABLED: disabledPeriodic(); break;
      case AUTONOMOUS: autonomousPeriodic(); break;
      case TELEOP: teleopPeriodic(); break;
      case TEST: testPeriodic(); break;
      default: break;
    }
    robotPeriodic();
    simulationPeriodic();
  }

  private static RunMode currentMode() {
    switch (SimHal.mode) {
      case AUTONOMOUS: return RunMode.AUTONOMOUS;
      case TELEOP: return RunMode.TELEOP;
      case TEST: return RunMode.TEST;
      default: return RunMode.DISABLED;
    }
  }
}
