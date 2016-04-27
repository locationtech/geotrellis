from . import *
from geotrellis.spark.io.index.KeyIndex import KeyIndex
from geotrellis.spark.SpatialKey import SpatialKey
from geotrellis.spark.io.json.KeyIndexFormats import ZSpatialKeyIndexFormat
from Z2 import Z2

class ZSpatialKeyIndex(KeyIndex[SpatialKey]):
    implicits = {'format': lambda: ZSpatialKeyIndexFormat()}
    def __init__(self, key_bounds):
        self._key_bounds = key_bounds
    def key_bounds(self):
        return self._key_bounds
    def _toz(self, key):
        return Z2(key.col, key.row)
    def to_index(self, key):
        return self._toz(key).z
    def index_ranges(self, key_range):
        return Z2.zranges(self._toz(key_range[0]), self._toz(key_range[1]))

