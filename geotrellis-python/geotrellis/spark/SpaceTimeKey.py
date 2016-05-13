from geotrellis.spark.SpatialKey import SpatialKey
from geotrellis.spark.TemporalKey import TemporalKey
from geotrellis.spark.io.json.KeyFormats import SpaceTimeKeyFormat
from geotrellis.spark.Boundable import Boundable

import datetime
import pytz
import functools

def generateCodec():
    from geotrellis.spark.io.avro.codecs.KeyCodecs import SpaceTimeKeyAvroCodec
    return SpaceTimeKeyAvroCodec()

@functools.total_ordering
class SpaceTimeKey(object):
    implicits = {
            'format': lambda: SpaceTimeKeyFormat(),
            'AvroRecordCodec': generateCodec}

    def __init__(self, col, row, instant):
        self.col = col
        self.row = row
        self.instant = instant

    @property
    def spatialKey(self):
        return SpatialKey(self.col, self.row)

    @property
    def temporalKey(self):
        return TemporalKey.applyStatic(self.time)

    @property
    def time(self):
        seconds = float(self.instant) / 1000
        return datetime.datetime.fromtimestamp(seconds, pytz.utc)

    def __lt__(self, other):
        return (self.spatialKey, self.temporalKey) < (other.spatialKey, other.temporalKey)

    def __eq__(self, other):
        if not isinstance(other, SpaceTimeKey):
            return False
        return (self.col == other.col and
                self.row == other.row and
                self.instant == other.instant)

    def __hash__(self):
        return hash((self.col, self.row, self.instant))

    def getComponent(self, component_type):
        if component_type is SpatialKey:
            return self.spatialKey
        elif component_type is TemporalKey:
            return self.temporalKey
        else:
            raise Exception("wrong component_type: {ct}".format(ct=component_type))

    def setComponent(self, value):
        if isinstance(value, SpatialKey):
            return SpaceTimeKey(value.col, value.row, self.time)
        elif isinstance(value, TemporalKey):
            return SpaceTimeKey(self.col, self.row, value.instant)
        else:
            raise Exception("wrong component_type: {ct}".format(ct=component_type))

    @staticmethod
    def applyStatic(first, second, third = None):
        if third is None:
            spatialKey = first
            temporalKey = second
            col, row, time = (spatialKey.col, spatialKey.row, temporalKey.time)
        else:
            col, row, time = first, second, third
        epoch = datetime.datetime.fromtimestamp(0, pytz.utc)
        millis = (time - epoch).total_seconds() * 1000
        return SpaceTimeKey(col, row, time)

    boundable = Boundable(
            lambda a, b: SpaceTimeKey(min(a.col, b.col), min(a.row, b.row), a.time if a.time < b.time else b.time),
            lambda a, b: SpaceTimeKey(max(a.col, b.col), max(a.row, b.row), a.time if a.time > b.time else b.time))
