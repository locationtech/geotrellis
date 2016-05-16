from __future__ import absolute_import
#from geotrellis.spark.io.index.zcurve.Z2Range import Z2Range
from geotrellis.spark.io.index.package_scala import zdiv
from geotrellis.spark.io.index.MergeQueue import MergeQueue
from geotrellis.python.util.utils import toBinaryString

class Z2(object):
    def __init__(self, x, y = None):
        if y is None:
            self.z = x
        else:
            self.z = Z2.split(x) | Z2.split(y) << 1

    def __lt__(self, other):
        return self.z < other.z

    def __le__(self, other):
        return self.z <= other.z

    def __gt__(self, other):
        return self.z > other.z

    def __ge__(self, other):
        return self.z >= other.z

    def __add__(self, offset):
        return Z2(self.z + offset)
    
    def __sub__(self, offset):
        return Z2(self.z - offset)

    def __eq__(self, other):
        return self.z == other.z

    def __ne__(self, other):
        return self.z != other.z

    def __hash__(self):
        return hash(self.z)

    @property
    def decode(self):
        return (Z2.combine(self.z), Z2.combine(self.z >> 1))

    def dim(self, i):
        return Z2.combine(self.z >> i)

    def mid(self, p):
        if p.z < self.z:
            return Z2(p.z + (self.z - p.z)/2)
        else:
            return Z2(self.z + (p.z - self.z)/2)

    @property
    def bitsToString(self):
        return "({0:16s})({1:8s},{2:8s})".format(
                toBinaryString(self.z),
                toBinaryString(self.dim(0)),
                toBinaryString(self.dim(1))
                )

    def __str__(self):
        return "{z}{dec}".format(z = self.z, dec = self.decode)

    MAX_BITS = 31
    MAX_MASK = 0x7fffffff
    MAX_DIM = 2

    @staticmethod
    def split(value):
        x = value & Z2.MAX_MASK
        x = (x ^ (x << 32)) & 0x00000000ffffffffL
        x = (x ^ (x << 16)) & 0x0000ffff0000ffffL
        x = (x ^ (x <<  8)) & 0x00ff00ff00ff00ffL # 11111111000000001111111100000000..
        x = (x ^ (x <<  4)) & 0x0f0f0f0f0f0f0f0fL # 1111000011110000
        x = (x ^ (x <<  2)) & 0x3333333333333333L # 11001100..
        x = (x ^ (x <<  1)) & 0x5555555555555555L # 1010...
        return x

    @staticmethod
    def combine(z):
        x = z & 0x5555555555555555L
        x = (x ^ (x >>  1)) & 0x3333333333333333L
        x = (x ^ (x >>  2)) & 0x0f0f0f0f0f0f0f0fL
        x = (x ^ (x >>  4)) & 0x00ff00ff00ff00ffL
        x = (x ^ (x >>  8)) & 0x0000ffff0000ffffL
        x = (x ^ (x >> 16)) & 0x00000000ffffffffL 
        return x

    @staticmethod
    def unapply(instance):
        return instance.decode() # wrapped with Some in original version

    @staticmethod
    def zdivide(p, rmin, rmax):
        litmax, bigmin = zdiv(Z2.load, Z2.MAX_DIM)(p.z, rmin.z, rmax.z)
        return (Z2(litmax), Z2(bigmin))

    @staticmethod
    def load(target, p, bits, dim):
        mask = ~(Z2.split(Z2.MAX_MASK >> (Z2.MAX_BITS-bits)) << dim)
        wiped = target & mask
        return wiped | (Z2.split(p) << dim)

    @staticmethod
    def zranges(min_z2, max_z2):
        from geotrellis.spark.io.index.zcurve.Z2Range import Z2Range
        mq = [MergeQueue()] # wrap with list to use in inner function
        sr = [Z2Range(min_z2, max_z2)]

        #rec_counter = [0]
        #report_counter = [0]

        def _zranges(prefix, offset, quad):
            #rec_counter[0] += 1

            _min = prefix | (quad << offset)
            _max = _min | (1L << offset) - 1

            qr = Z2Range(Z2(_min), Z2(_max))
            if sr[0].contains(qr):
                mq[0] += (qr.min.z, qr.max.z)
                #report_counter[0] += 1
            elif offset > 0 and sr[0].overlaps(qr):
                _zranges(_min, offset - Z2.MAX_DIM, 0)
                _zranges(_min, offset - Z2.MAX_DIM, 1)
                _zranges(_min, offset - Z2.MAX_DIM, 2)
                _zranges(_min, offset - Z2.MAX_DIM, 3)

        prefix = 0
        offset = Z2.MAX_BITS * Z2.MAX_DIM
        _zranges(prefix, offset, 0)
        return mq[0].toSeq

