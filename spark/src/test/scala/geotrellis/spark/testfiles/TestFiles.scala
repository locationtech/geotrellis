package geotrellis.spark.testfiles

import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.FileUtil
import org.apache.hadoop.fs.Path
import org.apache.spark._
import geotrellis.spark._
import geotrellis.spark.io.hadoop._

object TestFiles extends Logging {
  val ZOOM_LEVEL = 8

  def catalog(implicit sc: SparkContext): HadoopRasterCatalog = {

    val conf = sc.hadoopConfiguration
    val localFS = new Path(System.getProperty("java.io.tmpdir")).getFileSystem(conf)
    val catalogPath = new Path(localFS.getWorkingDirectory, "src/test/resources/test-catalog")
    val needGenerate = !localFS.exists(catalogPath)

    val defaultParams = 
      HadoopRasterCatalog.BaseParams
        .withKeyParams[SpaceTimeKey]("spacetime")
        .withKeyParams[SpatialKey]("spatial")

    val catalog = HadoopRasterCatalog(catalogPath, defaultParams)

    if (needGenerate) {
      logInfo(s"test-catalog empty, generating at $catalogPath")
      GenerateTestFiles.generate(catalog, sc)
    }

    catalog
  }
}

trait TestFiles { self: OnlyIfCanRunSpark =>
  lazy val spatialReader = TestFiles.catalog.reader[SpatialKey]
  lazy val spaceTimeReader = TestFiles.catalog.reader[SpaceTimeKey]

  def spatialTestFile(layerName: String): RasterRDD[SpatialKey] = {
    spatialReader.read(LayerId(layerName, TestFiles.ZOOM_LEVEL)).cache
  }

  def spaceTimeTestFile(layerName: String): RasterRDD[SpaceTimeKey] = {
    spaceTimeReader.read(LayerId(layerName, TestFiles.ZOOM_LEVEL)).cache
  }

  def AllOnesTestFile =
    spatialTestFile("all-ones")

  def AllTwosTestFile =
    spatialTestFile("all-twos")

  def AllHundredsTestFile =
    spatialTestFile("all-hundreds")

  def IncreasingTestFile =
    spatialTestFile("increasing")

  def DecreasingTestFile =
    spatialTestFile("decreasing")

  def EveryOtherUndefinedTestFile =
    spatialTestFile("every-other-undefined")

  def EveryOther0Point99Else1Point01TestFile =
    spatialTestFile("every-other-0.99-else-1.01")

  def EveryOther1ElseMinus1TestFile =
    spatialTestFile("every-other-1-else-1")

  def Mod10000TestFile =
    spatialTestFile("mod-10000")

  def AllOnesSpaceTime =
    spaceTimeTestFile("spacetime-all-ones")

  def AllTwosSpaceTime =
    spaceTimeTestFile("spacetime-all-twos")

  def AllHundredsSpaceTime =
    spaceTimeTestFile("spacetime-all-hundreds")

  /** This test raster id's each tile value as COL ROW TIME, by the
    * equation COL * 100 + ROW * 10 + TIME * 1. So col 3, row 5, time 2 would
    * be value 352
    */
  def CoordinateSpaceTime =
    spaceTimeTestFile("spacetime-coordinates")
}
