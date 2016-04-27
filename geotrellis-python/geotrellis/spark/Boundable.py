from geotrellis.spark.KeyBounds import KeyBounds

class Boundable(object):
    def __init__(self, min_bound_func, max_bound_func):
        self.min_bound_func = min_bound_func
        self.max_bound_func = max_bound_func

    def min_bound(self, p1, p2):
        return self.min_bound_func(p1, p2)

    def max_bound(self, p1, p2):
        return self.max_bound_func(p1, p2)

    def include(self, p, bounds):
        return KeyBounds(
                self.min_bound(bounds.min_key, p),
                self.max_bound(bounds.max_key, p))

    def includes(self, p, bounds):
        return bounds == self.include(p, bounds)

    def combine(self, b1, b2):
        return KeyBounds(
                self.min_bound(b1.min_key, b2.min_key),
                self.max_bound(b2.max_key, b2.max_key))

    def intersect(self, b1, b2):
        kb = KeyBounds(
                self.max_bound(b1.min_key, b2.min_key),
                self.min_bound(b2.max_key, b2.max_key))

        if self.min_bound(kb.min_key, kb.max_key) == kb.min_key:
            return kb
        else:
            return None

    def intersects(self, b1, b2):
        return self.intersect(b1, b2) is not None

