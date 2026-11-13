'use strict';

/* ======================================================================
   FRC Web Simülatör arayüzü. Bağımlılık yok (internet olmadan da çalışır).
   Takım kodu sunucuda derlenip çalıştırılır; burada sonuç oynatılır.
   ====================================================================== */

const CONFIG_TAB = 'robot-config.json';
const RUN_COLORS = ['#4cc2ff', '#ff8a5c', '#7bd88f', '#d49bff', '#ffd166', '#ff6b9d'];
const MAX_RUNS = 6;
const STORE_KEY = 'frcsim.project.v1';
const DEFAULT_JOYSTICK =
  '[{"t":0,"axes":[0,-0.8,0,0,0,0]},{"t":2,"axes":[0,0,0,0,0.5,0]},{"t":3,"axes":[0,0,0,0,0,0]}]';

const $ = (sel) => document.querySelector(sel);

const state = {
  files: {},            // "frc/robot/Robot.java" -> kaynak
  config: '{}',         // robot-config.json metni
  current: 'frc/robot/Robot.java',
  errorTab: null,
  runs: [],
  active: -1,
  runCounter: 0,
  t: 0,
  playing: false,
  speed: 1,
  chartKey: 'Mesafe (m)',
  chooserDefs: {},      // anahtar -> {options, default}
  chooserSel: {},       // anahtar -> seçili değer
  busy: false,
  dirty: true,
  static: false,        // GitHub Pages modu: sonuçlar yayın sırasında önceden hesaplanmış
  index: null,
  cache: {},
};

/* ------------------------------------------------------------- yardımcılar */

function esc(s) {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function storeGet(key) {
  try { return localStorage.getItem(key); } catch (e) { return null; }
}
function storeSet(key, value) {
  try { localStorage.setItem(key, value); } catch (e) { /* özel pencere vb. */ }
}

function cssVar(name) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}

function fmt(v, d = 2) {
  return v === null || v === undefined || Number.isNaN(v) ? '–' : Number(v).toFixed(d);
}

/* ------------------------------------------------------- sözdizimi renkleri */

const KEYWORDS = new Set(('abstract assert boolean break byte case catch char class const continue default do double else ' +
  'enum extends final finally float for if implements import instanceof int interface long native new package private ' +
  'protected public return short static strictfp super switch synchronized this throw throws transient try void volatile ' +
  'while var record true false null').split(' '));

function highlightJava(src) {
  const re = /(\/\*[\s\S]*?\*\/|\/\/[^\n]*)|("(?:[^"\\\n]|\\.)*"|'(?:[^'\\\n]|\\.)*')|(@\w+)|\b(\d[\d_]*\.?\d*(?:[eE][+-]?\d+)?[fFdDlL]?)\b|\b([A-Za-z_]\w*)\b/g;
  let out = '';
  let last = 0;
  let m;
  while ((m = re.exec(src)) !== null) {
    out += esc(src.slice(last, m.index));
    last = re.lastIndex;
    if (m[1]) out += '<span class="c">' + esc(m[1]) + '</span>';
    else if (m[2]) out += '<span class="s">' + esc(m[2]) + '</span>';
    else if (m[3]) out += '<span class="a">' + esc(m[3]) + '</span>';
    else if (m[4]) out += '<span class="n">' + esc(m[4]) + '</span>';
    else if (KEYWORDS.has(m[5])) out += '<span class="k">' + m[5] + '</span>';
    else if (/^[A-Z]/.test(m[5])) out += '<span class="t">' + m[5] + '</span>';
    else out += m[5];
  }
  return out + esc(src.slice(last)) + '\n';
}

function highlightJson(src) {
  const re = /("(?:[^"\\\n]|\\.)*")(\s*:)?|\b(true|false|null)\b|(-?\d+\.?\d*(?:[eE][+-]?\d+)?)/g;
  let out = '';
  let last = 0;
  let m;
  while ((m = re.exec(src)) !== null) {
    out += esc(src.slice(last, m.index));
    last = re.lastIndex;
    if (m[1] !== undefined) {
      out += '<span class="' + (m[2] ? 'p' : 's') + '">' + esc(m[1]) + '</span>' + (m[2] ? esc(m[2]) : '');
    } else if (m[3]) out += '<span class="k">' + m[3] + '</span>';
    else out += '<span class="n">' + m[4] + '</span>';
  }
  return out + esc(src.slice(last)) + '\n';
}

/* ------------------------------------------------------------------ editör */

const codeEl = $('#code');
const hlEl = $('#hl');
const gutterEl = $('#gutter');

function currentText() {
  return state.current === CONFIG_TAB ? state.config : (state.files[state.current] || '');
}

function renderEditor() {
  const text = codeEl.value;
  hlEl.innerHTML = state.current === CONFIG_TAB ? highlightJson(text) : highlightJava(text);
  const lines = text.split('\n').length;
  let g = '';
  for (let i = 1; i <= lines; i++) g += i + '\n';
  gutterEl.textContent = g;
  syncScroll();
}

function syncScroll() {
  hlEl.scrollTop = codeEl.scrollTop;
  hlEl.scrollLeft = codeEl.scrollLeft;
  gutterEl.scrollTop = codeEl.scrollTop;
}

function saveEditorToState() {
  if (state.current === CONFIG_TAB) state.config = codeEl.value;
  else if (state.current in state.files) state.files[state.current] = codeEl.value;
}

function persist() {
  if (state.static) return;
  storeSet(STORE_KEY, JSON.stringify({ files: state.files, config: state.config, current: state.current }));
}

