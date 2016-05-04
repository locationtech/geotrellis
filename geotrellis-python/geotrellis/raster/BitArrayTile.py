from geotrellis.raster.MutableArrayTile import MutableArrayTile
from geotrellis.raster.CellType import BitCellType
from geotrellis.raster.ConstantTile import BitConstantTile
from geotrellis.raster.package_scala import isData, i2d, d2i, intToByte

class BitArrayTile(MutableArrayTile):
    def __init__(self, arr, cols, rows):
        if len(arr) != (cols*rows + 7) / 8:
            raise Exception("BitArrayTile array length must " +
                    "be {good}, was {bad}".format(
                        good = (cols*rows + 7) / 8,
                        bad = len(arr)))
        self.array = arr
        self._cols = cols
        self._rows = rows

    def __eq__(self, other):
        if not isinstance(other, BitArrayTile):
            return False
        return (self.array == other.array and
                self.cols == other.cols and
                self.rows == other.rows)

    def __hash__(self):
        return hash((tuple(self.array), self.cols, self.rows))

    @property
    def cols(self):
        return self._cols

    @property
    def rows(self):
        return self._rows

    @property
    def cellType(self):
        return BitCellType

    def apply(self, i):
        return (self.array[i >> 3] >> (i & 7)) & 1

    def _update(self, i, z):
        BitArrayTile.update(self.array, i, z)

    def applyDouble(self, i):
        return i2d(self.apply(i))

    def updateDouble(self, i, z):
        BitArrayTile.updateDouble(self.array, i, z)

    def map(self, f):
        f0 = f(0) & 1
        f1 = f(1) & 1

        if f0 == 0 and f1 == 0:
            return BitConstantTile(False, self.cols, self.rows)
        elif f0 == 1 and f1 == 1:
            return BitConstantTile(True, self.cols, self.rows)
        elif f0 == 0 and f1 == 1:
            return self
        else:
            clone = self.array[:]
            i = 0
            length = len(self.array)
            while i < length:
                clone[i] = intToByte(self.array[i] ^ -1)
                i += 1
            return BitArrayTile(clone, self.cols, self.rows)

    def mapDouble(self, f):
        return self.map(lambda z: d2i(f(i2d(z))))

    def copy(self):
        return BitArrayTile(self.array[:], self.cols, self.rows)

    def toBytes(self):
        return array.array('b', self.array[:])

    @staticmethod
    def update(arr, i, z):
        div = i >> 3
        if (z & 1) == 0:
            arr[div] = intToByte(arr[div] & ~(1 << (i & 7)))
        else:
            arr[div] = intToByte(arr[div] | (1 << (i & 7)))

    @staticmethod
    def updateDouble(arr, i, z):
        BitArrayTile.update(arr, i, int(z) if isData(z) else 0)

    @staticmethod
    def ofDim(cols, rows):
        size = ((cols * rows) + 7) / 8
        arr = [0] * size
        return BitArrayTile(arr, cols, rows)

    @staticmethod
    def empty(cols, rows):
        return BitArrayTile.ofDim(cols, rows)

    @staticmethod
    def fill(v, cols, rows):
        if not v:
            return BitArrayTile.ofDim(cols, rows)
        else:
            size = ((cols * rows) + 7) / 8
            arr = [1] * size
            BitArrayTile(arr, cols, rows)

    @staticmethod
    def fromBytes(bytesarray, cols, rows):
        return BitArrayTile(bytesarray, cols, rows)
