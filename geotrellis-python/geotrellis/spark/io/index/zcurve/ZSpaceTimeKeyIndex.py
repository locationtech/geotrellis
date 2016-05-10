from geotrellis.spark.io.index.KeyIndex import KeyIndex
from geotrellis.spark.io.index.zcurve.Z3 import Z3
from geotrellis.spark.SpaceTimeKey import SpaceTimeKey

class ZSpaceTimeKeyIndex(KeyIndex[SpaceTimeKey]):
    def __init__(self, keyBounds, temporalResolution):
        self.keyBounds = keyBounds
        self.temporalResolution = temporalResolution

    def _toz(self, key):
        return Z3(key.col, key.row, int(key.instant / self.temporalResolution))

    def toIndex(self, key):
        return self._toz(key).z

    def indexRanges(self, keyRange):
        return Z3.zranges(self._toz(keyRange[0]), self._toz(keyRange[1]))

    @staticmethod
    def byMilliseconds(keyBounds, millis):
        return ZSpaceTimeKeyIndex(keyBounds, millis)

    @staticmethod
    def bySecond(keyBounds):
        return ZSpaceTimeKeyIndex.byMilliseconds(keyBounds, 1000)

    @staticmethod
    def bySeconds(keyBounds, seconds):
        return ZSpaceTimeKeyIndex.byMilliseconds(keyBounds, 1000 * seconds)

    @staticmethod
    def byMinute(keyBounds):
        return ZSpaceTimeKeyIndex.byMilliseconds(keyBounds, 1000 * 60)

    @staticmethod
    def byMinutes(keyBounds, minutes):
        return ZSpaceTimeKeyIndex.byMilliseconds(keyBounds, 1000 * 60 * minutes)

    @staticmethod
    def byHour(keyBounds):
        return ZSpaceTimeKeyIndex.byMilliseconds(keyBounds, 1000 * 60 * 60)

    @staticmethod
    def byHours(keyBounds, hours):
        return ZSpaceTimeKeyIndex.byMilliseconds(keyBounds, 1000 * 60 * 60 * hours)

    @staticmethod
    def byDay(keyBounds):
        return ZSpaceTimeKeyIndex.byMilliseconds(keyBounds, 1000 * 60 * 60 * 24)

    @staticmethod
    def byDays(keyBounds, days):
        return ZSpaceTimeKeyIndex.byMilliseconds(keyBounds, 1000 * 60 * 60 * 24 * days)

    @staticmethod
    def byMonth(keyBounds):
        return ZSpaceTimeKeyIndex.byMilliseconds(keyBounds, 1000 * 60 * 60 * 24 * 30)

    @staticmethod
    def byMonths(keyBounds, months):
        return ZSpaceTimeKeyIndex.byMilliseconds(keyBounds, 1000 * 60 * 60 * 24 * 30 * months)

    @staticmethod
    def byYear(keyBounds):
        return ZSpaceTimeKeyIndex.byMilliseconds(keyBounds, 1000 * 60 * 60 * 24 * 365)

    @staticmethod
    def byYears(keyBounds, years):
        return ZSpaceTimeKeyIndex.byMilliseconds(keyBounds, 1000 * 60 * 60 * 24 * 365 * years)
