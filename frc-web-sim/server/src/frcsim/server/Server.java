package frcsim.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import frcsim.physics.MotorSpec;
import frcsim.util.Json;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * FRC web simulatoru sunucusu.
 *
 * GET  /                 web arayuzu (statik dosyalar)
 * GET  /api/examples     ornek robot kodu + robot-config.json
 * GET  /api/motors       desteklenen motor adlari
 * POST /api/run          takim Java kodunu derler, ayri JVM'de simule eder, sonucu doner
 *
 * Varsayilan olarak yalnizca 127.0.0.1'de dinler. Internete acmadan once README "Guvenlik" bolumunu okuyun.
 */
public final class Server {
  private static final long MAX_BODY_BYTES = 1_000_000;
  private static final int MAX_FILES = 40;
  private static final int MAX_FILE_CHARS = 200_000;
  private static final int MAX_CONSOLE_BYTES = 8_000;
  private static final long COMPILE_TIMEOUT_SEC = 30;
  private static final long RUN_TIMEOUT_SEC = 20;
  private static final Pattern PATH_OK = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*(/[A-Za-z_][A-Za-z0-9_]*)*\\.java$");
  private static final String[] DENIED_PREFIXES = {"edu/", "frcsim/", "java/", "javax/", "jdk/", "sun/", "com/sun/"};

  private final Path webDir;
  private final Path examplesDir;
  private final Path coreJar;
  private final Path workRoot;
  private final String javaBin;
  private final String javacBin;
  private final Semaphore slots;

  private Server(Path webDir, Path examplesDir, Path coreJar, Path workRoot, int maxParallel) {
    this.webDir = webDir.toAbsolutePath().normalize();
    this.examplesDir = examplesDir.toAbsolutePath().normalize();
    this.coreJar = coreJar.toAbsolutePath().normalize();
    this.workRoot = workRoot.toAbsolutePath().normalize();
    String home = System.getProperty("java.home");
    this.javaBin = Paths.get(home, "bin", "java").toString();
    this.javacBin = Paths.get(home, "bin", "javac").toString();
    this.slots = new Semaphore(maxParallel);
  }

  public static void main(String[] args) throws Exception {
    // Takim kodu kisitlamasi SecurityManager'a dayanir; JDK 24+'ta SecurityManager tamamen kaldirildi.
    // Kisitsiz calismaktansa acik bir hatayla durmak daha guvenlidir.
    if (Runtime.version().feature() < 17 || Runtime.version().feature() >= 24) {
      throw new IllegalStateException("JDK 17-23 gerekli (bulunan: " + Runtime.version()
          + "). Takim kodunu kisitlayan SecurityManager JDK 24+'ta yok; bu surumde sunucu baslatilmaz.");
    }
    Map<String, String> opt = new LinkedHashMap<>();
    for (int i = 0; i + 1 < args.length; i += 2) opt.put(args[i], args[i + 1]);
    String host = opt.getOrDefault("--host", "127.0.0.1");
    int port = Integer.parseInt(opt.getOrDefault("--port", "8080"));
    Path root = Paths.get(opt.getOrDefault("--root", "."));
    Server s = new Server(
        root.resolve(opt.getOrDefault("--web", "web")),
        root.resolve(opt.getOrDefault("--examples", "robot")),
        root.resolve(opt.getOrDefault("--core-jar", "build/frcsim-core.jar")),
        root.resolve(opt.getOrDefault("--work", "build/work")),
        Integer.parseInt(opt.getOrDefault("--parallel", "2")));
    if (!Files.isRegularFile(s.coreJar)) throw new IllegalStateException("core jar bulunamadi: " + s.coreJar);
    if (!Files.isDirectory(s.webDir)) throw new IllegalStateException("web klasoru bulunamadi: " + s.webDir);
    Files.createDirectories(s.workRoot);
    s.cleanOldWork();

    HttpServer http = HttpServer.create(new InetSocketAddress(host, port), 50);
    http.createContext("/", s::handleStatic);
    http.createContext("/api/examples", s::handleExamples);
    http.createContext("/api/motors", s::handleMotors);
    http.createContext("/api/run", s::handleRun);
    http.setExecutor(Executors.newFixedThreadPool(8));
    http.start();
    System.out.println("FRC web simulatoru: http://" + host + ":" + http.getAddress().getPort() + "/");
  }

  // ------------------------------------------------------------------ statik

