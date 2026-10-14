
/* ============================================================
   业务能力侧 · 人：责任人详情
   说明：从业年限、服务户数、擅长、服务承诺均由【装企后台自填并审核】，
        平台不推算、不评分、不做跨企业对比。
   ============================================================ */
var PEOPLE = {
  xu:   { n:'徐勇',   r:'设计师',      c:'av-a', duty:'负责您家的设计与报价', good:'小户型改造 · 动线优化',
          y:9,  h:126, promise:'工作日 2 小时内回复',
          did:[['09-28','上门量房，量了客餐厅、主卧、次卧、卫生间'],['10-02','出了 3 套平面方案，和你一起定了 B 案'],['10-03','改了报价里两处，把总额调到你能接受的范围'],['10-19','约你到店确认客厅瓷砖']] },
  zhang:{ n:'张伟',   r:'项目经理',    c:'av-b', duty:'负责您家的工地现场、进度与验收', good:'水电与防水工艺',
          y:12, h:210, promise:'全天可联系（夜间请留言）',
          did:[['10-08','开工交底，和你确认了施工范围与成品保护'],['10-10','你发现客卫空鼓，当天安排返工'],['10-14','水电改造完成，提交 6 项验收']] },
  chen: { n:'陈斌',   r:'材料员',      c:'av-c', duty:'负责您家的材料下单、到场与核对', good:'主材比价与到场核验',
          y:6,  h:88,  promise:'材料到场当天同步给你',
          did:[['10-10','客厅瓷砖下单，预计 10-22 到场'],['10-16','防水材料到场，到场后拍照给你确认']] },
  li:   { n:'李国强', r:'泥瓦班组长',  c:'av-d', duty:'负责您家的拆改与泥瓦工程', good:'墙地砖与防水基层',
          y:15, h:150, promise:'按排期进场，进度同步给你',
          did:[['10-10','完成墙体拆除与垃圾清运'],['10-12','客卫空鼓返工完成，你复验通过']] },
  wang: { n:'王建国', r:'电工',        c:'av-e', duty:'负责您家的水电改造', good:'强弱电分槽与给排水',
          y:11, h:163, promise:'施工期间每天在群里报进度',
          did:[['10-11','进场开始强弱电布管'],['10-14','布线收尾、线盒加盖、打压试验完成']] },
  zw:   { n:'张卫东', r:'防水工',      c:'av-e', duty:'负责您家的防水工程', good:'卫生间防水与闭水试验',
          y:8,  h:112, promise:'闭水试验全程拍照留档',
          did:[['10-17','计划进场，卫生间防水第一遍']] }
};
function openPerson(k) {
  var p = PEOPLE[k];
  if (!p) return;
  document.getElementById('personTitle').textContent = p.n + ' · ' + p.r;
  var did = p.did.map(function (d) {
    return '<div class="tl-item" style="padding:9px 0"><div class="tl-dot done"><svg viewBox="0 0 24 24"><path d="M5 12.5l4.5 4.5L19 7.5"/></svg></div>'
      + '<div class="tl-body"><div class="tl-day tnum" style="font-size:12.5px">' + d[0] + '</div>'
      + '<div class="tl-what" style="font-size:12.5px">' + d[1] + '</div></div></div>';
  }).join('');
  document.getElementById('personBody').innerHTML =
    '<div class="p-hero">'
    + '<div class="av2 ' + p.c + '">' + p.n.charAt(0) + '</div>'
    + '<div><div class="nm2">' + p.n + '</div>'
    + '<div class="rl2">' + p.r + ' · 栖岸装饰</div>'
    + '<div class="dy2">' + p.duty + '</div></div></div>'
    + '<div class="secttl"><span class="bar"></span>他的情况</div>'
    + '<div class="card"><div class="p-kv">'
    + '<div><div class="n">' + p.y + '<span>年</span></div><div class="k">从业年限</div></div>'
    + '<div><div class="n">' + p.h + '<span>户</span></div><div class="k">服务过</div></div>'
    + '<div><div class="n">' + p.did.length + '<span>次</span></div><div class="k">在你家</div></div>'
    + '</div>'
    + '<div class="gate" style="margin-top:14px"><svg viewBox="0 0 24 24" stroke-linecap="round"><path d="M20 6.5 9.5 17 4 11.5"/></svg>'
    + '<span>以上信息由 <b>栖岸装饰</b> 自行填写并审核。平台不推算、不评分、不与其它公司比较。</span></div></div>'
    + '<div class="secttl"><span class="bar"></span>擅长与承诺</div>'
    + '<div class="card">'
    + '<div class="row between" style="padding:8px 0"><span class="sm muted">擅长</span><span class="sm strong">' + p.good + '</span></div>'
    + '<div class="divide" style="margin:8px -16px"></div>'
    + '<div class="row between" style="padding:8px 0"><span class="sm muted">服务承诺</span><span class="sm strong">' + p.promise + '</span></div>'
    + '</div>'
    + '<div class="secttl"><span class="bar"></span>他为您家做了什么</div>'
    + '<div class="card"><div class="tl">' + did + '</div></div>'
    + '<div style="height:18px"></div>';
  document.getElementById('personFoot').innerHTML =
    '<div class="btn plain" style="flex:0 0 96px" onclick="toast(\'正在拨号…\')">打电话</div>'
    + '<div class="btn primary grow" onclick="toast(\'已复制他的企业微信\')">在微信里找他</div>';
  openSheet('s-person');
}
