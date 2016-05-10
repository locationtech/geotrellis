from spec import Spec
from tests.geotrellis.spark.io.PersistenceSpec import PersistenceSpec
from tests.geotrellis.spark.io.SpatialKeyIndexMethods import _SpatialKeyIndexMethods
from tests.geotrellis.spark.TestEnvironment import _TestEnvironment
from tests.geotrellis.spark.testfiles.TestFiles import _TestFiles
from geotrellis.spark.SpatialKey import SpatialKey
from geotrellis.raster.Tile import Tile
from geotrellis.spark.TileLayerMetadata import TileLayerMetadata
from geotrellis.spark.io.file.FileLayerReader import FileLayerReader
from geotrellis.spark.io.file.FileLayerWriter import FileLayerWriter
from geotrellis.spark.LayerId import LayerId
from geotrellis.spark.io.index.ZCurveKeyIndexMethod import ZCurveKeyIndexMethod

class FileSpatialSpec(
        Spec,
        PersistenceSpec[SpatialKey, Tile, TileLayerMetadata],
        _SpatialKeyIndexMethods,
        _TestEnvironment,
        _TestFiles):

    @property
    def reader(self):
        return FileLayerReader(self.outputLocalPath)

    @property
    def writer(self):
        return FileLayerWriter(self.outputLocalPath)

    class inner(object):
        "Filesystem layer names"

        def test_bad_characters(self):
            "should not throw with bad characters in name"
            layer = self.AllOnesTestFile
            layerId = LayerId("Some!layer:%@~`{}id", 10)
            print(self.outputLocalPath)
            self.writer.write(SpatialKey, Tile, TileLayerMetadata, layerId, layer, ZCurveKeyIndexMethod)
            backin = self.reader.read(SpatialKey, Tile, TileLayerMetadata, layerId)
