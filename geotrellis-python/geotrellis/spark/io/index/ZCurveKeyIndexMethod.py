from geotrellis.spark.io.index.KeyIndexMethod import KeyIndexMethod
from geotrellis.spark.SpatialKey import SpatialKey
from geotrellis.spark.SpaceTimeKey import SpaceTimeKey
from geotrellis.spark.io.index.zcurve.ZSpatialKeyIndex import ZSpatialKeyIndex
from geotrellis.spark.io.index.rowmajor.RowMajorSpatialKeyIndex import RowMajorSpatialKeyIndex

class _ZCurveKeyIndexMethod(object):
    def spatialKeyIndexMethod(self):
        class TempKeyIndexMethod(KeyIndexMethod[SpatialKey]):
            def createIndex(self, keyBounds):
                return ZSpatialKeyIndex(keyBounds)
        return TempKeyIndexMethod()

    def byMilliseconds(self, millis):
        class TempKeyIndexMethod(KeyIndexMethod[SpaceTimeKey]):
            def createIndex(self, keyBounds):
                return ZSpaceTimeKeyIndex.byMilliseconds(keyBounds, millis)
        return TempKeyIndexMethod()
