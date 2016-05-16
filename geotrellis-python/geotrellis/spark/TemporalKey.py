from __future__ import absolute_import
import datetime
import pytz
import functools

@functools.total_ordering
class TemporalKey(object):
    def __init__(self, instant):
        self.instant = instant

    @property
    def time(self):
        seconds = float(self.instant) / 1000
        return datetime.datetime.fromtimestamp(seconds, pytz.utc)

    def __lt__(self, other):
        return self.instant < other.instant

    def __eq__(self, other):
        if not isinstance(other, TemporalKey):
            return False
        return self.instant == other.instant

    def __hash__(self):
        return hash(self.instant)

    @staticmethod
    def dateTimeToKey(time):
        return TemporalKey.applyStatic(time)

    @staticmethod
    def applyStatic(time):
        epoch = datetime.datetime.fromtimestamp(0, pytz.utc)
        millis = (time - epoch).total_seconds() * 1000
        return TemporalKey(millis)
