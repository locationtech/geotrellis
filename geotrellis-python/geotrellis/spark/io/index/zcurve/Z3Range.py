from __future__ import absolute_import
from geotrellis.spark.io.index.zcurve.Z3 import Z3

class Z3Range(object):
    def __init__(self, _min, _max):
        if _min.z > _max.z:
            raise Exception("NOT: {0} < {1}".format(_min.z, _max.z))
        self.min = _min
        self.max = _max

    def __eq__(self, other):
        if not isinstance(other, Z3Range):
            return False
        return self.min == other.min and self.max == other.max

    def __hash__(self):
        return hash((self.min, self.max))

    def mid(self):
        _min = self.min
        _max = self.max
        return _min.mid(_max)

    def length(self):
        return self.max.z - self.min.z

    def contains(self, bits):
        if isinstance(bits, Z3):
            x,y,z = bits.decode
            _min = self.min
            _max = self.max
            return (x >= _min.dim(0) and
                    x <= _max.dim(0) and
                    y >= _min.dim(1) and
                    y <= _max.dim(1) and
                    z >= _min.dim(2) and
                    z <= _max.dim(2))
        else:
            _range = bits
            return self.contains(_range.min) and self.contains(_range.max)

    def overlaps(self, r):
        def _overlaps(a1, a2, b1, b2):
            return max(a1, b1) <= min(a2, b2)
        return (_overlaps(self.min.dim(0), self.max.dim(0), r.min.dim(0), r.max.dim(0)) and 
                _overlaps(self.min.dim(1), self.max.dim(1), r.min.dim(1), r.max.dim(1)) and
                _overlaps(self.min.dim(2), self.max.dim(2), r.min.dim(2), r.max.dim(2)))

    def zdivide(self, xd):
        return Z3.zdivide(xd, self.min, self.max)

    def cut(self, xd, in_range):
        if self.min.z == self.max.z:
            return []
        elif in_range:
            if xd.z == self.min.z:
                return [Z3Range(self.max, self.max)]
            elif xd.z == self.max.z:
                return [Z3Range(self.min, self.min)]
            else:
                return [Z3Range(self.min, xd-1), Z2Range(xd+1, self.max)]
        else:
            litmax, bigmin = Z3.zdivide(xd, self.min, self.max)
            return [Z3Range(self.min, litmax), Z2Range(bigmin, self.max)]

