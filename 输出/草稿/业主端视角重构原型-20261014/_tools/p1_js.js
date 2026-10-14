
/* ============================================================
   P1 第一批 · 六项功能交互
   ============================================================ */
function toggleAmt(el) {
  el.classList.toggle('on');
  var on = el.classList.contains('on');
  Array.prototype.forEach.call(document.querySelectorAll('#p-confirm .cf .amt'), function (a) {
    a.style.display = on ? '' : 'none';
  });
  toast(on ? '已显示金额' : '已隐藏金额（仅你自己可见）');
}
function pick(op, group) {
  op.parentElement.querySelectorAll('span').forEach(function (s) { s.classList.remove('on'); });
  op.classList.add('on');
}
function submitChecklist() {
  var unsure = document.querySelectorAll('#s-checklist .ops span.unsure.on').length;
  closeSheet('s-checklist');
  toast(unsure ? '已记录，还有 ' + unsure + ' 项待确认，可随时问设计师' : '已记录，26 项已全部对完');
}
function confirmArrival(ok) {
  closeSheet('s-arrival');
  toast(ok ? '已确认与所选一致，可以开封使用' : '已提交异议，材料员会与你核对');
}
function pickComplaint(el) {
  el.parentElement.querySelectorAll('.opt').forEach(function (o) { o.classList.remove('on'); });
  el.classList.add('on');
}
function submitComplaint() {
  var t = document.getElementById('cpText').value.trim();
  var who = document.querySelector('#s-complaint .opt.on');
  if (!who) { toast('先选一下你要投诉的对象'); return; }
  if (!t) { toast('简单说一句发生了什么，方便负责人判断'); return; }
  document.getElementById('cpBody').innerHTML =
    '<div style="padding:22px 16px 0">' +
      '<div class="card" style="margin:0;text-align:center;padding:22px 18px">' +
        '<div style="width:56px;height:56px;border-radius:50%;background:var(--success-soft);margin:0 auto;display:flex;align-items:center;justify-content:center">' +
          '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="var(--success)" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6.5 9.5 17 4 11.5"/></svg></div>' +
        '<div style="font-size:16.5px;font-weight:700;margin-top:14px">已送达栖岸装饰负责人</div>' +
        '<div class="tiny muted" style="margin-top:8px;line-height:1.7">48 小时内必须有回复<br>超时系统会自动升级并留下记录</div>' +
      '</div>' +
      '<div class="card" style="margin:12px 0 0">' +
        '<div class="sla"><div class="ring2"><i>48h</i></div>' +
        '<div class="grow"><div class="strong sm">处理时限</div>' +
        '<div class="tiny muted" style="margin-top:3px">还剩 47 小时 58 分</div></div>' +
        '<span class="tag pending">处理中</span></div>' +
        '<div class="steps" style="margin-top:16px">' +
          '<div class="done"><div class="d2 on"></div><div class="lb on">已送达</div></div>' +
          '<div><div class="d2 doing"></div><div class="lb on">处理中</div></div>' +
          '<div><div class="d2"></div><div class="lb">已回复</div></div>' +
          '<div><div class="d2"></div><div class="lb">已关闭</div></div>' +
        '</div></div>' +
      '<div class="card" style="margin:12px 0 0"><div class="note"><b>你看得到的：</b>处理进度、承诺时限、倒计时。<br>' +
      '<b>你看不到的：</b>企业内部的处理与考核记录。<br>平台只做记录与保管，不介入企业与你的处理过程。</div></div>' +
      '<div style="height:20px"></div></div>';
  document.querySelector('#s-complaint .fixedbar').innerHTML =
    '<div class="btn plain grow" onclick="closeSheet(\'s-complaint\')">知道了</div>';
  toast('投诉已送达负责人');
}
/* 全景拖动 */
var panoDrag = null;
function panoDown(e) {
  var box = document.getElementById('panoBox');
  var x = (e.touches ? e.touches[0].clientX : e.clientX);
  panoDrag = { box: box, startX: x, startPos: panoPos };
  box.classList.add('grab');
}
function panoMove(e) {
  if (!panoDrag) return;
  var x = (e.touches ? e.touches[0].clientX : e.clientX);
  var d = panoDrag.startX - x;
  var max = panoDrag.box.clientWidth * 2;   // 背景宽 300%
  panoPos = Math.max(0, Math.min(max, panoDrag.startPos + d));
  panoDrag.box.querySelector('.bg').style.transform = 'translateX(-' + panoPos + 'px)';
}
function panoUp() {
  if (panoDrag) { panoDrag.box.classList.remove('grab'); panoDrag = null; }
}
var panoPos = 0;
function pickRoom(el) {
  el.parentElement.querySelectorAll('span').forEach(function (s) { s.classList.remove('on'); });
  el.classList.add('on');
  var bg = document.getElementById('panoBox').querySelector('.bg');
  var tone = { '卫生间': '#5A6C78', '厨房': '#6B7280', '客厅': '#4E6070' }[el.textContent.trim()] || '#5A6C78';
  bg.style.background = 'linear-gradient(180deg,' + tone + ' 0%,#7C8D98 42%,#66757F 43%,#414F59 100%)';
  panoPos = 0; bg.style.transform = 'translateX(0)';
}