codeEl.addEventListener('input', () => {
  saveEditorToState();
  if (state.errorTab === state.current) { state.errorTab = null; renderTabs(); }
  renderEditor();
  persist();
});
codeEl.addEventListener('scroll', syncScroll);

codeEl.addEventListener('keydown', (e) => {
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') { e.preventDefault(); run(); return; }
  if (state.static) return;   // salt okunur
  if (e.key === 'Tab' && !e.shiftKey && !e.ctrlKey && !e.altKey) {
    e.preventDefault();
    codeEl.setRangeText('  ', codeEl.selectionStart, codeEl.selectionEnd, 'end');
    codeEl.dispatchEvent(new Event('input'));
  } else if (e.key === 'Enter' && !e.shiftKey) {
    // otomatik girinti
    e.preventDefault();
    const pos = codeEl.selectionStart;
    const before = codeEl.value.slice(0, pos);
    const lineStart = before.lastIndexOf('\n') + 1;
    const line = before.slice(lineStart);
    let indent = /^\s*/.exec(line)[0];
    if (/\{\s*$/.test(line)) indent += '  ';
    codeEl.setRangeText('\n' + indent, pos, codeEl.selectionEnd, 'end');
    codeEl.dispatchEvent(new Event('input'));
  }
});

function renderTabs() {
  const tabs = $('#tabs');
  tabs.textContent = '';
  const names = Object.keys(state.files).concat([CONFIG_TAB]);
  for (const name of names) {
    const tab = document.createElement('div');
    tab.className = 'tab' + (state.errorTab === name ? ' err' : '');
    tab.setAttribute('role', 'tab');
    tab.setAttribute('aria-selected', String(name === state.current));
    tab.tabIndex = 0;
    const label = document.createElement('span');
    label.textContent = name === CONFIG_TAB ? name : name.split('/').pop();
    label.title = name;
    tab.appendChild(label);
    if (name !== CONFIG_TAB && !state.static) {
      const x = document.createElement('span');
      x.className = 'x';
      x.textContent = '×';
      x.title = 'Dosyayı sil';
      x.addEventListener('click', (ev) => { ev.stopPropagation(); deleteFile(name); });
      tab.appendChild(x);
    }
    const open = () => selectFile(name);
    tab.addEventListener('click', open);
    tab.addEventListener('keydown', (ev) => { if (ev.key === 'Enter' || ev.key === ' ') { ev.preventDefault(); open(); } });
    tabs.appendChild(tab);
  }
  if (state.static) return;
  const add = document.createElement('button');
  add.className = 'tab add';
  add.textContent = '+ Dosya';
  add.addEventListener('click', addFile);
  tabs.appendChild(add);
}

function selectFile(name) {
  saveEditorToState();
  state.current = name;
  codeEl.value = currentText();
  renderTabs();
  renderEditor();
  persist();
}

function addFile() {
  const name = (prompt('Dosya yolu (örn. frc/robot/Intake.java):', 'frc/robot/Intake.java') || '').trim();
  if (!name) return;
  if (!/^[A-Za-z_][A-Za-z0-9_]*(\/[A-Za-z_][A-Za-z0-9_]*)*\.java$/.test(name)) {
    alert('Geçersiz yol. Örnek: frc/robot/Intake.java'); return;
  }
  if (name in state.files) { alert('Bu dosya zaten var.'); return; }
  const parts = name.slice(0, -5).split('/');
  const cls = parts.pop();
  state.files[name] = (parts.length ? 'package ' + parts.join('.') + ';\n\n' : '') + 'public class ' + cls + ' {\n  \n}\n';
  selectFile(name);
}

function deleteFile(name) {
  if (!confirm(name + ' silinsin mi?')) return;
  delete state.files[name];
  if (state.current === name) state.current = CONFIG_TAB in state.files ? CONFIG_TAB : (Object.keys(state.files)[0] || CONFIG_TAB);
  codeEl.value = currentText();
  renderTabs();
  renderEditor();
  persist();
}

/* ---------------------------------------------------------------- konsol */

const consoleEl = $('#console');

function logLine(text, cls) {
  document.body.classList.add('has-log');
  const span = document.createElement('span');
  if (cls) span.className = cls;
  span.textContent = text + '\n';
  consoleEl.appendChild(span);
  consoleEl.scrollTop = consoleEl.scrollHeight;
}

function setStatus(text, busy) {
  const el = $('#status');
  el.textContent = text;
  el.classList.toggle('busy', !!busy);
}

$('#btn-clear').addEventListener('click', () => { consoleEl.textContent = ''; });

function jumpToError(message) {
  // javac: "frc/robot/Robot.java:23: error: ..."
  const m = /^(\S+\.java):(\d+): error/m.exec(message);
  if (!m) return;
  const file = Object.keys(state.files).find((f) => f === m[1] || f.endsWith('/' + m[1]));
  if (!file) return;
  state.errorTab = file;
  selectFile(file);
  const lineNo = parseInt(m[2], 10);
  const lines = codeEl.value.split('\n');
  let start = 0;
  for (let i = 0; i < lineNo - 1 && i < lines.length; i++) start += lines[i].length + 1;
  codeEl.focus();
  codeEl.setSelectionRange(start, start + (lines[lineNo - 1] || '').length);
  codeEl.scrollTop = Math.max(0, (lineNo - 4) * 20);
  syncScroll();
}

/* ------------------------------------------------------------- çalıştırma */

function readJoystick() {
  try {
    const v = JSON.parse($('#joystick').value);
    if (!Array.isArray(v)) throw new Error('liste olmalı');
    return v;
  } catch (e) {
    throw new Error('Teleop joystick senaryosu geçerli bir JSON listesi olmalı: ' + e.message);
  }
}

async function run() {
  if (state.static) return addScenario($('#scenario').value, false);
  if (state.busy) return;
  saveEditorToState();
  persist();
  let cfg;
  try {
    cfg = JSON.parse(state.config);
  } catch (e) {
    logLine('robot-config.json geçerli JSON değil: ' + e.message, 'err');
    state.errorTab = CONFIG_TAB;
    renderTabs();
    return;
  }
  const mode = $('#mode').value;
  const body = {
    files: state.files,
    config: cfg,
    mode,
    duration: Math.max(1, Math.min(60, parseFloat($('#duration').value) || 6)),
    chooser: state.chooserSel,
  };
  if (mode === 'teleop') {
    try { body.joystick = readJoystick(); } catch (e) { logLine(e.message, 'err'); return; }
  }

  state.busy = true;
  state.errorTab = null;
  renderTabs();
  $('#run').disabled = true;
  setStatus('Derleniyor ve çalıştırılıyor…', true);
  const t0 = performance.now();
  let data;
  try {
    const res = await fetch('/api/run', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
    data = await res.json();
  } catch (e) {
    logLine('Sunucuya ulaşılamadı: ' + e.message, 'err');
    setStatus('Hata', false);
    state.busy = false;
    $('#run').disabled = false;
    return;
  }
  state.busy = false;
  $('#run').disabled = false;
  const secs = ((performance.now() - t0) / 1000).toFixed(1);

  if (data.console) logLine(data.console, 'dim');
  if (data.choosers) updateChoosers(data.choosers);

  if (!data.ok) {
    const stageName = { compile: 'Derleme hatası', config: 'Yapılandırma hatası', timeout: 'Zaman aşımı',
      run: 'Çalışma hatası', busy: 'Sunucu meşgul', request: 'Geçersiz istek' }[data.stage] || 'Hata';
    logLine('— ' + stageName + ' —', 'err');
    logLine(data.error || 'Bilinmeyen hata', 'err');
    if (data.stage === 'compile') jumpToError(data.error || '');
    if (data.stage === 'config') { state.errorTab = CONFIG_TAB; renderTabs(); }
    setStatus(stageName + ' (' + secs + ' s)', false);
    if (!(data.frames && data.frames.t && data.frames.t.length > 2)) return;
    addRun(data, true);   // robot hata verene kadar nereye gittiyse onu göster
    return;
  }
  logLine('Çalıştırma tamam (' + secs + ' s)', 'okk');
  setStatus('Tamamlandı (' + secs + ' s)', false);
  addRun(data, false);
}

function runLabel(mode) {
  const parts = Object.keys(state.chooserSel).map((k) => state.chooserSel[k]).filter(Boolean);
  const base = parts.length ? parts.join(' · ') : (mode === 'teleop' ? 'Teleop' : 'Otonom');
  return '#' + (++state.runCounter) + ' ' + base;
}

function addRun(data, failed, label, scenarioId) {
  const used = new Set(state.runs.map((r) => r.color));
  const color = RUN_COLORS.find((c) => !used.has(c)) || RUN_COLORS[state.runCounter % RUN_COLORS.length];
  const run = { data, color, visible: true, failed, label: label || runLabel($('#mode').value), scenarioId };
  state.runs.push(run);
  if (state.runs.length > MAX_RUNS) state.runs.shift();
  state.active = state.runs.indexOf(run);
  state.t = 0;
  state.playing = true;
  populateChartKeys();
  recomputeView();
  renderRuns();
  syncPlayButton();
  $('#empty').hidden = true;
  state.dirty = true;
}

function updateChoosers(defs) {
  state.chooserDefs = defs;
  const holder = $('#choosers');
  holder.textContent = '';
  for (const key of Object.keys(defs)) {
    const def = defs[key];
    if (!state.chooserSel[key] || !def.options.includes(state.chooserSel[key])) state.chooserSel[key] = def.default;
    const label = document.createElement('label');
    label.textContent = key + ' ';
    const sel = document.createElement('select');
    for (const opt of def.options) {
      const o = document.createElement('option');
      o.value = opt;
      o.textContent = opt;
      if (opt === state.chooserSel[key]) o.selected = true;
      sel.appendChild(o);
    }
    sel.addEventListener('change', () => { state.chooserSel[key] = sel.value; });
    label.appendChild(sel);
    holder.appendChild(label);
  }
}

/* ----------------------------------------------------------- run listesi */

function activeRun() { return state.runs[state.active] || null; }
function visibleRuns() { return state.runs.filter((r) => r.visible); }

function renderRuns() {
  const holder = $('#runs');
  holder.textContent = '';
  state.runs.forEach((r, i) => {
    const el = document.createElement('div');
    el.className = 'run' + (i === state.active ? ' active' : '') + (r.visible ? '' : ' hidden') + (r.failed ? ' failed' : '');
    el.style.setProperty('--rc', r.color);
    const dot = document.createElement('span');
    dot.className = 'dot';
    const lbl = document.createElement('button');
    lbl.className = 'lbl';
    lbl.textContent = r.label;
    lbl.title = 'Oynatmak için seç';
    lbl.addEventListener('click', () => { state.active = i; state.t = 0; state.playing = true; renderRuns(); syncPlayButton(); state.dirty = true; });
    const val = document.createElement('span');
    val.className = 'val';
    const series = getSeries(r, state.chartKey);
    const last = series ? series[series.length - 1] : null;
    val.textContent = last === null || last === undefined ? '' : 'son ' + fmt(last);
    const eye = document.createElement('button');
    eye.className = 'mini';
    eye.textContent = r.visible ? '👁' : '–';
    eye.title = r.visible ? 'Gizle' : 'Göster';
    eye.addEventListener('click', () => { r.visible = !r.visible; recomputeView(); renderRuns(); state.dirty = true; });
    const del = document.createElement('button');
    del.className = 'mini';
    del.textContent = '×';
    del.title = 'Sil';
    del.addEventListener('click', () => {
      state.runs.splice(i, 1);
      if (state.active >= state.runs.length) state.active = state.runs.length - 1;
      if (!state.runs.length) $('#empty').hidden = false;
      populateChartKeys(); recomputeView(); renderRuns(); state.dirty = true;
    });
    el.append(dot, lbl, val, eye, del);
    holder.appendChild(el);
  });
}

/* ----------------------------------------------------------- veri erişimi */

const BUILTIN = {
  'Hız (m/s)': (f) => f.v,
  'Açısal hız (rad/s)': (f) => f.omega,
  'Sol motor komutu': (f) => f.cmdL,
  'Sağ motor komutu': (f) => f.cmdR,
  'Konum x (m)': (f) => f.x,
  'Konum y (m)': (f) => f.y,
};

function getSeries(run, key) {
  if (BUILTIN[key]) return BUILTIN[key](run.data.frames);
  const t = run.data.telemetry || {};
  return t[key] || null;
}

function populateChartKeys() {
  const sel = $('#chart-key');
  const telemetry = new Set();
  for (const r of state.runs) Object.keys(r.data.telemetry || {}).forEach((k) => telemetry.add(k));
  sel.textContent = '';
  const add = (label, keys) => {
    if (!keys.length) return;
    const g = document.createElement('optgroup');
    g.label = label;
    for (const k of keys) {
      const o = document.createElement('option');
      o.value = k;
      o.textContent = k;
      g.appendChild(o);
    }
    sel.appendChild(g);
  };
  add('SmartDashboard (kodunuzdan)', Array.from(telemetry));
  add('Robot fiziği', Object.keys(BUILTIN));
  const all = Array.from(telemetry).concat(Object.keys(BUILTIN));
  if (!state.chartKeyUserSet || !all.includes(state.chartKey)) {
    // Kullanıcı seçmediyse: kodun yazdığı "Mesafe" kanalı, yoksa ilk SmartDashboard kanalı, yoksa hız.
    const tele = Array.from(telemetry);
    state.chartKey = tele.find((k) => /^Mesafe/i.test(k)) || tele[0] || 'Hız (m/s)';
  }
  sel.value = state.chartKey;
}
$('#chart-key').addEventListener('change', (e) => {
  state.chartKey = e.target.value;
  state.chartKeyUserSet = true;
  renderRuns();
  state.dirty = true;
});

function endTime(run) {
  const t = run.data.frames.t;
  return t[t.length - 1];
}

function frameAt(run, time) {
  const f = run.data.frames;
  const p = run.data.period;
  const x = Math.max(0, Math.min(f.t.length - 1, time / p));
  const i = Math.floor(x);
  const j = Math.min(f.t.length - 1, i + 1);
  const a = x - i;
  const lerp = (arr) => arr[i] + (arr[j] - arr[i]) * a;
  return { x: lerp(f.x), y: lerp(f.y), theta: lerp(f.theta) };
}

function targetOf(run) {
  // SmartDashboard'a "Hedef (m)" yazdıysanız sahada hedef çizgisi çıkar.
  const t = run.data.telemetry || {};
  for (const key of Object.keys(t)) {
    if (/^Hedef/i.test(key) && /\(m\)\s*$/.test(key)) {
      const arr = t[key];
      for (let i = arr.length - 1; i >= 0; i--) if (arr[i] !== null) return { key, value: arr[i] };
    }
  }
  return null;
}

/* ------------------------------------------------------------ tuval hazırlığı */

function setupCanvas(cv) {
  const dpr = window.devicePixelRatio || 1;
  const w = Math.max(1, Math.round(cv.clientWidth * dpr));
  const h = Math.max(1, Math.round(cv.clientHeight * dpr));
  if (cv.width !== w || cv.height !== h) { cv.width = w; cv.height = h; }
  cv._dpr = dpr;
  const g = cv.getContext('2d');
  g.setTransform(1, 0, 0, 1, 0, 0);
  g.clearRect(0, 0, w, h);
  return g;
}

function niceTicks(min, max, n) {
  if (min === max) { min -= 1; max += 1; }
  const step0 = (max - min) / n;
  const mag = Math.pow(10, Math.floor(Math.log10(step0)));
  const norm = step0 / mag;
  const step = (norm < 1.5 ? 1 : norm < 3 ? 2 : norm < 7 ? 5 : 10) * mag;
  const ticks = [];
  for (let v = Math.ceil(min / step) * step; v <= max + step * 1e-9; v += step) ticks.push(Math.round(v / step) * step);
  return { ticks, step };
}

/* ------------------------------------------------------------------- saha */

const fieldCv = $('#field');
let view = { minX: -0.5, maxX: 5.5, minY: -1.5, maxY: 1.5 };

function recomputeView() {
  let minX = -0.5, maxX = 5.5, minY = -1.5, maxY = 1.5;
  for (const r of visibleRuns()) {
    const f = r.data.frames;
    for (let i = 0; i < f.x.length; i++) {
      if (f.x[i] < minX) minX = f.x[i];
      if (f.x[i] > maxX) maxX = f.x[i];
      if (f.y[i] < minY) minY = f.y[i];
      if (f.y[i] > maxY) maxY = f.y[i];
    }
    const tg = targetOf(r);
    if (tg) { minX = Math.min(minX, tg.value); maxX = Math.max(maxX, tg.value); }
    const pad = (r.data.trackWidthMeters || 0.6);
    minX -= 0; maxX += 0; minY -= 0; maxY += 0;
    minX = Math.min(minX, -pad); minY = Math.min(minY, -pad); maxY = Math.max(maxY, pad);
  }
  const m = 0.6;
  view = { minX: minX - m, maxX: maxX + m, minY: minY - m, maxY: maxY + m };
}

function drawRobot(g, sx, sy, scale, pose, run, alpha, solid) {
  const track = run.data.trackWidthMeters || 0.6;
  const w = track + 0.12;
  const l = track * 1.15;
  const cx = sx(pose.x);
  const cy = sy(pose.y);
  g.save();
  g.translate(cx, cy);
  g.rotate(-pose.theta);
  g.globalAlpha = alpha;
  g.fillStyle = run.color;
  g.strokeStyle = run.color;
  g.lineWidth = Math.max(1.5, scale * 0.025);
  const hl = (l * scale) / 2;
  const hw = (w * scale) / 2;
  g.beginPath();
  g.roundRect(-hl, -hw, hl * 2, hw * 2, Math.min(6, hw * 0.25));
  if (solid) { g.globalAlpha = alpha * 0.28; g.fill(); g.globalAlpha = alpha; }
  g.stroke();
  // ön yön oku
  g.beginPath();
  g.moveTo(hl * 0.15, -hw * 0.45);
  g.lineTo(hl * 0.8, 0);
  g.lineTo(hl * 0.15, hw * 0.45);
  g.closePath();
  g.fillStyle = run.color;
  g.globalAlpha = alpha;
  g.fill();
  g.restore();
}

function drawField() {
  const g = setupCanvas(fieldCv);
  const W = fieldCv.width;
  const H = fieldCv.height;
  const dpr = fieldCv._dpr;
  g.fillStyle = cssVar('--field');
  g.fillRect(0, 0, W, H);

  const margin = 28 * dpr;
  const spanX = view.maxX - view.minX;
  const spanY = view.maxY - view.minY;
  const scale = Math.min((W - margin * 2) / spanX, (H - margin * 2) / spanY);
  const ox = (W - spanX * scale) / 2;
  const oy = (H - spanY * scale) / 2;
  const sx = (x) => ox + (x - view.minX) * scale;
  const sy = (y) => H - (oy + (y - view.minY) * scale);

  // ızgara
  const stepM = scale / dpr > 90 ? 0.5 : scale / dpr > 38 ? 1 : scale / dpr > 18 ? 2 : 5;
  g.lineWidth = 1;
  g.font = (11 * dpr) + 'px ' + cssVar('--mono');
  g.fillStyle = '#5d6a80';
  g.textBaseline = 'top';
  for (let x = Math.ceil(view.minX / stepM) * stepM; x <= view.maxX; x += stepM) {
    g.strokeStyle = Math.abs(x) < 1e-9 ? '#3a475c' : '#1b2431';
    g.beginPath(); g.moveTo(sx(x), sy(view.minY)); g.lineTo(sx(x), sy(view.maxY)); g.stroke();
    g.fillText(fmt(x, stepM < 1 ? 1 : 0), sx(x) + 3 * dpr, H - 14 * dpr);
  }
  g.textBaseline = 'alphabetic';
  for (let y = Math.ceil(view.minY / stepM) * stepM; y <= view.maxY; y += stepM) {
    g.strokeStyle = Math.abs(y) < 1e-9 ? '#3a475c' : '#1b2431';
    g.beginPath(); g.moveTo(sx(view.minX), sy(y)); g.lineTo(sx(view.maxX), sy(y)); g.stroke();
    g.fillText(fmt(y, stepM < 1 ? 1 : 0), 4 * dpr, sy(y) - 3 * dpr);
  }
  g.fillStyle = '#5d6a80';
  g.fillText('metre', W - 44 * dpr, H - 18 * dpr);

  const runs = visibleRuns();
  const act = activeRun();

  // hedef çizgileri
  for (const r of runs) {
    const tg = targetOf(r);
    if (!tg) continue;
    g.save();
    g.setLineDash([6 * dpr, 5 * dpr]);
    g.strokeStyle = '#ffd166';
    g.lineWidth = 1.5 * dpr;
    g.beginPath(); g.moveTo(sx(tg.value), sy(view.minY)); g.lineTo(sx(tg.value), sy(view.maxY)); g.stroke();
    g.restore();
    g.fillStyle = '#ffd166';
    g.fillText('hedef ' + fmt(tg.value, 1) + ' m', sx(tg.value) + 5 * dpr, 16 * dpr);
    break;
  }

  // izler
  for (const r of runs) {
    const f = r.data.frames;
    const isActive = r === act;
    const upto = Math.min(f.x.length - 1, Math.floor(state.t / r.data.period) + 1);
    g.strokeStyle = r.color;
    g.lineWidth = (isActive ? 2.5 : 1.8) * dpr;
    g.globalAlpha = isActive ? 0.95 : 0.55;
    g.beginPath();
    for (let i = 0; i <= upto; i++) {
      if (i === 0) g.moveTo(sx(f.x[i]), sy(f.y[i])); else g.lineTo(sx(f.x[i]), sy(f.y[i]));
    }
    g.stroke();
    // bitiş noktası
    const last = f.x.length - 1;
    g.setLineDash([]);
    g.fillStyle = r.color;
    g.globalAlpha = 0.9;
    g.beginPath();
    g.arc(sx(f.x[last]), sy(f.y[last]), 3.2 * dpr, 0, Math.PI * 2);
    g.fill();
    g.globalAlpha = 1;
  }

  // robotlar (aktif olan dolu, diğerleri hayalet)
  for (const r of runs) {
    if (r === act) continue;
    drawRobot(g, sx, sy, scale, frameAt(r, Math.min(state.t, endTime(r))), r, 0.6, false);
  }
  if (act && act.visible) drawRobot(g, sx, sy, scale, frameAt(act, Math.min(state.t, endTime(act))), act, 1, true);
  else if (act) drawRobot(g, sx, sy, scale, frameAt(act, Math.min(state.t, endTime(act))), act, 0.2, false);
}

/* ----------------------------------------------------------------- grafik */

const chartCv = $('#chart');
let chartGeom = null;

function drawChart() {
  const g = setupCanvas(chartCv);
  const W = chartCv.width;
  const H = chartCv.height;
  const dpr = chartCv._dpr;
  g.fillStyle = cssVar('--field');
  g.fillRect(0, 0, W, H);
  const runs = visibleRuns();
  if (!runs.length) { chartGeom = null; return; }

  const pl = 46 * dpr, pr = 12 * dpr, pt = 12 * dpr, pb = 24 * dpr;
  let tMax = 0;
  let yMin = Infinity;
  let yMax = -Infinity;
  const series = [];
  for (const r of runs) {
    const s = getSeries(r, state.chartKey);
    if (!s) continue;
    series.push({ run: r, s });
    tMax = Math.max(tMax, endTime(r));
    for (const v of s) {
      if (v === null || Number.isNaN(v)) continue;
      if (v < yMin) yMin = v;
      if (v > yMax) yMax = v;
    }
  }
  // aynı birimdeki "Hedef" serileri kesikli referans çizgisi
  const unit = (/\(([^)]*)\)\s*$/.exec(state.chartKey) || [])[1];
  const refs = [];
  if (unit) {
    const seen = new Set();
    for (const r of runs) {
      for (const key of Object.keys(r.data.telemetry || {})) {
        if (/^Hedef/i.test(key) && key !== state.chartKey && new RegExp('\\(' + unit.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '\\)\\s*$').test(key) && !seen.has(key)) {
          seen.add(key);
          const arr = r.data.telemetry[key];
          for (let i = arr.length - 1; i >= 0; i--) if (arr[i] !== null) { refs.push({ key, v: arr[i] }); break; }
        }
      }
    }
  }
  for (const rf of refs) { yMin = Math.min(yMin, rf.v); yMax = Math.max(yMax, rf.v); }
  if (!series.length || !Number.isFinite(yMin)) {
    g.fillStyle = '#5d6a80';
    g.font = (12 * dpr) + 'px ' + cssVar('--sans');
    g.fillText('Bu kanal seçili çalıştırmalarda yok.', pl, H / 2);
    chartGeom = null;
    return;
  }
  const padY = (yMax - yMin || 1) * 0.08;
  yMin -= padY; yMax += padY;
  const x0 = pl, x1 = W - pr, y0 = H - pb, y1 = pt;
  const X = (t) => x0 + (t / tMax) * (x1 - x0);
  const Y = (v) => y0 - ((v - yMin) / (yMax - yMin)) * (y0 - y1);
  chartGeom = { x0, x1, tMax, dpr };

  // eksenler
  g.font = (11 * dpr) + 'px ' + cssVar('--mono');
  g.lineWidth = 1;
  const yt = niceTicks(yMin, yMax, 5);
  g.textAlign = 'right';
  g.fillStyle = '#6b778c';
  for (const v of yt.ticks) {
    g.strokeStyle = '#1b2431';
    g.beginPath(); g.moveTo(x0, Y(v)); g.lineTo(x1, Y(v)); g.stroke();
    g.fillText(fmt(v, yt.step < 1 ? 1 : 0), x0 - 6 * dpr, Y(v) + 4 * dpr);
  }
  g.textAlign = 'center';
  const xt = niceTicks(0, tMax, 6);
  for (const v of xt.ticks) {
    g.strokeStyle = '#1b2431';
    g.beginPath(); g.moveTo(X(v), y1); g.lineTo(X(v), y0); g.stroke();
    g.fillText(fmt(v, xt.step < 1 ? 1 : 0) + ' s', X(v), H - 7 * dpr);
  }
  g.textAlign = 'left';

  // etkin (enabled) bölge gölgesi
  const act = activeRun();
  if (act && act.data.activeStart !== undefined) {
    g.fillStyle = 'rgba(255,255,255,0.025)';
    g.fillRect(X(act.data.activeStart), y1, X(Math.min(tMax, act.data.activeEnd)) - X(act.data.activeStart), y0 - y1);
  }

  // referans çizgileri
  for (const rf of refs) {
    g.save();
    g.setLineDash([6 * dpr, 5 * dpr]);
    g.strokeStyle = '#ffd166';
    g.lineWidth = 1.5 * dpr;
    g.beginPath(); g.moveTo(x0, Y(rf.v)); g.lineTo(x1, Y(rf.v)); g.stroke();
    g.restore();
    g.fillStyle = '#ffd166';
    g.fillText(rf.key.replace(/\s*\([^)]*\)\s*$/, '').toLowerCase(), x0 + 6 * dpr, Y(rf.v) - 5 * dpr);
  }

  // seriler
  for (const { run, s } of series) {
    const t = run.data.frames.t;
    g.strokeStyle = run.color;
    g.lineWidth = (run === act ? 2.4 : 1.8) * dpr;
    g.globalAlpha = run === act ? 1 : 0.75;
    g.beginPath();
    let pen = false;
    for (let i = 0; i < s.length; i++) {
      const v = s[i];
      if (v === null || Number.isNaN(v)) { pen = false; continue; }
      if (!pen) { g.moveTo(X(t[i]), Y(v)); pen = true; } else g.lineTo(X(t[i]), Y(v));
    }
    g.stroke();
    g.globalAlpha = 1;
  }

  // zaman imleci
  const cx = X(Math.min(state.t, tMax));
  g.strokeStyle = '#e6eaf1';
  g.globalAlpha = 0.7;
  g.lineWidth = 1 * dpr;
  g.beginPath(); g.moveTo(cx, y1); g.lineTo(cx, y0); g.stroke();
  g.globalAlpha = 1;
  for (const { run, s } of series) {
    const i = Math.min(s.length - 1, Math.round(Math.min(state.t, endTime(run)) / run.data.period));
    const v = s[i];
    if (v === null || Number.isNaN(v)) continue;
    g.fillStyle = run.color;
    g.beginPath(); g.arc(X(run.data.frames.t[i]), Y(v), 3.5 * dpr, 0, Math.PI * 2); g.fill();
  }
}

