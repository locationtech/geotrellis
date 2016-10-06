from __future__ import absolute_import
from geotrellis.spark.tiling.LayoutScheme import LayoutScheme, LayoutLevel
from geotrellis.spark.tiling.LayoutDefinition import LayoutDefinition
from geotrellis.raster.GridExtent import GridExtent

class FloatingLayoutScheme(LayoutScheme):
    DEFAULT_TILE_SIZE = 256
    def __init__(self, tileCols = None, tileRows = None):
        if tileRows is None:
            if tileCols is None:
                tileCols = FloaintLayoutScheme.DEFAULT_TILE_SIZE
                tileRows = FloaintLayoutScheme.DEFAULT_TILE_SIZE
            else:
                tileRows = tileCols
        self.tileCols = tileCols
        self.tileRows = tileRows

    def levelFor(self, extent, cellSize):
        return LayoutLevel(
                0,
                LayoutDefinition(
                    GridExtent(extent, cellSize),
                    self.tileCols,
                    self.tileRows))

    def zoomOut(level):
        raise Exception("zoomOut not supported for FloatingLayoutScheme")
    def zoomIn(level):
        raise Exception("zoomIn not supported for FloatingLayoutScheme")
