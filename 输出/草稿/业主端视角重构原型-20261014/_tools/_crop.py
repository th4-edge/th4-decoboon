import zlib,struct,sys
def load(p):
    d=open(p,'rb').read(); i=8; idat=b''; w=h=ct=None
    while i<len(d):
        ln=struct.unpack('>I',d[i:i+4])[0]; typ=d[i+4:i+8]; data=d[i+8:i+8+ln]; i+=12+ln
        if typ==b'IHDR': w,h,bd,ct=struct.unpack('>IIBB',data[:10])
        elif typ==b'IDAT': idat+=data
        elif typ==b'IEND': break
    raw=zlib.decompress(idat); ch={0:1,2:3,4:2,6:4}[ct]; bpp=ch; stride=w*bpp
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
def save(p,w,h,buf):
    raw=bytearray()
    for y in range(h):
        raw.append(0); raw+=buf[y*w*3:(y+1)*w*3]
    def chunk(t,d):
        c=struct.pack('>I',len(d))+t+d
        return c+struct.pack('>I',zlib.crc32(t+d)&0xffffffff)
    out=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(bytes(raw),9))+chunk(b'IEND',b'')
    open(p,'wb').write(out)
def crop(src,dst,x0,y0,x1,y1):
    w,h,bpp,buf=load(src)
    x0=max(0,x0);y0=max(0,y0);x1=min(w,x1);y1=min(h,y1)
    cw,ch=x1-x0,y1-y0; out=bytearray()
    for y in range(y0,y1):
        out+=buf[(y*w+x0)*bpp:(y*w+x1)*bpp]
    save(dst,cw,ch,bytes(out)); return cw,ch
if __name__=='__main__':
    print(crop(sys.argv[1],sys.argv[2],*map(int,sys.argv[3:7])))