chartCv.addEventListener('click', (e) => {
  if (!chartGeom) return;
  const rect = chartCv.getBoundingClientRect();
  const px = (e.clientX - rect.left) * chartGeom.dpr;
  const frac = (px - chartGeom.x0) / (chartGeom.x1 - chartGeom.x0);
  state.t = Math.max(0, Math.min(1, frac)) * chartGeom.tMax;
  state.playing = false;
  syncPlayButton();
  state.dirty = true;
});

/* ----------------------------------------------------------------- oynatma */

const seekEl = $('#seek');
const playBtn = $('#play');

function syncPlayButton() {
  playBtn.textContent = state.playing ? '⏸' : '▶';
}

playBtn.addEventListener('click', () => {
  const act = activeRun();
  if (!act) return;
  if (!state.playing && state.t >= endTime(act) - 1e-6) state.t = 0;
  state.playing = !state.playing;
  syncPlayButton();
});
seekEl.addEventListener('input', () => {
  const act = activeRun();
  if (!act) return;
  state.t = (seekEl.value / 1000) * endTime(act);
  state.playing = false;
  syncPlayButton();
  state.dirty = true;
});
$('#speed').addEventListener('change', (e) => { state.speed = parseFloat(e.target.value) || 1; });

let lastTs = 0;
function frame(ts) {
  const dt = lastTs ? Math.min(0.1, (ts - lastTs) / 1000) : 0;
  lastTs = ts;
  const act = activeRun();
  if (state.playing && act) {
    const end = Math.max(...visibleRuns().map(endTime), endTime(act));
    state.t = Math.min(end, state.t + dt * state.speed);
    if (state.t >= end) { state.playing = false; syncPlayButton(); }
    state.dirty = true;
  }
  if (state.dirty) {
    state.dirty = false;
    drawField();
    drawChart();
    if (act) {
      const end = endTime(act);
      seekEl.value = String(end > 0 ? Math.round((Math.min(state.t, end) / end) * 1000) : 0);
      $('#time').textContent = fmt(Math.min(state.t, end)) + ' / ' + fmt(end) + ' s';
    } else {
      $('#time').textContent = '0.00 / 0.00 s';
    }
  }
  requestAnimationFrame(frame);
}

