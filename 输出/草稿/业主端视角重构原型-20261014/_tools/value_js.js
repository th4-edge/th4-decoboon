
/* ============================================================
   价值能力 · 装修历程：环境音乐 / 粒子 / 滚动揭示 / 数字滚动
   ============================================================ */
var musicPref = null;   // null=未表态（进页自动开） true=开 false=关

/* ---------- 环境音乐：Web Audio 现场合成，不依赖任何音频文件 ---------- */
var Ambient = (function () {
  var ac = null, master = null, step = 0, timer = null, on = false;
  var BASE = 196; // G3
  var CHORDS = [[-5, 0, 4, 7], [-3, 2, 5, 9], [0, 4, 7, 11], [-1, 4, 7, 12]];

  function ensure() {
    if (ac) return;
    var AC = window.AudioContext || window.webkitAudioContext;
    if (!AC) return;
    ac = new AC();
    master = ac.createGain();
    master.gain.value = 0.0001;
    var lp = ac.createBiquadFilter();
    lp.type = 'lowpass'; lp.frequency.value = 1500; lp.Q.value = 0.3;
    var dly = ac.createDelay(1.5); dly.delayTime.value = 0.45;
    var fb = ac.createGain(); fb.gain.value = 0.36;
    var wet = ac.createGain(); wet.gain.value = 0.30;
    master.connect(lp);
    lp.connect(ac.destination);
    lp.connect(dly); dly.connect(fb); fb.connect(dly); dly.connect(wet); wet.connect(ac.destination);
  }
  function pad(freqs, t, dur) {
    freqs.forEach(function (f, i) {
      var o = ac.createOscillator();
      o.type = i === 0 ? 'sine' : 'triangle';
      o.frequency.value = f;
      var g = ac.createGain();
      g.gain.value = 0.0001;
      o.connect(g); g.connect(master);
      var peak = 0.085 / (1 + i * 0.75);
      g.gain.setValueAtTime(0.0001, t);
      g.gain.linearRampToValueAtTime(peak, t + 2.6);
      g.gain.linearRampToValueAtTime(0.0001, t + dur);
      o.start(t); o.stop(t + dur + 0.06);
    });
  }
  function bell(f, t) {
    var o = ac.createOscillator(); o.type = 'sine'; o.frequency.value = f;
    var g = ac.createGain(); g.gain.value = 0.0001;
    o.connect(g); g.connect(master);
    g.gain.setValueAtTime(0.0001, t);
    g.gain.linearRampToValueAtTime(0.07, t + 0.03);
    g.gain.exponentialRampToValueAtTime(0.0002, t + 3.2);
    o.start(t); o.stop(t + 3.4);
  }
  function schedule() {
    if (!on || !ac) return;
    var t = ac.currentTime + 0.12;
    var c = CHORDS[step % CHORDS.length];
    pad(c.map(function (n) { return BASE * Math.pow(2, n / 12); }), t, 11);
    if (Math.random() < 0.62) {
      var pick = c[1 + Math.floor(Math.random() * 3)];
      bell(BASE * Math.pow(2, (pick + 12) / 12), t + 2.4 + Math.random() * 3.4);
    }
    step++;
    timer = setTimeout(schedule, 9000);
  }
  return {
    start: function () {
      ensure();
      if (!ac) return;
      if (ac.state === 'suspended') { try { ac.resume(); } catch (e) {} }
      if (on) return;
      on = true;
      master.gain.cancelScheduledValues(ac.currentTime);
      master.gain.setTargetAtTime(0.5, ac.currentTime, 0.9);
      schedule();
    },
    stop: function () {
      if (!ac || !on) return;
      on = false;
      clearTimeout(timer);
      master.gain.cancelScheduledValues(ac.currentTime);
      master.gain.setTargetAtTime(0.0001, ac.currentTime, 0.5);
    },
    isOn: function () { return on; }
  };
})();

function startMusic() {
  if (musicPref === false) return;
  Ambient.start();
  var b = document.getElementById('musicBtn');
  if (b) b.classList.add('on');
}
function stopMusic() {
  Ambient.stop();
  var b = document.getElementById('musicBtn');
  if (b) b.classList.remove('on');
}
function toggleMusic() {
  var btn = document.getElementById('musicBtn');
  var tip = document.getElementById('musicTip');
  if (Ambient.isOn()) {
    Ambient.stop(); musicPref = false;
    if (btn) btn.classList.remove('on');
    if (tip) { tip.textContent = '音乐已关闭'; tip.classList.add('show'); }
    setTimeout(function () { if (tip) tip.classList.remove('show'); }, 1800);
  } else {
    Ambient.start(); musicPref = true;
    if (btn) btn.classList.add('on');
    if (tip) tip.classList.remove('show');
  }
}

/* ---------- 光尘粒子 ---------- */
function makeDust() {
  var box = document.getElementById('dust');
  if (!box || box.dataset.done) return;
  box.dataset.done = '1';
  var html = '';
  for (var i = 0; i < 14; i++) {
    var l = Math.random() * 100;
    var b = Math.random() * 45;
    var dur = 9 + Math.random() * 11;
    var delay = -Math.random() * 18;
    var sz = Math.random() < 0.3 ? 3 : 2;
    html += '<i style="left:' + l.toFixed(1) + '%;bottom:' + b.toFixed(0) + '%;width:' + sz + 'px;height:' + sz +
      'px;animation-duration:' + dur.toFixed(1) + 's;animation-delay:' + delay.toFixed(1) + 's"></i>';
  }
  box.innerHTML = html;
}

/* ---------- 滚动揭示 + 数字滚动 ---------- */
var storyIO = null;
function runCount(el) {
  if (!el.querySelector) return;
  var t = el.querySelector('[data-count]');
  if (!t) return;
  var to = parseFloat(t.dataset.count || '0');
  var unit = t.querySelector('span');
  var unitHTML = unit ? unit.outerHTML : '';
  var t0 = performance.now();
  function frame(now) {
    var p = Math.min(1, (now - t0) / 1100);
    var v = Math.round(to * (1 - Math.pow(1 - p, 3)));
    t.innerHTML = v + unitHTML;
    if (p < 1) requestAnimationFrame(frame);
  }
  requestAnimationFrame(frame);
}
function initStory() {
  var page = document.getElementById('p-story');
  if (!page) return;
  makeDust();
  if (storyIO || page.dataset.ready) return;
  page.dataset.ready = '1';
  storyIO = new IntersectionObserver(function (entries) {
    entries.forEach(function (e) {
      if (!e.isIntersecting) return;
      e.target.classList.add('in');
      runCount(e.target);
      storyIO.unobserve(e.target);
    });
  }, { root: page, threshold: 0.14, rootMargin: '0px 0px -6% 0px' });
  Array.prototype.forEach.call(page.querySelectorAll('.rv'), function (el, i) {
    el.style.transitionDelay = ((i % 3) * 90) + 'ms';
    storyIO.observe(el);
  });
}
