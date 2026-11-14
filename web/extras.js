'use strict';

/* ======================================================================
   Fusion Code ek paneller (app.js'ten sonra yuklenir):
   - Anlik degerler seridi (konum, yon, hiz, motor komutu)
   - Komut zaman cizelgesi (Command-Based)
   - Gorsel Robot Kurucu (robot-config.json formu + dogrulama)
   ====================================================================== */

Object.assign(STR.tr, {
  restart_aria: 'Basa sar', tl_title: 'Komut zaman çizelgesi', tl_aria: 'Komut zaman çizelgesi',
  tab_robot: '⚙ Robot',
  r_x: 'x', r_y: 'y', r_head: 'yön', r_v: 'hız', r_w: 'dönüş', r_l: 'sol', r_r: 'sağ', r_on: 'etkin', r_off: 'devre dışı',
  tl_interrupted: 'iptal edildi', tl_done: 'bitti', tl_open: 'çalışıyordu',
  cfg_intro: 'Robotunu burada kur. Değişiklikler robot-config.json dosyasına otomatik yazılır.',
  cfg_name: 'Robot adı', cfg_sec_drive: 'Sürüş sistemi', cfg_sec_wiring: 'Kablolama', cfg_sec_enc: 'Enkoderler', cfg_sec_gyro: 'Gyro',
  cfg_motor: 'Motor', cfg_mps: 'Taraf başına motor', cfg_gearing: 'Dişli oranı', cfg_mass: 'Kütle (kg)', cfg_moi: 'Atalet momenti (kg·m²)',
  cfg_wheel: 'Tekerlek yarıçapı (m)', cfg_track: 'İz genişliği (m)', cfg_eff: 'Verim (0–1)', cfg_roll: 'Yuvarlanma sürtünmesi', cfg_turn: 'Dönüş sürtünmesi',
  cfg_trac: 'Çekiş katsayısı', cfg_cur: 'Akım sınırı (A)', cfg_idle: 'Boşta modu', cfg_brake: 'Fren (brake)', cfg_coast: 'Serbest (coast)',
  cfg_lpwm: 'Sol PWM kanalları', cfg_rpwm: 'Sağ PWM kanalları', cfg_pwm_hint: 'virgülle: 0,1', cfg_lrev: 'Sol ters', cfg_rrev: 'Sağ ters',
  cfg_enc_a: 'A', cfg_enc_b: 'B', cfg_enc_side: 'Taraf', cfg_enc_cpr: 'Darbe/tur', cfg_enc_rev: 'ters', cfg_left: 'sol', cfg_right: 'sağ',
  cfg_add_enc: '+ Enkoder ekle', cfg_gyro_ch: 'Gyro kanalı (0–7)', cfg_download: 'robot-config.json indir', cfg_json_bad: 'robot-config.json geçerli JSON değil. JSON sekmesinde düzeltin.',
  cfg_all_ok: 'Ayarlar geçerli.', cfg_remove: 'Sil',
  v_motor: 'Bilinmeyen motor: {v}', v_pos: '"{k}" pozitif bir sayı olmalı.', v_eff: 'Verim 1\'den büyük olamaz.',
  v_fric: 'Sürtünme katsayıları geçersiz (çekiş > 0, diğerleri ≥ 0).', v_pwm_range: '{k}: PWM kanalı 0–19 arasında olmalı.', v_pwm_empty: '{k} boş olamaz.',
  v_pwm_both: 'PWM kanalı {n} hem solda hem sağda kullanılmış.', v_pwm_dup: '{k}: aynı kanal iki kez yazılmış.',
  v_enc_ch: 'Enkoder kanalları geçersiz (A={a}, B={b}).', v_enc_dup: 'Enkoder kanalları çakışıyor: {a},{b}.', v_gyro: 'Gyro kanalı 0–7 arasında olmalı.',
  w_enc_none: 'Hiç enkoder yok: mesafe/PID ölçümü yapılamaz.', w_enc_side: 'Her taraf için bir enkoder eklemeniz önerilir ({s} yok).',
  w_mass: 'Kütle 70 kg üstünde: gerçek FRC robotları genelde bundan hafiftir, değeri kontrol edin.',
  w_calib: 'Bu değerler kalibre edilmemiş tahminlerdir; gerçek robotla ölçmeden birebir aynı sonuç beklemeyin.',
});
Object.assign(STR.en, {
  restart_aria: 'Restart', tl_title: 'Command timeline', tl_aria: 'Command timeline',
  tab_robot: '⚙ Robot',
  r_x: 'x', r_y: 'y', r_head: 'heading', r_v: 'speed', r_w: 'turn rate', r_l: 'left', r_r: 'right', r_on: 'enabled', r_off: 'disabled',
  tl_interrupted: 'interrupted', tl_done: 'finished', tl_open: 'still running',
  cfg_intro: 'Build your robot here. Changes are written to robot-config.json automatically.',
  cfg_name: 'Robot name', cfg_sec_drive: 'Drivetrain', cfg_sec_wiring: 'Wiring', cfg_sec_enc: 'Encoders', cfg_sec_gyro: 'Gyro',
  cfg_motor: 'Motor', cfg_mps: 'Motors per side', cfg_gearing: 'Gear ratio', cfg_mass: 'Mass (kg)', cfg_moi: 'Moment of inertia (kg·m²)',
  cfg_wheel: 'Wheel radius (m)', cfg_track: 'Track width (m)', cfg_eff: 'Efficiency (0–1)', cfg_roll: 'Rolling friction', cfg_turn: 'Turning friction',
  cfg_trac: 'Traction coefficient', cfg_cur: 'Current limit (A)', cfg_idle: 'Idle mode', cfg_brake: 'Brake', cfg_coast: 'Coast',
  cfg_lpwm: 'Left PWM channels', cfg_rpwm: 'Right PWM channels', cfg_pwm_hint: 'comma separated: 0,1', cfg_lrev: 'Left reversed', cfg_rrev: 'Right reversed',
  cfg_enc_a: 'A', cfg_enc_b: 'B', cfg_enc_side: 'Side', cfg_enc_cpr: 'Counts/rev', cfg_enc_rev: 'reversed', cfg_left: 'left', cfg_right: 'right',
  cfg_add_enc: '+ Add encoder', cfg_gyro_ch: 'Gyro channel (0–7)', cfg_download: 'Download robot-config.json', cfg_json_bad: 'robot-config.json is not valid JSON. Fix it in the JSON tab.',
  cfg_all_ok: 'Settings are valid.', cfg_remove: 'Remove',
  v_motor: 'Unknown motor: {v}', v_pos: '"{k}" must be a positive number.', v_eff: 'Efficiency cannot be above 1.',
  v_fric: 'Friction values are invalid (traction > 0, others ≥ 0).', v_pwm_range: '{k}: PWM channel must be 0–19.', v_pwm_empty: '{k} cannot be empty.',
  v_pwm_both: 'PWM channel {n} is used on both left and right.', v_pwm_dup: '{k}: the same channel is listed twice.',
  v_enc_ch: 'Invalid encoder channels (A={a}, B={b}).', v_enc_dup: 'Encoder channels collide: {a},{b}.', v_gyro: 'Gyro channel must be 0–7.',
  w_enc_none: 'No encoders: distance/PID measurements will not work.', w_enc_side: 'Add an encoder for each side (missing: {s}).',
  w_mass: 'Mass above 70 kg: real FRC robots are usually lighter, double-check the value.',
  w_calib: 'These values are uncalibrated estimates; do not expect identical results on a real robot until you measure it.',
});
applyStaticTexts();

