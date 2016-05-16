from __future__ import absolute_import
from geotrellis.raster.UserDefinedNoDataConversions import UserDefinedDoubleNoDataConversions
from geotrellis.raster.MutableArrayTile import MutableArrayTile
from geotrellis.raster.CellType import DoubleCellType, DoubleConstantNoDataCellType, DoubleUserDefinedNoDataCellType 
from geotrellis.raster.package_scala import f2i, i2f, f2d, d2f, doubleNODATA

import array
import struct

class DoubleArrayTile(MutableArrayTile):
    def __init__(self, arr, cols, rows):
        if isinstance(arr, list):
            self.array = array.array('d', arr)
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

    def toArrayDouble(self):
        return self.array.tolist()

    def toBytes(self):
        length = len(self.array)
        typecode = "{len}d".format(len = length)
        arr = array.array('b')
        arr.fromstring(struct.pack(typecode, *self.array.tolist()))
        return arr

    def copy(self):
        arr = array.array('d', self.array.tolist())
        return ArrayTile.applyStatic(arr, self.cols, self.rows)

    @staticmethod
    def applyStatic(arr, cols, rows, cellType = DoubleConstantNoDataCellType):
        if cellType is DoubleCellType:
            return DoubleRawArrayTile(arr, cols, rows)
        elif cellType is DoubleConstantNoDataCellType:
            return DoubleConstantNoDataArrayTile(arr, cols, rows)
        elif isinstance(cellType, DoubleUserDefinedNoDataCellType):
            return DoubleUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

    @staticmethod
    def ofDim(cols, rows, cellType = DoubleConstantNoDataCellType):
        arr = array.array('d', [0.0] * (cols*rows))
        if cellType is DoubleCellType:
            return DoubleRawArrayTile(arr, cols, rows)
        elif cellType is DoubleConstantNoDataCellType:
            return DoubleConstantNoDataArrayTile(arr, cols, rows)
        elif isinstance(cellType, DoubleUserDefinedNoDataCellType):
            return DoubleUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

    @staticmethod
    def empty(cols, rows, cellType = DoubleConstantNoDataCellType):
        if cellType is DoubleCellType:
            return DoubleArrayTile.ofDim(cols, rows, cellType)
        elif cellType is DoubleConstantNoDataCellType:
            return DoubleArrayTile.fill(doubleNODATA, cols, rows, cellType)
        elif isinstance(cellType, DoubleUserDefinedNoDataCellType):
            nd = cellType.noDataValue
            return DoubleArrayTile.fill(nd, cols, rows, cellType)

    @staticmethod
    def fill(v, cols, rows, cellType = DoubleConstantNoDataCellType):
        arr = array.array('d', [v] * (cols*rows))
        if cellType is DoubleCellType:
            return DoubleRawArrayTile(arr, cols, rows)
        elif cellType is DoubleConstantNoDataCellType:
            return DoubleConstantNoDataArrayTile(arr, cols, rows)
        elif isinstance(cellType, DoubleUserDefinedNoDataCellType):
            return DoubleUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

    @staticmethod
    def fromBytes(bytesarray, cols, rows, cellType = DoubleConstantNoDataCellType):
        arr = array.array('d')
        length = len(bytesarray)
        typecode = "{len}b".format(len = length)
        arr.fromstring(struct.pack(typecode, *bytesarray.tolist()))

        if cellType is DoubleCellType:
            return DoubleRawArrayTile(arr, cols, rows)
        elif cellType is DoubleConstantNoDataCellType:
            return DoubleConstantNoDataArrayTile(arr, cols, rows)
        elif isinstance(cellType, DoubleUserDefinedNoDataCellType):
            return DoubleUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

def generateCodec():
    from geotrellis.spark.io.avro.codecs.TileCodecs import DoubleArrayTileCodec
    return DoubleArrayTileCodec()

class DoubleRawArrayTile(DoubleArrayTile):
    implicits = {"AvroRecordCodec": generateCodec}

    def __init__(self, arr, cols, rows):
        DoubleArrayTile.__init__(self, arr, cols, rows)
        self._cellType = DoubleCellType

    def __eq__(self, other):
        if not isinstance(other, DoubleRawArrayTile):
            return False
        return (self.array == other.array and
                self.cols == other.cols and
                self.rows == other.rows)
    
    def __hash__(self):
        return hash((tuple(self.array), self.cols, self.rows))

    def apply(self, i):
        return int(self.array[i])

    def applyDouble(self, i):
        return self.array[i]

    def _update(self, i, z):
        self.array[i] = float(z)

    def updateDouble(self, i, z):
        self.array[i] = z

class DoubleConstantNoDataArrayTile(DoubleArrayTile):
    implicits = {"AvroRecordCodec": generateCodec}

    def __init__(self, arr, cols, rows):
        DoubleArrayTile.__init__(self, arr, cols, rows)
        self._cellType = DoubleConstantNoDataCellType

    def __eq__(self, other):
        if not isinstance(other, DoubleConstantNoDataArrayTile):
            return False
        return (self.array == other.array and
                self.cols == other.cols and
                self.rows == other.rows)
    
    def __hash__(self):
        return hash((tuple(self.array), self.cols, self.rows))

    def apply(self, i):
        return d2i(self.array[i])

    def applyDouble(self, i):
        return self.array[i]

    def _update(self, i, z):
        self.array[i] = i2d(z)

    def updateDouble(self, i, z):
        self.array[i] = z

class DoubleUserDefinedNoDataArrayTile(UserDefinedDoubleNoDataConversions, DoubleArrayTile):
    implicits = {"AvroRecordCodec": generateCodec}

    def __init__(self, arr, cols, rows, cellType):
        DoubleArrayTile.__init__(self, arr, cols, rows)
        self._cellType = cellType

    def __eq__(self, other):
        if not isinstance(other, DoubleUserDefinedNoDataArrayTile):
            return False
        return (self.array == other.array and
                self.cols == other.cols and
                self.rows == other.rows and
                self.cellType == other.cellType)
    
    def __hash__(self):
        return hash((tuple(self.array), self.cols, self.rows, self.cellType))

    def apply(self, i):
        return self.udd2i(self.array[i])

    def applyDouble(self, i):
        return self.udd2d(self.array[i])

    def _update(self, i, z):
        self.array[i] = self.i2udd(z)

    def updateDouble(self, i, z):
        self.array[i] = self.d2udd(z)
