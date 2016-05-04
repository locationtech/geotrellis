from geotrellis.raster.Tile import Tile
from geotrellis.raster.ArrayTile import ArrayTile
from geotrellis.raster.CellType import (BitCellType, ByteConstantNoDataCellType,
        ShortConstantNoDataCellType, IntConstantNoDataCellType,
        FloatConstantNoDataCellType, DoubleConstantNoDataCellType)
from geotrellis.raster.BitArrayTile import BitArrayTile
from geotrellis.raster.ByteArrayTile import ByteArrayTile
from geotrellis.raster.ShortArrayTile import ShortArrayTile
from geotrellis.raster.UShortArrayTile import UShortArrayTile
from geotrellis.raster.IntArrayTile import IntArrayTile
from geotrellis.raster.FloatArrayTile import FloatArrayTile
from geotrellis.raster.DoubleArrayTile import DoubleArrayTile
from geotrellis.raster.package_scala import (i2b, b2i, b2d, i2ub, ub2i, ub2d,
        i2s, s2i, s2d, i2us, us2i, us2d, i2d, d2f, d2i, f2i, f2d,
        intToByte)

import array
import struct

class ConstantTile(Tile):

    def get(self, col, row):
        return self._ival

    def getDouble(self, col, row):
        return self._dval

    def toArray(self):
        arr = [self._ival] * self.size
        return array.array('i', arr)

    def toArrayDouble(self):
        arr = [self._dval] * self.size
        return array.array('d', arr)

    def convert(self, newType):
        if isinstance(newType, BitCellType):
            return BitConstantTile(False if self._ival == 0 else True, cols, rows)
        elif isinstance(newType, ByteConstantNoDataCellType):
            return ByteConstantTile(i2b(self._ival), cols, rows)
        elif isinstance(newType, UByteConstantNoDataCellType):
            return UByteConstantTile(i2ub(self._ival), cols, rows)
        elif isinstance(newType, ShortConstantNoDataCellType):
            return ShortConstantTile(i2s(self._ival), cols, rows)
        elif isinstance(newType, UShortConstantNoDataCellType):
            return UShortConstantTile(i2us(self._ival), cols, rows)
        elif isinstance(newType, IntConstantNoDataCellType):
            return IntConstantTile(self._ival, cols, rows)
        elif isinstance(newType, FloatConstantNoDataCellType):
            return FloatConstantTile(d2f(self._dval), cols, rows)
        elif isinstance(newType, DoubleConstantNoDataCellType):
            return DoubleConstantTile(self._dval, cols, rows)
        else:
            raise Exception("Unsupported ConstantTile CellType: {ct}".format(ct = newType))

    def foreach(self, f):
        i = 0
        length = self.size
        while i < length:
            f(self._ival)
            i += 1

    def foreachDouble(self, f):
        i = 0
        length = self.size
        while i < length:
            f(self._dval)
            i += 1

    def foreachIntVisitor(visitor):
        for row in xrange(0, self.rows):
            for col in xrange(0, self.cols):
                visitor(col, row, self._ival)

    def foreachDoubleVisitor(visitor):
        for row in xrange(0, self.rows):
            for col in xrange(0, self.cols):
                visitor(col, row, self._dval)

    def map(f):
        return IntConstantTile(f(self._ival), self.cols, self.rows)

    def combine(other, f):
        return other.map(lambda z: f(self._ival, z))

    def mapDouble(f):
        return DoubleConstantTile(f(self._dval), self.cols, self.rows)

    def combineDouble(other, f):
        return other.mapDouble(lambda z: f(self._dval, z))

    def mapIntMapper(mapper):
        tile = ArrayTile.alloc(self.cellType, self.cols, self.rows)
        for row in xrange(0, self.rows):
            for col in xrange(0, self.cols):
                tile.set(col, row, mapper(col, row, self.get(col, row)))
        return tile

    def mapDoubleMapper(mapper):
        tile = ArrayTile.alloc(self.cellType, self.cols, self.rows)
        for row in xrange(0, self.rows):
            for col in xrange(0, self.cols):
                tile.setDouble(col, row, mapper(col, row, self.getDouble(col, row)))
        return tile

class BitConstantTile(ConstantTile):
    def __init__(self, val, cols, rows):
        if isinstance(val, int):
            v = False if val == 0 else True
        else:
            v = val
        self._ival = 1 if v else 0
        self._dval = 1.0 if v else 0.0
        self._cols = cols
        self._rows = rows

    def __eq__(self, other):
        if not isinstance(other, BitConstantTile):
            return False
        return (self._ival == other._ival and
                self.cols == other.cols and
                self.rows == other.rows)

    def __hash__(self):
        return hash((self._ival, self.cols, self.rows))

    @property
    def cols(self):
        return self._cols

    @property
    def rows(self):
        return self._rows

    @property
    def cellType(self):
        return BitCellType

    def toArrayTile(self):
        return self.mutable()

    def mutable(self):
        return BitArrayTile.fill(v, self.cols, self.rows)
    
    def toBytes(self):
        arr = [intToByte(self._ival)]
        return array.array('b', arr)

class ByteConstantTile(ConstantTile):
    def __init__(self, v, cols, rows):
        self._v = v
        self._ival = b2i(v)
        self._dval = b2d(v)
        self._cols = cols
        self._rows = rows

    def __eq__(self, other):
        if not isinstance(other, ByteConstantTile):
            return False
        return (self._v == other._v and
                self.cols == other.cols and
                self.rows == other.rows)

    def __hash__(self):
        return hash((self._v, self.cols, self.rows))

    @property
    def cols(self):
        return self._cols

    @property
    def rows(self):
        return self._rows

    @property
    def cellType(self):
        return ByteConstantNoDataCellType

    def toArrayTile(self):
        return self.mutable()

    def mutable(self):
        return ByteArrayTile.fill(self._v, self.cols, self.rows)

    def toBytes(self):
        return array.array('b', [self._v])

