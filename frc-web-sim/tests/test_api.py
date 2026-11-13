"""Sunucu uc-uca testleri: normal calisma, derleme hatasi ve saldirgan takim kodu.

Calistirma:  python3 tests/test_api.py          (hizli testler + yavas testler)
             python3 tests/test_api.py -k Fast  (yalniz hizli testler)
"""
import json
import os
import signal
import subprocess
import sys
import time
import unittest
import urllib.error
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PORT = int(os.environ.get("FRCSIM_TEST_PORT", "18080"))
BASE = f"http://127.0.0.1:{PORT}"
MARKER = "/tmp/frcsim-pwned"
CHOOSER_KEY = "Otonom"
WEAK = "1 - Zayif kod (geri bildirimsiz)"
PID = "2 - PID (encoder ile)"

server = None


def post(path, obj, timeout=90):
    data = json.dumps(obj).encode()
    req = urllib.request.Request(BASE + path, data=data, headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, json.loads(r.read())
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read())


def get(path):
    with urllib.request.urlopen(BASE + path, timeout=30) as r:
        return r.status, r.read()


def setUpModule():
    global server
    env = {"PATH": os.environ["PATH"], "JAVA_TOOL_OPTIONS": ""}
    cp = f"{ROOT}/build/frcsim-core.jar:{ROOT}/build/server-classes"
    server = subprocess.Popen(
        ["java", "-cp", cp, "frcsim.server.Server", "--root", ROOT, "--port", str(PORT)],
        env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    for _ in range(100):
        try:
            get("/api/motors")
            return
        except Exception:
            time.sleep(0.1)
    raise RuntimeError("sunucu baslamadi")


def tearDownModule():
    if server:
        server.send_signal(signal.SIGTERM)
        server.wait(timeout=10)


def example():
    _, body = get("/api/examples")
    d = json.loads(body)
    return d["files"], json.loads(d["config"])


def run_example(choice, **extra):
    files, cfg = example()
    req = {"files": files, "config": cfg, "mode": "autonomous", "duration": 6,
           "chooser": {CHOOSER_KEY: choice}}
    req.update(extra)
    return post("/api/run", req)


def with_payload(payload, helpers=""):
    """Ornek Robot.java'nin robotInit'ine saldirgan kod ekler."""
    files, cfg = example()
    src = files["frc/robot/Robot.java"]
    src = src.replace("public void robotInit() {",
                      "public void robotInit() {\n    try { attack(); } catch (Exception e) { throw new RuntimeException(e); }", 1)
    idx = src.rindex("}")
    src = src[:idx] + f"\n  static void attack() throws Exception {{ {payload} }}\n{helpers}\n}}\n"
    files["frc/robot/Robot.java"] = src
    return {"files": files, "config": cfg, "mode": "autonomous", "duration": 2}


class Fast(unittest.TestCase):
    def test_static_and_api(self):
        status, _ = get("/")
        self.assertEqual(status, 200)
        status, body = get("/api/motors")
        self.assertIn("NEO", json.loads(body))
        files, cfg = example()
        self.assertIn("frc/robot/Robot.java", files)
        self.assertIn("drivetrain", cfg)

    def test_static_path_traversal(self):
        with self.assertRaises(urllib.error.HTTPError) as cm:
            get("/..%2f..%2fetc/passwd")
        self.assertIn(cm.exception.code, (400, 404))

    def test_weak_overshoots_pid_stops(self):
        status, weak = run_example(WEAK)
        self.assertEqual(status, 200)
        self.assertTrue(weak["ok"], weak.get("error"))
        status, pid = run_example(PID)
        self.assertTrue(pid["ok"], pid.get("error"))
        dw = [v for v in weak["telemetry"]["Mesafe (m)"] if v is not None]
        dp = [v for v in pid["telemetry"]["Mesafe (m)"] if v is not None]
        self.assertGreater(dw[-1], 3.5, "zayif kod hedefi (3 m) asmali")
        self.assertLess(abs(dp[-1] - 3.0), 0.1, "PID hedefte durmali")
        # encoder olcumu ile gercek (fizik) konum uyumlu olmali
        self.assertLess(abs(dp[-1] - pid["frames"]["x"][-1]), 0.01)
        self.assertEqual(pid["choosers"][CHOOSER_KEY]["options"], [WEAK, PID])

    def test_compile_error_reports_line(self):
        files, cfg = example()
        files["frc/robot/Robot.java"] = files["frc/robot/Robot.java"].replace(
            "private String selected = AUTO_WEAK;", "private String selected = 42;")
        status, res = post("/api/run", {"files": files, "config": cfg})
        self.assertFalse(res["ok"])
        self.assertEqual(res["stage"], "compile")
        self.assertIn("Robot.java:", res["error"])
        self.assertNotIn("/home/", res["error"], "sunucu yollari sizmamali")

    def test_bad_config_reported(self):
        files, cfg = example()
        cfg["drivetrain"]["motor"] = "YokBoyleMotor"
        _, res = post("/api/run", {"files": files, "config": cfg})
        self.assertFalse(res["ok"])
        self.assertEqual(res["stage"], "config")
        self.assertIn("YokBoyleMotor", res["error"])

    def test_forbidden_file_paths(self):
        files, cfg = example()
        for bad in ("edu/wpi/first/wpilibj/Timer.java", "../evil.java", "frcsim/hal/SimHal.java",
                    "frc/robot/../../x.java", "/abs/Evil.java", "frc/Robot.txt"):
            f = dict(files)
            f[bad] = "class X {}"
            status, res = post("/api/run", {"files": f, "config": cfg})
            self.assertEqual(status, 400, bad)
            self.assertFalse(res["ok"], bad)

    def test_runtime_exception_has_user_line(self):
        files, cfg = example()
        files["frc/robot/Robot.java"] = files["frc/robot/Robot.java"].replace(
            "distancePid.setTolerance(0.03);", "distancePid.setTolerance(0.03); Object o = null; o.toString();")
        _, res = post("/api/run", {"files": files, "config": cfg})
        self.assertFalse(res["ok"])
        self.assertIn("NullPointerException", res["error"])
        self.assertIn("Robot.java", res["error"])

    def test_teleop_joystick_drives_forward(self):
        files, cfg = example()
        _, res = post("/api/run", {"files": files, "config": cfg, "mode": "teleop", "duration": 2,
                                   "joystick": [{"t": 0, "axes": [0, -1.0, 0, 0, 0, 0]}]})
        self.assertTrue(res["ok"], res.get("error"))
        self.assertGreater(res["frames"]["x"][-1], 1.0)


ATTACKS = {
    "write_file": f'new java.io.FileWriter("{MARKER}").write("x");',
    "read_etc_passwd": 'new java.io.FileInputStream("/etc/passwd").read();',
    "traversal_read": ('java.io.File base = new java.io.File(Robot.class.getProtectionDomain().getCodeSource().getLocation().toURI());'
                       'new java.io.FileInputStream(new java.io.File(base, "../../../../../../../../etc/passwd")).read();'),
    "delete_file": 'new java.io.File("/tmp").delete(); java.nio.file.Files.delete(java.nio.file.Paths.get("/etc/hostname"));',
    "exec": 'Runtime.getRuntime().exec("id");',
    "processbuilder": 'new ProcessBuilder("touch", "' + MARKER + '").start();',
    "exit": "System.exit(3);",
    "halt": "Runtime.getRuntime().halt(3);",
    "getenv": 'System.getenv("PATH");',
    "setproperty": 'System.setProperty("user.dir", "/");',
    "set_accessible": 'Robot.class.getDeclaredFields()[0].setAccessible(true);',
    "remove_sm": "System.setSecurityManager(null);",
    "classloader": "new java.net.URLClassLoader(new java.net.URL[0]);",
    "shutdown_hook": "Runtime.getRuntime().addShutdownHook(new Thread());",
    "load_library": 'System.loadLibrary("zip");',
}


class Hostile(unittest.TestCase):
    def setUp(self):
        if os.path.exists(MARKER):
            os.remove(MARKER)

    def test_blocked(self):
        for name, payload in ATTACKS.items():
            with self.subTest(attack=name):
                status, res = post("/api/run", with_payload(payload))
                self.assertFalse(res["ok"], f"{name}: saldiri engellenmedi! {res.get('error')}")
                self.assertIn("izin verilmiyor", res["error"], f"{name}: beklenmeyen hata: {res['error']}")
                self.assertFalse(os.path.exists(MARKER), f"{name}: dosya olustu!")

    def test_network_is_unreachable(self):
        """Gercek ozellik: yerel bir dinleyiciye HICBIR baglanti gelmemeli."""
        import socket
        listener = socket.socket()
        listener.bind(("127.0.0.1", 0))
        listener.listen(5)
        listener.settimeout(1.0)
        port = listener.getsockname()[1]
        attacks = {
            "socket": f'new java.net.Socket("127.0.0.1", {port});',
            "url": f'new java.net.URL("http://127.0.0.1:{port}/").openStream();',
            "http_client": (f'java.net.http.HttpClient.newHttpClient().send(java.net.http.HttpRequest.newBuilder('
                            f'java.net.URI.create("http://127.0.0.1:{port}/")).build(), '
                            f'java.net.http.HttpResponse.BodyHandlers.ofString());'),
            "udp": (f'new java.net.DatagramSocket().send(new java.net.DatagramPacket(new byte[1], 1, '
                    f'java.net.InetAddress.getByName("127.0.0.1"), {port}));'),
        }
        try:
            for name, payload in attacks.items():
                with self.subTest(attack=name):
                    _, res = post("/api/run", with_payload(payload))
                    self.assertFalse(res["ok"], f"{name}: ag cagrisi basarili sayildi!")
                    try:
                        conn, _ = listener.accept()
                        conn.close()
                        self.fail(f"{name}: dinleyiciye baglanti ULASTI")
                    except socket.timeout:
                        pass
        finally:
            listener.close()

    def test_thread_escape_is_blocked(self):
        payload = (f'Thread t = new Thread(() -> {{ try {{ new java.io.FileWriter("{MARKER}").write("x"); }}'
                   f' catch (Exception e) {{ System.out.println("THREAD-BLOCKED " + e.getMessage()); }} }}); t.start(); t.join();')
        _, res = post("/api/run", with_payload(payload))
        self.assertFalse(os.path.exists(MARKER), "thread ile dosya yazildi!")
        self.assertIn("THREAD-BLOCKED", res.get("console", ""))

    def test_server_survives_and_still_works(self):
        status, res = run_example(PID)
        self.assertTrue(res["ok"], res.get("error"))


class Slow(unittest.TestCase):
    def test_infinite_loop_times_out_and_floods_are_capped(self):
        t0 = time.time()
        payload = 'while (true) { System.out.println("spam spam spam spam spam spam spam spam"); }'
        status, res = post("/api/run", with_payload(payload))
        took = time.time() - t0
        self.assertEqual(res["stage"], "timeout", res)
        self.assertLess(took, 40)
        self.assertLessEqual(len(res.get("console", "")), 8200, "konsol cikti siniri asildi")
        # sunucu hala saglikli
        _, ok = run_example(PID)
        self.assertTrue(ok["ok"])

    def test_memory_bomb(self):
        payload = "java.util.List<long[]> l = new java.util.ArrayList<>(); while (true) l.add(new long[10_000_000]);"
        status, res = post("/api/run", with_payload(payload))
        self.assertFalse(res["ok"], res)
        _, ok = run_example(PID)
        self.assertTrue(ok["ok"])

    def test_no_leftover_work_dirs(self):
        work = os.path.join(ROOT, "build", "work")
        time.sleep(1)
        self.assertEqual(os.listdir(work), [], "gecici calisma klasorleri temizlenmeli")


if __name__ == "__main__":
    unittest.main(verbosity=2)
