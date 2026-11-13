package edu.wpi.first.wpilibj;

import frcsim.hal.SimHal;
import java.util.ArrayList;
import java.util.List;

public class TimedRobot extends IterativeRobotBase {
  public static final double kDefaultPeriod = 0.02;

  private static final class Callback {
    final Runnable run;
    final double period;
    double next;

    Callback(Runnable run, double period, double next) {
      this.run = run;
      this.period = period;
      this.next = next;
    }
  }

  private final List<Callback> m_callbacks = new ArrayList<>();

  protected TimedRobot() {
    this(kDefaultPeriod);
  }

  protected TimedRobot(double period) {
    super(period);
  }

  public void addPeriodic(Runnable callback, double periodSeconds) {
    addPeriodic(callback, periodSeconds, 0.0);
  }

  public void addPeriodic(Runnable callback, double periodSeconds, double offsetSeconds) {
    if (periodSeconds <= 0) throw new IllegalArgumentException("periodSeconds pozitif olmali");
    m_callbacks.add(new Callback(callback, periodSeconds, SimHal.timeSeconds + offsetSeconds));
  }

  @Override
  public void loopFunc() {
    super.loopFunc();
    double now = SimHal.timeSeconds;
    for (Callback cb : m_callbacks) {
      // Kucuk bir tolerans: kayan nokta hatasi yuzunden bir dongu atlanmasin.
      while (cb.next <= now + 1e-9) {
        cb.run.run();
        cb.next += cb.period;
      }
    }
  }

  @Override
  public void startCompetition() {
    // Simulatorde dongu simulator tarafindan yurutulur.
  }

  @Override
  public void endCompetition() {}
}
