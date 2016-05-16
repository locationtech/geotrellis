from __future__ import absolute_import
from geotrellis.spark.KeyBounds import KeyBounds

class Boundable(object):
    def __init__(self, minBoundFunc, maxBoundFunc):
        self.minBoundFunc = minBoundFunc
        self.maxBoundFunc = maxBoundFunc

    def minBound(self, p1, p2):
        return self.minBoundFunc(p1, p2)

    def maxBound(self, p1, p2):
        return self.maxBoundFunc(p1, p2)

    def include(self, p, bounds):
        return KeyBounds(
                self.minBound(bounds.minKey, p),
                self.maxBound(bounds.maxKey, p))

    def includes(self, p, bounds):
        return bounds == self.include(p, bounds)

    def combine(self, b1, b2):
        return KeyBounds(
                self.minBound(b1.minKey, b2.minKey),
                self.maxBound(b2.maxKey, b2.maxKey))

    def intersect(self, b1, b2):
        kb = KeyBounds(
                self.maxBound(b1.minKey, b2.minKey),
                self.minBound(b2.maxKey, b2.maxKey))

        if self.minBound(kb.minKey, kb.maxKey) == kb.minKey:
            return kb
        else:
            return None

    def intersects(self, b1, b2):
        return self.intersect(b1, b2) is not None

