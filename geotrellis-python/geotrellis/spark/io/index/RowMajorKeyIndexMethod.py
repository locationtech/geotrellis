from geotrellis.spark.io.index.KeyIndexMethod import KeyIndexMethod
from geotrellis.spark.SpatialKey import SpatialKey
from geotrellis.spark.io.index.rowmajor.RowMajorSpatialKeyIndex import RowMajorSpatialKeyIndex

class _RowMajor(object):
    @staticMethod
    def spatialKeyIndexMethod():
        class TempKeyIndexMethod(KeyIndexMethod[SpatialKey]):
            def createIndex(self, keyBounds):
                return RowMajorSpatialKeyIndex(keyBounds)
        return TempKeyIndexMethod()
