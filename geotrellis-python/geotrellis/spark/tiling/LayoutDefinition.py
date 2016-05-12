from geotrellis.raster.GridDefinition import GridDefinition
from geotrellis.raster.TileLayout import TileLayout
#from geotrellis.vector.Extent import Extent
from geotrellis.spark.tiling.MapKeyTransform import MapKeyTransform

class LayoutDefinition(GridDefinition):
    def __init__(self, first, second, third = None):
        extent, tileLayout = _get_params(first, second, third)
        cellSize = tileLayout.cellSize(extent)
        cellwidth = cellSize.width
        cellheight = cellSize.height
        self._cellwidth = cellwidth
        self._cellheight = cellheight
        self.extent = extent
        self.tileLayout = tileLayout
        self._mapTransform = None

    @property
    def cellwidth(self):
        return self._cellwidth

    @property
    def cellheight(self):
        return self._cellheight

    @property
    def tileCols(self):
        return self.tileLayout.tileCols

    @property
    def tileRows(self):
        return self.tileLayout.tileRows

    @property
    def layoutCols(self):
        return self.tileLayout.layoutCols

    @property
    def layoutRows(self):
        return self.tileLayout.layoutRows

    @property
    def mapTransform(self):
        if self._mapTransform is not None:
            return self._mapTransform
        self._mapTransform = MapKeyTransform(self.extent, self.tileLayout.layoutDimensions)
        return self._mapTransform

    def __eq__(self, other):
        if not isinstance(other, LayoutDefinition):
            return False
        return (self.extent == other.extent and
                self.tileLayout == other.tileLayout)

    def __hash__(self):
        return hash((self.extent, self.tileLayout))

def _get_params(first, second, third):
    from geotrellis.vector.Extent import Extent
    if isinstance(first, Extent):
        return first, second
    grid = first
    tileCols = second
    tileRows = third if third is not None else second

    extent = grid.extent
    cellSize = grid.cellSize
    totalPixelWidth = float(extent.width) / cellSize.width
    totalPixelHeight = float(extent.height) / cellSize.height
    tileLayoutCols = int( math.ceil(totalPixelWidth / tileCols) )
    tileLayoutRows = int( math.ceil(totalPixelHeight / tileRows) )

    layout = TileLayout(tileLayoutCols, tileLayoutRows, tileCols, tileRows)
    layoutExtent = Extent(
            extent.xmin,
            extent.ymax - layout.totalRows * cellSize.height,
            extent.xmin + layout.totalCols * cellSize.width,
            extent.ymax)

    return layoutExtent, layout
