from geotrellis.raster.CellGrid import CellGrid
from geotrellis.raster.ArrayMultibandTile import ArrayMultibandTile

class MultibandTile(CellGrid):
    def subsetBands(self, *bandSequence):
        if len(bandSequence) == 1 and isinstance(bandSequence[0], list):
            bandSequence = bandSequence[0]
        return self._subsetBands(bandSequence)

    @staticmethod
    def applyStatic(*bands):
        if len(bands) == 1 and isinstance(bands[0], list):
            bands = bands[0]
        return ArrayMultibandTile.applyStatic(bands)
