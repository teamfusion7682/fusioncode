package frcsim.physics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Fircasiz DC motor modeli sabitleri (WPILib DCMotor ile ayni formuller, 12 V nominal). */
public final class MotorSpec {
  public final String name;
  /** Tork sabiti (N*m / A). */
  public final double kt;
  /** Sargi direnci (ohm). */
  public final double resistance;
  /** Hiz sabiti (rad/s / V). */
  public final double kv;

  private MotorSpec(String name, double stallTorqueNm, double stallAmps, double freeAmps, double freeRpm) {
    this.name = name;
    this.kt = stallTorqueNm / stallAmps;
    this.resistance = 12.0 / stallAmps;
    double freeSpeed = freeRpm * 2.0 * Math.PI / 60.0;
    this.kv = freeSpeed / (12.0 - resistance * freeAmps);
  }

  private static final Map<String, MotorSpec> ALL = new LinkedHashMap<>();

  private static void add(MotorSpec m) {
    ALL.put(m.name.toLowerCase(Locale.ROOT), m);
  }

  static {
    add(new MotorSpec("NEO", 2.6, 105, 1.8, 5676));
    add(new MotorSpec("NEO550", 0.97, 100, 1.4, 11000));
    add(new MotorSpec("Vortex", 3.6, 211, 3.6, 6784));
    add(new MotorSpec("CIM", 2.42, 133, 2.7, 5310));
    add(new MotorSpec("MiniCIM", 1.41, 89, 3.0, 5840));
    add(new MotorSpec("Falcon500", 4.69, 257, 1.5, 6380));
    add(new MotorSpec("KrakenX60", 7.09, 366, 2.0, 6000));
  }

  public static MotorSpec byName(String name) {
    MotorSpec m = name == null ? null : ALL.get(name.toLowerCase(Locale.ROOT));
    if (m == null) {
      throw new IllegalArgumentException("Bilinmeyen motor '" + name + "'. Gecerli motorlar: " + names());
    }
    return m;
  }

  public static List<String> names() {
    List<String> r = new ArrayList<>();
    for (MotorSpec m : ALL.values()) r.add(m.name);
    return r;
  }
}
