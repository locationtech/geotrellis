from __future__ import absolute_import
from geotrellis.raster.CellSize import CellSize

class TileLayout(object):
    def __init__(self, layoutCols, layoutRows, tileCols, tileRows):
        self.layoutCols = layoutCols
        self.layoutRows = layoutRows
        self.tileCols = tileCols
        self.tileRows = tileRows

    def __eq__(self, other):
        if not isinstance(other, TileLayout):
            return False
        return (self.layoutCols == other.layoutCols and
                self.layoutRows == other.layoutRows and
                self.tileCols == other.tileCols and
                self.tileRows == other.tileRows)

    def __hash__(self):
        return hash((self.layoutCols, self.layoutRows,
            self.tileCols, self.tileRows))

    @property
    def isTiled(self):
        return self.layoutCols > 1 or self.layoutRows > 1

    @property
    def totalCols(self):
        return self.layoutCols * self.tileCols

    @property
    def totalRows(self):
        return self.layoutRows * self.tileRows

    @property
    def layoutDimensions(self):
        return (self.layoutCols, self.layoutRows)

    @property
    def tileDimensions(self):
        return (self.tileCols, self.tileRows)

    @property
    def tileSize(self):
        return self.tileCols * self.tileRows

    def cellSize(self, extent):
        return CellSize(extent.width / self.totalCols, extent.height / self.totalRows)
    
    def combine(self, other):
        maxLayoutCols = max(self.layoutCols, other.layoutCols)
        maxLayoutRows = max(self.layoutRows, other.layoutRows)
        maxTileCols   = max(self.tileCols, other.tileCols)
        maxTileRows   = max(self.tileRows, other.tileRows)

        return TileLayout(maxLayoutCols, maxLayoutRows, maxTileCols, maxTileRows)

    @staticmethod
    def singleTile(first, second = None):
        if second is None:
            extent = first
            cols, rows = extent.cols, extent.rows
        else:
            cols, rows = first, second
        return TileLayout(1, 1, cols, rows)
