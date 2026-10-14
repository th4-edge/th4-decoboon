import sys; sys.path.insert(0,'.')
from _px import load
def cls(r,g,b):
    mx,mn=max(r,g,b),min(r,g,b)
    if mx<70: return '#'
    if r>235 and g>235 and b>235: return '.'
    if r>240 and g>235 and b<232 and b>200: return 'y'
    if g>r+12 and g>b+2 and g>110: return 'G'
    if r>g+12 and g>b+8 and r>150: return 'O'
    if abs(r-g)<14 and abs(g-b)<18 and 222<=mx<=246: return '-'
    if mx<170: return 'x'
    return 'o'
def art(path,cols=52,rows=46):
    w,h,bpp,buf=load(path)
    def px(x,y):
        o=(y*w+x)*bpp; return buf[o],buf[o+1],buf[o+2]
    y=h//2; row=[px(x,y) for x in range(w)]
    dark=[i for i,c in enumerate(row) if max(c)<70]
    if not dark: print(path,'no bezel'); return
    x0,x1=dark[0],dark[-1]   # 机身外圈
    # 屏幕 = 内缩 13px(CSS)*2
    sx0,sx1=x0+26,x1-26
    y0,y1=0,h
    print('== '+(path.split('/')[-1])+f'  bezel CSS {x0/2:.0f}..{x1/2:.0f}')
    for ry in range(rows):
        line=''
        for rx in range(cols):
            X=sx0+(sx1-sx0)*rx//cols; Y=y0+(y1-y0)*ry//rows
            line+=cls(*px(X,Y))
        print('  '+line)
for p in sys.argv[1:]: art(p)
