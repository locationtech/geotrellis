package geotrellis.spark.io.file

import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark._
import geotrellis.spark.io.avro.codecs._
import com.github.nscala_time.time.Imports._

abstract class FileSpaceTimeSpec
    extends PersistenceSpec[SpaceTimeKey, Tile, RasterMetadata]
    with TestEnvironment
    with TestFiles
    with CoordinateSpaceTimeTests {
  lazy val reader = FileLayerReader[SpaceTimeKey, Tile, RasterMetadata](outputLocalPath)
  lazy val deleter = FileLayerDeleter[SpaceTimeKey, Tile, RasterMetadata](outputLocalPath)
  lazy val copier = FileLayerCopier[SpaceTimeKey, Tile, RasterMetadata](outputLocalPath)
  lazy val mover  = FileLayerMover[SpaceTimeKey, Tile, RasterMetadata](outputLocalPath)
  lazy val reindexer = FileLayerReindexer[SpaceTimeKey, Tile, RasterMetadata](outputLocalPath, ZCurveKeyIndexMethod.byPattern("YMM"))
  lazy val tiles = FileTileReader[SpaceTimeKey, Tile](outputLocalPath)
  lazy val sample =  CoordinateSpaceTime
}

class FileSpaceTimeZCurveByYearSpec extends FileSpaceTimeSpec {
  lazy val writer = FileLayerWriter[SpaceTimeKey, Tile, RasterMetadata](outputLocalPath, ZCurveKeyIndexMethod.byYear)
}

class FileSpaceTimeZCurveByFuncSpec extends FileSpaceTimeSpec {
  lazy val writer = FileLayerWriter[SpaceTimeKey, Tile, RasterMetadata](outputLocalPath, ZCurveKeyIndexMethod.by{ x =>  if (x < DateTime.now) 1 else 0 })
}

class FileSpaceTimeHilbertSpec extends FileSpaceTimeSpec {
  lazy val writer = FileLayerWriter[SpaceTimeKey, Tile, RasterMetadata](outputLocalPath, HilbertKeyIndexMethod(DateTime.now - 20.years, DateTime.now, 4))
}

class FileSpaceTimeHilbertWithResolutionSpec extends FileSpaceTimeSpec {
  lazy val writer = FileLayerWriter[SpaceTimeKey, Tile, RasterMetadata](outputLocalPath,  HilbertKeyIndexMethod(2))
}
