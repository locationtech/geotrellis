from __future__ import absolute_import
from geotrellis.spark.io.index.zcurve.Z2 import Z2

class Z2Range(object):
    def __init__(self, _min, _max):
        if _min.z > _max.z:
            raise Exception("NOT: {0} < {1}".format(_min.z, _max.z))
        self.min = _min
        self.max = _max

    def __eq__(self, other):
        if not isinstance(other, Z2Range):
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

    def contains(self, z):
        if isinstance(z, Z2):
            x,y = z.decode
            _min = self.min
            _max = self.max
            return (x >= _min.dim(0) and
                    x <= _max.dim(0) and
                    y >= _min.dim(1) and
                    y <= _max.dim(1))
        else:
            return self.contains(z.min) and self.contains(z.max)

    def overlaps(self, r):
        def _overlaps(a1, a2, b1, b2):
            return max(a1, b1) <= min(a2, b2)
        return (_overlaps(self.min.dim(0), self.max.dim(0), r.min.dim(0), r.max.dim(0)) and 
                _overlaps(self.min.dim(1), self.max.dim(1), r.min.dim(1), r.max.dim(1)))

    def cut(self, xd, in_range):
        if self.min.z == self.max.z:
            return []
        elif in_range:
            if xd.z == self.min.z:
                return [Z2Range(self.max, self.max)]
            elif xd.z == self.max.z:
                return [Z2Range(self.min, self.min)]
            else:
                return [Z2Range(self.min, xd-1), Z2Range(xd+1, self.max)]
        else:
            litmax, bigmin = Z2.zdivide(xd, self.min, self.max)
            return [Z2Range(self.min, litmax), Z2Range(bigmin, self.max)]

