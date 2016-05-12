from geotrellis.raster.UserDefinedNoDataConversions import UserDefinedIntNoDataConversions
from geotrellis.raster.MutableArrayTile import MutableArrayTile
from geotrellis.raster.CellType import IntCellType, IntConstantNoDataCellType, IntUserDefinedNoDataCellType
from geotrellis.raster.package_scala import i2d, d2i, NODATA

import array
import struct

class IntArrayTile(MutableArrayTile):
    def __init__(self, arr, cols, rows):
        if isinstance(arr, list):
            self.array = array.array('i', arr)
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

    def toArray(self):
        return self.array.tolist()

    def toBytes(self):
        length = len(self.array)
        typecode = "{len}i".format(len = length)
        arr = array.array('b')
        arr.fromstring(struct.pack(typecode, *self.array.tolist()))
        return arr

    def copy(self):
        arr = array.array('i', self.array.tolist())
        return ArrayTile.applyStatic(arr, self.cols, self.rows)

    @staticmethod
    def applyStatic(arr, cols, rows, cellType = IntConstantNoDataCellType):
        if cellType is IntCellType:
            return IntRawArrayTile(arr, cols, rows)
        elif cellType is IntConstantNoDataCellType:
            return IntConstantNoDataArrayTile(arr, cols, rows)
        elif isinstance(cellType, IntUserDefinedNoDataCellType):
            return IntUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

    @staticmethod
    def ofDim(cols, rows, cellType = IntConstantNoDataCellType):
        arr = array.array('i', [0] * (cols*rows))
        if cellType is IntCellType:
            return IntRawArrayTile(arr, cols, rows)
        elif cellType is IntConstantNoDataCellType:
            return IntConstantNoDataArrayTile(arr, cols, rows)
        elif isinstance(cellType, IntUserDefinedNoDataCellType):
            return IntUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

    @staticmethod
    def empty(cols, rows, cellType = IntConstantNoDataCellType):
        if cellType is IntCellType:
            return IntArrayTile.ofDim(cols, rows, cellType)
        elif cellType is IntConstantNoDataCellType:
            return IntArrayTile.fill(NODATA, cols, rows, cellType)
        elif isinstance(cellType, IntUserDefinedNoDataCellType):
            nd = cellType.noDataValue
            return IntArrayTile.fill(nd, cols, rows, cellType)

    @staticmethod
    def fill(v, cols, rows, cellType = IntConstantNoDataCellType):
        arr = array.array('i', [v] * (cols*rows))
        if cellType is IntCellType:
            return IntRawArrayTile(arr, cols, rows)
        elif cellType is IntConstantNoDataCellType:
            return IntConstantNoDataArrayTile(arr, cols, rows)
        elif isinstance(cellType, IntUserDefinedNoDataCellType):
            return IntUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

    @staticmethod
    def fromBytes(bytesarray, cols, rows, cellType = IntConstantNoDataCellType):
        arr = array.array('i')
        length = len(bytesarray)
        typecode = "{len}b".format(len = length)
        arr.fromstring(struct.pack(typecode, *bytesarray.tolist()))

        if cellType is IntCellType:
            return IntRawArrayTile(arr, cols, rows)
        elif cellType is IntConstantNoDataCellType:
            return IntConstantNoDataArrayTile(arr, cols, rows)
        elif isinstance(cellType, IntUserDefinedNoDataCellType):
            return IntUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

def generateCodec():
    from geotrellis.spark.io.avro.codecs.TileCodecs import IntArrayTileCodec
    return IntArrayTileCodec()

class IntRawArrayTile(IntArrayTile):
    implicits = {"AvroRecordCodec": generateCodec}

    def __init__(self, arr, cols, rows):
        IntArrayTile.__init__(self, arr, cols, rows)
        self._cellType = IntCellType

    def __eq__(self, other):
        if not isinstance(other, IntRawArrayTile):
            return False
        return (self.array == other.array and
                self.cols == other.cols and
                self.rows == other.rows)
    
    def __hash__(self):
        return hash((self.array, self.cols, self.rows))

    def apply(self, i):
        return self.array[i]

    def applyDouble(self, i):
        return float(self.array[i])

    def _update(self, i, z):
        self.array[i] = z

    def updateDouble(self, i, z):
        self.array[i] = int(z)

class IntConstantNoDataArrayTile(IntArrayTile):
    implicits = {"AvroRecordCodec": generateCodec}

    def __init__(self, arr, cols, rows):
        IntArrayTile.__init__(self, arr, cols, rows)
        self._cellType = IntConstantNoDataCellType

    def __eq__(self, other):
        if not isinstance(other, IntConstantNoDataArrayTile):
            return False
        return (self.array == other.array and
                self.cols == other.cols and
                self.rows == other.rows)
    
    def __hash__(self):
        return hash((self.array, self.cols, self.rows))

    def apply(self, i):
        return self.array[i]

    def applyDouble(self, i):
        return i2d(self.array[i])

    def _update(self, i, z):
        self.array[i] = z

    def updateDouble(self, i, z):
        self.array[i] = d2i(z)

class IntUserDefinedNoDataArrayTile(UserDefinedIntNoDataConversions, IntArrayTile):
    implicits = {"AvroRecordCodec": generateCodec}

    def __init__(self, arr, cols, rows, cellType):
        IntArrayTile.__init__(self, arr, cols, rows)
        self._cellType = cellType

    def __eq__(self, other):
        if not isinstance(other, IntUserDefinedNoDataArrayTile):
            return False
        return (self.array == other.array and
                self.cols == other.cols and
                self.rows == other.rows and
                self.cellType == other.cellType)
    
    def __hash__(self):
        return hash((self.array, self.cols, self.rows, self.cellType))

    def apply(self, i):
        return self.udi2i(self.array[i])

    def applyDouble(self, i):
        return self.udi2d(self.array[i])

    def _update(self, i, z):
        self.array[i] = self.i2udi(z)

    def updateDouble(self, i, z):
        self.array[i] = self.d2udi(z)
