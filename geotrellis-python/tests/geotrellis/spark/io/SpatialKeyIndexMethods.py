from geotrellis.spark.io.index.RowMajorKeyIndexMethod import RowMajorKeyIndexMethod
from geotrellis.spark.io.index.ZCurveKeyIndexMethod import ZCurveKeyIndexMethod
from geotrellis.spark.io.index.HilbertKeyIndexMethod import HilbertKeyIndexMethod

class _SpatialKeyIndexMethods(object):
    @property
    def keyIndexMethods(self):
        return {
                "row major": RowMajorKeyIndexMethod,
                "z order": ZCurveKeyIndexMethod,
                "hilbert": HilbertKeyIndexMethod}