/* ------------------------------------------------------------ yardimcilar */

function sampleAt(run, time) {
  const f = run.data.frames;
  const x = Math.max(0, Math.min(f.t.length - 1, time / run.data.period));
  const i = Math.floor(x);
  const j = Math.min(f.t.length - 1, i + 1);
  const a = x - i;
  const L = (arr) => (arr ? arr[i] + (arr[j] - arr[i]) * a : 0);
  return { x: L(f.x), y: L(f.y), theta: L(f.theta), v: L(f.v), omega: L(f.omega), cmdL: L(f.cmdL), cmdR: L(f.cmdR) };
}

function wrapDeg(d) {
  let r = ((d + 180) % 360 + 360) % 360 - 180;
  return Object.is(r, -0) ? 0 : r;
}

/* ------------------------------------------------------- anlik degerler */

const readoutEl = $('#readout');
let lastReadout = '';

function drawReadout(act) {
  if (!act) { if (lastReadout) { readoutEl.textContent = ''; lastReadout = ''; } return; }
  const s = sampleAt(act, state.t);
  const d = act.data;
  const enabled = d.activeStart === undefined || (state.t >= d.activeStart && state.t < d.activeEnd);
  const key = [fmt(s.x), fmt(s.y), fmt(wrapDeg(s.theta * 180 / Math.PI), 1), fmt(s.v), fmt(s.omega), fmt(s.cmdL), fmt(s.cmdR), enabled, state.lang].join('|');
  if (key === lastReadout) return;
  lastReadout = key;
  readoutEl.textContent = '';
  const add = (label, value, unit) => {
    const sp = document.createElement('span');
    sp.append(label + ' ');
    const b = document.createElement('b');
    b.textContent = value + (unit || '');
    sp.append(b);
    readoutEl.append(sp);
  };
  const pill = document.createElement('span');
  pill.className = 'pill' + (enabled ? ' on' : '');
  pill.textContent = enabled ? t('r_on') : t('r_off');
  readoutEl.append(pill);
  add(t('r_x'), fmt(s.x), ' m');
  add(t('r_y'), fmt(s.y), ' m');
  add(t('r_head'), fmt(wrapDeg(s.theta * 180 / Math.PI), 1), '°');
  add(t('r_v'), fmt(s.v), ' m/s');
  add(t('r_w'), fmt(s.omega), ' rad/s');
  add(t('r_l'), fmt(s.cmdL), '');
  add(t('r_r'), fmt(s.cmdR), '');
}

