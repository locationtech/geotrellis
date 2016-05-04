from geotrellis.raster.GridDefinition import GridDefinition

class GridExtent(GridDefinition):
    def __init__(self, extent, cellwidth, cellheight = None):
        if cellheight is None:
            cellSize = cellwidth
            cellwidth, cellheight = cellSize.width, cellSize.height
        self.extent = extent
        self.cellwidth = cellwidth
        self.cellheight = cellheight
    
    def __eq__(self, other):
        if not isinstance(other, GridExtent):
            return False
        return (self.extent == other.extent and
                self.cellwidth == other.cellwidth and
                self.cellheight == other.cellheight)

    def __hash__(self):
        return hash((self.extent, self.cellwidth, self.cellheight))
