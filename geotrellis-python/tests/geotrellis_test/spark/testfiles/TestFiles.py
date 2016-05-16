from __future__ import absolute_import
from tests.geotrellis_test.spark.TestEnvironment import _TestEnvironment
from tests.geotrellis_test.spark.testfiles.SpatialTestFileValues import (
        ConstantSpatialTiles,
        IncreasingSpatialTiles,
        DecreasingSpatialTiles,
        EveryOtherSpatialTiles,
        ModSpatialTiles)
from geotrellis.raster.GridBounds import GridBounds
from geotrellis.raster.TileLayout import TileLayout
from geotrellis.raster.CellType import FloatConstantNoDataCellType
from geotrellis.spark.tiling.MapKeyTransform import MapKeyTransform
from geotrellis.spark.tiling.LayoutDefinition import LayoutDefinition
from geotrellis.spark.tiling.package_scala import worldExtent
from geotrellis.proj4.LatLng import LatLng
from geotrellis.spark.KeyBounds import KeyBounds
from geotrellis.spark.SpatialKey import SpatialKey
from geotrellis.spark.TileLayerMetadata import TileLayerMetadata
from geotrellis.spark.ContextRDD import ContextRDD

class _TestFiles(_TestEnvironment):

    _AllOnesTestFile = None
    _AllTwosTestFile = None
    _AllHundredsTestFile = None
    _IncreasingTestFile = None
    _DecreasingTestFile = None
    _EveryOtherUndefinedTestFile = None
    _EveryOther0Point99Else1Point01TestFile = None
    _EveryOther1ElseMinus1TestFile = None
    _Mod10000TestFile = None
    _AllOnesSpaceTime = None
    _AllTwosSpaceTime = None
    _AllHundredsSpaceTime = None
    _CoordinateSpaceTime = None

    ZOOM_LEVEL = 8
    partitionCount = 4

    @staticmethod
    def generateSpatial(layerName, sc):
        def generateMetadata():
            cellType = FloatConstantNoDataCellType
            crs = LatLng
            tileLayout = TileLayout(8,8,3,4)
            mapTransform = MapKeyTransform(crs, tileLayout.layoutDimensions)
            gridBounds = GridBounds(1,1,6,7)
            extent = mapTransform(gridBounds)
            keyBounds = KeyBounds(SpatialKey(1,1), SpatialKey(6,7))
            return TileLayerMetadata(cellType, LayoutDefinition(worldExtent(crs), tileLayout), extent, crs, keyBounds)
            
        md = generateMetadata()
        gridBounds = md.gridBounds
        tileLayout = md.tileLayout
        def generateSpatialTestFile():
            if layerName == "all-ones":
                return ConstantSpatialTiles (tileLayout, 1)
            elif layerName == "all-twos":
                return ConstantSpatialTiles (tileLayout, 2)
            elif layerName == "all-hundreds":
                return ConstantSpatialTiles (tileLayout, 100)
            elif layerName == "increasing":
                return IncreasingSpatialTiles (tileLayout, gridBounds)
            elif layerName == "decreasing":
                return DecreasingSpatialTiles (tileLayout, gridBounds)
            elif layerName == "every-other-undefined":
                return EveryOtherSpatialTiles (tileLayout, gridBounds, float("nan"), 0.0)
            elif layerName == "every-other-0.99-else-1.01":
                return EveryOtherSpatialTiles (tileLayout, gridBounds, 0.99, 1.01)
            elif layerName == "every-other-1-else-1":
                return EveryOtherSpatialTiles (tileLayout, gridBounds, - 1, 1)
            elif layerName == "mod-10000":
                return ModSpatialTiles (tileLayout, gridBounds, 10000)
        spatialTestFile = generateSpatialTestFile()
        tiles = []
        for row in xrange(gridBounds.rowMin, gridBounds.rowMax+1):
            for col in xrange(gridBounds.colMin, gridBounds.colMax+1):
                key = SpatialKey(col, row)
                tile = spatialTestFile(key)
                tup = (key, tile)
                tiles.append(tup)
        return ContextRDD(sc.parallelize(tiles, _TestFiles.partitionCount), md)

    def spatialTestFile(self, name):
        return _TestFiles.generateSpatial(name, self._sc())

    def spaceTimeTestFile(self, name):
        # return _TestFiles.generateSpaceTime(name)
        pass

    @property
    def AllOnesTestFile(self):
        if self._AllOnesTestFile:
            return self._AllOnesTestFile
        self._AllOnesTestFile = self.spatialTestFile("all-ones")
        return self._AllOnesTestFile

    @property
    def AllTwosTestFile(self):
        if self._AllTwosTestFile:
            return self._AllTwosTestFile
        self._AllTwosTestFile = self.spatialTestFile("all-twos")
        return self._AllTwosTestFile

    @property
    def AllHundredsTestFile(self):
        if self._AllHundredsTestFile:
            return self._AllHundredsTestFile
        self._AllHundredsTestFile = self.spatialTestFile("all-hundreds")
        return self._AllHundredsTestFile

    @property
    def IncreasingTestFile(self):
        if self._IncreasingTestFile:
            return self._IncreasingTestFile
        self._IncreasingTestFile = self.spatialTestFile("increasing")
        return self._IncreasingTestFile

    @property
    def DecreasingTestFile(self):
        if self._DecreasingTestFile:
            return self._DecreasingTestFile
        self._DecreasingTestFile = self.spatialTestFile("decreasing")
        return self._DecreasingTestFile

    @property
    def EveryOtherUndefinedTestFile(self):
        if self._EveryOtherUndefinedTestFile:
            return self._EveryOtherUndefinedTestFile
        self._EveryOtherUndefinedTestFile = self.spatialTestFile("every-other-undefined")
        return self._EveryOtherUndefinedTestFile

    @property
    def EveryOther0Point99Else1Point01TestFile(self):
        if self._EveryOther0Point99Else1Point01TestFile:
            return self._EveryOther0Point99Else1Point01TestFile
        self._EveryOther0Point99Else1Point01TestFile = self.spatialTestFile("every-other-0.99-else-1.01")
        return self._EveryOther0Point99Else1Point01TestFile

    @property
    def EveryOther1ElseMinus1TestFile(self):
        if self._EveryOther1ElseMinus1TestFile:
            return self._EveryOther1ElseMinus1TestFile
        self._EveryOther1ElseMinus1TestFile = self.spatialTestFile("every-other-1-else-1")
        return self._EveryOther1ElseMinus1TestFile

    @property
    def Mod10000TestFile(self):
        if self._Mod10000TestFile:
            return self._Mod10000TestFile
        self._Mod10000TestFile = self.spatialTestFile("mod-10000")
        return self._Mod10000TestFile

    @property
    def AllOnesSpaceTime(self):
        if self._AllOnesSpaceTime:
            return self._AllOnesSpaceTime
        self._AllOnesSpaceTime = self.spaceTimeTestFile("spacetime-all-ones")
        return self._AllOnesSpaceTime

    @property
    def AllTwosSpaceTime(self):
        if self._AllTwosSpaceTime:
            return self._AllTwosSpaceTime
        self._AllTwosSpaceTime = self.spaceTimeTestFile("spacetime-all-twos")
        return self._AllTwosSpaceTime

    @property
    def AllHundredsSpaceTime(self):
        if self._AllHundredsSpaceTime:
            return self._AllHundredsSpaceTime
        self._AllHundredsSpaceTime = self.spaceTimeTestFile("spacetime-all-hundreds")
        return self._AllHundredsSpaceTime

    @property
    def CoordinateSpaceTime(self):
        if self._CoordinateSpaceTime:
            return self._CoordinateSpaceTime
        self._CoordinateSpaceTime = self.spaceTimeTestFile("spacetime-coordinates")
        return self._CoordinateSpaceTime
