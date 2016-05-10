from geotrellis.spark.io.index.KeyIndexMethod import KeyIndexMethod
from geotrellis.spark.SpatialKey import SpatialKey
from geotrellis.spark.SpaceTimeKey import SpaceTimeKey
from geotrellis.spark.io.index.zcurve.ZSpatialKeyIndex import ZSpatialKeyIndex
from geotrellis.spark.io.index.zcurve.ZSpaceTimeKeyIndex import ZSpaceTimeKeyIndex
from geotrellis.spark.io.index.rowmajor.RowMajorSpatialKeyIndex import RowMajorSpatialKeyIndex

class _ZCurveKeyIndexMethod(object):
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
    def bySecond(keyBounds):
        return ZCurveKeyIndexMethod.byMilliseconds(keyBounds, 1000)

    @staticmethod
    def bySeconds(keyBounds, seconds):
        return ZCurveKeyIndexMethod.byMilliseconds(keyBounds, 1000 * seconds)

    @staticmethod
    def byMinute(keyBounds):
        return ZCurveKeyIndexMethod.byMilliseconds(keyBounds, 1000 * 60)

    @staticmethod
    def byMinutes(keyBounds, minutes):
        return ZCurveKeyIndexMethod.byMilliseconds(keyBounds, 1000 * 60 * minutes)

    @staticmethod
    def byHour(keyBounds):
        return ZCurveKeyIndexMethod.byMilliseconds(keyBounds, 1000 * 60 * 60)

    @staticmethod
    def byHours(keyBounds, hours):
        return ZCurveKeyIndexMethod.byMilliseconds(keyBounds, 1000 * 60 * 60 * hours)

    @staticmethod
    def byDay(keyBounds):
        return ZCurveKeyIndexMethod.byMilliseconds(keyBounds, 1000 * 60 * 60 * 24)

    @staticmethod
    def byDays(keyBounds, days):
        return ZCurveKeyIndexMethod.byMilliseconds(keyBounds, 1000 * 60 * 60 * 24 * days)

    @staticmethod
    def byMonth(keyBounds):
        return ZCurveKeyIndexMethod.byMilliseconds(keyBounds, 1000 * 60 * 60 * 24 * 30)

    @staticmethod
    def byMonths(keyBounds, months):
        return ZCurveKeyIndexMethod.byMilliseconds(keyBounds, 1000 * 60 * 60 * 24 * 30 * months)

    @staticmethod
    def byYear(keyBounds):
        return ZCurveKeyIndexMethod.byMilliseconds(keyBounds, 1000 * 60 * 60 * 24 * 365)

    @staticmethod
    def byYears(keyBounds, years):
        return ZCurveKeyIndexMethod.byMilliseconds(keyBounds, 1000 * 60 * 60 * 24 * 365 * years)