  private void handleStatic(HttpExchange ex) throws IOException {
    try {
      if (!ex.getRequestMethod().equals("GET") && !ex.getRequestMethod().equals("HEAD")) {
        sendText(ex, 405, "Method Not Allowed");
        return;
      }
      String path = ex.getRequestURI().getPath();
      if (path.equals("/")) path = "/index.html";
      Path file = webDir.resolve(path.substring(1)).normalize();
      if (!file.startsWith(webDir) || !Files.isRegularFile(file)) {
        sendText(ex, 404, "Bulunamadi");
        return;
      }
      byte[] data = Files.readAllBytes(file);
      ex.getResponseHeaders().set("Content-Type", contentType(file.getFileName().toString()));
      ex.getResponseHeaders().set("Cache-Control", "no-cache");
      addSecurityHeaders(ex);
      ex.sendResponseHeaders(200, ex.getRequestMethod().equals("HEAD") ? -1 : data.length);
      if (!ex.getRequestMethod().equals("HEAD")) {
        try (OutputStream os = ex.getResponseBody()) {
          os.write(data);
        }
      }
    } finally {
      ex.close();
    }
  }

  private static String contentType(String name) {
    if (name.endsWith(".html")) return "text/html; charset=utf-8";
    if (name.endsWith(".js")) return "text/javascript; charset=utf-8";
    if (name.endsWith(".css")) return "text/css; charset=utf-8";
    if (name.endsWith(".json")) return "application/json; charset=utf-8";
    if (name.endsWith(".svg")) return "image/svg+xml";
    if (name.endsWith(".png")) return "image/png";
    if (name.endsWith(".ico")) return "image/x-icon";
    return "application/octet-stream";
  }

  private static void addSecurityHeaders(HttpExchange ex) {
    ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
    ex.getResponseHeaders().set("Referrer-Policy", "no-referrer");
    ex.getResponseHeaders().set("Content-Security-Policy",
        "default-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'");
  }

  // --------------------------------------------------------------------- api

  private void handleMotors(HttpExchange ex) throws IOException {
    try {
      sendJson(ex, 200, MotorSpec.names());
    } finally {
      ex.close();
    }
  }

  private void handleExamples(HttpExchange ex) throws IOException {
    try {
      Map<String, Object> out = new LinkedHashMap<>();
      Map<String, Object> files = new LinkedHashMap<>();
      Path src = examplesDir.resolve("src");
      if (Files.isDirectory(src)) {
        List<Path> list = new ArrayList<>();
        try (Stream<Path> s = Files.walk(src)) {
          s.filter(p -> p.toString().endsWith(".java")).sorted().forEach(list::add);
        }
        for (Path p : list) {
          files.put(src.relativize(p).toString().replace('\\', '/'), new String(Files.readAllBytes(p), StandardCharsets.UTF_8));
        }
      }
      out.put("files", files);
      Path cfg = examplesDir.resolve("robot-config.json");
      out.put("config", Files.isRegularFile(cfg) ? new String(Files.readAllBytes(cfg), StandardCharsets.UTF_8) : "{}");
      sendJson(ex, 200, out);
    } finally {
      ex.close();
    }
  }

