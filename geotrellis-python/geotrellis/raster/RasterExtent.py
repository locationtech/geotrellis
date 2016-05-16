from __future__ import absolute_import
from geotrellis.spark.io.package_scala import ExceptionWithCause
from geotrellis.raster.GridDefinition import GridDefinition
from geotrellis.raster.Tile import Tile
from geotrellis.vector.Extent import Extent

import math

class GeoAttrsError(ExceptionWithCause):
    def __init__(self, msg, **kwargs):
        ExceptionWithCause.__init__(self, msg, **kwargs)

class RasterExtent(GridDefinition):
    def __init__(self, extent, cellwidth, cellheight, cols, rows):
        if cols < 0:
            raise GeoAttrsError("invalid cols: {0}".format(cols))
        if rows < 0:
            raise GeoAttrsError("invalid rows: {0}".format(rows))
        self.extent = extent
        self.cellwidth = cellwidth
        self.cellheight = cellheight
        self.cols = cols
        self.rows = rows

    def __eq__(self, other):
        if not isinstance(other, RasterExtent):
            return False
        return (self.extent == other.extent and
                self.cellwidth == other.cellwidth and
                self.cellheight == other.cellheight and
                self.cols == other.cols and
                self.rows == other.rows)

    def __hash__(self):
        return hash((self.extent, self.cellwidth, self.cellheight,
            self.cols, self.rows))

    @property
    def size(self):
        return self.cols * self.rows

    @property
    def dimensions(self):
        return (self.cols, self.rows)

    def mapToGrid(self, x, y = None):
        if y is None:
            mapCoord = x
            x, y = mapCoord
        col = int(math.floor((x - self.extent.xmin) / self.cellwidth))
        row = int(math.floor((self.extent.ymax - y) / self.cellheight))
        return (col, row)

    def mapXToGrid(self, x):
        return int(math.floor(self.mapXToGridDouble(x)))

    def mapXToGridDouble(self, x):
        return (x - self.extent.xmin) / self.cellwidth

    def mapYToGrid(self, y):
        return int(math.floor(self.mapYToGridDouble(y)))

    def mapYToGridDouble(self, y):
        return (self.extent.ymax) / self.cellheight

    def gridToMap(self, col, row):
        x = col * self.cellwidth + self.extent.xmin + (self.cellwidth / 2)
        y = self.extent.ymax - (row * self.cellheight) - (self.cellheight / 2)
        return (x, y)

    def gridColToMap(self, col):
        return col * self.cellwidth + self.extent.xmin + (self.cellwidth / 2)

    def gridRowToMap(self, row):
        return self.extent.ymax - (row * self.cellheight) - (self.cellheight / 2)

    def gridBoundsFor(self, subExtent, clamp = True):
        colMin, rowMin = self.mapToGrid(subExtent.xmin, subExtent.ymax)
        colMaxDouble = self.mapXToGridDouble(subExtent.xmax)
        if abs(colMaxDouble - math.floor(colMaxDouble)) < RasterExtent.epsilon:
            colMax = int(colMaxDouble) - 1
        else:
            colMax = int(colMaxDouble)

        rowMaxDouble = self.mapYToGridDouble(subExtent.ymin)
        if abs(rowMaxDouble - math.floor(rowMaxDouble)) < RasterExtent.epsilon:
            rowMax = int(rowMaxDouble) - 1
        else:
            rowMax = int(rowMaxDouble)

        if clamp:
            return GridBounds(
                    max(colMin, 0),
                    max(rowMin, 0),
                    min(colMax, cols-1),
                    min(rowMax, rows-1))
        else:
            return GridBounds(colMin, rowMin, colMax, rowMax)

    def combine(self, that):
        if self.cellwidth != that.cellwidth:
            raise GeoAttrsError("illegal cellwidths: {this} and {that}".format(
                this = self.cellwidth, that = that.cellwidth))
        if self.cellheight != that.cellheight:
            raise GeoAttrsError("illegal cellheights: {this} and {that}".format(
                this = self.cellheight, that = that.cellheight))

        newExtent = self.extent.combine(that.extent)
        newRows = int(math.ceil(newExtent.height / self.cellheight))
        newCols = int(math.ceil(newExtent.width / self.cellwidth))

        return RasterExtent(newExtent, self.cellwidth, self.cellheight, newCols, newRows)

    def withResolution(self, first, second = None):
        if second is None:
            cellSize = first
            targetCellWidth = cellSize.width
            targetCellHeight = cellSize.height
        else:
            targetCellWidth, targetCellHeight = first, second

        newCols = int(math.ceil((self.extent.xmax - self.extent.xmin) / targetCellWidth))
        newRows = int(math.ceil((self.extent.ymax - self.extent.ymin) / targetCellHeight))
        return RasterExtent(self.extent, targetCellWidth, targetCellHeight, newCols, newRows)

    def withDimensions(self, targetCols, targetRows):
        return RasterExtent.applyStatic(self.extent, targetCols, targetRows)

    def adjustTo(self, tileLayout):
        totalCols = tileLayout.tileCols * tileLayout.layoutCols
        totalRows = tileLayout.tileRows * tileLayout.layoutRows

        resampledExtent = Extent(self.extent.xmin,
                self.extent.ymax - (self.cellheight * totalRows),
                self.extent.xmin + (self.cellwidth * totalCols),
                self.extent.ymax)

        return RasterExtent(resampledExtent, self.cellwidth, self.cellheight, totalCols, totalRows)

    def rasterExtentFor(self, gridBounds):
        xminCenter, ymaxCenter = self.gridToMap(gridBounds.colMin, gridBounds.rowMin)
        xmaxCenter, yminCenter = self.gridToMap(gridBounds.colMax, gridBounds.rowMax)
        hcw, hch = self.cellwidth / 2, self.cellheight / 2
        e = Extent(xminCenter - hcw, yminCenter - hch, xmaxCenter + hcw, ymaxCenter + hch)
        return RasterExtent(e, self.cellwidth, self.cellheight, gridBounds.width, gridBounds.height)

    epsilon = 0.0000001

    @staticmethod
    def applyStatic(first, second, third = None):
        if third is None:
            if isinstance(first, Tile):
                tile = first
                extent = second
                return RasterExtent.applyStatic(extent, tile.cols, tile.rows)
            elif isinstance(second, Tile):
                extent = first
                tile = second
                return RasterExtent.applyStatic(extent, tile.cols, tile.rows)
            else:
                extent = first
                cellSize = second
                cols = int(extent.width / cellSize.width)
                rows = int(extent.height / cellSize.height)
                return RasterExtent(extent, cellSize.width, cellSize.height, cols, rows)
        else:
            extent = first
            cols, rows = second, third
            cw = extent.width / cols
            ch = extent.height / rows
            return RasterExtent(extent, cw, ch, cols, rows)
