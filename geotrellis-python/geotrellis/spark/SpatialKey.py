from geotrellis.spark.io.json.KeyFormats import SpatialKeyFormat
from geotrellis.spark.Boundable import Boundable
import functools

def generateCodec():
    from geotrellis.spark.io.avro.codecs.KeyCodecs import SpatialKeyAvroCodec
    return SpatialKeyAvroCodec()

@functools.total_ordering
class SpatialKey(object):
    implicits = {
            'format': lambda: SpatialKeyFormat(),
            'AvroRecordCodec': generateCodec}

    def __init__(self, col, row):
        self.col = int(col)
        self.row = int(row)
    def __getitem__(self, index):
        if index == 0:
            return self.col
        elif index == 1:
            return self.row
        else:
            raise Exception("Index {0} is out of [0,1] bounds.".format(index))
    def __lt__(self, other):
        return SpatialKey.toTuple(self) < SpatialKey.toTuple(other)

    def __eq__(self, other):
        if not isinstance(other, SpatialKey):
            return False
        return (self.col == other.col and
                self.row == other.row)
    def __hash__(self):
        return hash((col, row))

    @staticmethod
    def toTuple(key):
        return (key.col, key.row)

    @staticmethod
    def fromTuple(tup):
        col, row = tup
        return SpatialKey(col, row)

    boundable = Boundable(
            lambda a, b: SpatialKey(min(a.col, b.col), min(a.row, b.row)),
            lambda a, b: SpatialKey(max(a.col, b.col), max(a.row, b.row)))