/* ----------------------------------------------- komut zaman cizelgesi */

const tlBox = $('#timeline-box');
const tlCv = $('#timeline');
const TL_ROW = 18;
const TL_TOP = 20;
const TL_MAX_ROWS = 8;
let tlBars = [];
let tlGeom = null;

function packLanes(cmds) {
  const byDepth = new Map();
  for (const c of cmds) {
    if (!byDepth.has(c.depth)) byDepth.set(c.depth, []);
    byDepth.get(c.depth).push(c);
  }
  const depths = Array.from(byDepth.keys()).sort((a, b) => a - b);
  let row = 0;
  const placed = [];
  for (const d of depths) {
    const lanes = [];
    for (const c of byDepth.get(d).slice().sort((a, b) => a.start - b.start)) {
      let lane = lanes.findIndex((end) => end <= c.start + 1e-9);
      if (lane < 0) { lane = lanes.length; lanes.push(0); }
      lanes[lane] = c.end;
      placed.push({ c, row: row + lane });
    }
    row += Math.max(1, lanes.length);
  }
  return { placed, rows: row };
}

function drawTimeline(act) {
  const cmds = act && act.data.commands ? act.data.commands : [];
  const show = cmds.length > 0;
  if (tlBox.hidden === show) { tlBox.hidden = !show; state.dirty = true; }
  if (!show) { tlBars = []; return; }

  const packed = packLanes(cmds);
  const rows = Math.min(TL_MAX_ROWS, packed.rows);
  const cssH = TL_TOP + rows * TL_ROW + 6;
  if (tlBox.style.height !== cssH + 'px') tlBox.style.height = cssH + 'px';

  const g = setupCanvas(tlCv);
  const dpr = tlCv._dpr;
  const W = tlCv.width;
  const H = tlCv.height;
  g.fillStyle = cssVar('--field');
  g.fillRect(0, 0, W, H);

  const x0 = 46 * dpr;
  const x1 = W - 12 * dpr;
  let tMax = 0;
  for (const r of visibleRuns()) tMax = Math.max(tMax, endTime(r));
  tMax = Math.max(tMax, endTime(act));
  const X = (tm) => x0 + (tm / tMax) * (x1 - x0);
  tlGeom = { x0, x1, tMax, dpr };

  // yardimci dikey cizgiler (grafikle ayni olcek)
  const xt = niceTicks(0, tMax, 6);
  g.strokeStyle = '#14205a';
  g.lineWidth = 1;
  for (const v of xt.ticks) { g.beginPath(); g.moveTo(X(v), TL_TOP * dpr - 2 * dpr); g.lineTo(X(v), H); g.stroke(); }

  const fills = ['#C8E6FF', '#7f9bff', '#a99bff', '#7bd88f'];
  tlBars = [];
  g.font = (11 * dpr) + 'px ' + cssVar('--sans');
  g.textBaseline = 'middle';
  for (const p of packed.placed) {
    if (p.row >= TL_MAX_ROWS) continue;
    const c = p.c;
    const bx = X(c.start);
    const bw = Math.max(3 * dpr, X(c.end) - X(c.start));
    const by = (TL_TOP + p.row * TL_ROW) * dpr;
    const bh = (TL_ROW - 3) * dpr;
    tlBars.push({ x: bx, y: by, w: bw, h: bh, c });
    const base = fills[Math.min(fills.length - 1, c.depth)];
    g.fillStyle = c.interrupted ? 'rgba(255,138,92,0.85)' : base;
    g.globalAlpha = c.open ? 0.55 : 0.95;
    g.beginPath();
    if (g.roundRect) g.roundRect(bx, by, bw, bh, 4 * dpr); else g.rect(bx, by, bw, bh);
    g.fill();
    g.globalAlpha = 1;
    // metin (sigdigi kadar)
    g.save();
    g.beginPath(); g.rect(bx, by, bw, bh); g.clip();
    g.fillStyle = '#060A1A';
    g.fillText(c.name, bx + 5 * dpr, by + bh / 2 + 0.5 * dpr);
    g.restore();
  }
  if (packed.rows > TL_MAX_ROWS) {
    g.fillStyle = '#8aa0d0';
    g.textAlign = 'right';
    g.fillText('+' + (packed.rows - TL_MAX_ROWS), W - 8 * dpr, 12 * dpr);
    g.textAlign = 'left';
  }

  // zaman imleci
  const cx = X(Math.min(state.t, tMax));
  g.strokeStyle = 'rgba(248,250,255,.75)';
  g.lineWidth = 1 * dpr;
  g.beginPath(); g.moveTo(cx, TL_TOP * dpr - 3 * dpr); g.lineTo(cx, H); g.stroke();
  g.textBaseline = 'alphabetic';
}

