from __future__ import absolute_import
from spec import Spec
from nose import tools
from tests.geotrellis_test.spark.io.PersistenceSpec import _PersistenceSpec
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
from geotrellis.spark.io.file.FileValueReader import file_value_reader

@tools.nottest
class FileSpatialSpec(
        Spec,
        # TODO Tile doesn't have implicits to get AvroRecordCodec from
        #_PersistenceSpec[SpatialKey, Tile, TileLayerMetadata[SpatialKey]],
        _PersistenceSpec[SpatialKey, FloatArrayTile, TileLayerMetadata[SpatialKey]],
        _SpatialKeyIndexMethods,
        _TestFiles,
        _TestEnvironment,
        object):

    @property
    def reader(self):
        return FileLayerReader(self.sc, self.outputLocalPath)

    @property
    def writer(self):
        return FileLayerWriter(self.outputLocalPath)

    @property
    def tiles(self):
        return file_value_reader(self.outputLocalPath)

    @property
    def sample(self):
        return self.AllOnesTestFile

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

