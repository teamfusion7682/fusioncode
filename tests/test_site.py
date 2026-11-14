"""tools/build_site.py testleri (GitHub Actions'ta calisan uretim hatti).

Calistirma:  python3 tests/test_site.py
Onkosul:     ./build.sh
"""
import json
import os
import shutil
import subprocess
import sys
import tempfile
import unittest

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TOOL = os.path.join(ROOT, "tools", "build_site.py")


def build(robot_dir, out_dir, extra_env=None, args=()):
    env = {"PATH": os.environ["PATH"], "JAVA_TOOL_OPTIONS": ""}
    env.update(extra_env or {})
    p = subprocess.run([sys.executable, "-I", TOOL, "--robot", robot_dir, "--out", out_dir,
                        "--web", os.path.join(ROOT, "web"),
                        "--core-jar", os.path.join(ROOT, "build", "frcsim-core.jar"), *args],
                       capture_output=True, text=True, env=env, cwd=ROOT, timeout=300)
    return p


class SiteTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp(prefix="sitetest-")
        self.robot = os.path.join(self.tmp, "robot")
        shutil.copytree(os.path.join(ROOT, "robot"), self.robot)
        self.out = os.path.join(self.tmp, "site")

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def robot_java(self):
        return os.path.join(self.robot, "src", "frc", "robot", "Robot.java")

    def patch_robot(self, old, new):
        p = self.robot_java()
        text = open(p, encoding="utf-8").read()
        self.assertIn(old, text)
        open(p, "w", encoding="utf-8").write(text.replace(old, new, 1))

    def load(self, name):
        with open(os.path.join(self.out, "data", name), encoding="utf-8") as fh:
            return json.load(fh)

    def test_success_outputs(self):
        p = build(self.robot, self.out)
        self.assertEqual(p.returncode, 0, p.stderr + p.stdout)
        idx = self.load("index.json")
        self.assertEqual([s["id"] for s in idx["scenarios"]], ["weak", "pid", "teleop"])
        self.assertIn("frc/robot/Robot.java", idx["files"])
        self.assertIn("drivetrain", json.loads(idx["config"]))
        weak = [v for v in self.load("weak.json")["telemetry"]["Mesafe (m)"] if v is not None]
        pid = [v for v in self.load("pid.json")["telemetry"]["Mesafe (m)"] if v is not None]
        self.assertGreater(weak[-1], 3.5)
        self.assertLess(abs(pid[-1] - 3.0), 0.1)
        self.assertGreater(self.load("teleop.json")["frames"]["x"][-1], 1.0)
        with open(os.path.join(self.out, "index.html"), encoding="utf-8") as fh:
            html = fh.read()
        self.assertIn('content="static"', html)
        self.assertNotIn('content="dev"', html)
        self.assertTrue(os.path.exists(os.path.join(self.out, ".nojekyll")))
        self.assertTrue(os.path.exists(os.path.join(self.out, "app.js")))

    def test_repo_env_is_validated(self):
        p = build(self.robot, self.out, {"GITHUB_REPOSITORY": "takim1234/robot-sim"})
        self.assertEqual(p.returncode, 0, p.stderr)
        self.assertEqual(self.load("index.json")["repo"], "takim1234/robot-sim")
        p = build(self.robot, self.out, {"GITHUB_REPOSITORY": 'x/y"><script>'})
        self.assertEqual(p.returncode, 0, p.stderr)
        self.assertIsNone(self.load("index.json")["repo"], "gecersiz repo adi siteye gecmemeli")

    def test_compile_error_fails_with_annotation(self):
        self.patch_robot("private String selected = AUTO_WEAK;", "private String selected = 42;")
        p = build(self.robot, self.out, {"GITHUB_ACTIONS": "true"})
        self.assertEqual(p.returncode, 1)
        self.assertIn("Robot.java", p.stderr)
        self.assertRegex(p.stdout, r"::error file=.*Robot\.java,line=\d+::.*incompatible types")
        self.assertFalse(os.path.exists(os.path.join(self.out, "data", "index.json")),
                         "hatali derlemede site uretilmemeli")

    def test_scenario_crash_fails_build(self):
        self.patch_robot("distancePid.setTolerance(0.03);", "Object o = null; o.toString();")
        p = build(self.robot, self.out, {"GITHUB_ACTIONS": "true"})
        self.assertEqual(p.returncode, 1)
        self.assertIn("NullPointerException", p.stderr)
        self.assertIn("weak", p.stderr)

    def test_infinite_loop_times_out(self):
        self.patch_robot("distancePid.setTolerance(0.03);", "if (System.nanoTime() > 0) { while (true) { } }")
        p = build(self.robot, self.out, args=("--timeout", "6"))
        self.assertEqual(p.returncode, 1)
        self.assertIn("bitmedi", p.stderr)

    def test_hostile_code_is_contained_in_ci(self):
        marker = "/tmp/frcsim-ci-pwned"
        if os.path.exists(marker):
            os.remove(marker)
        self.patch_robot("distancePid.setTolerance(0.03);",
                         'try { new java.io.FileWriter("%s").write("x"); } catch (java.io.IOException e) { }' % marker)
        p = build(self.robot, self.out)
        self.assertEqual(p.returncode, 1)
        self.assertIn("izin verilmiyor", p.stderr)
        self.assertFalse(os.path.exists(marker))

    def test_auto_discovery_without_scenarios_json(self):
        os.remove(os.path.join(self.robot, "scenarios.json"))
        p = build(self.robot, self.out)
        self.assertEqual(p.returncode, 0, p.stderr)
        idx = self.load("index.json")
        self.assertEqual(len(idx["scenarios"]), 2)
        self.assertEqual({s["id"] for s in idx["scenarios"]}, {"auto-1", "auto-2"})
        finals = sorted(self.load(s["file"])["frames"]["x"][-1] for s in idx["scenarios"])
        self.assertLess(abs(finals[0] - 3.0), 0.1)
        self.assertGreater(finals[1], 3.5)

    def test_bad_scenarios_json(self):
        path = os.path.join(self.robot, "scenarios.json")
        bad = {
            "duplicate": {"scenarios": [{"id": "a"}, {"id": "a"}]},
            "bad id": {"scenarios": [{"id": "../x"}]},
            "bad mode": {"scenarios": [{"id": "a", "mode": "fly"}]},
            "bad duration": {"scenarios": [{"id": "a", "duration": 9999}]},
            "empty": {"scenarios": []},
        }
        for name, doc in bad.items():
            with self.subTest(case=name):
                json.dump(doc, open(path, "w"))
                p = build(self.robot, self.out)
                self.assertEqual(p.returncode, 1, name)
                self.assertIn("scenarios.json", p.stderr)

    def test_invalid_config_fails(self):
        cfg = os.path.join(self.robot, "robot-config.json")
        data = json.load(open(cfg))
        data["drivetrain"]["motor"] = "YokBoyleMotor"
        json.dump(data, open(cfg, "w"))
        p = build(self.robot, self.out)
        self.assertEqual(p.returncode, 1)
        self.assertIn("YokBoyleMotor", p.stderr)

    def test_forbidden_source_paths(self):
        evil = os.path.join(self.robot, "src", "edu", "wpi", "first", "wpilibj")
        os.makedirs(evil)
        open(os.path.join(evil, "Timer.java"), "w").write("package edu.wpi.first.wpilibj; public class Timer {}")
        p = build(self.robot, self.out)
        self.assertEqual(p.returncode, 1)
        self.assertIn("kullanilamaz", p.stderr)


if __name__ == "__main__":
    unittest.main(verbosity=2)
