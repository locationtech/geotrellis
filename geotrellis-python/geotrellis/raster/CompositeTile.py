from geotrellis.raster.Tile import Tile
from geotrellis.raster.ArrayTile import ArrayTile
from geotrellis.raster.package_scala import assertEqualDimensions
from geotrellis.raster.RasterExtent import GeoAttrsError
from geotrellis.raster.TileLayout import TileLayout
from geotrellis.raster.split.Split import Split
from geotrellis.python.util.utils import isValidInt, _INTMAX

class CompositeTile(Tile):
    def __init__(self, tiles, tileLayout):
        if not isValidInt(tileLayout.totalCols):
            raise Exception("Total cols is not integer, cannot create such a large composite tile.")
        if not isValidInt(tileLayout.totalRows):
            raise Exception("Total rows is not integer, cannot create such a large composite tile.")
        self.tiles = tiles
        self.tileLayout = tileLayout
        self._cols = tileLayout.totalCols
        self._rows = tileLayout.totalRows

        self._tileList = list(tiles)
        self._tileCols = tileLayout.layoutCols

    @property
    def cols(self):
        return self._cols

    @property
    def rows(self):
        return self._rows

    @property
    def cellType(self):
        return self.tiles[0].cellType

    def __eq__(self, other):
        if not isinstance(other, CompositeTile):
            return False
        return self.tiles == other.tiles and self.tileLayout == other.tileLayout

    def __hash__(self):
        return hash((tuple(self.tiles), self.tileLayout))

    def _getTile(self, tcol, trow):
        return self._tileList[trow * self._tileCols + tcol]

    def convert(self, targetCellType):
        return self.mutable(targetCellType)

    def toArrayTile(self):
        return self.mutable()

    def mutable(self, targetCellType = None):
        if targetCellType is None:
            targetCellType = self.cellType
        if self.size > _INTMAX:
            raise Exception("This tiled raster is too big to convert into an array")
        tile = ArrayTile.alloc(targetCellType, self.cols, self.rows)
        length = self.size
        layoutCols = self.tileLayout.layoutCols
        layoutRows = self.tileLayout.layoutRows
        tileCols = self.tileLayout.tileCols
        tileRows = self.tileLayout.tileRows
        if not self.cellType.isFloatingPoint:
            for trow in xrange(0, layoutRows):
                for tcol in xrange(0, layoutCols):
                    sourceTile = self._getTile(tcol, trow)
                    for prow in xrange(0, tileRows):
                        for pcol in xrange(0, tileCols):
                            acol = (tileCols * tcol) + pcol
                            arow = (tileRows * trow) + prow
                            tile.set(acol, arow, sourceTile.get(pcol, prow))
        else:
            for trow in xrange(0, layoutRows):
                for tcol in xrange(0, layoutCols):
                    sourceTile = self._getTile(tcol, trow)
                    for prow in xrange(0, tileRows):
                        for pcol in xrange(0, tileCols):
                            acol = (tileCols * tcol) + pcol
                            arow = (tileRows * trow) + prow
                            tile.setDouble(acol, arow, sourceTile.getDouble(pcol, prow))
        return tile

    def toArray(self):
        if self.size > _INTMAX:
            raise Exception("This tile raster is too big to convert into an array.")
        arr = [None] * self.size
        length = self.size
        layoutCols = self.tileLayout.layoutCols
        layoutRows = self.tileLayout.layoutRows
        tileCols = self.tileLayout.tileCols
        tileRows = self.tileLayout.tileRows
        totalCols = layoutCols * tileCols

        for trow in xrange(0, layoutRows):
            for tcol in xrange(0, layoutCols):
                tile = self._getTile(tcol, trow)
                for prow in xrange(0, tileRows):
                    for pcol in xrange(0, tileCols):
                        acol = (tileCols * tcol) + pcol
                        arow = (tileRows * trow) + prow
                        arr[arow * totalCols + acol] = tile.get(pcol, prow)
        return arr

    def toArrayDouble(self):
        if self.size > _INTMAX:
            raise Exception("This tile raster is too big to convert into an array.")
        arr = [None] * self.size
        length = self.size
        layoutCols = self.tileLayout.layoutCols
        layoutRows = self.tileLayout.layoutRows
        tileCols = self.tileLayout.tileCols
        tileRows = self.tileLayout.tileRows
        totalCols = layoutCols * tileCols

        for trow in xrange(0, layoutRows):
            for tcol in xrange(0, layoutCols):
                tile = self._getTile(tcol, trow)
                for prow in xrange(0, tileRows):
                    for pcol in xrange(0, tileCols):
                        acol = (tileCols * tcol) + pcol
                        arow = (tileRows * trow) + prow
                        arr[arow * totalCols + acol] = tile.getDouble(pcol, prow)
        return arr

    def toBytes(self):
        return self.toArrayTile().toBytes()

    def get(self, col, row):
        tcol = col / self.tileLayout.tileCols
        trow = row / self.tileLayout.tileRows
        pcol = col % self.tileLayout.tileCols
        prow = row % self.tileLayout.tileRows

        return self._getTile(tcol, trow).get(pcol, prow)

    def getDouble(self, col, row):
        tcol = col / self.tileLayout.tileCols
        trow = row / self.tileLayout.tileRows
        pcol = col % self.tileLayout.tileCols
        prow = row % self.tileLayout.tileRows

        return self._getTile(tcol, trow).getDouble(pcol, prow)

    def foreach(self, f):
        layoutCols = self.tileLayout.layoutCols
        layoutRows = self.tileLayout.layoutRows
        tileCols = self.tileLayout.tileCols
        tileRows = self.tileLayout.tileRows

        for trow in xrange(0, layoutRows):
            for tcol in xrange(0, layoutCols):
                tile = self._getTile(tcol, trow)
                for prow in xrange(0, tileRows):
                    for pcol in xrange(0, tileCols):
                        f(tile.get(pcol, prow))

    def foreachDouble(self, f):
        layoutCols = self.tileLayout.layoutCols
        layoutRows = self.tileLayout.layoutRows
        tileCols = self.tileLayout.tileCols
        tileRows = self.tileLayout.tileRows

        for trow in xrange(0, layoutRows):
            for tcol in xrange(0, layoutCols):
                tile = self._getTile(tcol, trow)
                for prow in xrange(0, tileRows):
                    for pcol in xrange(0, tileCols):
                        f(tile.getDouble(pcol, prow))

    def foreachIntVisitor(self, visitor):
        layoutCols = self.tileLayout.layoutCols
        layoutRows = self.tileLayout.layoutRows
        tileCols = self.tileLayout.tileCols
        tileRows = self.tileLayout.tileRows

        for trow in xrange(0, layoutRows):
            for tcol in xrange(0, layoutCols):
                tile = self._getTile(tcol, trow)
                for prow in xrange(0, tileRows):
                    for pcol in xrange(0, tileCols):
                        acol = (tileCols * tcol) + pcol
                        arow = (tileRows * trow) + prow
                        visitor(acol, arow, tile.get(pcol, prow))

    def foreachDoubleVisitor(self, visitor):
        layoutCols = self.tileLayout.layoutCols
        layoutRows = self.tileLayout.layoutRows
        tileCols = self.tileLayout.tileCols
        tileRows = self.tileLayout.tileRows

        for trow in xrange(0, layoutRows):
            for tcol in xrange(0, layoutCols):
                tile = self._getTile(tcol, trow)
                for prow in xrange(0, tileRows):
                    for pcol in xrange(0, tileCols):
                        acol = (tileCols * tcol) + pcol
                        arow = (tileRows * trow) + prow
                        visitor(acol, arow, tile.getDouble(pcol, prow))

    def map(self, f):
        result = ArrayTile.alloc(self.cellType, self.cols, self.rows)
        layoutCols = self.tileLayout.layoutCols
        layoutRows = self.tileLayout.layoutRows
        tileCols = self.tileLayout.tileCols
        tileRows = self.tileLayout.tileRows

        for trow in xrange(0, layoutRows):
            for tcol in xrange(0, layoutCols):
                tile = self._getTile(tcol, trow)
                for prow in xrange(0, tileRows):
                    for pcol in xrange(0, tileCols):
                        acol = (tileCols * tcol) + pcol
                        arow = (tileRows * trow) + prow
                        result.set(acol, arow, f(tile.get(pcol, prow)))
        return result

    def mapDouble(self, f):
        result = ArrayTile.alloc(self.cellType, self.cols, self.rows)
        layoutCols = self.tileLayout.layoutCols
        layoutRows = self.tileLayout.layoutRows
        tileCols = self.tileLayout.tileCols
        tileRows = self.tileLayout.tileRows

        for trow in xrange(0, layoutRows):
            for tcol in xrange(0, layoutCols):
                tile = self._getTile(tcol, trow)
                for prow in xrange(0, tileRows):
                    for pcol in xrange(0, tileCols):
                        acol = (tileCols * tcol) + pcol
                        arow = (tileRows * trow) + prow
                        result.setDouble(acol, arow, f(tile.getDouble(pcol, prow)))
        return result

    def mapIntMapper(self, mapper):
        result = ArrayTile.alloc(self.cellType, self.cols, self.rows)
        layoutCols = self.tileLayout.layoutCols
        layoutRows = self.tileLayout.layoutRows
        tileCols = self.tileLayout.tileCols
        tileRows = self.tileLayout.tileRows

        for trow in xrange(0, layoutRows):
            for tcol in xrange(0, layoutCols):
                tile = self._getTile(tcol, trow)
                for prow in xrange(0, tileRows):
                    for pcol in xrange(0, tileCols):
                        acol = (tileCols * tcol) + pcol
                        arow = (tileRows * trow) + prow
                        result.set(acol, arow, mapper(acol, arow, tile.get(pcol, prow)))
        return result

    def mapDoubleMapper(self, mapper):
        result = ArrayTile.alloc(self.cellType, self.cols, self.rows)
        layoutCols = self.tileLayout.layoutCols
        layoutRows = self.tileLayout.layoutRows
        tileCols = self.tileLayout.tileCols
        tileRows = self.tileLayout.tileRows

        for trow in xrange(0, layoutRows):
            for tcol in xrange(0, layoutCols):
                tile = self._getTile(tcol, trow)
                for prow in xrange(0, tileRows):
                    for pcol in xrange(0, tileCols):
                        acol = (tileCols * tcol) + pcol
                        arow = (tileRows * trow) + prow
                        result.setDouble(acol, arow, mapper(acol, arow, tile.getDouble(pcol, prow)))
        return result

    def combine(self, other, f):
        assertEqualDimensions(self, other)

        result = ArrayTile.alloc(self.cellType, self.cols, self.rows)
        layoutCols = self.tileLayout.layoutCols
        layoutRows = self.tileLayout.layoutRows
        tileCols = self.tileLayout.tileCols
        tileRows = self.tileLayout.tileRows

        for trow in xrange(0, layoutRows):
            for tcol in xrange(0, layoutCols):
                tile = self._getTile(tcol, trow)
                for prow in xrange(0, tileRows):
                    for pcol in xrange(0, tileCols):
                        acol = (tileCols * tcol) + pcol
                        arow = (tileRows * trow) + prow
                        result.set(acol, arow, f(tile.get(pcol, prow), other.get(acol, arow)))
        return result

    def combineDouble(self, other, f):
        assertEqualDimensions(self, other)

        result = ArrayTile.alloc(self.cellType, self.cols, self.rows)
        layoutCols = self.tileLayout.layoutCols
        layoutRows = self.tileLayout.layoutRows
        tileCols = self.tileLayout.tileCols
        tileRows = self.tileLayout.tileRows

        for trow in xrange(0, layoutRows):
            for tcol in xrange(0, layoutCols):
                tile = self._getTile(tcol, trow)
                for prow in xrange(0, tileRows):
                    for pcol in xrange(0, tileCols):
                        acol = (tileCols * tcol) + pcol
                        arow = (tileRows * trow) + prow
                        result.setDouble(acol, arow, f(tile.getDouble(pcol, prow), other.getDouble(acol, arow)))
        return result

    @staticmethod
    def apply(tile, tileLayout):
        if isinstance(tile, CompositeTile):
            if tileLayout != tile.tileLayout:
                raise GeoAttrsError("This tile is a composite tile with a " +
                        "different layout than the argument tile layout." +
                        " {tl} does not match {ttl}".format(tl = tileLayout, ttl = tile.tileLayout))
            return tile
        else:
            return CompositeTile.wrap(tile, tileLayout)

    @staticmethod
    def wrap(tile, one, two = None, three = None):
        tileLayout, cropped = _get_params(tile, one, two, three)
        return CompositeTile(tile.split(tileLayout, Split.Options(cropped = cropped)), tileLayout)

def _get_params(tile, one, two, three):
    if three is not None:
        tileCols, tileRows, cropped = one, two, three
        tileLayout = TileLayout(tileCols, tileRows, ((tile.cols - 1) / tileCols) + 1, ((tile.rows - 1) / tileRows) + 1)
        return tileLayout, cropped
    elif two is not None:
        if isinstance(one, TileLayout):
            tileLayout, cropped = one, two
            return tileLayout, cropped
        else:
            tileCols, tileRows = one, two
            tileLayout = TileLayout(tileCols, tileRows, ((tile.cols - 1) / tileCols) + 1, ((tile.rows - 1) / tileRows) + 1)
            cropped = True
            return tileLayout, cropped
    else:
        tileLayout = one
        cropped = True
        return tileLayout, cropped
