package edu.wpi.first.wpilibj;

import frcsim.hal.SimHal;
import java.util.function.Supplier;

/**
 * Simulatorde Main.main() -> RobotBase.startRobot(Robot::new) cagrisi robot fabrikasini kaydeder;
 * dongu simulator tarafindan yurutulur (gercek WPILib'de burada sonsuz dongu baslar).
 */
public abstract class RobotBase implements AutoCloseable {
  private static Supplier<? extends RobotBase> factory;

  public static <T extends RobotBase> void startRobot(Supplier<T> robotSupplier) {
    factory = robotSupplier;
  }

  /** Yalnizca simulator calistiricisi icin. */
  public static Supplier<? extends RobotBase> simTakeFactory() {
    Supplier<? extends RobotBase> f = factory;
    factory = null;
    return f;
  }

  public static boolean isReal() {
    return false;
  }

  public static boolean isSimulation() {
    return true;
  }

  public boolean isEnabled() {
    return SimHal.isEnabled();
  }

  public boolean isDisabled() {
    return !SimHal.isEnabled();
  }

  public boolean isAutonomous() {
    return SimHal.mode == SimHal.Mode.AUTONOMOUS;
  }

  public boolean isAutonomousEnabled() {
    return isAutonomous();
  }

  public boolean isTeleop() {
    return SimHal.mode == SimHal.Mode.TELEOP;
  }

  public boolean isTeleopEnabled() {
    return isTeleop();
  }

  public boolean isTest() {
    return SimHal.mode == SimHal.Mode.TEST;
  }

  public abstract void startCompetition();

  public abstract void endCompetition();

  @Override
  public void close() {}
}
