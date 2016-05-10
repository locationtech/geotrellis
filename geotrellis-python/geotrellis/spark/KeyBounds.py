from geotrellis.spark.io.json.KeyFormats import KeyBoundsFormat
# from geotrellis.spark.SpatialKey import SpatialKey # we have to postpone the import to avoid errors caused by circular imports
from geotrellis.raster.GridBounds import GridBounds

class Bounds(object):
    def isEmpty(self):
        pass

    def nonEmpty(self):
        return not self.isEmpty

    def intersect(self, other, b):
        pass

    def intersects(self, other, b):
        return self.intersect(other, b).nonEmpty

    def get(self):
        pass

    def getOrElse(self, defaultfunc):
        if self.isEmpty:
            return defaultFunc()
        else:
            return self.get()

    def map(self, func):
        if self.isEmpty:
            return EmptyBounds
        else:
            return func(self.get())

    def flatMap(self, func):
        if self.isEmpty:
            return EmptyBounds
        else:
            return func(self.get())

    @staticmethod
    def fromRdd(rdd):
        return rdd.map(lambda k, tile: bounds(k, k)).fold(EmptyBounds, lambda a, b: a.combine(b))

    @staticmethod
    def toIterable(b):
        if b is EmptyBounds:
            return []
        else:
            return [b]

def bounds(_min, _max):
    return KeyBounds(_min, _max)

class TempEmptyBounds(Bounds):
    def isEmpty(self):
        return True

    def include(self, key, b = None):
        return KeyBounds(key, key)

    def includes(self, key, b = None):
        return False

    def combine(self, other, b = None):
        return other

    def contains(self, other, b = None):
        return False
    
    def intersect(self, other, b = None):
        return EmptyBounds

    def get(self):
        raise Exception("EmptyBounds.get") # TODO NoSuchElementException analog

    def getSpatialBounds(self, other, ev = None):
        return self

EmptyBounds = TempEmptyBounds()

class _KeyBoundsMeta(object):

    items = {}

    def __getitem__(self, keyType):
        if keyType in self.items.keys():
            return self.items[keyType]
        class tempo(_KeyBounds):
            implicits = {'format': lambda: KeyBoundsFormat(keyType.implicits['format']())}
        self.items[keyType] = tempo
        return tempo

    # TODO code duplication (see _KeyBounds.__init__)
    def __call__(self, minKey, maxKey = None):
        from geotrellis.spark.SpatialKey import SpatialKey
        if maxKey is None:
            if isinstance(minKey, GridBounds):
                gridBounds = minKey
                minKey = SpatialKey(gridBounds.colMin, gridBounds.rowMin)
                maxKey = SpatialKey(gridBounds.colMax, gridBounds.rowMax)
            else:
                raise Exception("wrong arguments ({0}) passed to KeyBounds constructor.".format(minKey))
        keyType = type(minKey)
        return self[keyType](minKey, maxKey)

    # static methods

    def includeKey(self, seq, key, boundable):
        mapped = map(lambda kb: kb.includes(key, boundable), seq)
        return reduce(lambda a, b: a or b, mapped)

    def toTuple(self, keyBounds):
        return (keyBounds.minKey, keyBounds.maxKey)

KeyBounds = _KeyBoundsMeta()

class _KeyBounds(Bounds):
    def __init__(self, minKey, maxKey = None):
        from geotrellis.spark.SpatialKey import SpatialKey
        if maxKey is None:
            if isinstance(minKey, GridBounds):
                gridBounds = minKey
                minKey = SpatialKey(gridBounds.colMin, gridBounds.rowMin)
                maxKey = SpatialKey(gridBounds.colMax, gridBounds.rowMax)
            else:
                raise Exception("wrong arguments ({0}) passed to KeyBounds constructor.".format(minKey))
        self.minKey = minKey
        self.maxKey = maxKey

    @property
    def isEmpty(self):
        return False

    def include(self, key, b):
        return KeyBounds(b.minBound(self.minKey, key), b.maxBound(self.maxKey, key))

    def includes(self, key, b):
        return self.minKey == b.minBound(self.minKey, key) and self.maxKey == b.maxBound(self.maxKey, key)
    
    def combine(self, other, b):
        if other is EmptyBounds:
            return self
        else:
            newmin = b.minBound(self.minKey, other.minKey)
            newmax = b.maxBound(self.maxKey, other.maxKey)
            return KeyBounds(newmin, newmax)

    def contains(self, other, b):
        if other is EmptyBounds:
            return True
        else:
            return self.minKey == b.minBound(self.minKey, other.minKey) and self.maxKey == b.maxBound(self.maxKey, other.maxKey)

    def intersect(self, other, b):
        if other is EmptyBounds:
            return EmptyBounds
        else:
            newmin = b.maxBound(self.minKey, other.minKey)
            newmax = b.minBound(self.maxKey, other.maxKey)

            if b.minBound(newmin, newmax) == newmin:
                return KeyBounds(newmin, newmax)
            else:
                return EmptyBounds

    @property
    def get(self):
        return self

    def setSpatialBounds(self, other, ev):
        if isinstance(other, GridBounds):
            other = KeyBounds(
                        SpatialKey(other.colMin, other.rowMin),
                        SpatialKey(other.colMax, other.rowMax))
        newmin = ev.set()(self.minKey, other.minKey)
        newmax = ev.set()(self.maxKey, other.maxKey)
        return KeyBounds(newmin, newmax)
    
    @property
    def toGridBounds(self):
        mincol = self.minKey.col
        minrow = self.minKey.row
        maxcol = self.maxKey.col
        maxrow = self.maxKey.row
        return GridBounds(mincol, minrow, maxcol, maxrow)

