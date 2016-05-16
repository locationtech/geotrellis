from __future__ import absolute_import
from __future__ import absolute_import
from geotrellis.spark.io.index.KeyIndexMethod import KeyIndexMethod
from geotrellis.spark.SpatialKey import SpatialKey
from geotrellis.spark.SpaceTimeKey import SpaceTimeKey
from geotrellis.spark.io.index.zcurve.ZSpatialKeyIndex import ZSpatialKeyIndex
from geotrellis.spark.io.index.zcurve.ZSpaceTimeKeyIndex import ZSpaceTimeKeyIndex
from geotrellis.spark.io.index.rowmajor.RowMajorSpatialKeyIndex import RowMajorSpatialKeyIndex

class ZCurveKeyIndexMethod(object):
    @staticmethod
    def spatialKeyIndexMethod():
        class TempKeyIndexMethod(KeyIndexMethod[SpatialKey]):
            def createIndex(self, keyBounds):
                return ZSpatialKeyIndex(keyBounds)
        return TempKeyIndexMethod()

    @staticmethod
    def byMilliseconds(millis):
        class TempKeyIndexMethod(KeyIndexMethod[SpaceTimeKey]):
            def createIndex(self, keyBounds):
                return ZSpaceTimeKeyIndex.byMilliseconds(keyBounds, millis)
        return TempKeyIndexMethod()

    @staticmethod
    def bySecond():
        return ZCurveKeyIndexMethod.byMilliseconds(1000)

    @staticmethod
    def bySeconds(seconds):
        return ZCurveKeyIndexMethod.byMilliseconds(1000 * seconds)

    @staticmethod
    def byMinute():
        return ZCurveKeyIndexMethod.byMilliseconds(1000 * 60)

    @staticmethod
    def byMinutes(minutes):
        return ZCurveKeyIndexMethod.byMilliseconds(1000 * 60 * minutes)

    @staticmethod
    def byHour():
        return ZCurveKeyIndexMethod.byMilliseconds(1000 * 60 * 60)

    @staticmethod
    def byHours(hours):
        return ZCurveKeyIndexMethod.byMilliseconds(1000 * 60 * 60 * hours)

    @staticmethod
    def byDay():
        return ZCurveKeyIndexMethod.byMilliseconds(1000 * 60 * 60 * 24)

    @staticmethod
    def byDays(days):
        return ZCurveKeyIndexMethod.byMilliseconds(1000 * 60 * 60 * 24 * days)

    @staticmethod
    def byMonth():
        return ZCurveKeyIndexMethod.byMilliseconds(1000 * 60 * 60 * 24 * 30)

    @staticmethod
    def byMonths(months):
        return ZCurveKeyIndexMethod.byMilliseconds(1000 * 60 * 60 * 24 * 30 * months)

    @staticmethod
    def byYear():
        return ZCurveKeyIndexMethod.byMilliseconds(1000 * 60 * 60 * 24 * 365)

    @staticmethod
    def byYears(years):
        return ZCurveKeyIndexMethod.byMilliseconds(1000 * 60 * 60 * 24 * 365 * years)
