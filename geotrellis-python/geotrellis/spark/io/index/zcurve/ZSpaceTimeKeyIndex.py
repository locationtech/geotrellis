
class ZSpaceTimeKeyIndex(object):
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
        return ZSpaceTimeKeyIndex.byMilliseconds(keyBounds, 1000 * 60 * 60 * 24 * 30 * 365)

    @staticmethod
    def byYears(keyBounds, years):
        return ZSpaceTimeKeyIndex.byMilliseconds(keyBounds, 1000 * 60 * 60 * 24 * 30 * 365 * years)