function tlPointer(ev) {
  const act = activeRun();
  if (!act || !tlGeom) return;
  const rect = tlCv.getBoundingClientRect();
  const px = (ev.clientX - rect.left) * tlGeom.dpr;
  const py = (ev.clientY - rect.top) * tlGeom.dpr;
  const hit = tlBars.find((b) => px >= b.x && px <= b.x + b.w && py >= b.y && py <= b.y + b.h);
  let time;
  if (hit) time = hit.c.start;
  else time = Math.max(0, Math.min(tlGeom.tMax, ((px - tlGeom.x0) / (tlGeom.x1 - tlGeom.x0)) * tlGeom.tMax));
  state.t = Math.min(time, endTime(act));
  state.playing = false;
  syncPlayButton();
  state.dirty = true;
}
tlCv.addEventListener('click', tlPointer);
tlCv.addEventListener('mousemove', (ev) => {
  if (!tlGeom) return;
  const rect = tlCv.getBoundingClientRect();
  const px = (ev.clientX - rect.left) * tlGeom.dpr;
  const py = (ev.clientY - rect.top) * tlGeom.dpr;
  const hit = tlBars.find((b) => px >= b.x && px <= b.x + b.w && py >= b.y && py <= b.y + b.h);
  if (!hit) { tlCv.title = ''; return; }
  const c = hit.c;
  const status = c.open ? t('tl_open') : (c.interrupted ? t('tl_interrupted') : t('tl_done'));
  tlCv.title = c.name + '\n' + fmt(c.start) + ' → ' + fmt(c.end) + ' s (' + status + ')';
});

