from __future__ import absolute_import
from spec import Spec
from nose import tools
from tests.geotrellis_test.spark.io.PersistenceSpec import _PersistenceSpec
from tests.geotrellis_test.spark.io.SpaceTimeKeyIndexMethods import _SpaceTimeKeyIndexMethods
from tests.geotrellis_test.spark.TestEnvironment import _TestEnvironment
from tests.geotrellis_test.spark.testfiles.TestFiles import _TestFiles
from geotrellis.spark.SpaceTimeKey import SpaceTimeKey
from geotrellis.raster.Tile import Tile
from geotrellis.raster.FloatArrayTile import FloatArrayTile
from geotrellis.spark.TileLayerMetadata import TileLayerMetadata
from geotrellis.spark.io.file.FileLayerReader import FileLayerReader
from geotrellis.spark.io.file.FileLayerWriter import FileLayerWriter
from geotrellis.spark.io.file.FileValueReader import file_value_reader

@tools.istest
class FileSpaceTimeSpec(
        Spec,
        # TODO Tile doesn't have implicits to get AvroRecordCodec from
        #_PersistenceSpec[SpaceTimeKey, Tile, TileLayerMetadata[SpatceTimeey]],
        _PersistenceSpec[SpaceTimeKey, FloatArrayTile, TileLayerMetadata[SpaceTimeKey]],
        _SpaceTimeKeyIndexMethods,
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
        return self.CoordinateSpaceTime