  private void handleRun(HttpExchange ex) throws IOException {
    boolean acquired = false;
    try {
      if (!ex.getRequestMethod().equals("POST")) {
        sendText(ex, 405, "POST bekleniyor");
        return;
      }
      byte[] body = readLimited(ex.getRequestBody(), MAX_BODY_BYTES);
      if (body == null) {
        sendJson(ex, 413, error("request", "Istek cok buyuk (en fazla 1 MB)."));
        return;
      }
      Map<String, Object> req;
      try {
        req = Json.parseObject(new String(body, StandardCharsets.UTF_8));
      } catch (IllegalArgumentException e) {
        sendJson(ex, 400, error("request", "Gecersiz JSON: " + e.getMessage()));
        return;
      }
      try {
        acquired = slots.tryAcquire(15, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      if (!acquired) {
        sendJson(ex, 503, error("busy", "Sunucu mesgul, birkac saniye sonra tekrar deneyin."));
        return;
      }
      Map<String, Object> result;
      try {
        result = run(req);
      } catch (RequestException e) {
        sendJson(ex, 400, error("request", e.getMessage()));
        return;
      } catch (Exception e) {
        e.printStackTrace();
        sendJson(ex, 500, error("server", "Sunucu hatasi: " + e));
        return;
      }
      sendJson(ex, 200, result);
    } finally {
      if (acquired) slots.release();
      ex.close();
    }
  }

  private static final class RequestException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    RequestException(String m) {
      super(m);
    }
  }

  private Map<String, Object> run(Map<String, Object> req) throws Exception {
    Map<String, Object> filesObj = Json.obj(req, "files");
    if (filesObj == null || filesObj.isEmpty()) throw new RequestException("'files' bos olamaz.");
    if (filesObj.size() > MAX_FILES) throw new RequestException("En fazla " + MAX_FILES + " dosya.");
    Map<String, String> files = new LinkedHashMap<>();
    for (Map.Entry<String, Object> e : filesObj.entrySet()) {
      String name = e.getKey();
      if (!PATH_OK.matcher(name).matches()) {
        throw new RequestException("Gecersiz dosya yolu '" + name + "'. Ornek: frc/robot/Robot.java");
      }
      for (String bad : DENIED_PREFIXES) {
        if (name.startsWith(bad)) throw new RequestException("'" + bad + "' paketleri kullanilamaz: " + name);
      }
      if (!(e.getValue() instanceof String)) throw new RequestException("Dosya icerigi metin olmali: " + name);
      String content = (String) e.getValue();
      if (content.length() > MAX_FILE_CHARS) throw new RequestException("Dosya cok buyuk: " + name);
      files.put(name, content);
    }
    Object cfg = req.get("config");
    if (cfg != null && !(cfg instanceof Map)) throw new RequestException("'config' bir JSON nesnesi olmali.");

    Path dir = workRoot.resolve(UUID.randomUUID().toString());
    Path src = dir.resolve("src");
    Path classes = dir.resolve("classes");
    try {
      Files.createDirectories(src);
      Files.createDirectories(classes);
      List<String> sourcePaths = new ArrayList<>();
      for (Map.Entry<String, String> e : files.entrySet()) {
        Path p = src.resolve(e.getKey()).normalize();
        if (!p.startsWith(src)) throw new RequestException("Gecersiz yol: " + e.getKey());
        Files.createDirectories(p.getParent());
        Files.write(p, e.getValue().getBytes(StandardCharsets.UTF_8));
        sourcePaths.add(p.toString());
      }

      // 1) Derleme (ayri surec, -proc:none: annotation processor calistirilmaz)
      List<String> javac = new ArrayList<>();
      javac.add(javacBin);
      javac.add("--release"); javac.add("17");
      javac.add("-proc:none");
      javac.add("-implicit:none");
      javac.add("-nowarn");
      javac.add("-encoding"); javac.add("UTF-8");
      javac.add("-cp"); javac.add(coreJar.toString());
      javac.add("-d"); javac.add(classes.toString());
      javac.addAll(sourcePaths);
      Proc c = exec(javac, dir, COMPILE_TIMEOUT_SEC, 60_000);
      if (c.timedOut) {
        return error("compile", "Derleme zaman asimina ugradi.");
      }
      if (c.exit != 0) {
        String msg = c.output.replace(src.toString() + "/", "").trim();
        return error("compile", msg.isEmpty() ? "Derleme basarisiz." : msg);
      }

      // 2) Calistirma (ayri JVM; temiz ortam, bellek siniri, zaman asimi)
      Map<String, Object> job = new LinkedHashMap<>();
      job.put("classesDir", classes.toString());
      job.put("mainClass", Json.str(req, "mainClass", "frc.robot.Main"));
      job.put("mode", Json.str(req, "mode", "autonomous"));
      job.put("duration", Json.num(req, "duration", 15.0));
      job.put("config", cfg == null ? new LinkedHashMap<String, Object>() : cfg);
      Object chooser = req.get("chooser");
      if (chooser instanceof Map) job.put("chooser", chooser);
      Object joystick = req.get("joystick");
      if (joystick instanceof List) job.put("joystick", joystick);
      Path resultFile = dir.resolve("result.json");
      job.put("resultFile", resultFile.toString());
      Path jobFile = dir.resolve("job.json");
      Files.write(jobFile, Json.stringify(job).getBytes(StandardCharsets.UTF_8));

      List<String> java = new ArrayList<>();
      java.add(javaBin);
      java.add("-Xmx256m");
      java.add("-Xss2m");
      java.add("-XX:+ExitOnOutOfMemoryError");
      java.add("-XX:TieredStopAtLevel=1");
      java.add("-Djava.security.manager=allow");
      java.add("-Dfile.encoding=UTF-8");
      java.add("-cp"); java.add(coreJar.toString());
      java.add("frcsim.host.SimRunner");
      java.add(jobFile.toString());
      Proc r = exec(java, dir, RUN_TIMEOUT_SEC, MAX_CONSOLE_BYTES);

      Map<String, Object> out;
      if (r.timedOut) {
        out = error("timeout", "Robot kodu " + RUN_TIMEOUT_SEC
            + " saniyede bitmedi (sonsuz dongu veya cok agir hesaplama olabilir). "
            + "robotInit/periodic icinde bloklayan dongu kullanmayin.");
      } else if (Files.isRegularFile(resultFile)) {
        out = Json.parseObject(new String(Files.readAllBytes(resultFile), StandardCharsets.UTF_8));
      } else {
        out = error("run", "Simulasyon sonuc uretmedi (cikis kodu " + r.exit + ")."
            + (r.output.isEmpty() ? "" : "\n" + stripWarnings(r.output)));
      }
      String console = stripWarnings(r.output);
      if (!console.isEmpty()) out.put("console", console);
      return out;
    } finally {
      deleteTree(dir);
    }
  }

  /** setSecurityManager uyarilarini kullaniciya gostermeyiz. */
  private static String stripWarnings(String s) {
    StringBuilder sb = new StringBuilder();
    for (String line : s.split("\n")) {
      if (line.startsWith("WARNING: ") || line.startsWith("Picked up ")) continue;
      sb.append(line).append('\n');
    }
    return sb.toString().trim();
  }

  private static Map<String, Object> error(String stage, String message) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("ok", false);
    m.put("stage", stage);
    m.put("error", message);
    return m;
  }

