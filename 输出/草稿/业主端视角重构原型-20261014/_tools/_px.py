import zlib,struct,sys
def load(p):
    d=open(p,'rb').read(); assert d[:8]==b'\x89PNG\r\n\x1a\n'
    i=8; idat=b''; w=h=bd=ct=None
    while i<len(d):
        ln=struct.unpack('>I',d[i:i+4])[0]; typ=d[i+4:i+8]; data=d[i+8:i+8+ln]; i+=12+ln
        if typ==b'IHDR': w,h,bd,ct=struct.unpack('>IIBB',data[:10])
        elif typ==b'IDAT': idat+=data
        elif typ==b'IEND': break
    raw=zlib.decompress(idat); ch={0:1,2:3,3:1,4:2,6:4}[ct]; bpp=ch*bd//8; stride=w*bpp
    out=bytearray(); prev=bytearray(stride); pos=0
    for y in range(h):
        f=raw[pos]; pos+=1; line=bytearray(raw[pos:pos+stride]); pos+=stride
        for x in range(stride):
            a=line[x-bpp] if x>=bpp else 0; b=prev[x]; c=prev[x-bpp] if x>=bpp else 0
            if f==1: line[x]=(line[x]+a)&255
            elif f==2: line[x]=(line[x]+b)&255
            elif f==3: line[x]=(line[x]+(a+b)//2)&255
            elif f==4:
                pp=a+b-c; pa=abs(pp-a); pb=abs(pp-b); pc=abs(pp-c)
                pr=a if (pa<=pb and pa<=pc) else (b if pb<=pc else c)
                line[x]=(line[x]+pr)&255
        out+=line; prev=line
    return w,h,bpp,bytes(out)
def px(w,bpp,buf,x,y): o=y*w*bpp+x*bpp; return buf[o],buf[o+1],buf[o+2]
if __name__=='__main__':
    for name in sys.argv[1:]:
        w,h,bpp,buf=load(name)
        # 采样：手机左右边框位置在 x≈30 与 x≈830（2x）
        samples={}
        for label,x,y in [('top-left',40,60),('hero-mid',380,300),('mid',380,1400),('bottom',380,1780)]:
            samples[label]=px(w,bpp,buf,x,y)
        print(f"{name} {w}x{h} bpp={bpp}")
        for k,v in samples.items(): print('   ',k,'#%02X%02X%02X'%v)
