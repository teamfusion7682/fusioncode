#!/usr/bin/env python3
"""Takimin robot kodunu derler, senaryolari JVM'de simule eder ve statik siteyi uretir.

GitHub Actions bunu her push'ta calistirir; cikti klasoru (varsayilan: site/) GitHub Pages'e yuklenir.
Sunucu gerekmez: sayfa, burada uretilen onceden hesaplanmis sonuclari oynatir.

Kullanim:  python3 tools/build_site.py [--robot robot] [--out site]
Hata olursa cikis kodu 1'dir ve Actions'ta satir bazli aciklama (annotation) olarak gorunur;
bu durumda site GUNCELLENMEZ, onceki surum yayinda kalir.
"""
import argparse
import datetime
import itertools
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile

PATH_OK = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*(/[A-Za-z_][A-Za-z0-9_]*)*\.java$")
DENIED_PREFIXES = ("edu/", "frcsim/", "java/", "javax/", "jdk/", "sun/", "com/sun/")
ID_OK = re.compile(r"^[a-z0-9][a-z0-9-]{0,40}$")
REPO_OK = re.compile(r"^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")
MODES = ("autonomous", "teleop", "test")
MAX_AUTO_COMBOS = 12
MAX_FILES = 40
MAX_FILE_BYTES = 200_000


def annotate(msg, file=None, line=None):
    """GitHub Actions'ta kirmizi hata notu olarak gorunur."""
    if not os.environ.get("GITHUB_ACTIONS"):
        return
    params = []
    if file:
        params.append("file=" + file.replace("%", "%25").replace(",", "%2C").replace(":", "%3A"))
    if line:
        params.append("line=" + str(line))
    safe = msg.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
    print("::error " + ",".join(params) + "::" + safe if params else "::error::" + safe)


def fail(msg, file=None, line=None):
    annotate(msg, file, line)
    print("HATA: " + msg, file=sys.stderr)
    sys.exit(1)


def clean_env():
    return {"PATH": os.environ.get("PATH", "/usr/bin:/bin"), "LANG": "C.UTF-8", "JAVA_TOOL_OPTIONS": ""}


def check_java():
    try:
        out = subprocess.run(["java", "-version"], capture_output=True, text=True, env=clean_env(), timeout=30)
    except (OSError, subprocess.TimeoutExpired) as e:
        fail("java calistirilamadi: %s. JDK 17-23 kurun." % e)
    m = re.search(r'version "(\d+)', out.stderr + out.stdout)
    if not m:
        fail("java surumu okunamadi.")
    major = int(m.group(1))
    if major < 17 or major >= 24:
        fail("JDK 17-23 gerekli (bulunan: %d). Workflow'da java-version: '21' kullanin." % major)


