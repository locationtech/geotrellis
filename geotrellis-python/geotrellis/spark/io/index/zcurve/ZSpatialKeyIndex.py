from geotrellis.spark.io.index.KeyIndex import KeyIndex
from geotrellis.spark.SpatialKey import SpatialKey
from geotrellis.spark.io.json.KeyIndexFormats import ZSpatialKeyIndexFormat
from Z2 import Z2

class ZSpatialKeyIndex(KeyIndex[SpatialKey]):
    implicits = {'format': lambda: ZSpatialKeyIndexFormat()}
    def __init__(self, keyBounds):
        self._keyBounds = keyBounds
    @property
    def keyBounds(self):
        return self._keyBounds
    def _toz(self, key):
        return Z2(key.col, key.row)
    def toIndex(self, key):
        return self._toz(key).z
    def indexRanges(self, keyRange):
        return Z2.zranges(self._toz(keyRange[0]), self._toz(keyRange[1]))

