package frcsim.hal;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sanal donanim katmani (HAL). Robot kodu tarafindaki WPILib siniflari buradan okur/yazar;
 * fizik motoru ayni yerden motor cikislarini alip sensor degerlerini geri yazar.
 * Her calistirma ayri bir JVM surecinde oldugu icin static durum guvenlidir.
 */
public final class SimHal {
  private SimHal() {}

  public enum Mode { DISABLED, AUTONOMOUS, TELEOP, TEST }

  public static final int PWM_CHANNELS = 20;
  public static final int JOYSTICK_PORTS = 6;
  public static final int MAX_AXES = 12;
  public static final int MAX_BUTTONS = 32;

  public static double timeSeconds = 0.0;
  public static Mode mode = Mode.DISABLED;
  public static double batteryVolts = 12.0;

  /** Ham PWM cikislari (-1..1). Motor kontrolcusunun ters cevirmesi dahil edilmis degerdir. */
  public static final double[] pwm = new double[PWM_CHANNELS];

  /** Gyro: derece, saat yonu pozitif (WPILib kurali). */
  public static final double[] gyroAngleDeg = new double[8];
  public static final double[] gyroRateDegPerSec = new double[8];

  private static final Map<Integer, double[]> encoders = new HashMap<>();
  private static final double[][] joyAxes = new double[JOYSTICK_PORTS][MAX_AXES];
  private static final boolean[][] joyButtons = new boolean[JOYSTICK_PORTS][MAX_BUTTONS + 1];
  private static final int[] joyPov = new int[JOYSTICK_PORTS];

  public static final Map<String, Double> dashboardNumbers = new LinkedHashMap<>();
  public static final Map<String, String> dashboardStrings = new LinkedHashMap<>();
  public static final Map<String, ChooserView> choosers = new LinkedHashMap<>();
  public static final Map<String, String> chooserSelection = new HashMap<>();

  static {
    reset();
  }

  public static boolean isEnabled() {
    return mode != Mode.DISABLED;
  }

  public static void setEncoder(int channelA, double counts, double countsPerSec) {
    double[] e = encoders.get(channelA);
    if (e == null) {
      e = new double[2];
      encoders.put(channelA, e);
    }
    e[0] = counts;
    e[1] = countsPerSec;
  }

  /** {sayac, sayac/saniye}. Tanimsiz kanalda sifir doner. */
  public static double[] getEncoder(int channelA) {
    double[] e = encoders.get(channelA);
    return e == null ? new double[2] : e;
  }

  public static void setAxis(int port, int axis, double value) {
    if (port >= 0 && port < JOYSTICK_PORTS && axis >= 0 && axis < MAX_AXES) joyAxes[port][axis] = value;
  }

  public static double getAxis(int port, int axis) {
    if (port < 0 || port >= JOYSTICK_PORTS || axis < 0 || axis >= MAX_AXES) return 0.0;
    return joyAxes[port][axis];
  }

  /** Dugme numarasi 1 tabanli (WPILib ile ayni). */
  public static void setButton(int port, int button, boolean value) {
    if (port >= 0 && port < JOYSTICK_PORTS && button >= 1 && button <= MAX_BUTTONS) {
      joyButtons[port][button] = value;
    }
  }

  public static boolean getButton(int port, int button) {
    if (port < 0 || port >= JOYSTICK_PORTS || button < 1 || button > MAX_BUTTONS) return false;
    return joyButtons[port][button];
  }

  public static void setPov(int port, int degrees) {
    if (port >= 0 && port < JOYSTICK_PORTS) joyPov[port] = degrees;
  }

  public static int getPov(int port) {
    return port < 0 || port >= JOYSTICK_PORTS ? -1 : joyPov[port];
  }

  public static void reset() {
    timeSeconds = 0.0;
    mode = Mode.DISABLED;
    batteryVolts = 12.0;
    java.util.Arrays.fill(pwm, 0.0);
    java.util.Arrays.fill(gyroAngleDeg, 0.0);
    java.util.Arrays.fill(gyroRateDegPerSec, 0.0);
    encoders.clear();
    for (double[] a : joyAxes) java.util.Arrays.fill(a, 0.0);
    for (boolean[] b : joyButtons) java.util.Arrays.fill(b, false);
    java.util.Arrays.fill(joyPov, -1);
    dashboardNumbers.clear();
    dashboardStrings.clear();
    choosers.clear();
    chooserSelection.clear();
  }
}
