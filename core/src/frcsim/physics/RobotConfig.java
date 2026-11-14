package frcsim.physics;

import frcsim.hal.SimHal;
import frcsim.util.Json;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * robot-config.json: robotun fiziksel ozellikleri ve kablolamasi.
 * Robot kurma arayuzu (sonraki asama) bu dosyayi uretecek.
 */
public final class RobotConfig {
  public static final class EncoderConfig {
    public int a;
    public int b;
    public String side = "left";
    public double countsPerRev = 2048;
    public boolean reversed = false;
  }

  public String robotName = "robot";

  public String motor = "NEO";
  public int motorsPerSide = 2;
  public double gearing = 10.71;
  public double massKg = 50.0;
  public double momentOfInertiaKgM2 = 7.5;
  public double wheelRadiusMeters = 0.0762;
  public double trackWidthMeters = 0.6;
  public double efficiency = 0.9;
  public double rollingFriction = 0.02;
  public double turnFriction = 0.10;
  public double tractionCoefficient = 1.0;
  public double currentLimitAmps = 60.0;
  public String idleMode = "brake";

  public int[] leftPwm = {0};
  public boolean leftReversed = false;
  public int[] rightPwm = {1};
  public boolean rightReversed = true;

  public final List<EncoderConfig> encoders = new ArrayList<>();
  public int gyroChannel = 0;

  public static RobotConfig defaults() {
    RobotConfig c = new RobotConfig();
    c.encoders.add(encoder(0, 1, "left"));
    c.encoders.add(encoder(2, 3, "right"));
    return c;
  }

  private static EncoderConfig encoder(int a, int b, String side) {
    EncoderConfig e = new EncoderConfig();
    e.a = a;
    e.b = b;
    e.side = side;
    return e;
  }

  public static RobotConfig fromMap(Map<String, Object> root) {
    RobotConfig c = defaults();
    if (root != null) {
      c.robotName = Json.str(root, "robotName", c.robotName);

      Map<String, Object> d = Json.obj(root, "drivetrain");
      c.motor = Json.str(d, "motor", c.motor);
      c.motorsPerSide = Json.integer(d, "motorsPerSide", c.motorsPerSide);
      c.gearing = Json.num(d, "gearing", c.gearing);
      c.massKg = Json.num(d, "massKg", c.massKg);
      c.momentOfInertiaKgM2 = Json.num(d, "momentOfInertiaKgM2", c.momentOfInertiaKgM2);
      c.wheelRadiusMeters = Json.num(d, "wheelRadiusMeters", c.wheelRadiusMeters);
      c.trackWidthMeters = Json.num(d, "trackWidthMeters", c.trackWidthMeters);
      c.efficiency = Json.num(d, "efficiency", c.efficiency);
      c.rollingFriction = Json.num(d, "rollingFriction", c.rollingFriction);
      c.turnFriction = Json.num(d, "turnFriction", c.turnFriction);
      c.tractionCoefficient = Json.num(d, "tractionCoefficient", c.tractionCoefficient);
      c.currentLimitAmps = Json.num(d, "currentLimitAmps", c.currentLimitAmps);
      c.idleMode = Json.str(d, "idleMode", c.idleMode);

      Map<String, Object> w = Json.obj(root, "wiring");
      Map<String, Object> l = Json.obj(w, "left");
      Map<String, Object> r = Json.obj(w, "right");
      c.leftPwm = Json.intArray(l, "pwm", c.leftPwm);
      c.leftReversed = Json.bool(l, "reversed", c.leftReversed);
      c.rightPwm = Json.intArray(r, "pwm", c.rightPwm);
      c.rightReversed = Json.bool(r, "reversed", c.rightReversed);

      List<Object> encs = Json.list(root, "encoders");
      if (encs != null) {
        c.encoders.clear();
        for (Object o : encs) {
          if (!(o instanceof Map)) throw new IllegalArgumentException("'encoders' icindeki her oge bir nesne olmali");
          @SuppressWarnings("unchecked")
          Map<String, Object> m = (Map<String, Object>) o;
          EncoderConfig e = new EncoderConfig();
          e.a = Json.integer(m, "a", -1);
          e.b = Json.integer(m, "b", -1);
          e.side = Json.str(m, "side", "left");
          e.countsPerRev = Json.num(m, "countsPerRev", 2048);
          e.reversed = Json.bool(m, "reversed", false);
          c.encoders.add(e);
        }
      }

      c.gyroChannel = Json.integer(Json.obj(root, "gyro"), "channel", c.gyroChannel);
    }
    c.validate();
    return c;
  }

  public void validate() {
    MotorSpec.byName(motor);
    positive("motorsPerSide", motorsPerSide);
    positive("gearing", gearing);
    positive("massKg", massKg);
    positive("momentOfInertiaKgM2", momentOfInertiaKgM2);
    positive("wheelRadiusMeters", wheelRadiusMeters);
    positive("trackWidthMeters", trackWidthMeters);
    positive("efficiency", efficiency);
    positive("currentLimitAmps", currentLimitAmps);
    if (efficiency > 1.0) throw new IllegalArgumentException("efficiency 1'den buyuk olamaz");
    if (rollingFriction < 0 || turnFriction < 0 || tractionCoefficient <= 0) {
      throw new IllegalArgumentException("Surtunme katsayilari gecersiz");
    }
    if (!idleMode.equalsIgnoreCase("brake") && !idleMode.equalsIgnoreCase("coast")) {
      throw new IllegalArgumentException("idleMode 'brake' veya 'coast' olmali");
    }
    channels("wiring.left.pwm", leftPwm);
    channels("wiring.right.pwm", rightPwm);
    Set<Integer> used = new HashSet<>();
    for (int ch : leftPwm) used.add(ch);
    for (int ch : rightPwm) {
      if (!used.add(ch)) throw new IllegalArgumentException("PWM kanali " + ch + " hem sol hem sagda kullanilmis");
    }
    Set<Integer> encChannels = new HashSet<>();
    for (EncoderConfig e : encoders) {
      if (e.a < 0 || e.b < 0 || e.a == e.b) throw new IllegalArgumentException("Encoder kanallari gecersiz (a=" + e.a + ", b=" + e.b + ")");
      if (!encChannels.add(e.a) || !encChannels.add(e.b)) {
        throw new IllegalArgumentException("Encoder kanallari cakisiyor: " + e.a + "," + e.b);
      }
      if (!e.side.equals("left") && !e.side.equals("right")) {
        throw new IllegalArgumentException("Encoder 'side' degeri 'left' veya 'right' olmali");
      }
      positive("encoder.countsPerRev", e.countsPerRev);
    }
    if (gyroChannel < 0 || gyroChannel >= 8) throw new IllegalArgumentException("gyro.channel 0-7 arasinda olmali");
  }

  private static void positive(String name, double v) {
    if (!(v > 0)) throw new IllegalArgumentException("'" + name + "' pozitif olmali (verilen: " + v + ")");
  }

  private static void channels(String name, int[] chans) {
    if (chans.length == 0) throw new IllegalArgumentException("'" + name + "' bos olamaz");
    for (int ch : chans) {
      if (ch < 0 || ch >= SimHal.PWM_CHANNELS) {
        throw new IllegalArgumentException("'" + name + "' icinde gecersiz PWM kanali: " + ch);
      }
    }
  }
}
