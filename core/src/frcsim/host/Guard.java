package frcsim.host;

import java.io.FilePermission;
import java.lang.reflect.ReflectPermission;
import java.net.NetPermission;
import java.net.SocketPermission;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Permission;
import java.security.SecurityPermission;
import java.util.List;
import java.util.PropertyPermission;
import java.util.logging.LoggingPermission;

/**
 * Takim kodunu (sinif yukleyicisi `untrusted` olan siniflari) kisitlayan SecurityManager.
 * Cagri yigininda takim sinifi varsa istek denetlenir; yalniz simulator/JDK kodu calisiyorsa serbesttir.
 *
 * Kisitlar: dosya yazma/silme/calistirma, ag, System.exit/halt, ortam degiskenleri,
 * ozellik yazma, yerel kutuphane, sinif yukleyici olusturma, setAccessible, shutdown hook,
 * guvenlik/log/ag ayarlarini degistirme. Okuma yalniz JDK, simulator jar'i ve takim siniflariyla sinirli.
 *
 * NOT: SecurityManager JDK'de kaldirilmaya aday bir mekanizmadir ve tek basina yeterli bir guvenlik
 * siniri DEGILDIR. Herkese acik barindirmada konteyner/sandbox (agsiz, kaynak sinirli) kullanin.
 */
@SuppressWarnings("removal")
final class Guard extends SecurityManager {
  private final ClassLoader untrusted;
  private final List<Path> readRoots;

  Guard(ClassLoader untrusted, List<String> readRoots) {
    this.untrusted = untrusted;
    this.readRoots = new java.util.ArrayList<>();
    for (String r : readRoots) this.readRoots.add(Paths.get(r).toAbsolutePath().normalize());
  }

  /** Sinif yuklemelerini onceden tetikle (SecurityManager kendi kontrolunde sinif yuklemesin). */
  void warmUp() {
    fromUntrusted();
    checkPermission(new FilePermission("/nonexistent-warmup", "read"));
    checkPermission(new FilePermission("/nonexistent-warmup", "write"));
    checkPermission(new SocketPermission("localhost:1", "connect"));
    checkPermission(new PropertyPermission("warmup", "read"));
    checkPermission(new ReflectPermission("warmup"));
    checkPermission(new RuntimePermission("warmup"));
    checkPermission(new NetPermission("warmup"));
    checkPermission(new SecurityPermission("warmup"));
    checkPermission(new LoggingPermission("control", null));
    isAllowedRead("/nonexistent-warmup/../x");
  }

  private boolean fromUntrusted() {
    for (Class<?> c : getClassContext()) {
      if (c.getClassLoader() == untrusted) return true;
    }
    return false;
  }

  private SecurityException deny(Permission p) {
    String actions = p.getActions();
    return new SecurityException("Simulatorde izin verilmiyor: " + p.getClass().getSimpleName()
        + " " + p.getName() + (actions == null || actions.isEmpty() ? "" : " (" + actions + ")"));
  }

  /** ".." ile kacisi engellemek icin yolu normallestirip kok klasorlerle karsilastirir. */
  private boolean isAllowedRead(String name) {
    if ("<<ALL FILES>>".equals(name)) return false;
    final Path p;
    try {
      p = Paths.get(name).toAbsolutePath().normalize();
    } catch (RuntimeException e) {
      return false;
    }
    for (Path root : readRoots) {
      if (p.startsWith(root)) return true;
    }
    return false;
  }

  @Override
  public void checkPermission(Permission p) {
    if (!fromUntrusted()) return;
    if (p instanceof FilePermission) {
      if (p.getActions().equals("read") && isAllowedRead(p.getName())) return;
      throw deny(p);
    }
    if (p instanceof SocketPermission || p instanceof NetPermission || p instanceof LoggingPermission) {
      throw deny(p);
    }
    if (p instanceof PropertyPermission) {
      if (p.getActions().contains("write")) throw deny(p);
      return;
    }
    if (p instanceof ReflectPermission) {
      if (p.getName().equals("suppressAccessChecks")) throw deny(p);
      return;
    }
    if (p instanceof SecurityPermission) {
      String n = p.getName();
      if (n.startsWith("set") || n.startsWith("insert") || n.startsWith("remove") || n.startsWith("put")) {
        throw deny(p);
      }
      return;
    }
    if (p instanceof RuntimePermission) {
      String n = p.getName();
      if (n.startsWith("exitVM")
          || n.startsWith("getenv")
          || n.equals("setSecurityManager")
          || n.equals("createSecurityManager")
          || n.startsWith("loadLibrary")
          || n.equals("createClassLoader")
          || n.equals("setContextClassLoader")
          || n.equals("setIO")
          || n.equals("setFactory")
          || n.equals("shutdownHooks")
          || n.equals("setDefaultUncaughtExceptionHandler")
          || n.equals("modifyThreadGroup")
          || n.equals("manageProcess")
          || n.startsWith("defineClassInPackage")
          || n.startsWith("accessClassInPackage.sun.")
          || n.startsWith("accessClassInPackage.jdk.")) {
        throw deny(p);
      }
    }
  }

  @Override
  public void checkPermission(Permission p, Object context) {
    checkPermission(p);
  }
}