if (typeof ResizeObserver !== 'undefined') {
  new ResizeObserver(() => { state.dirty = true; }).observe($('.sim-pane'));
}
window.addEventListener('resize', () => { state.dirty = true; });

/* ------------------------------------------------------ proje (kaydet/yükle) */

$('#run').addEventListener('click', run);

$('#mode').addEventListener('change', () => {
  $('#teleop-box').hidden = $('#mode').value !== 'teleop';
});

$('#btn-save').addEventListener('click', () => {
  saveEditorToState();
  const blob = new Blob([JSON.stringify({ format: 'frcsim-project', version: 1, files: state.files, config: state.config }, null, 2)],
    { type: 'application/json' });
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = 'frc-proje.json';
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(a.href), 2000);
});

$('#btn-load').addEventListener('click', () => $('#file-load').click());
$('#file-load').addEventListener('change', async (e) => {
  const file = e.target.files[0];
  e.target.value = '';
  if (!file) return;
  try {
    const p = JSON.parse(await file.text());
    if (!p || typeof p.files !== 'object' || p.files === null || typeof p.config !== 'string') throw new Error('Dosya bir FRC simülatör projesi değil.');
    for (const name of Object.keys(p.files)) if (typeof p.files[name] !== 'string') throw new Error('Geçersiz dosya içeriği: ' + name);
    loadProject(p.files, p.config);
    logLine('Proje yüklendi: ' + file.name, 'okk');
  } catch (err) {
    logLine('Proje yüklenemedi: ' + err.message, 'err');
  }
});

