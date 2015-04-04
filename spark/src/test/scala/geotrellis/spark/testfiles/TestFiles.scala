package geotrellis.spark.testfiles

import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.FileUtil
import org.apache.hadoop.fs.Path
import org.apache.spark._
import geotrellis.spark._
import geotrellis.spark.io.hadoop._

object TestFiles extends Logging {
  val ZOOM_LEVEL = 8

  def catalog(implicit sc: SparkContext): RasterCatalog = {

    val conf = sc.hadoopConfiguration
    val localFS = new Path(System.getProperty("java.io.tmpdir")).getFileSystem(conf)
    val catalogPath = new Path(localFS.getWorkingDirectory, "src/test/resources/test-catalog")
    val needGenerate = !localFS.exists(catalogPath)
    val catalog = RasterCatalog(catalogPath)

    if (needGenerate) {
      logInfo(s"test-catalog empty, generating at $catalogPath")
      GenerateTestFiles.generate(catalog, sc)
    }

    catalog
  }
}

trait TestFiles { self: OnlyIfCanRunSpark =>
  lazy val reader = TestFiles.catalog.reader[SpatialKey]

  def testFile(layerName: String): RasterRDD[SpatialKey] = {
    reader.read(LayerId(layerName, TestFiles.ZOOM_LEVEL)).cache
  }

  def AllOnesTestFile =
    testFile("all-ones")

  def AllTwosTestFile =
    testFile("all-twos")

  def AllHundredsTestFile =
    testFile("all-hundreds")

  def IncreasingTestFile =
    testFile("increasing")

  def DecreasingTestFile =
    testFile("decreasing")

  def EveryOtherUndefinedTestFile =
    testFile("every-other-undefined")

  def EveryOther0Point99Else1Point01TestFile =
    testFile("every-other-0.99-else-1.01")

  def EveryOther1ElseMinus1TestFile =
    testFile("every-other-1-else-1")

  def Mod10000TestFile =
    testFile("mod-10000")
}
