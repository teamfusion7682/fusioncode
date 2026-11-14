"""Command-Based destegi + ornek proje testleri.

Calistirma:  python3 tests/test_command.py
Onkosul:     ./build.sh
"""
import json
import math
import os
import shutil
import subprocess
import sys
import tempfile
import unittest

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, "tests"))
from test_site import build  # noqa: E402


class CommandBasedTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tmp = tempfile.mkdtemp(prefix="cmdtest-")
        cls.robot = os.path.join(cls.tmp, "robot")
        shutil.copytree(os.path.join(ROOT, "robot-ornekleri", "command-tabanli"), cls.robot)
        cls.out = os.path.join(cls.tmp, "site")
        p = build(cls.robot, cls.out)
        assert p.returncode == 0, p.stderr + p.stdout

    @classmethod
    def tearDownClass(cls):
        shutil.rmtree(cls.tmp, ignore_errors=True)

    def load(self, name):
        with open(os.path.join(self.out, "data", name), encoding="utf-8") as fh:
            return json.load(fh)

    def test_route_sequence_finishes_in_order(self):
        d = self.load("rota.json")
        cmds = {c["name"]: c for c in d["commands"] if c["depth"] == 1}
        a = cmds["DriveDistance(2.0 m)"]
        b = cmds["TurnToAngle(90.0 derece)"]
        c = cmds["DriveDistance(1.0 m)"]
        self.assertLessEqual(a["end"], b["start"] + 0.03)
        self.assertLessEqual(b["end"], c["start"] + 0.03)
        for x in (a, b, c):
            self.assertFalse(x["interrupted"])
        f = d["frames"]
        self.assertAlmostEqual(f["x"][-1], 2.0, delta=0.15)
        self.assertAlmostEqual(f["y"][-1], 1.0, delta=0.2)
        self.assertAlmostEqual(math.degrees(f["theta"][-1]), 90.0, delta=6.0)

    def test_group_is_parent_of_children(self):
        d = self.load("rota.json")
        parent = [c for c in d["commands"] if c["name"] == "Rota"][0]
        self.assertEqual(parent["depth"], 0)
        kids = [c for c in d["commands"] if c["depth"] == 1 and c["start"] >= parent["start"] - 1e-9 and c["end"] <= parent["end"] + 1e-9]
        self.assertEqual(len(kids), 3)

    def test_timeout_interrupts_drive(self):
        d = self.load("zaman-asimi.json")
        drive = [c for c in d["commands"] if c["name"].startswith("DriveDistance")][0]
        self.assertTrue(drive["interrupted"])
        self.assertAlmostEqual(drive["end"] - drive["start"], 1.5, delta=0.06)
        self.assertLess(d["frames"]["x"][-1], 5.0)  # 5 m'ye ulasamadi, kesildi

    def test_default_command_drives_in_teleop(self):
        d = self.load("teleop.json")
        self.assertGreater(d["frames"]["x"][-1], 1.0)
        self.assertTrue(any(c["name"] == "RunCommand" for c in d["commands"]))

    def test_deterministic(self):
        out2 = os.path.join(self.tmp, "site2")
        p = build(self.robot, out2)
        self.assertEqual(p.returncode, 0, p.stderr)
        for n in ("rota.json", "zaman-asimi.json"):
            a = json.load(open(os.path.join(self.out, "data", n), encoding="utf-8"))
            b = json.load(open(os.path.join(out2, "data", n), encoding="utf-8"))
            self.assertEqual(a["frames"]["x"], b["frames"]["x"])
            self.assertEqual(a["commands"], b["commands"])


class ExamplesCompileTest(unittest.TestCase):
    def test_pid_story_example_builds(self):
        tmp = tempfile.mkdtemp(prefix="pidstory-")
        try:
            robot = os.path.join(tmp, "robot")
            src = os.path.join(robot, "src", "frc", "robot")
            os.makedirs(src)
            shutil.copy(os.path.join(ROOT, "robot", "src", "frc", "robot", "Main.java"), src)
            shutil.copy(os.path.join(ROOT, "robot-ornekleri", "pid-hikayesi", "Robot.java"), src)
            shutil.copy(os.path.join(ROOT, "robot-ornekleri", "pid-hikayesi", "scenarios.json"), robot)
            shutil.copy(os.path.join(ROOT, "robot", "robot-config.json"), robot)
            p = build(robot, os.path.join(tmp, "site"))
            self.assertEqual(p.returncode, 0, p.stderr + p.stdout)
            d = json.load(open(os.path.join(tmp, "site", "data", "rota.json"), encoding="utf-8"))
            self.assertAlmostEqual(d["frames"]["y"][-1], 1.0, delta=0.25)
        finally:
            shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    unittest.main()