$('#btn-example').addEventListener('click', async () => {
  if (!confirm('Kodunuz örnek proje ile değiştirilsin mi? (Önce "İndir" ile yedek alabilirsiniz.)')) return;
  await loadExample();
});

function loadProject(files, config, current) {
  state.files = files;
  state.config = config;
  state.current = current && (current === CONFIG_TAB || current in files) ? current :
    ('frc/robot/Robot.java' in files ? 'frc/robot/Robot.java' : (Object.keys(files)[0] || CONFIG_TAB));
  state.errorTab = null;
  codeEl.value = currentText();
  renderTabs();
  renderEditor();
  persist();
}

async function loadExample() {
  try {
    const res = await fetch('/api/examples');
    const d = await res.json();
    loadProject(d.files, d.config);
  } catch (e) {
    logLine('Örnek yüklenemedi: ' + e.message, 'err');
  }
}

/* ---------------------------------------------------------------- başlangıç */

/* ------------------------------------------- statik mod (GitHub Pages) */

const REPO_RE = /^[A-Za-z0-9_.-]+\/[A-Za-z0-9_.-]+$/;

function renderStaticNote(index) {
  const note = $('#static-note');
  note.textContent = '';
  note.append('Bu sayfa, yayın sırasında GitHub’ın hesapladığı sonuçları oynatır (son derleme: ' + (index.built || '?') + '). ' +
    'Kodu değiştirmek için ');
  if (index.repo && REPO_RE.test(index.repo)) {
    const a = document.createElement('a');
    a.href = 'https://github.dev/' + index.repo;
    a.target = '_blank';
    a.rel = 'noopener';
    a.textContent = 'depoyu github.dev’de açın';
    note.append(a);
  } else {
    note.append('depodaki robot/src klasörünü düzenleyin');
  }
  note.append(', kaydedip commit edin; site 1–2 dakikada güncellenir.');
}