  // ---------------------------------------------------------------- surecler

  private static final class Proc {
    int exit;
    boolean timedOut;
    String output = "";
  }

  /** Temiz ortamla surec baslatir; ciktiyi sinirli okur; zaman asiminda oldurur. */
  private static Proc exec(List<String> cmd, Path cwd, long timeoutSec, int maxOutput) throws Exception {
    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.directory(cwd.toFile());
    pb.redirectErrorStream(true);
    pb.environment().clear();
    pb.environment().put("PATH", "/usr/bin:/bin");
    pb.environment().put("LANG", "C.UTF-8");
    Process p = pb.start();
    p.getOutputStream().close();
    final ByteArrayOutputStream buf = new ByteArrayOutputStream();
    Thread reader = new Thread(() -> {
      byte[] b = new byte[4096];
      try (InputStream in = p.getInputStream()) {
        int n;
        while ((n = in.read(b)) > 0) {
          synchronized (buf) {
            if (buf.size() < maxOutput) buf.write(b, 0, Math.min(n, maxOutput - buf.size()));
          }
        }
      } catch (IOException ignored) {
        // surec oldu
      }
    });
    reader.setDaemon(true);
    reader.start();
    Proc r = new Proc();
    if (!p.waitFor(timeoutSec, TimeUnit.SECONDS)) {
      r.timedOut = true;
      p.descendants().forEach(ProcessHandle::destroyForcibly);
      p.destroyForcibly();
      p.waitFor(5, TimeUnit.SECONDS);
    } else {
      r.exit = p.exitValue();
    }
    reader.join(2000);
    synchronized (buf) {
      r.output = new String(buf.toByteArray(), StandardCharsets.UTF_8);
      if (buf.size() >= maxOutput) r.output += "\n... (cikti kisaltildi)";
    }
    return r;
  }

  // ------------------------------------------------------------------ yardim

  private void cleanOldWork() throws IOException {
    try (Stream<Path> s = Files.list(workRoot)) {
      s.forEach(Server::deleteTree);
    }
  }

  private static void deleteTree(Path dir) {
    if (!Files.exists(dir)) return;
    try {
      Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path f, BasicFileAttributes a) throws IOException {
          Files.deleteIfExists(f);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path d, IOException e) throws IOException {
          Files.deleteIfExists(d);
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException ignored) {
      // temizlik en iyi cabadir
    }
  }

  private static byte[] readLimited(InputStream in, long max) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] b = new byte[8192];
    int n;
    while ((n = in.read(b)) > 0) {
      out.write(b, 0, n);
      if (out.size() > max) return null;
    }
    return out.toByteArray();
  }

  private static void sendText(HttpExchange ex, int code, String text) throws IOException {
    byte[] b = text.getBytes(StandardCharsets.UTF_8);
    ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
    ex.sendResponseHeaders(code, b.length);
    try (OutputStream os = ex.getResponseBody()) {
      os.write(b);
    }
  }

  private static void sendJson(HttpExchange ex, int code, Object value) throws IOException {
    byte[] b = Json.stringify(value).getBytes(StandardCharsets.UTF_8);
    ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    ex.getResponseHeaders().set("Cache-Control", "no-store");
    addSecurityHeaders(ex);
    ex.sendResponseHeaders(code, b.length);
    try (OutputStream os = ex.getResponseBody()) {
      os.write(b);
    }
  }
}
