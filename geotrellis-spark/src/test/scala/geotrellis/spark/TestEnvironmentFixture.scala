package geotrellis.spark
import org.apache.commons.io.FileUtils
import org.scalatest.BeforeAndAfterAll
import org.scalatest.fixture.FunSpec
import java.io.File
import java.nio.file.FileSystems
import org.apache.hadoop.fs.Path
import org.apache.hadoop.fs.FileSystem
import geotrellis.spark.utils.SparkUtils
import org.apache.spark.SparkContext

/*
 * This trait sets up the test directories on the local fs and hdfs 
 * 
 * It uses commons-io in at least one case (recursive directory deletion)
 */
trait TestEnvironmentFixture extends FunSpec with BeforeAndAfterAll {

  type FixtureParam = SparkContext

  def withFixture(test: OneArgTest) {
    val sc = SparkUtils.createSparkContext("local", "some name")
    try {
      test(sc)
    } finally {
      sc.stop
      // To avoid Akka rebinding to the same port, since it doesn't unbind immediately on shutdown
      System.clearProperty("spark.driver.port")
    }
  }

  // get the name of the class which mixes in this trait
  val name = this.getClass.getName

  // a hadoop configuration
  val conf = SparkUtils.createHadoopConfiguration

  val localFS = getLocalFS

  // e.g., root directory on local file system for source data (e.g., tiffs)
  // localFS.getWorkingDirectory is for e.g., /home/jdoe/git/geotrellis
  final val inputHome = new Path(localFS.getWorkingDirectory, "geotrellis-spark/src/test/resources")

  // root directory name on both local file system and hdfs for all tests
  private final val outputHome = "testFiles"

  // test directory paths on local and hdfs 
  // outputHomeLocal - root directory of all tests on the local file system (e.g., file:///tmp/testFiles)
  // outputHomeHdfs - root directory of all tests on hdfs (e.g., hdfs:///tmp)
  // outputLocal - directory of this particular test (e.g., file:///tmp/testFiles/geotrellis.spark.cmd.IngestSpec)
  val (outputHomeLocal, outputHomeHdfs, outputLocal) = setupTestDirs

  //override def beforeAll {
  //}

  override def afterAll =
    FileUtils.deleteDirectory(new File(outputLocal.toUri()))

  private def getLocalFS: FileSystem = new Path(System.getProperty("java.io.tmpdir")).getFileSystem(conf)

  private def setupTestDirs: (Path, Path, Path) = {
    val tmpDir = System.getProperty("java.io.tmpdir")

    val outputHomeLocalHandle = new File(tmpDir, outputHome)
    if (!outputHomeLocalHandle.exists)
      outputHomeLocalHandle.mkdirs()

    val hadoopTmpDir = conf.get("hadoop.tmp.dir", "/tmp")

    // file handle to the test directory on local file system
    val outputLocalHandle = new File(outputHomeLocalHandle.toString(), name)
    if (!outputLocalHandle.exists)
      outputLocalHandle.mkdirs()

    (new Path(outputHomeLocalHandle.toURI()), new Path(hadoopTmpDir), new Path(outputLocalHandle.toURI()))
  }
}