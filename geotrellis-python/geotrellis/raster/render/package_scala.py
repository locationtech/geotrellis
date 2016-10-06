
class RGBA(object):
    def __init__(self, r, g = None, b = None, a = None):
        if g is None and b is None and a is None:
            self.int = r
        else:
            if isinstance(a, float):
                if a < 0 or a > 100:
                    raise Exception("a is supposed to be a percentage, but is out of [0, 100] bounds")
                a = int(a * 2.55)
            self.int = (r << 24) + (g << 16) + (b << 8) + a

def red(n): return (n >> 24) & 0xff
def green(n): return (n >> 16) & 0xff
def blue(n): return (n >> 8) & 0xff
def alpha(n): return n & 0xff
def isOpaque(n): return alpha(n) == 255
def isTransparent(n): return alpha(n) == 0
def isGrey(n): return red(n) == green(n) and green(n) == blue(n)
def unzip(n): return (red(n), green(n), blue(n), alpha(n))
def toARGB(n): return (n >> 8) | (alpha(n) << 24)
def unzipRGBA(n): return (red(n), green(n), blue(n), alpha(n))
def unzipRGB(n): return (red(n), green(n), blue(n))

def RGB(r, g, b): return ((r << 24) + (g << 16) + (b << 8)) | 0xFF