class UByteConstantTile(ConstantTile):
        self._v = v
        self._ival = ub2i(v)
        self._dval = ub2d(v)
        self._cols = cols
        self._rows = rows

    def __eq__(self, other):
        if not isinstance(other, UByteConstantTile):
            return False
        return (self._v == other._v and
                self.cols == other.cols and
                self.rows == other.rows)

    def __hash__(self):
        return hash((self._v, self.cols, self.rows))

    @property
    def cols(self):
        return self._cols

    @property
    def rows(self):
        return self._rows

    @property
    def cellType(self):
        return ByteConstantNoDataCellType

    def toArrayTile(self):
        return self.mutable()

    def mutable(self):
        return ByteArrayTile.fill(self._v, self.cols, self.rows)

    def toBytes(self):
        return array.array('b', [self._v])

    def resample(self, current, target, method):
        return ByteConstantTile(self._v, target.cols, target.rows)

class ShortConstantTile(ConstantTile):
    def __init__(self, v, cols, rows):
        self._v = v
        self._ival = s2i(v)
        self._dval = s2d(v)
        self._cols = cols
        self._rows = rows

    def __eq__(self, other):
        if not isinstance(other, ShortConstantTile):
            return False
        return (self._v == other._v and
                self.cols == other.cols and
                self.rows == other.rows)

    def __hash__(self):
        return hash((self._v, self.cols, self.rows))

    @property
    def cols(self):
        return self._cols

    @property
    def rows(self):
        return self._rows

    @property
    def cellType(self):
        return ShortConstantNoDataCellType

    def toArrayTile(self):
        return self.mutable()

    def mutable(self):
        return ShortArrayTile.fill(self._v, self.cols, self.rows)

    def toBytes(self):
        arr = array.array('b')
        arr.fromstring(struct.pack('h', self._v))
        return arr

class UShortConstantTile(ConstantTile):
    def __init__(self, v, cols, rows):
        self._v = v
        self._ival = us2i(v)
        self._dval = us2d(v)
        self._cols = cols
        self._rows = rows

    def __eq__(self, other):
        if not isinstance(other, UShortConstantTile):
            return False
        return (self._v == other._v and
                self.cols == other.cols and
                self.rows == other.rows)

    def __hash__(self):
        return hash((self._v, self.cols, self.rows))

    @property
    def cols(self):
        return self._cols

    @property
    def rows(self):
        return self._rows

    @property
    def cellType(self):
        return ShortConstantNoDataCellType

    def toArrayTile(self):
        return self.mutable()

    def mutable(self):
        return UShortArrayTile.fill(self._v, self.cols, self.rows)

    def toBytes(self):
        arr = array.array('b')
        arr.fromstring(struct.pack('h', self._v))
        return arr

    def resample(self, current, target, method):
        return ShortConstantTile(self._v, target.cols, target.rows)

class IntConstantTile(ConstantTile):
    def __init__(self, v, cols, rows):
        self._ival = v
        self._dval = i2d(v)
        self._cols = cols
        self._rows = rows

    def __eq__(self, other):
        if not isinstance(other, IntConstantTile):
            return False
        return (self._ival == other._ival and
                self.cols == other.cols and
                self.rows == other.rows)

    def __hash__(self):
        return hash((self._ival, self.cols, self.rows))

    @property
    def cols(self):
        return self._cols

    @property
    def rows(self):
        return self._rows

    @property
    def cellType(self):
        return IntConstantNoDataCellType

    def toArrayTile(self):
        return self.mutable()

    def mutable(self):
        return IntArrayTile.fill(self._ival, self.cols, self.rows)

    def toBytes(self):
        arr = array.array('b')
        arr.fromstring(struct.pack('i', self._ival))
        return arr

class FloatConstantTile(ConstantTile):
    def __init__(self, v, cols, rows):
        self._v = v
        self._ival = f2i(v)
        self._dval = f2d(v)
        self._cols = cols
        self._rows = rows

    def __eq__(self, other):
        if not isinstance(other, FloatConstantTile):
            return False
        return (self._v == other._v and
                self.cols == other.cols and
                self.rows == other.rows)

    def __hash__(self):
        return hash((self._v, self.cols, self.rows))

    @property
    def cols(self):
        return self._cols

    @property
    def rows(self):
        return self._rows

    @property
    def cellType(self):
        return FloatConstantNoDataCellType

    def toArrayTile(self):
        return self.mutable()

    def mutable(self):
        return FloatArrayTile.fill(self._v, self.cols, self.rows)

    def toBytes(self):
        arr = array.array('b')
        arr.fromstring(struct.pack('f', self._v))
        return arr

class DoubleConstantTile(ConstantTile):
    def __init__(self, v, cols, rows):
        self._ival = d2i(v)
        self._dval = v
        self._cols = cols
        self._rows = rows

    def __eq__(self, other):
        if not isinstance(other, DoubleConstantTile):
            return False
        return (self._dval == other._dval and
                self.cols == other.cols and
                self.rows == other.rows)

    def __hash__(self):
        return hash((self._dval, self.cols, self.rows))

    @property
    def cols(self):
        return self._cols

    @property
    def rows(self):
        return self._rows

    @property
    def cellType(self):
        return DoubleConstantNoDataCellType

    def toArrayTile(self):
        return self.mutable()

    def mutable(self):
        return DoubleArrayTile.fill(self._dval, self.cols, self.rows)

    def toBytes(self):
        arr = array.array('b')
        arr.fromstring(struct.pack('d', self._dval))
        return arr