def collect_sources(src_dir, repo_src_prefix):
    if not os.path.isdir(src_dir):
        fail("Robot kaynak klasoru bulunamadi: %s" % src_dir)
    files = {}
    for root, _dirs, names in os.walk(src_dir):
        for n in sorted(names):
            if not n.endswith(".java"):
                continue
            full = os.path.join(root, n)
            rel = os.path.relpath(full, src_dir).replace(os.sep, "/")
            if not PATH_OK.match(rel):
                fail("Gecersiz dosya yolu: %s (ornek: frc/robot/Robot.java)" % rel, repo_src_prefix + rel)
            if rel.startswith(DENIED_PREFIXES):
                fail("Bu paketler kullanilamaz: %s" % rel, repo_src_prefix + rel)
            if os.path.getsize(full) > MAX_FILE_BYTES:
                fail("Dosya cok buyuk (en fazla %d KB): %s" % (MAX_FILE_BYTES // 1000, rel), repo_src_prefix + rel)
            with open(full, encoding="utf-8") as fh:
                files[rel] = fh.read()
    if not files:
        fail("%s icinde .java dosyasi yok." % src_dir)
    if len(files) > MAX_FILES:
        fail("En fazla %d dosya desteklenir." % MAX_FILES)
    return files


def compile_sources(src_dir, files, core_jar, classes_dir, repo_src_prefix):
    cmd = ["javac", "--release", "17", "-proc:none", "-implicit:none", "-nowarn", "-encoding", "UTF-8",
           "-cp", core_jar, "-d", classes_dir] + [os.path.join(src_dir, f) for f in files]
    p = subprocess.run(cmd, capture_output=True, text=True, env=clean_env(), timeout=120)
    if p.returncode != 0:
        text = (p.stdout + p.stderr).replace(os.path.abspath(src_dir) + os.sep, "").replace(src_dir + os.sep, "")
        text = "\n".join(l for l in text.splitlines() if not l.startswith("Picked up")).strip()
        for m in re.finditer(r"^(\S+\.java):(\d+): error: (.*)$", text, re.M):
            annotate(m.group(3), repo_src_prefix + m.group(1), m.group(2))
        print("Derleme hatasi:\n" + text, file=sys.stderr)
        sys.exit(1)


def simulate(job, core_jar, work, name, timeout):
    job_file = os.path.join(work, name + "-job.json")
    result_file = os.path.join(work, name + "-result.json")
    job = dict(job)
    job["resultFile"] = result_file
    with open(job_file, "w", encoding="utf-8") as fh:
        json.dump(job, fh)
    cmd = ["java", "-Xmx256m", "-Xss2m", "-XX:+ExitOnOutOfMemoryError", "-XX:TieredStopAtLevel=1",
           "-Djava.security.manager=allow", "-Dfile.encoding=UTF-8", "-cp", core_jar,
           "frcsim.host.SimRunner", job_file]
    try:
        p = subprocess.run(cmd, capture_output=True, text=True, env=clean_env(), timeout=timeout, cwd=work)
    except subprocess.TimeoutExpired:
        return {"ok": False, "stage": "timeout",
                "error": "%d saniyede bitmedi (sonsuz dongu olabilir)." % timeout}
    if not os.path.isfile(result_file):
        tail = "\n".join(l for l in (p.stdout + p.stderr).splitlines()
                         if not l.startswith(("WARNING:", "Picked up")))[-800:]
        return {"ok": False, "stage": "run", "error": "Simulasyon sonuc uretmedi (cikis %d).\n%s" % (p.returncode, tail)}
    with open(result_file, encoding="utf-8") as fh:
        return json.load(fh)


def load_json(path, what):
    try:
        with open(path, encoding="utf-8") as fh:
            return json.load(fh)
    except FileNotFoundError:
        fail("%s bulunamadi: %s" % (what, path))
    except ValueError as e:
        fail("%s gecerli JSON degil (%s): %s" % (what, path, e))


def validate_scenarios(doc, path):
    if not isinstance(doc, dict) or not isinstance(doc.get("scenarios"), list) or not doc["scenarios"]:
        fail("%s: 'scenarios' bos olmayan bir liste olmali." % path)
    out, seen = [], set()
    for i, s in enumerate(doc["scenarios"]):
        where = "%s: senaryo #%d" % (path, i + 1)
        if not isinstance(s, dict):
            fail(where + " bir nesne olmali.")
        sid = s.get("id")
        if not isinstance(sid, str) or not ID_OK.match(sid):
            fail(where + ": 'id' kucuk harf/rakam/tire olmali (ornek: pid-testi).")
        if sid in seen:
            fail(where + ": '%s' kimligi tekrar ediyor." % sid)
        seen.add(sid)
        mode = s.get("mode", "autonomous")
        if mode not in MODES:
            fail(where + ": 'mode' su degerlerden biri olmali: " + ", ".join(MODES))
        duration = s.get("duration", 6)
        if not isinstance(duration, (int, float)) or isinstance(duration, bool) or not 1 <= duration <= 60:
            fail(where + ": 'duration' 1-60 saniye arasinda olmali.")
        chooser = s.get("chooser", {})
        if not isinstance(chooser, dict) or not all(isinstance(v, str) for v in chooser.values()):
            fail(where + ": 'chooser' anahtar->metin eslemesi olmali.")
        joystick = s.get("joystick")
        if joystick is not None and not isinstance(joystick, list):
            fail(where + ": 'joystick' bir liste olmali.")
        label = s.get("label", sid)
        if not isinstance(label, str):
            fail(where + ": 'label' metin olmali.")
        out.append({"id": sid, "label": label, "mode": mode, "duration": duration,
                    "chooser": chooser, "joystick": joystick})
    return out


def discover_scenarios(config, core_jar, work, timeout):
    """scenarios.json yoksa: otonom secicisindeki her secenek icin bir senaryo uret."""
    res = simulate({"classesDir": os.path.join(work, "classes"), "mainClass": "frc.robot.Main",
                    "mode": "autonomous", "duration": 2, "config": config}, core_jar, work, "discover", timeout)
    if not res.get("ok"):
        fail("Robot kodu calistirilamadi: %s" % res.get("error"))
    choosers = res.get("choosers") or {}
    keys = sorted(choosers)
    if not keys:
        return [{"id": "otonom", "label": "Otonom", "mode": "autonomous", "duration": 8, "chooser": {}, "joystick": None}]
    combos = list(itertools.product(*[choosers[k]["options"] for k in keys]))
    if len(combos) > MAX_AUTO_COMBOS:
        fail("Secici kombinasyonlari cok fazla (%d > %d). robot/scenarios.json ile senaryolari kendiniz secin."
             % (len(combos), MAX_AUTO_COMBOS))
    out = []
    for n, combo in enumerate(combos, 1):
        out.append({"id": "auto-%d" % n, "label": " · ".join(combo), "mode": "autonomous", "duration": 8,
                    "chooser": dict(zip(keys, combo)), "joystick": None})
    return out


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--robot", default="robot", help="robot klasoru (src/, robot-config.json, scenarios.json)")
    ap.add_argument("--web", default="web", help="arayuz klasoru")
    ap.add_argument("--core-jar", default="build/frcsim-core.jar")
    ap.add_argument("--out", default="site")
    ap.add_argument("--timeout", type=int, default=60, help="senaryo basina saniye")
    args = ap.parse_args()

    robot_dir = os.path.abspath(args.robot)
    src_dir = os.path.join(robot_dir, "src")
    core_jar = os.path.abspath(args.core_jar)
    repo_src_prefix = os.path.relpath(src_dir, os.getcwd()).replace(os.sep, "/") + "/"
    if not os.path.isfile(core_jar):
        fail("Cekirdek jar bulunamadi (%s). Once ./build.sh calistirin." % core_jar)
    check_java()

    config = load_json(os.path.join(robot_dir, "robot-config.json"), "robot-config.json")
    with open(os.path.join(robot_dir, "robot-config.json"), encoding="utf-8") as fh:
        config_text = fh.read()
    files = collect_sources(src_dir, repo_src_prefix)

    work = tempfile.mkdtemp(prefix="frcsim-site-")
    try:
        classes = os.path.join(work, "classes")
        os.makedirs(classes)
        compile_sources(src_dir, files, core_jar, classes, repo_src_prefix)
        print("Derleme tamam (%d dosya)." % len(files))

        scen_path = os.path.join(robot_dir, "scenarios.json")
        title = None
        if os.path.isfile(scen_path):
            doc = load_json(scen_path, "scenarios.json")
            scenarios = validate_scenarios(doc, os.path.relpath(scen_path, os.getcwd()))
            title = doc.get("title") if isinstance(doc.get("title"), str) else None
        else:
            print("scenarios.json yok: otonom secicisinden senaryolar otomatik uretiliyor.")
            scenarios = discover_scenarios(config, core_jar, work, args.timeout)

        out_dir = os.path.abspath(args.out)
        if os.path.isdir(out_dir):
            shutil.rmtree(out_dir)
        shutil.copytree(args.web, out_dir)
        os.makedirs(os.path.join(out_dir, "data"))

        index_html = os.path.join(out_dir, "index.html")
        with open(index_html, encoding="utf-8") as fh:
            html = fh.read()
        marker = '<meta name="frcsim-mode" content="dev">'
        if marker not in html:
            fail("web/index.html icinde mod isareti bulunamadi: " + marker)
        with open(index_html, "w", encoding="utf-8") as fh:
            fh.write(html.replace(marker, '<meta name="frcsim-mode" content="static">'))
        open(os.path.join(out_dir, ".nojekyll"), "w").close()

        index_scen = []
        print("%-14s %-9s %7s %9s %9s" % ("senaryo", "mod", "kare", "son x (m)", "boyut"))
        for s in scenarios:
            job = {"classesDir": classes, "mainClass": "frc.robot.Main", "mode": s["mode"],
                   "duration": s["duration"], "config": config, "chooser": s["chooser"]}
            if s["joystick"] is not None:
                job["joystick"] = s["joystick"]
            res = simulate(job, core_jar, work, "sc-" + s["id"], args.timeout)
            if not res.get("ok"):
                fail("Senaryo '%s' basarisiz (%s):\n%s" % (s["id"], res.get("stage"), res.get("error")),
                     repo_src_prefix.rstrip("/"))
            res.pop("choosers", None)
            fname = s["id"] + ".json"
            path = os.path.join(out_dir, "data", fname)
            with open(path, "w", encoding="utf-8") as fh:
                json.dump(res, fh, separators=(",", ":"))
            fr = res["frames"]
            print("%-14s %-9s %7d %9.3f %7d KB" % (s["id"], s["mode"], len(fr["t"]), fr["x"][-1],
                                                    os.path.getsize(path) // 1024))
            index_scen.append({"id": s["id"], "label": s["label"], "mode": s["mode"],
                               "duration": s["duration"], "file": fname})

        repo = os.environ.get("GITHUB_REPOSITORY", "")
        index = {
            "format": "frcsim-site", "version": 1,
            "title": title or str(config.get("robotName", "FRC robot")),
            "robotName": str(config.get("robotName", "")),
            "built": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d %H:%M UTC"),
            "repo": repo if REPO_OK.match(repo) else None,
            "files": files, "config": config_text, "scenarios": index_scen,
        }
        with open(os.path.join(out_dir, "data", "index.json"), "w", encoding="utf-8") as fh:
            json.dump(index, fh, ensure_ascii=False, separators=(",", ":"))
        print("Site hazir: %s (%d senaryo)" % (out_dir, len(index_scen)))
    finally:
        shutil.rmtree(work, ignore_errors=True)


if __name__ == "__main__":
    main()
