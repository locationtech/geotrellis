from __future__ import absolute_import
from geotrellis.spark.io.index.RowMajorKeyIndexMethod import RowMajorKeyIndexMethod
from geotrellis.spark.io.index.ZCurveKeyIndexMethod import ZCurveKeyIndexMethod

class _SpatialKeyIndexMethods(object):
    @property
    def keyIndexMethods(self):
        return {
                "row major": RowMajorKeyIndexMethod.spatialKeyIndexMethod(),
                "z order": ZCurveKeyIndexMethod.spatialKeyIndexMethod()
                }
