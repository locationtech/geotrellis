from geotrellis.raster.UserDefinedNoDataConversions import UserDefinedShortNoDataConversions
from geotrellis.raster.MutableArrayTile import MutableArrayTile
from geotrellis.raster.CellType import UShortCellType, UShortConstantNoDataCellType
from geotrellis.raster.package_scala import intToShort, us2i, us2d, i2us, d2us, 
ushortNODATA

import array
import struct

class UShortArrayTile(MutableArrayTile):
    def __init__(self, arr, cols, rows):
        if isinstance(arr, list):
            self.array = array.array('H', arr)
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
        typecode = "{len}H".format(len = length)
        arr = array.array('b')
        arr.fromstring(struct.pack(typecode, *self.array.tolist()))
        return arr

    def copy(self):
        arr = array.array('H', self.array.tolist())
        return UShortArrayTile.applyStatic(arr, self.cols, self.rows)

    @staticmethod
    def applyStatic(arr, cols, rows, cellType = UShortConstantNoDataCellType):
        if cellType is UShortCellType:
            return UShortRawArrayTile(arr, cols, rows)
        elif cellType is UShortConstantNoDataCellType:
            return UShortConstantNoDataArrayTile(arr, cols, rows)
        elif isinstance(cellType, UShortUserDefinedNoDataCellType):
            return UShortUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

    @staticmethod
    def ofDim(cols, rows, cellType = UShortConstantNoDataCellType):
        arr = array.array('H', [0] * (cols*rows))
        if cellType is UShortCellType:
            return UShortRawArrayTile(arr, cols, rows)
        elif cellType is UShortConstantNoDataCellType:
            return UShortConstantNoDataArrayTile(arr, cols, rows)
        elif isinstance(cellType, UShortUserDefinedNoDataCellType):
            return UShortUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

    @staticmethod
    def empty(cols, rows, cellType = UShortConstantNoDataCellType):
        if cellType is UShortCellType:
            return UShortArrayTile.ofDim(cols, rows, cellType)
        elif cellType is UShortConstantNoDataCellType:
            return UShortArrayTile.fill(ushortNODATA, cols, rows, cellType)
        elif isinstance(cellType, UShortUserDefinedNoDataCellType):
            nd = cellType.noDataValue
            return UShortArrayTile.fill(nd, cols, rows, cellType)

    @staticmethod
    def fill(v, cols, rows, cellType = UShortConstantNoDataCellType):
        arr = array.array('H', [v] * (cols*rows))
        if cellType is UShortCellType:
            return UShortRawArrayTile(arr, cols, rows)
        elif cellType is UShortConstantNoDataCellType:
            return UShortConstantNoDataArrayTile(arr, cols, rows)
        elif isinstance(cellType, UShortUserDefinedNoDataCellType):
            return UShortUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

    @staticmethod
    def fromBytes(bytesarray, cols, rows, cellType = UShortConstantNoDataCellType):
        arr = array.array('H')
        length = len(bytesarray)
        typecode = "{len}b".format(len = length)
        arr.fromstring(struct.pack(typecode, *bytesarray.tolist()))

        if cellType is UShortCellType:
            return UShortRawArrayTile(arr, cols, rows)
        elif cellType is UShortConstantNoDataCellType:
            return UShortConstantNoDataArrayTile(arr, cols, rows)
        elif isinstance(cellType, UShortUserDefinedNoDataCellType):
            return UShortUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

def generateCodec():
    from geotrellis.spark.io.avro.codecs.TileCodecs import UShortArrayTileCodec
    return UShortArrayTileCodec()

def UShortRawArrayTile(UShortArrayTile):
    implicits = {"AvroRecordCodec": generateCodec}

    def __init__(self, arr, cols, rows):
        UShortArrayTile.__init__(self, arr, cols, rows)
        self._cellType = UShortCellType

    def __eq__(self, other):
        if not isinstance(other, UShortRawArrayTile):
            return False
        return (self.array == other.array and
                self.cols == other.cols and
                self.rows == other.rows)
    
    def __hash__(self):
        return hash((tuple(self.array), self.cols, self.rows))

    def apply(self, i):
        return self.array[i] & 0xFFFF

    def applyDouble(self, i):
        return float(self.array[i] & 0xFFFF)

    def _update(self, i, z):
        self.array[i] = intToShort(z)

    def updateDouble(self, i, z):
        self.array[i] = intToShort(int(z))

def UShortConstantNoDataArrayTile(UShortArrayTile):
    implicits = {"AvroRecordCodec": generateCodec}

    def __init__(self, arr, cols, rows):
        UShortArrayTile.__init__(self, arr, cols, rows)
        self._cellType = UShortConstantNoDataCellType

    def __eq__(self, other):
        if not isinstance(other, UShortConstantNoDataArrayTile):
            return False
        return (self.array == other.array and
                self.cols == other.cols and
                self.rows == other.rows)
    
    def __hash__(self):
        return hash((tuple(self.array), self.cols, self.rows))

    def apply(self, i):
        return us2i(self.array[i])

    def applyDouble(self, i):
        return us2d(self.array[i])

    def _update(self, i, z):
        self.array[i] = i2us(z)

    def updateDouble(self, i, z):
        self.array[i] = d2us(z)

class UShortUserDefinedNoDataArrayTile(UserDefinedShortNoDataConversions, UShortArrayTile):
    implicits = {"AvroRecordCodec": generateCodec}

    def __init__(self, arr, cols, rows, cellType):
        UShortArrayTile.__init__(self, arr, cols, rows)
        self._cellType = cellType

    def __eq__(self, other):
        if not isinstance(other, UShortUserDefinedNoDataArrayTile):
            return False
        return (self.array == other.array and
                self.cols == other.cols and
                self.rows == other.rows and
                self.cellType == other.cellType)
    
    def __hash__(self):
        return hash((tuple(self.array), self.cols, self.rows, self.cellType))

    def apply(self, i):
        return self.udus2i(self.array[i])

    def applyDouble(self, i):
        return self.udus2d(self.array[i])

    def _update(self, i, z):
        self.array[i] = self.i2uds(z)

    def updateDouble(self, i, z):
        self.array[i] = self.d2uds(z)
