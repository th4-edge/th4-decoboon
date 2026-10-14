import sys; sys.path.insert(0,'.')
from _px import load
def cls(r,g,b):
    mx,mn=max(r,g,b),min(r,g,b)
    if mx<70: return '#'          # 近黑（机身）
    if r>235 and g>235 and b>235: return '.'   # 白（卡片）
    if g>r+14 and g>b+2 and g>110: return 'G'  # 绿（品牌）
    if r>235 and g>235 and b>200: return 'y'   # 暖白（警示卡）
    if abs(r-g)<14 and abs(g-b)<16 and 225<=mx<=245: return '-'  # 浅灰底
    if mx<160: return 'x'         # 中深（照片/文字块）
    return 'o'
def art(path,cols=48,rows=44):
    w,h,bpp,buf=load(path)
    # 只取手机区域：CSS 20..410 -> 2x 40..820
    x0,x1,y0,y1=40,min(820,w),64,min(1752,h)
    print('== '+path.split('/')[-1])
    for ry in range(rows):
        line=''
        for rx in range(cols):
            X=x0+(x1-x0)*rx//cols; Y=y0+(y1-y0)*ry//rows
            o=Y*w*bpp+X*bpp
            line+=cls(buf[o],buf[o+1],buf[o+2])
        print('  '+line)
for p in sys.argv[1:]: art(p)