$('#restart').addEventListener('click', () => {
  const act = activeRun();
  if (!act) return;
  state.t = 0;
  state.playing = true;
  syncPlayButton();
  state.dirty = true;
});

window.drawExtras = function drawExtras(act) {
  drawReadout(act);
  drawTimeline(act);
};

/* -------------------------------------------------------- robot kurucu */

const MOTORS = ['NEO', 'NEO550', 'Vortex', 'CIM', 'MiniCIM', 'Falcon500', 'KrakenX60'];
const CFG_DEFAULTS = {
  robotName: 'robot',
  drivetrain: { motor: 'NEO', motorsPerSide: 2, gearing: 10.71, massKg: 50, momentOfInertiaKgM2: 7.5, wheelRadiusMeters: 0.0762,
    trackWidthMeters: 0.6, efficiency: 0.9, rollingFriction: 0.02, turnFriction: 0.1, tractionCoefficient: 1.0, currentLimitAmps: 60, idleMode: 'brake' },
  wiring: { left: { pwm: [0], reversed: false }, right: { pwm: [1], reversed: true } },
  gyro: { channel: 0 },
};

function dg(obj, key, def) { return obj && obj[key] !== undefined ? obj[key] : def; }

function validateConfig(c) {
  const out = [];
  const err = (key, vars) => out.push({ level: 'error', msg: t(key, vars) });
  const warn = (key, vars) => out.push({ level: 'warn', msg: t(key, vars) });
  const d = c.drivetrain || {};
  const D = CFG_DEFAULTS.drivetrain;
  const motor = dg(d, 'motor', D.motor);
  if (!MOTORS.some((m) => m.toLowerCase() === String(motor).toLowerCase())) err('v_motor', { v: motor });
  for (const k of ['motorsPerSide', 'gearing', 'massKg', 'momentOfInertiaKgM2', 'wheelRadiusMeters', 'trackWidthMeters', 'efficiency', 'currentLimitAmps']) {
    const v = dg(d, k, D[k]);
    if (!(typeof v === 'number' && v > 0)) err('v_pos', { k });
  }
  if (dg(d, 'efficiency', D.efficiency) > 1) err('v_eff');
  if (dg(d, 'rollingFriction', D.rollingFriction) < 0 || dg(d, 'turnFriction', D.turnFriction) < 0 || !(dg(d, 'tractionCoefficient', D.tractionCoefficient) > 0)) err('v_fric');

  const w = c.wiring || {};
  const lp = dg(w.left, 'pwm', [0]);
  const rp = dg(w.right, 'pwm', [1]);
  for (const [k, arr] of [['wiring.left.pwm', lp], ['wiring.right.pwm', rp]]) {
    if (!Array.isArray(arr) || !arr.length) { err('v_pwm_empty', { k }); continue; }
    if (arr.some((n) => !Number.isInteger(n) || n < 0 || n > 19)) err('v_pwm_range', { k });
    if (new Set(arr).size !== arr.length) err('v_pwm_dup', { k });
  }
  if (Array.isArray(lp) && Array.isArray(rp)) for (const n of lp) if (rp.includes(n)) err('v_pwm_both', { n });

  const encs = Array.isArray(c.encoders) ? c.encoders : [];
  const used = new Set();
  for (const e of encs) {
    const a = e.a, b = e.b;
    if (!Number.isInteger(a) || !Number.isInteger(b) || a < 0 || b < 0 || a === b) { err('v_enc_ch', { a, b }); continue; }
    if (used.has(a) || used.has(b)) err('v_enc_dup', { a, b });
    used.add(a); used.add(b);
    if (!(dg(e, 'countsPerRev', 2048) > 0)) err('v_pos', { k: 'countsPerRev' });
  }
  if (!encs.length) warn('w_enc_none');
  else for (const side of ['left', 'right']) if (!encs.some((e) => (e.side || 'left') === side)) warn('w_enc_side', { s: t(side === 'left' ? 'cfg_left' : 'cfg_right') });
  const g = dg(c.gyro, 'channel', 0);
  if (!Number.isInteger(g) || g < 0 || g > 7) err('v_gyro');
  if (dg(d, 'massKg', D.massKg) > 70) warn('w_mass');
  return out;
}

