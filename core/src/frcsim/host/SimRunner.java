package frcsim.host;

import edu.wpi.first.wpilibj.IterativeRobotBase;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.TimedRobot;
import frcsim.hal.ChooserView;
import frcsim.hal.SimHal;
import frcsim.physics.DrivetrainModel;
import frcsim.physics.RobotConfig;
import frcsim.util.Json;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.function.Supplier;

/**
 * Tek bir robot kodu calistirmasini yurutur: kodu yukler, sabit adimli (varsayilan 20 ms)
 * simulasyon dongusunu surer, kareleri ve SmartDashboard verilerini result.json'a yazar.
 * Her calistirma ayri bir JVM surecidir (izolasyon + zaman asimi icin).
 *
 * Kullanim: java -cp frcsim-core.jar frcsim.host.SimRunner job.json
 */
public final class SimRunner {
  private static final double LEAD_SECONDS = 0.1;
  private static final double TAIL_SECONDS = 0.3;

  private SimRunner() {}

  public static void main(String[] args) throws Exception {
    Path jobFile = Paths.get(args[0]);
    Map<String, Object> job = Json.parseObject(new String(Files.readAllBytes(jobFile), StandardCharsets.UTF_8));
    Path resultFile = Paths.get(Json.str(job, "resultFile", "result.json"));

    Map<String, Object> result = new LinkedHashMap<>();
    try {
      run(job, result);
    } catch (Throwable t) {
      result.put("ok", false);
      result.put("stage", "run");
      result.put("error", t.toString());
    }
    Files.write(resultFile, Json.stringify(result).getBytes(StandardCharsets.UTF_8));
    System.out.flush();
    System.err.flush();
    System.exit(0);
  }

