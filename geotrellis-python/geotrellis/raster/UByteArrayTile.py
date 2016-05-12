from geotrellis.raster.UserDefinedNoDataConversions import UserDefinedByteNoDataConversions
from geotrellis.raster.MutableArrayTile import MutableArrayTile
from geotrellis.raster.CellType import UByteCellType, UByteConstantNoDataCellType, UByteUserDefinedNoDataCellType
from geotrellis.raster.package_scala import intToByte, ub2i, ub2d, i2ub, d2ub, ubyteNODATA

import array
import struct

class UByteArrayTile(MutableArrayTile):
    def __init__(self, arr, cols, rows):
        if isinstance(arr, list):
            self.array = array.array('B', arr)
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
        arr = array.array('b')
        length = len(self.array)
        typecode = "{len}B".format(len = length)
        arr.fromstring(struct.pack(typecode, *self.array.tolist()))
        return arr

    def copy(self):
        arr = array.array('B', self.array.tolist())
        return UByteArrayTile.applyStatic(arr, self.cols, self.rows, self.cellType)

    @staticmethod
    def applyStatic(arr, cols, rows, cellType = UByteConstantNoDataCellType):
        if cellType is UByteCellType:
            return UByteRawArrayTile(arr, cols, rows)
        elif cellType is UByteConstantNoDataCellType:
            return UByteConstantNoDataArrayTile(arr, cols, rows)
        elif isinstance(cellType, UByteUserDefinedNoDataCellType):
            return UByteUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

    @staticmethod
    def ofDim(cols, rows, cellType = UByteConstantNoDataCellType):
        arr = array.array('B', [0] * (cols*rows))
        if cellType is UByteCellType:
            return UByteRawArrayTile(arr, cols, rows)
        elif cellType is UByteConstantNoDataCellType:
            return UByteConstantNoDataArrayTile(arr, cols, rows)
        elif isinstance(cellType, UByteUserDefinedNoDataCellType):
            return UByteUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

    @staticmethod
    def empty(cols, rows, cellType = UByteConstantNoDataCellType):
        if cellType is UByteCellType:
            return UByteArrayTile.ofDim(cols, rows, cellType)
        elif cellType is UByteConstantNoDataCellType:
            return UByteArrayTile.fill(ubyteNODATA, cols, rows, cellType)
        elif isinstance(cellType, UByteUserDefinedNoDataCellType):
            nd = cellType.noDataValue
            return UByteArrayTile.fill(nd, cols, rows, cellType)

    @staticmethod
    def fill(v, cols, rows, cellType = UByteConstantNoDataCellType):
        arr = array.array('B', [v] * (cols*rows))
        if cellType is UByteCellType:
            return UByteRawArrayTile(arr, cols, rows)
        elif cellType is UByteConstantNoDataCellType:
            return UByteConstantNoDataArrayTile(arr, cols, rows)
        elif isinstance(cellType, UByteUserDefinedNoDataCellType):
            return UByteUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

    @staticmethod
    def fromBytes(bytesarray, cols, rows, cellType = UByteConstantNoDataCellType):
        arr = array.array('B')
        length = len(bytesarray)
        typecode = "{len}b".format(len = length)
        arr.fromstring(struct.pack(typecode, *bytesarray.tolist()))

        if cellType is UByteCellType:
            return UByteRawArrayTile(arr, cols, rows)
        elif cellType is UByteConstantNoDataCellType:
            return UByteConstantNoDataArrayTile(arr, cols, rows)
        elif isinstance(cellType, UByteUserDefinedNoDataCellType):
            return UByteUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

def generateCodec():
    from geotrellis.spark.io.avro.codecs.TileCodecs import UByteArrayTileCodec
    return UByteArrayTileCodec()

class UByteRawArrayTile(UByteArrayTile):
    implicits = {"AvroRecordCodec": generateCodec}

    def __init__(self, arr, cols, rows):
        UByteArrayTile.__init__(self, arr, cols, rows)
        self._cellType = UByteCellType

    def __eq__(self, other):
        if not isinstance(other, UByteRawArrayTile):
            return False
        return (self.array == other.array and
                self.cols == other.cols and
                self.rows == other.rows)
    
    def __hash__(self):
        return hash((tuple(self.array), self.cols, self.rows))

    def apply(self, i):
        return self.array[i] & 0xFF

    def applyDouble(self, i):
        return float(self.array[i] & 0xFF)

    def _update(self, i, z):
        self.array[i] = intToByte(z)

    def updateDouble(self, i, z):
        self.array[i] = intToByte(int(z))

class UByteConstantNoDataArrayTile(UByteArrayTile):
    implicits = {"AvroRecordCodec": generateCodec}

    def __init__(self, arr, cols, rows):
        UByteArrayTile.__init__(self, arr, cols, rows)
        self._cellType = UByteConstantNoDataCellType

    def __eq__(self, other):
        if not isinstance(other, UByteConstantNoDataArrayTile):
            return False
        return (self.array == other.array and
                self.cols == other.cols and
                self.rows == other.rows)
    
    def __hash__(self):
        return hash((tuple(self.array), self.cols, self.rows))

    def apply(self, i):
        return ub2i(self.array[i])

    def applyDouble(self, i):
        return ub2d(self.array[i])

    def _update(self, i, z):
        self.array[i] = i2ub(z)

    def updateDouble(self, i, z):
        self.array[i] = d2ub(z)

class UByteUserDefinedNoDataArrayTile(UserDefinedByteNoDataConversions, UByteArrayTile):
    implicits = {"AvroRecordCodec": generateCodec}

    def __init__(self, arr, cols, rows, cellType):
        UByteArrayTile.__init__(self, arr, cols, rows)
        self._cellType = cellType

    def __eq__(self, other):
        if not isinstance(other, UByteUserDefinedNoDataArrayTile):
            return False
        return (self.array == other.array and
                self.cols == other.cols and
                self.rows == other.rows and
                self.cellType == other.cellType)
    
    def __hash__(self):
        return hash((tuple(self.array), self.cols, self.rows, self.cellType))

    def apply(self, i):
        return self.udub2i(self.array[i])

    def applyDouble(self, i):
        return self.udub2d(self.array[i])

    def _update(self, i, z):
        self.array[i] = self.i2udb(z)

    def updateDouble(self, i, z):
        self.array[i] = self.d2udb(z)