let cfgObj = null;

function cfgCommit() {
  state.config = JSON.stringify(cfgObj, null, 2);
  if (state.current === CONFIG_TAB) codeEl.value = state.config;
  persist();
  renderCfgMessages();
  scheduleLive();
}

function setPath(path, value) {
  let o = cfgObj;
  for (let i = 0; i < path.length - 1; i++) {
    if (typeof o[path[i]] !== 'object' || o[path[i]] === null) o[path[i]] = {};
    o = o[path[i]];
  }
  o[path[path.length - 1]] = value;
}

function getPath(path, def) {
  let o = cfgObj;
  for (const k of path) { if (o === undefined || o === null) return def; o = o[k]; }
  return o === undefined ? def : o;
}

function numField(path, labelKey, step, def) {
  const label = document.createElement('label');
  label.append(t(labelKey));
  const inp = document.createElement('input');
  inp.type = 'number';
  inp.step = String(step);
  inp.value = String(getPath(path, def));
  inp.addEventListener('input', () => {
    const v = parseFloat(inp.value);
    inp.classList.toggle('bad', !Number.isFinite(v));
    if (!Number.isFinite(v)) return;
    setPath(path, v);
    cfgCommit();
  });
  label.append(inp);
  return label;
}

function textField(path, labelKey, def) {
  const label = document.createElement('label');
  label.append(t(labelKey));
  const inp = document.createElement('input');
  inp.type = 'text';
  inp.value = String(getPath(path, def));
  inp.addEventListener('input', () => { setPath(path, inp.value); cfgCommit(); });
  label.append(inp);
  return label;
}

function selectField(path, labelKey, options, def) {
  const label = document.createElement('label');
  label.append(t(labelKey));
  const sel = document.createElement('select');
  const cur = String(getPath(path, def));
  for (const [val, text] of options) {
    const o = document.createElement('option');
    o.value = val; o.textContent = text;
    if (val.toLowerCase() === cur.toLowerCase()) o.selected = true;
    sel.append(o);
  }
  sel.addEventListener('change', () => { setPath(path, sel.value); cfgCommit(); });
  label.append(sel);
  return label;
}

function checkField(path, labelKey, def) {
  const label = document.createElement('label');
  label.className = 'chk';
  const inp = document.createElement('input');
  inp.type = 'checkbox';
  inp.checked = !!getPath(path, def);
  inp.addEventListener('change', () => { setPath(path, inp.checked); cfgCommit(); });
  label.append(inp, t(labelKey));
  return label;
}

function pwmField(path, labelKey, def) {
  const label = document.createElement('label');
  label.append(t(labelKey));
  const inp = document.createElement('input');
  inp.type = 'text';
  inp.value = getPath(path, def).join(', ');
  inp.placeholder = t('cfg_pwm_hint');
  inp.addEventListener('input', () => {
    const parts = inp.value.split(',').map((s) => s.trim()).filter(Boolean);
    const nums = parts.map(Number);
    const ok = parts.length > 0 && nums.every((n) => Number.isInteger(n));
    inp.classList.toggle('bad', !ok);
    if (!ok) return;
    setPath(path, nums);
    cfgCommit();
  });
  label.append(inp);
  return label;
}

