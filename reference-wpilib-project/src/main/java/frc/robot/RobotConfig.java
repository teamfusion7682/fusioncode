package frc.robot;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wpi.first.wpilibj.Filesystem;
import java.io.File;
import java.io.IOException;

/**
 * robot-config.json dosyasini okur. Robot kurma arayuzu (ileride) bu dosyayi uretecek;
 * simdilik elle duzenleyerek farkli robotlari deneyebilirsiniz.
 *
 * Bilinmeyen alanlar (ileride eklenecek kol, asansor vb.) hata vermez, yok sayilir.
 */
public class RobotConfig {
  public static class DrivetrainConfig {
    public String motor = "NEO"; // NEO, CIM, MiniCIM, Falcon500
    public int motorsPerSide = 2;
    public double gearing = 10.71;
    public double massKg = 50.0;
    public double momentOfInertiaKgM2 = 7.5;
    public double wheelRadiusMeters = 0.0762;
    public double trackWidthMeters = 0.6;
  }

  public static class PortsConfig {
    public int leftMotorPwm = 0;
    public int rightMotorPwm = 1;
    public int[] leftEncoder = {0, 1};
    public int[] rightEncoder = {2, 3};
    public int gyroAnalog = 0;
  }

  public String robotName = "varsayilan-robot";
  public DrivetrainConfig drivetrain = new DrivetrainConfig();
  public PortsConfig ports = new PortsConfig();

  /** Dosya okunamazsa varsayilan degerlerle devam eder ve uyari basar. */
  public static RobotConfig load() {
    File file = new File(Filesystem.getDeployDirectory(), "robot-config.json");
    ObjectMapper mapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    try {
      return mapper.readValue(file, RobotConfig.class);
    } catch (IOException e) {
      System.err.println("robot-config.json okunamadi, varsayilan kullaniliyor: " + e.getMessage());
      return new RobotConfig();
    }
  }
}
