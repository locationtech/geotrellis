from __future__ import absolute_import
from geotrellis.raster.CellSize import CellSize
#from geotrellis.raster.RasterExtent import RasterExtent, GeoAttrsError
from geotrellis.vector.Extent import Extent
#from geotrellis.raster.GridExtent import GridExtent
from geotrellis.raster.package_scala import INTMAX

class GridDefinition(object):
    @property
    def cellSize(self):
        return CellSize(self.cellwidth, self.cellheight)

    @property
    def toGridExtent(self):
        from geotrellis.raster.GridExtent import GridExtent
        return GridExtent(self.extent, self.cellwidth, self.cellheight)

    def toRasterExtent(self):
        from geotrellis.raster.RasterExtent import RasterExtent, GeoAttrsError
        targetCols = round( float(self.extent.width) / self.cellwidth)
        targetRows = round( float(self.extent.height) / self.cellheight)
        if targetCols > INTMAX:
            raise GeoAttrsError(
                    "Cannot convert GridExtent into a RasterExtent:" +
                    " number of columns exceeds maximum integer value " +
                    "({tc} > {mx})".format(tc = targetCols, mx = INTMAX))
        if targetRows > INTMAX:
            raise GeoAttrsError(
                    "Cannot convert GridExtent into a RasterExtent:" +
                    " number of rows exceeds maximum integer value " +
                    "({tr} > {mx})".format(tc = targetRows, mx = INTMAX))
        return RasterExtent(self.extent, self.cellwidth, self.cellheight, targetCols, targetRows)

    def createAlignedGridExtent(self, targetExtent):
        from geotrellis.raster.GridExtent import GridExtent
        xmin = self.extent.xmin + (math.floor((targetExtent.xmin - self.extent.xmin) / self.cellwidth) * self.cellwidth)
        xmax = self.extent.xmax - (math.floor((self.extent.xmax - targetExtent.xmax) / self.cellwidth) * self.cellwidth)
        ymin = self.extent.ymin + (math.floor((targetExtent.ymin - self.extent.ymin) / self.cellheight) * self.cellheight)
        ymax = self.extent.ymax - (math.floor((self.extent.ymax - targetExtent.ymax) / self.cellheight) * self.cellheight)

        return GridExtent(Extent(xmin, ymin, xmax, ymax), self.cellwidth, self.cellheight)

    def createAlignedRasterExtent(self, targetExtent):
        return self.createAlignedGridExtent(targetExtent).toRasterExtent()

    def extentFor(self, gridBounds, clamp = True):
        xmin = gridBounds.colMin * self.cellwidth + self.extent.xmin
        ymax = self.extent.ymax - (gridBounds.rowMin * self.cellheight)
        xmax = xmin + (gridBounds.width * self.cellwidth)
        ymin = ymax - (gridBounds.height * self.cellheight)

        if clamp:
            return Extent(
                    max(min(xmin, self.extent.xmax), self.extent.xmin),
                    max(min(ymin, self.extent.ymax), self.extent.ymin),
                    max(min(xmax, self.extent.xmax), self.extent.xmin),
                    max(min(xmax, self.extent.ymax), self.extent.ymin))
        else:
            return Extent(xmin, ymin, xmax, ymax)
