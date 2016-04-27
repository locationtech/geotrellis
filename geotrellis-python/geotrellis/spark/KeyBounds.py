from geotrellis.spark.io.json.KeyFormats import KeyBoundsFormat
# from geotrellis.spark.SpatialKey import SpatialKey # we have to postpone the import to avoid errors caused by circular imports
from geotrellis.raster.GridBounds import GridBounds

class Bounds(object):
    def is_empty(self):
        pass

    def non_empty(self):
        return not self.is_empty()

    def intersect(self, other, b):
        pass

    def intersects(self, other, b):
        return self.intersect(other, b).non_empty()

    def get(self):
        pass

    def get_or_else(self, default_func):
        if self.is_empty():
            return default_func()
        else:
            return self.get()

    def map(self, func):
        if self.is_empty():
            return EmptyBounds
        else:
            return func(self.get())

    def flat_map(self, func):
        if self.is_empty():
            return EmptyBounds
        else:
            return func(self.get())

    @staticmethod
    def from_rdd(rdd):
        return rdd.map(lambda k, tile: bounds(k, k)).fold(EmptyBounds, lambda a, b: a.combine(b))

    @staticmethod
    def to_iterable(b):
        if b is EmptyBounds:
            return []
        else:
            return [b]

def bounds(_min, _max):
    return KeyBounds(_min, _max)

class TempEmptyBounds(Bounds):
    def is_empty(self):
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

    def get_spatial_bounds(self, other, ev = None):
        return self

EmptyBounds = TempEmptyBounds()

class _KeyBoundsMeta(object):
    items = {}
    def __getitem__(self, key_type):
        if key_type in self.items.keys():
            return self.items[key_type]
        class tempo(_KeyBounds):
            implicits = {'format': lambda: KeyBoundsFormat(key_type.implicits['format']())}
        self.items[key_type] = tempo
        return tempo
    # TODO code duplication (see _KeyBounds.__init__)
    def __call__(self, min_key, max_key = None):
        from geotrellis.spark.SpatialKey import SpatialKey
        if max_key is None:
            if isinstance(min_key, GridBounds):
                grid_bounds = min_key
                min_key = SpatialKey(grid_bounds.col_min, grid_bounds.row_min)
                max_key = SpatialKey(grid_bounds.col_max, grid_bounds.row_max)
            else:
                raise Exception("wrong arguments ({0}) passed to KeyBounds constructor.".format(min_key))
        key_type = type(min_key)
        return self[key_type](min_key, max_key)

KeyBounds = _KeyBoundsMeta()

class _KeyBounds(Bounds):
    def __init__(self, min_key, max_key = None):
        from geotrellis.spark.SpatialKey import SpatialKey
        if max_key is None:
            if isinstance(min_key, GridBounds):
                grid_bounds = min_key
                min_key = SpatialKey(grid_bounds.col_min, grid_bounds.row_min)
                max_key = SpatialKey(grid_bounds.col_max, grid_bounds.row_max)
            else:
                raise Exception("wrong arguments ({0}) passed to KeyBounds constructor.".format(min_key))
        self.min_key = min_key
        self.max_key = max_key

    def is_empty(self):
        return False

    def include(self, key, b):
        return KeyBounds(b.min_bound(self.min_key, key), b.max_bound(self.max_key, key))

    def includes(self, key, b):
        return self.min_key == b.min_bound(self.min_key, key) and self.max_key == b.max_bound(self.max_key, key)
    
    def combine(self, other, b):
        if other is EmptyBounds:
            return self
        else:
            new_min = b.min_bound(self.min_key, other.min_key)
            new_max = b.max_bound(self.max_key, other.max_key)
            return KeyBounds(new_min, new_max)

    def contains(self, other, b):
        if other is EmptyBounds:
            return True
        else:
            return self.min_key == b.min_bound(self.min_key, other.min_key) and self.max_key == b.max_bound(self.max_key, other.max_key)

    def intersect(self, other, b):
        if other is EmptyBounds:
            return EmptyBounds
        else:
            new_min = b.max_bound(self.min_key, other.min_key)
            new_max = b.min_bound(self.max_key, other.max_key)

            if b.min_bound(new_min, new_max) == new_min:
                return KeyBounds(new_min, new_max)
            else:
                return EmptyBounds

    def get(self):
        return self

    def set_spatial_bounds(self, other, ev):
        if isinstance(other, GridBounds):
            other = KeyBounds(
                        SpatialKey(other.col_min, other.row_min),
                        SpatialKey(other.col_max, other.row_max))
        new_min = ev.set()(self.min_key, other.min_key)
        new_max = ev.set()(self.max_key, other.max_key)
        return KeyBounds(new_min, new_max)
    
    @staticmethod
    def include_key(seq, key):
        mapped = map(lambda kb: kb.includes(key), seq)
        return reduce(lambda a, b: a or b, mapped)

    @staticmethod
    def to_tuple(key_bounds):
        return (key_bounds.min_key, key_bounds.max_key)

    def to_grid_bounds(self):
        min_col = self.min_key.col
        min_row = self.min_key.row
        max_col = self.max_key.col
        max_row = self.max_key.row
        return GridBounds(min_col, min_row, max_col, max_row)

