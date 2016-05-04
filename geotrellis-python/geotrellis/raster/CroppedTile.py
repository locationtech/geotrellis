from geotrellis.raster.Tile import Tile
from geotrellis.raster.RasterExtent import RasterExtent
from geotrellis.raster.ArrayTile import ArrayTile
from geotrellis.raster.package_scala import NODATA, assertEqualDimensions

class CroppedTile(Tile):
    def __init__(self, sourceTile, one, two = None):
        if two is not None:
            sourceExtent = one
            targetExtent = two
            gridBounds = RasterExtent(
                    sourceExtent,
                    sourceTile.cols,
                    sourceTile.rows).gridBoundsFor(targetExtent)
        else:
            gridBounds = one
        self.sourceTile = sourceTile
        self._gridBounds = gridBounds
        self._colMin = gridBounds.colMin
        self._rowMin = gridBounds.rowMin
        self._sourceCols = sourceTile.cols
        self._sourceRows = sourceTile.rows

    def __eq__(self, other):
        if not isinstance(other, CroppedTile):
            return False
        return (self.sourceTile == other.sourceTile and
                self.gridBounds == other.gridBounds)

    def __hash__(self):
        return hash((self.sourceTile, self.gridBounds))

    @property
    def gridBounds(self):
        return self._gridBounds

    @property
    def cols(self):
        return self.gridBounds.width

    @property
    def rows(self):
        return self.gridBounds.height

    @property
    def cellType(self):
        return self.sourceTile.cellType

    def convert(self, targetCellType):
        return self.mutable(targetCellType)

    def get(col, row):
        c = col + self.gridBounds.colMin
        r = row + self.gridBounds.rowMin
        if c < 0 or r < 0 or c >= self._sourceCols or r >= self._sourceRows:
            return NODATA
        else:
            return self.sourceTile.get(c, r)

    def getDouble(col, row):
        c = col + self.gridBounds.colMin
        r = row + self.gridBounds.rowMin
        if c < 0 or r < 0 or c >= self._sourceCols or r >= self._sourceRows:
            return float("nan")
        else:
            return self.sourceTile.getDouble(c, r)

    def toArrayTile(self):
        return self.mutable()

    def mutable(self, targetCellType = None):
        if targetCellType is None:
            targetCellType = self.cellType
        tile = ArrayTile.alloc(targetCellType, self.cols, self.rows)

        if not self.cellType.isFloatingPoint:
            for row in xrange(0, self.rows):
                for col in xrange(0, self.cols):
                    tile.set(col, row, self.get(col, row))
        else:
            for row in xrange(0, self.rows):
                for col in xrange(0, self.cols):
                    tile.setDouble(col, row, self.getDouble(col, row))
        return tile

    def toArray(self):
        arr = [None] * self.size

        for row in xrange(0, self.rows):
            for col in xrange(0, self.cols):
                arr[i] = self.get(col, row)
        return arr

    def toArrayDouble(self):
        arr = [None] * self.size

        for row in xrange(0, self.rows):
            for col in xrange(0, self.cols):
                arr[i] = self.getDouble(col, row)
        return arr

    def toBytes(self):
        return self.toArrayTile().toBytes()

    def foreach(self, f):
        for row in xrange(0, self.rows):
            for col in xrange(0, self.cols):
                f(self.get(col, row))

    def foreachDouble(self, f):
        for row in xrange(0, self.rows):
            for col in xrange(0, self.cols):
                f(self.getDouble(col, row))

    def foreachIntVisitor(self, visitor):
        for row in xrange(0, self.rows):
            for col in xrange(0, self.cols):
                visitor(col, row, self.get(col, row))

    def foreachDoubleVisitor(self, visitor):
        for row in xrange(0, self.rows):
            for col in xrange(0, self.cols):
                visitor(col, row, self.getDouble(col, row))

    def map(self, f):
        tile = ArrayTile.alloc(self.cellType, self.cols, self.rows)
        for row in xrange(0, self.rows):
            for col in xrange(0, self.cols):
                tile.set(col, row, f(self.get(col, row)))
        return tile

    def mapDouble(self, f):
        tile = ArrayTile.alloc(self.cellType, self.cols, self.rows)
        for row in xrange(0, self.rows):
            for col in xrange(0, self.cols):
                tile.setDouble(col, row, f(self.getDouble(col, row)))
        return tile

    def mapIntMapper(self, mapper):
        tile = ArrayTile.alloc(self.cellType, self.cols, self.rows)
        for row in xrange(0, self.rows):
            for col in xrange(0, self.cols):
                tile.set(col, row, mapper(col, row, self.get(col, row)))
        return tile

    def mapDoubleMapper(self, mapper):
        tile = ArrayTile.alloc(self.cellType, self.cols, self.rows)
        for row in xrange(0, self.rows):
            for col in xrange(0, self.cols):
                tile.setDouble(col, row, mapper(col, row, self.getDouble(col, row)))
        return tile

    def combine(self, other, f):
        assertEqualDimensions(self, other)
        tile = ArrayTile.alloc(self.cellType, self.cols, self.rows)
        for row in xrange(0, self.rows):
            for col in xrange(0, self.cols):
                tile.set(col, row, f(self.get(col, row), other.get(col, row)))
        return tile

    def combineDouble(self, other, f):
        assertEqualDimensions(self, other)
        tile = ArrayTile.alloc(self.cellType, self.cols, self.rows)
        for row in xrange(0, self.rows):
            for col in xrange(0, self.cols):
                tile.setDouble(col, row, f(self.getDouble(col, row), other.getDouble(col, row)))
        return tile
