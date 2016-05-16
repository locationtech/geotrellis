from __future__ import absolute_import
#from geotrellis.spark.io.index.zcurve.Z3Range import Z3Range
from geotrellis.spark.io.index.package_scala import zdiv
from geotrellis.spark.io.index.MergeQueue import MergeQueue
from geotrellis.python.util.utils import toBinaryString
import functools

@functools.total_ordering
class Z3(object):
    def __init__(self, x, y = None, z = None):
        if y is None and z is None:
            self.z = x
        else:
            self.z = Z3.split(x) | Z3.split(y) << 1 | Z3.split(z) << 2

    def __lt__(self, other):
        return self.z < other.z

    def __eq__(self, other):
        return self.z == other.z

    def __add__(self, offset):
        return Z3(self.z + offset)

    def __sub__(self, offset):
        return Z3(self.z - offset)

    def __hash__(self):
        return hash(self.z)

    @property
    def decode(self):
        return (Z3.combine(self.z),
                Z3.combine(self.z >> 1),
                Z3.combine(self.z >> 2))

    def dim(self, i):
        return Z3.combine(self.z >> i)

    def inRange(self, rmin, rmax):
        x,y,z = self.decode
        return (x >= rmin.dim(0) and
                x <= rmax.dim(0) and
                y >= rmin.dim(1) and
                y <= rmax.dim(1) and
                z >= rmin.dim(2) and
                z <= rmax.dim(2))

    def mid(self, p):
        if p.z < self.z:
            return Z3(p.z + (self.z - p.z) / 2)
        else:
            return Z3(self.z + (p.z - self.z) / 2)

    @property
    def bitsToString(self):
        return "({0:16s})({1:8s},{2:8s},{3:8s})".format(
                toBinaryString(self.z),
                toBinaryString(self.dim(0)),
                toBinaryString(self.dim(1)),
                toBinaryString(self.dim(2))
                )

    def __str__(self):
        return "{z}{dec}".format(z = self.z, dec = self.decode)

    MAX_BITS = 21
    MAX_MASK = 0x1fffffL
    MAX_DIM = 3

    @staticmethod
    def split(value):
        x = value & Z3.MAX_MASK;
        x = (x | x << 32) & 0x1f00000000ffffL
        x = (x | x << 16) & 0x1f0000ff0000ffL
        x = (x | x << 8)  & 0x100f00f00f00f00fL
        x = (x | x << 4)  & 0x10c30c30c30c30c3L
        x = (x | x << 2)  & 0x1249249249249249L
        return x

    @staticmethod
    def combine(z):
        x = z & 0x1249249249249249L
        x = (x ^ (x >>  2)) & 0x10c30c30c30c30c3L
        x = (x ^ (x >>  4)) & 0x100f00f00f00f00fL 
        x = (x ^ (x >>  8)) & 0x1f0000ff0000ffL 
        x = (x ^ (x >> 16)) & 0x1f00000000ffffL 
        x = (x ^ (x >> 32)) & Z3.MAX_MASK
        return x

    @staticmethod
    def unapply(instance):
        return instance.decode

    @staticmethod
    def zdivide(p, rmin, rmax):
        litmax, bigmin = zdiv(Z3.load, Z3.MAX_DIM)(p.z, rmin.z, rmax.z)
        return (Z3(litmax), Z3(bigmin))

    @staticmethod
    def load(target, p, bits, dim):
        mask = ~(Z3.split(Z3.MAX_MASK >> (Z3.MAX_BITS - bits)) << dim)
        wiped = target & mask
        return wiped | (Z3.split(p) << dim)

    @staticmethod
    def zranges(min_z3, max_z3):
        mq = MergeQueue()
        sr = Z2Range(min_z3, max_z3)

        rec_counter = 0
        report_counter = 0

        def _zranges(prefix, offset, quad):
            rec_counter += 1

            _min = prefix | (quad << offset)
            _max = _min | (1L << offset) - 1

            from geotrellis.spark.io.index.zcurve.Z3Range import Z3Range
            qr = Z3Range(Z3(_min), Z3(_max))
            if sr.contains(qr):
                mq += (qr.min.z, qr.max.z)
                report_counter += 1
            elif offset > 0 and sr.overlaps(qr):
                _zranges(_min, offset - Z3.MAX_DIM, 0)
                _zranges(_min, offset - Z3.MAX_DIM, 1)
                _zranges(_min, offset - Z3.MAX_DIM, 2)
                _zranges(_min, offset - Z3.MAX_DIM, 3)
                _zranges(_min, offset - Z3.MAX_DIM, 4)
                _zranges(_min, offset - Z3.MAX_DIM, 5)
                _zranges(_min, offset - Z3.MAX_DIM, 6)
                _zranges(_min, offset - Z3.MAX_DIM, 7)

        prefix = 0
        offset = Z3.MAX_BITS * Z3.MAX_DIM
        _zranges(prefix, offset, 0)
        return mq.toSeq
