from __future__ import absolute_import
from spec import Spec
from tests.geotrellis_test.spark.io.PersistenceSpec import PersistenceSpec
from tests.geotrellis_test.spark.io.SpatialKeyIndexMethods import _SpatialKeyIndexMethods
from tests.geotrellis_test.spark.TestEnvironment import _TestEnvironment
from tests.geotrellis_test.spark.testfiles.TestFiles import _TestFiles
from geotrellis.spark.SpatialKey import SpatialKey
from geotrellis.raster.Tile import Tile
from geotrellis.raster.FloatArrayTile import FloatArrayTile
from geotrellis.spark.TileLayerMetadata import TileLayerMetadata
from geotrellis.spark.io.file.FileLayerReader import FileLayerReader
from geotrellis.spark.io.file.FileLayerWriter import FileLayerWriter
from geotrellis.spark.LayerId import LayerId
from geotrellis.spark.io.index.ZCurveKeyIndexMethod import ZCurveKeyIndexMethod

class FileSpatialSpec(
        Spec,
        PersistenceSpec[SpatialKey, Tile, TileLayerMetadata[SpatialKey]],
        _SpatialKeyIndexMethods,
        _TestFiles,
        _TestEnvironment):

    @property
    def reader(self):
        return FileLayerReader(self.sc, self.outputLocalPath)

    @property
    def writer(self):
        return FileLayerWriter(self.outputLocalPath)

    def test_persistence_spec_checks(self):
        "persistence spec checks"
        pers_spec = PersistenceSpec[SpatialKey, Tile, TileLayerMetadata[SpatialKey]]
        pers_spec.test_persistence_spec_checks(self)

    class inner(object):
        "Filesystem layer names"

        def test_bad_characters(self):
            "should not throw with bad characters in name"
            layer = self.AllOnesTestFile
            layerId = LayerId("Some!layer:%@~`{}id", 10)
            print(self.outputLocalPath)
            #self.writer.write(SpatialKey, Tile, TileLayerMetadata, layerId, layer, ZCurveKeyIndexMethod.spatialKeyIndexMethod())
            self.writer.write(SpatialKey, FloatArrayTile, TileLayerMetadata[SpatialKey], layerId, layer, ZCurveKeyIndexMethod.spatialKeyIndexMethod())
            #backin = self.reader.read(SpatialKey, Tile, TileLayerMetadata, layerId)
            backin = self.reader.read(SpatialKey, FloatArrayTile, TileLayerMetadata[SpatialKey], layerId)