function section(titleKey) {
  const f = document.createElement('fieldset');
  f.className = 'cfg-sec';
  const l = document.createElement('legend');
  l.textContent = t(titleKey);
  f.append(l);
  return f;
}

function grid(...children) {
  const g = document.createElement('div');
  g.className = 'cfg-grid';
  g.append(...children);
  return g;
}

function renderCfgMessages() {
  const box = $('#cfg-msgs');
  if (!box) return;
  box.textContent = '';
  const msgs = validateConfig(cfgObj);
  const add = (level, text) => {
    const d = document.createElement('div');
    d.className = 'cfg-msg ' + level;
    d.textContent = text;
    box.append(d);
  };
  for (const m of msgs) add(m.level, m.msg);
  if (!msgs.some((m) => m.level === 'error')) add('ok', t('cfg_all_ok'));
  add('warn', t('w_calib'));
}

function renderEncoders(holder) {
  holder.textContent = '';
  const encs = cfgObj.encoders;
  const sideOpts = [['left', t('cfg_left')], ['right', t('cfg_right')]];
  encs.forEach((e, i) => {
    const row = document.createElement('div');
    row.className = 'cfg-enc';
    const mk = (type, val, onChange, extra) => {
      const inp = document.createElement('input');
      inp.type = type;
      inp.value = String(val);
      if (extra) Object.assign(inp, extra);
      inp.addEventListener('input', () => onChange(inp));
      return inp;
    };
    const intInput = (key, ph) => mk('number', dg(e, key, 0), (inp) => {
      const v = parseInt(inp.value, 10);
      inp.classList.toggle('bad', !Number.isInteger(v));
      if (!Number.isInteger(v)) return;
      e[key] = v; cfgCommit();
    }, { step: 1, placeholder: ph });
    const side = document.createElement('select');
    for (const [v, tx] of sideOpts) {
      const o = document.createElement('option'); o.value = v; o.textContent = tx;
      if ((e.side || 'left') === v) o.selected = true;
      side.append(o);
    }
    side.addEventListener('change', () => { e.side = side.value; cfgCommit(); });
    const cpr = mk('number', dg(e, 'countsPerRev', 2048), (inp) => {
      const v = parseFloat(inp.value);
      inp.classList.toggle('bad', !(v > 0));
      if (!(v > 0)) return;
      e.countsPerRev = v; cfgCommit();
    }, { step: 1 });
    const revLbl = document.createElement('label');
    const rev = document.createElement('input');
    rev.type = 'checkbox'; rev.checked = !!e.reversed;
    rev.addEventListener('change', () => { e.reversed = rev.checked; cfgCommit(); });
    revLbl.append(rev, ' ' + t('cfg_enc_rev'));
    const del = document.createElement('button');
    del.type = 'button'; del.className = 'ghost small'; del.textContent = '×'; del.title = t('cfg_remove');
    del.addEventListener('click', () => { encs.splice(i, 1); renderEncoders(holder); cfgCommit(); });
    row.append(intInput('a', t('cfg_enc_a')), intInput('b', t('cfg_enc_b')), side, cpr, revLbl, del);
    holder.append(row);
  });
}

