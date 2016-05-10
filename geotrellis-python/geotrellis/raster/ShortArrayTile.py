from geotrellis.raster.UserDefinedNoDataConversions import UserDefinedShortNoDataConversions
from geotrellis.raster.MutableArrayTile import MutableArrayTile
from geotrellis.raster.CellType import ShortCellType, ShortConstantNoDataCellType
from geotrellis.raster.package_scala import intToShort, s2i, s2d, i2s, d2s, shortNODATA
# from geotrellis.spark.io.avro.codecs.TileCodecs import ShortArrayTileCodec

import array
import struct

class ShortArrayTile(MutableArrayTile):
    def __init__(self, arr, cols, rows):
        if isinstance(arr, list):
            self.array = array.array('h', arr)
        else:
            self.array = arr
        self._cols = cols
        self._rows = rows

    @property
    def cols(self):
        return self._cols

    @property
    def rows(self):
        return self._rows

    @property
    def cellType(self):
        return self._cellType

    def toBytes(self):
        length = len(self.array)
        typecode = "{len}h".format(len = length)
        arr = array.array('b')
        arr.fromstring(struct.pack(typecode, *self.array.tolist()))
        return arr

    def copy(self):
        arr = array.array('h', self.array.tolist())
        return ArrayTile.applyStatic(arr, self.cols, self.rows)

    @staticmethod
    def applyStatic(arr, cols, rows, cellType = ShortConstantNoDataCellType):
        if cellType is ShortCellType:
            return ShortRawArrayTile(arr, cols, rows)
        elif cellType is ShortConstantNoDataCellType:
            return ShortConstantNoDataArrayTile(arr, cols, rows)
        elif isinstance(cellType, ShortUserDefinedNoDataCellType):
            return ShortUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

    @staticmethod
    def ofDim(cols, rows, cellType = ShortConstantNoDataCellType):
        arr = array.array('h', [0] * (cols*rows))
        if cellType is ShortCellType:
            return ShortRawArrayTile(arr, cols, rows)
        elif cellType is ShortConstantNoDataCellType:
            return ShortConstantNoDataArrayTile(arr, cols, rows)
        elif isinstance(cellType, ShortUserDefinedNoDataCellType):
            return ShortUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

    @staticmethod
    def empty(cols, rows, cellType = ShortConstantNoDataCellType):
        if cellType is ShortCellType:
            return ShortArrayTile.ofDim(cols, rows, cellType)
        elif cellType is ShortConstantNoDataCellType:
            return ShortArrayTile.fill(shortNODATA, cols, rows, cellType)
        elif isinstance(cellType, ShortUserDefinedNoDataCellType):
            nd = cellType.noDataValue
            return ShortArrayTile.fill(nd, cols, rows, cellType)

    @staticmethod
    def fill(v, cols, rows, cellType = ShortConstantNoDataCellType):
        arr = array.array('h', [v] * (cols*rows))
        if cellType is ShortCellType:
            return ShortRawArrayTile(arr, cols, rows)
        elif cellType is ShortConstantNoDataCellType:
            return ShortConstantNoDataArrayTile(arr, cols, rows)
        elif isinstance(cellType, ShortUserDefinedNoDataCellType):
            return ShortUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

    @staticmethod
    def fromBytes(bytesarray, cols, rows, cellType = ShortConstantNoDataCellType):
        arr = array.array('h')
        length = len(bytesarray)
        typecode = "{len}b".format(len = length)
        arr.fromstring(struct.pack(typecode, *bytesarray.tolist()))

        if cellType is ShortCellType:
            return ShortRawArrayTile(arr, cols, rows)
        elif cellType is ShortConstantNoDataCellType:
            return ShortConstantNoDataArrayTile(arr, cols, rows)
        elif isinstance(cellType, ShortUserDefinedNoDataCellType):
            return ShortUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

def generateCodec():
    from geotrellis.spark.io.avro.codecs.TileCodecs import ShortArrayTileCodec
    return ShortArrayTileCodec()

def ShortRawArrayTile(ShortArrayTile):
    implicits = {"AvroRecordCodec": generateCodec}

    def __init__(self, arr, cols, rows):
        ShortArrayTile.__init__(self, arr, cols, rows)
        self._cellType = ShortCellType

    def __eq__(self, other):
        if not isinstance(other, ShortRawArrayTile):
            return False
        return (self.array == other.array and
                self.cols == other.cols and
                self.rows == other.rows)
    
    def __hash__(self):
        return hash((tuple(self.array), self.cols, self.rows))

    def apply(self, i):
        return self.array[i]

    def applyDouble(self, i):
        return float(self.array[i])

    def _update(self, i, z):
        self.array[i] = intToShort(z)

    def updateDouble(self, i, z):
        self.array[i] = intToShort(int(z))

def ShortConstantNoDataArrayTile(ShortArrayTile):
    implicits = {"AvroRecordCodec": generateCodec}

    def __init__(self, arr, cols, rows):
        ShortArrayTile.__init__(self, arr, cols, rows)
        self._cellType = ShortConstantNoDataCellType

    def __eq__(self, other):
        if not isinstance(other, ShortConstantNoDataArrayTile):
            return False
        return (self.array == other.array and
                self.cols == other.cols and
                self.rows == other.rows)
    
    def __hash__(self):
        return hash((tuple(self.array), self.cols, self.rows))

    def apply(self, i):
        return s2i(self.array[i])

    def applyDouble(self, i):
        return s2d(self.array[i])

    def _update(self, i, z):
        self.array[i] = i2s(z)

    def updateDouble(self, i, z):
        self.array[i] = d2s(z)

class ShortUserDefinedNoDataArrayTile(UserDefinedShortNoDataConversions, ShortArrayTile):
    implicits = {"AvroRecordCodec": generateCodec}

    def __init__(self, arr, cols, rows, cellType):
        ShortArrayTile.__init__(self, arr, cols, rows)
        self._cellType = cellType

    def __eq__(self, other):
        if not isinstance(other, ShortUserDefinedNoDataArrayTile):
            return False
        return (self.array == other.array and
                self.cols == other.cols and
                self.rows == other.rows and
                self.cellType == other.cellType)
    
    def __hash__(self):
        return hash((tuple(self.array), self.cols, self.rows, self.cellType))

    def apply(self, i):
        return self.uds2i(self.array[i])

    def applyDouble(self, i):
        return self.uds2d(self.array[i])

    def _update(self, i, z):
        self.array[i] = self.i2uds(z)

    def updateDouble(self, i, z):
        self.array[i] = self.d2uds(z)
