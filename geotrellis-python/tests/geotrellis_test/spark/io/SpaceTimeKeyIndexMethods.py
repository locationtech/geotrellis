from __future__ import absolute_import
from geotrellis.spark.io.index.ZCurveKeyIndexMethod import ZCurveKeyIndexMethod

class _SpaceTimeKeyIndexMethods(object):
    @property
    def keyIndexMethods(self):
        return {
                "z order by year": ZCurveKeyIndexMethod.byYear(),
                "z order by 6 months": ZCurveKeyIndexMethod.byMonths(6)
                }
