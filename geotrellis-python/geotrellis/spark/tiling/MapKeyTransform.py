from geotrellis.raster.GridBounds import GridBounds
from geotrellis.spark.SpatialKey import SpatialKey
from geotrellis.spark.tiling.package_scala import worldExtent
from geotrellis.vector.Extent import Extent
from geotrellis.proj4.CRS import CRS
from geotrellis.spark.tiling.LayoutScheme import LayoutLevel

class MapKeyTransform(object):
    def __init__(self, first, second, third = None):
        extent, layoutCols, layoutRows = _get_params(first, second, third)
        self.extent = extent
        self.layoutCols = layoutCols
        self.layoutRows = layoutRows
        self._tileWidth = None
        self._tileHeight = None

    @property
    def tileWidth(self):
        if self._tileWidth is not None:
            return self._tileWidth
        self._tileWidth = self.extent.width / layoutCols
        return self._tileWidth

    @property
    def tileHeight(self):
        if self._tileHeight is not None:
            return self._tileHeight
        self._tileHeight = self.extent.height / layoutRows
        return self._tileHeight

    def __call__(self, first, second = None):
        if second is not None and isinstance(first, float) and isinstance(second, float):
            x, y = first, second
            tcol = ((x - self.extent.xmin) / self.extent.width) * layoutCols
            trow = ((self.extent.ymax - y) / self.extent.height) * layoutRows
            return (int(tcol), int(trow))
        elif second is not None:
            col, row = first, second
            return Extent(
                    self.extent.xmin + col * self.tileWidth,
                    self.extent.ymax - (row+1) * self.tileHeight,
                    self.extent.xmin + (col+1) * self.tileWidth,
                    self.extent.ymax - row * self.tileHeight)
        elif isinstance(first, SpatialKey):
            key = first
            self(key.col, key.row)
        elif isinstance(first, GridBounds):
            gridBounds = first
            e1 = self(gridBounds.colMin, gridBounds.rowMin)
            e2 = self(gridBounds.colMax, gridBounds.rowMax)
            return e1.expandToInclude(e2)
        elif isinstance(first, Extent):
            otherExtent = first
            key = self(otherExtent.xmin, otherExtent.ymax)
            colMin = key.col
            rowMin = key.row
            d = (otherExtent.xmax - self.extent.xmin) / (self.extent.width / self.layoutCols)
            if d == math.floor(d) and d != colMin:
                colMax = int(d) - 1
            else:
                colMax = int(d)

            d = (self.extent.ymax - otherExtent.ymin) / (self.extent.height / self.layoutRows)
            if d == math.floor(d) and d != rowMin:
                rowMax = int(d) - 1
            else:
                rowMax = int(d)

            return GridBounds(colMin, rowMin, colMax, rowMax)
        else:
            raise Exception("wrong params: ({first}, {second})".format(
                first = first,
                second = second))

def _get_params(first, second, third):
    if isinstance(first, CRS) and isinstance(second, LayoutLevel) and third is None:
        crs = first
        level = second
        return worldExtent(crs), level.layout.layoutCols, level.layout.layoutRows
    elif isinstance(first, CRS) and isinstance(second, tuple) and third is None:
        crs = first
        layoutDimensions = second
        return worldExtent(crs), layoutDimensions[0], layoutDimensions[1]
    elif isinstance(first, CRS) and isinstance(second, int) and isinstance(third, int):
        crs = first
        layoutCols = second
        layoutRows = third
        return worldExtent(crs), layoutCols, layoutRows
    elif isinstance(first, Extent) and isinstance(second, tuple) and third is None:
        extent = first
        layoutDimensions = second
        return extent, layoutDimensions[0], layoutDimensions[1]
    elif isinstance(first, Extent) and isinstance(second, int) and isinstance(third, int):
        return first, second, third
    else:
        raise Exception("wrong params ({one}, {two}, {three})".format(
            one = first, two = second, three = third))