  private static void run(Map<String, Object> job, Map<String, Object> result) throws Exception {
    RobotConfig config;
    try {
      config = RobotConfig.fromMap(Json.obj(job, "config"));
    } catch (IllegalArgumentException e) {
      result.put("ok", false);
      result.put("stage", "config");
      result.put("error", "robot-config.json hatali: " + e.getMessage());
      return;
    }

    SimHal.reset();
    Map<String, Object> chooser = Json.obj(job, "chooser");
    if (chooser != null) {
      for (Map.Entry<String, Object> e : chooser.entrySet()) {
        if (e.getValue() instanceof String) SimHal.chooserSelection.put(e.getKey(), (String) e.getValue());
      }
    }

    Path classesDir = Paths.get(Json.str(job, "classesDir", "classes")).toAbsolutePath().normalize();
    URLClassLoader loader = new URLClassLoader(new URL[] {classesDir.toUri().toURL()}, SimRunner.class.getClassLoader());

    // Takim kodu yuklenmeden once kisitlari devreye al.
    List<String> roots = new ArrayList<>();
    roots.add(Paths.get(System.getProperty("java.home")).toAbsolutePath().normalize().toString());
    roots.add(classesDir.toString());
    try {
      Path self = Paths.get(SimRunner.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      roots.add(self.toAbsolutePath().normalize().toString());
    } catch (Exception ignored) {
      // jar yolu bulunamazsa yalnizca diger kokler kullanilir
    }
    Guard guard = new Guard(loader, roots);
    guard.warmUp();
    installGuard(guard);

    String phase = "robot yuklenirken";
    double now = 0.0;
    String error = null;
    IterativeRobotBase robot = null;
    DrivetrainModel model = new DrivetrainModel(config);

    Frames frames = new Frames();
    try {
      robot = createRobot(loader, classesDir, Json.str(job, "mainClass", "frc.robot.Main"));
      model.writeSensors();
      phase = "robotInit";
      robot.simStart();
    } catch (Throwable t) {
      error = describe("Robot " + phase + " hata verdi", t);
    }

    SimHal.Mode runMode = parseMode(Json.str(job, "mode", "autonomous"));
    double period = robot != null ? robot.simGetPeriod() : 0.02;
    double duration = Math.max(0.5, Math.min(60.0, Json.num(job, "duration", 15.0)));
    List<Object> joystick = Json.list(job, "joystick");

    int lead = (int) Math.round(LEAD_SECONDS / period);
    int active = (int) Math.round(duration / period);
    int tail = (int) Math.round(TAIL_SECONDS / period);
    int total = lead + active + tail;

    frames.record(0.0, model);
    frames.snapshotDashboard();

    if (error == null) {
      for (int i = 0; i < total && error == null; i++) {
        now = i * period;
        SimHal.timeSeconds = now;
        SimHal.mode = (i >= lead && i < lead + active) ? runMode : SimHal.Mode.DISABLED;
        applyJoystick(joystick, now);
        try {
          robot.loopFunc();
        } catch (Throwable t) {
          error = describe(String.format("Robot kodu %.2f s'de hata verdi", now), t);
          break;
        }
        model.step(period, SimHal.isEnabled());
        model.writeSensors();
        frames.record(modeCode(SimHal.mode), (i + 1) * period, model);
        frames.snapshotDashboard();
      }
    }

    result.put("ok", error == null);
    if (error != null) {
      result.put("stage", "run");
      result.put("error", error);
    }
    result.put("period", period);
    result.put("robotName", config.robotName);
    result.put("trackWidthMeters", config.trackWidthMeters);
    result.put("wheelRadiusMeters", config.wheelRadiusMeters);
    result.put("activeStart", lead * period);
    result.put("activeEnd", (lead + active) * period);
    result.put("frames", frames.toMap());
    result.put("telemetry", frames.telemetryMap());
    result.put("strings", new LinkedHashMap<>(SimHal.dashboardStrings));

    Map<String, Object> choosers = new LinkedHashMap<>();
    for (Map.Entry<String, ChooserView> e : SimHal.choosers.entrySet()) {
      Map<String, Object> c = new LinkedHashMap<>();
      c.put("options", e.getValue().optionNames());
      c.put("default", e.getValue().defaultName());
      choosers.put(e.getKey(), c);
    }
    result.put("choosers", choosers);
  }

  @SuppressWarnings("removal")
  private static void installGuard(Guard guard) {
    System.setSecurityManager(guard);
  }

  private static IterativeRobotBase createRobot(URLClassLoader loader, Path classesDir, String mainName) throws Exception {
    Supplier<? extends RobotBase> factory = null;

    Class<?> mainClass = tryLoad(loader, mainName);
    if (mainClass != null) {
      Method main;
      try {
        main = mainClass.getMethod("main", String[].class);
      } catch (NoSuchMethodException e) {
        main = null;
      }
      if (main != null && Modifier.isStatic(main.getModifiers())) {
        try {
          main.invoke(null, (Object) new String[0]);
        } catch (InvocationTargetException e) {
          throw e.getCause() instanceof Exception ? (Exception) e.getCause() : e;
        }
        factory = RobotBase.simTakeFactory();
      }
    }

    if (factory == null) {
      // Main yok ya da startRobot cagirmadi: TimedRobot alt sinifini kendimiz bul.
      List<String> names = classNames(classesDir);
      Class<?> best = null;
      for (String n : names) {
        Class<?> c = tryLoad(loader, n);
        if (c == null || Modifier.isAbstract(c.getModifiers()) || !TimedRobot.class.isAssignableFrom(c)) continue;
        if (best == null || n.equals("frc.robot.Robot")) best = c;
      }
      if (best == null) {
        throw new IllegalStateException(
            "TimedRobot'tan turetilmis bir robot sinifi bulunamadi (ornek: frc.robot.Robot). "
                + "Main.java icinde RobotBase.startRobot(Robot::new) cagrildigindan emin olun.");
      }
      final Class<?> robotClass = best;
      factory = () -> {
        try {
          java.lang.reflect.Constructor<?> ctor = robotClass.getDeclaredConstructor();
          ctor.setAccessible(true);
          return (RobotBase) ctor.newInstance();
        } catch (InvocationTargetException e) {
          throw new RuntimeException(e.getCause());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      };
    }

    RobotBase robot = factory.get();
    if (!(robot instanceof IterativeRobotBase)) {
      throw new IllegalStateException("Simulator su an yalnizca TimedRobot tabanli robotlari destekliyor");
    }
    return (IterativeRobotBase) robot;
  }

  private static Class<?> tryLoad(ClassLoader loader, String name) {
    try {
      return Class.forName(name, true, loader);
    } catch (ClassNotFoundException | NoClassDefFoundError e) {
      return null;
    }
  }

  private static List<String> classNames(Path classesDir) throws IOException {
    List<String> names = new ArrayList<>();
    try (Stream<Path> s = Files.walk(classesDir)) {
      s.filter(p -> p.toString().endsWith(".class")).forEach(p -> {
        String rel = classesDir.relativize(p).toString();
        names.add(rel.substring(0, rel.length() - 6).replace('/', '.').replace('\\', '.'));
      });
    }
    return names;
  }

  private static SimHal.Mode parseMode(String s) {
    switch (s.toLowerCase()) {
      case "teleop": return SimHal.Mode.TELEOP;
      case "test": return SimHal.Mode.TEST;
      default: return SimHal.Mode.AUTONOMOUS;
    }
  }

  private static double modeCode(SimHal.Mode m) {
    switch (m) {
      case AUTONOMOUS: return 1;
      case TELEOP: return 2;
      case TEST: return 3;
      default: return 0;
    }
  }

  /** joystick: [{t, axes:[..], buttons:[1,3]}...] -- parca parca sabit giris (port 0). */
  private static void applyJoystick(List<Object> script, double now) {
    if (script == null) return;
    Map<String, Object> current = null;
    for (Object o : script) {
      if (!(o instanceof Map)) continue;
      @SuppressWarnings("unchecked")
      Map<String, Object> m = (Map<String, Object>) o;
      if (Json.num(m, "t", 0.0) <= now + 1e-9) current = m;
    }
    for (int a = 0; a < SimHal.MAX_AXES; a++) SimHal.setAxis(0, a, 0.0);
    for (int b = 1; b <= SimHal.MAX_BUTTONS; b++) SimHal.setButton(0, b, false);
    if (current == null) return;
    List<Object> axes = Json.list(current, "axes");
    if (axes != null) {
      for (int a = 0; a < axes.size() && a < SimHal.MAX_AXES; a++) {
        if (axes.get(a) instanceof Number) SimHal.setAxis(0, a, ((Number) axes.get(a)).doubleValue());
      }
    }
    List<Object> buttons = Json.list(current, "buttons");
    if (buttons != null) {
      for (Object b : buttons) {
        if (b instanceof Number) SimHal.setButton(0, ((Number) b).intValue(), true);
      }
    }
  }

  private static String describe(String prefix, Throwable t) {
    Throwable root = t;
    if (root instanceof InvocationTargetException && root.getCause() != null) root = root.getCause();
    StringBuilder sb = new StringBuilder(prefix).append(":\n").append(root.toString());
    int shown = 0;
    for (StackTraceElement e : root.getStackTrace()) {
      String cn = e.getClassName();
      if (cn.startsWith("frcsim.host.") || cn.startsWith("java.base") || cn.startsWith("jdk.")
          || cn.startsWith("sun.") || cn.startsWith("java.lang.reflect")) {
        continue;
      }
      sb.append("\n    at ").append(e);
      if (++shown >= 8) break;
    }
    return sb.toString();
  }

  /** Kare ve SmartDashboard verilerini sutun halinde biriktirir. */
  private static final class Frames {
    final List<Double> t = new ArrayList<>();
    final List<Double> x = new ArrayList<>();
    final List<Double> y = new ArrayList<>();
    final List<Double> theta = new ArrayList<>();
    final List<Double> v = new ArrayList<>();
    final List<Double> omega = new ArrayList<>();
    final List<Double> vl = new ArrayList<>();
    final List<Double> vr = new ArrayList<>();
    final List<Double> cmdL = new ArrayList<>();
    final List<Double> cmdR = new ArrayList<>();
    final List<Double> leftM = new ArrayList<>();
    final List<Double> rightM = new ArrayList<>();
    final List<Double> mode = new ArrayList<>();
    final Map<String, List<Double>> tele = new LinkedHashMap<>();

    void record(double time, DrivetrainModel m) {
      record(0, time, m);
    }

    void record(double modeValue, double time, DrivetrainModel m) {
      t.add(time);
      x.add(m.x);
      y.add(m.y);
      theta.add(m.theta);
      v.add(m.linearVelocity());
      omega.add(m.angularVelocity());
      vl.add(m.vl);
      vr.add(m.vr);
      cmdL.add(m.cmdLeft);
      cmdR.add(m.cmdRight);
      leftM.add(m.leftDist);
      rightM.add(m.rightDist);
      mode.add(modeValue);
    }

    void snapshotDashboard() {
      int index = t.size() - 1;
      for (Map.Entry<String, Double> e : new ArrayList<>(SimHal.dashboardNumbers.entrySet())) {
        List<Double> l = tele.computeIfAbsent(e.getKey(), k -> new ArrayList<>());
        while (l.size() < index) l.add(Double.NaN);
        l.add(e.getValue());
      }
    }

    Map<String, Object> toMap() {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("t", t);
      m.put("x", x);
      m.put("y", y);
      m.put("theta", theta);
      m.put("v", v);
      m.put("omega", omega);
      m.put("vl", vl);
      m.put("vr", vr);
      m.put("cmdL", cmdL);
      m.put("cmdR", cmdR);
      m.put("leftM", leftM);
      m.put("rightM", rightM);
      m.put("mode", mode);
      return m;
    }

    Map<String, Object> telemetryMap() {
      Map<String, Object> m = new LinkedHashMap<>();
      for (Map.Entry<String, List<Double>> e : tele.entrySet()) {
        List<Double> l = e.getValue();
        while (l.size() < t.size()) l.add(Double.NaN);
        m.put(e.getKey(), l);
      }
      return m;
    }
  }
}