window.renderConfigurator = function renderConfigurator() {
  const root = $('#configurator');
  root.textContent = '';
  let parsed;
  try { parsed = JSON.parse(state.config || '{}'); } catch (e) { parsed = null; }
  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
    const m = document.createElement('div');
    m.className = 'cfg-msg error';
    m.textContent = t('cfg_json_bad');
    root.append(m);
    return;
  }
  cfgObj = parsed;
  if (!Array.isArray(cfgObj.encoders)) cfgObj.encoders = [];

  const intro = document.createElement('p');
  intro.className = 'cfg-h';
  intro.style.fontWeight = '400';
  intro.style.color = 'var(--muted)';
  intro.textContent = t('cfg_intro');
  const msgs = document.createElement('div');
  msgs.id = 'cfg-msgs';
  msgs.className = 'cfg-msgs';
  root.append(intro, msgs);

  const D = CFG_DEFAULTS.drivetrain;
  const dp = (k) => ['drivetrain', k];

  root.append(grid(textField(['robotName'], 'cfg_name', CFG_DEFAULTS.robotName)));

  const drive = section('cfg_sec_drive');
  drive.append(grid(
    selectField(dp('motor'), 'cfg_motor', MOTORS.map((m) => [m, m]), D.motor),
    numField(dp('motorsPerSide'), 'cfg_mps', 1, D.motorsPerSide),
    numField(dp('gearing'), 'cfg_gearing', 0.01, D.gearing),
    numField(dp('massKg'), 'cfg_mass', 0.5, D.massKg),
    numField(dp('momentOfInertiaKgM2'), 'cfg_moi', 0.1, D.momentOfInertiaKgM2),
    numField(dp('wheelRadiusMeters'), 'cfg_wheel', 0.001, D.wheelRadiusMeters),
    numField(dp('trackWidthMeters'), 'cfg_track', 0.01, D.trackWidthMeters),
    numField(dp('efficiency'), 'cfg_eff', 0.01, D.efficiency),
    numField(dp('rollingFriction'), 'cfg_roll', 0.005, D.rollingFriction),
    numField(dp('turnFriction'), 'cfg_turn', 0.01, D.turnFriction),
    numField(dp('tractionCoefficient'), 'cfg_trac', 0.05, D.tractionCoefficient),
    numField(dp('currentLimitAmps'), 'cfg_cur', 1, D.currentLimitAmps),
    selectField(dp('idleMode'), 'cfg_idle', [['brake', t('cfg_brake')], ['coast', t('cfg_coast')]], D.idleMode),
  ));
  root.append(drive);

  const wiring = section('cfg_sec_wiring');
  wiring.append(grid(
    pwmField(['wiring', 'left', 'pwm'], 'cfg_lpwm', [0]),
    checkField(['wiring', 'left', 'reversed'], 'cfg_lrev', false),
    pwmField(['wiring', 'right', 'pwm'], 'cfg_rpwm', [1]),
    checkField(['wiring', 'right', 'reversed'], 'cfg_rrev', true),
  ));
  root.append(wiring);

  const enc = section('cfg_sec_enc');
  const encHolder = document.createElement('div');
  renderEncoders(encHolder);
  const addEnc = document.createElement('button');
  addEnc.type = 'button'; addEnc.className = 'ghost small'; addEnc.textContent = t('cfg_add_enc');
  addEnc.addEventListener('click', () => {
    const usedCh = new Set();
    for (const e of cfgObj.encoders) { usedCh.add(e.a); usedCh.add(e.b); }
    let a = 0;
    while (usedCh.has(a) || usedCh.has(a + 1)) a += 2;
    cfgObj.encoders.push({ a, b: a + 1, side: cfgObj.encoders.some((e) => e.side === 'left') ? 'right' : 'left', countsPerRev: 2048 });
    renderEncoders(encHolder);
    cfgCommit();
  });
  enc.append(encHolder, addEnc);
  root.append(enc);

  const gyro = section('cfg_sec_gyro');
  gyro.append(grid(numField(['gyro', 'channel'], 'cfg_gyro_ch', 1, 0)));
  root.append(gyro);

  const actions = document.createElement('div');
  actions.className = 'cfg-actions';
  const dl = document.createElement('button');
  dl.type = 'button'; dl.className = 'ghost'; dl.textContent = t('cfg_download');
  dl.addEventListener('click', () => {
    const blob = new Blob([JSON.stringify(cfgObj, null, 2)], { type: 'application/json' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'robot-config.json';
    document.body.appendChild(a); a.click(); a.remove();
    setTimeout(() => URL.revokeObjectURL(a.href), 2000);
  });
  actions.append(dl);
  root.append(actions);

  renderCfgMessages();
};

// Sayfa acilisinda sekme zaten Robot ise form gelsin.
if (state.current === ROBOT_TAB) syncPane();