async function addScenario(id, quiet) {
  const sc = state.index && state.index.scenarios.find((s) => s.id === id);
  if (!sc) return;
  const existing = state.runs.findIndex((r) => r.scenarioId === id);
  if (existing >= 0 && !quiet) {
    state.active = existing; state.t = 0; state.playing = true;
    renderRuns(); syncPlayButton(); state.dirty = true;
    return;
  }
  let data = state.cache[id];
  if (!data) {
    try {
      const res = await fetch('data/' + encodeURIComponent(sc.file));
      if (!res.ok) throw new Error('HTTP ' + res.status);
      data = await res.json();
    } catch (e) {
      logLine('Senaryo yüklenemedi (' + sc.label + '): ' + e.message, 'err');
      return;
    }
    state.cache[id] = data;
  }
  addRun(data, false, sc.label, id);
}

async function loadAllScenarios() {
  state.runs = [];
  state.active = -1;
  state.runCounter = 0;
  for (const s of state.index.scenarios.slice(0, MAX_RUNS)) await addScenario(s.id, true);
  state.active = state.runs.length - 1;
  state.t = 0;
  state.playing = state.runs.length > 0;
  renderRuns();
  syncPlayButton();
  state.dirty = true;
}

async function initStatic() {
  state.static = true;
  document.body.classList.add('static');
  $('#run').textContent = '▶ Oynat';
  let index;
  try {
    const res = await fetch('data/index.json', { cache: 'no-store' });
    if (!res.ok) throw new Error('HTTP ' + res.status);
    index = await res.json();
  } catch (e) {
    logLine('Site verisi yüklenemedi: ' + e.message, 'err');
    setStatus('Hata', false);
    return;
  }
  state.index = index;
  const title = typeof index.title === 'string' && index.title ? index.title : 'FRC robot';
  document.title = title + ' · FRC Web Simülatör';
  $('.tag').textContent = title;
  loadProject(index.files, index.config);
  codeEl.readOnly = true;
  const sel = $('#scenario');
  for (const s of index.scenarios) {
    const o = document.createElement('option');
    o.value = s.id;
    o.textContent = s.label;
    sel.appendChild(o);
  }
  renderStaticNote(index);
  $('#btn-all').addEventListener('click', loadAllScenarios);
  await loadAllScenarios();
  setStatus('Hazır · ' + index.scenarios.length + ' senaryo', false);
}

(async function init() {
  $('#joystick').value = DEFAULT_JOYSTICK;
  const meta = document.querySelector('meta[name="frcsim-mode"]');
  if (meta && meta.content === 'static') {
    await initStatic();
    populateChartKeys();
    requestAnimationFrame(frame);
    window.__frcsim = { state, run };
    return;
  }
  let loaded = false;
  const saved = storeGet(STORE_KEY);
  if (saved) {
    try {
      const p = JSON.parse(saved);
      if (p && p.files && Object.keys(p.files).length && typeof p.config === 'string') {
        loadProject(p.files, p.config, p.current);
        loaded = true;
      }
    } catch (e) { /* bozuk kayıt: örneğe düş */ }
  }
  if (!loaded) await loadExample();
  populateChartKeys();
  requestAnimationFrame(frame);
  // Test/otomasyon için dışarıya küçük bir kanca
  window.__frcsim = { state, run };
})();
