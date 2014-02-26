package geotrellis.spark
import geotrellis.spark.utils.SparkUtils

import org.apache.commons.io.FileUtils
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.scalatest._
import org.scalatest.BeforeAndAfterAll

import java.io.File

/*
 * These set of traits handle the creation and deletion of test directories on the local fs and hdfs,
 * and creation/freeing of SparkContext.  
 * 
 * It uses commons-io in at least one case (recursive directory deletion)
 * 
 * It is split into three traits   
 * TestEnvironmentBase - which handles the creation/deletion of various test directories
 * TestEnvironment - which inherits from FunSpec 
 * TestEnvironmentFixture - which inherits from fixture.FunSpec, and provides withFixture implementation 
 * to handle creation/freeing of SparkContext for tests that use Spark. 
 * 
 * The reason it is split into the three traits is because there wasn't a clear way to get 
 * a single trait to inherit from either/both FunSpec and fixture.FunSpec. This was the best 
 * way to reduce as much as possible code duplication. The only duplication is the definition 
 * of beforeAll/afterAll
 * 
 * See http://grokbase.com/t/gg/scala-user/12ak3cacsm/how-do-i-override-and-select-a-base-method-from-two-conflicting-traits
 * In this case, FunSpec and fixture.FunSpec end up conflicting and some of the conflicting members are vals,
 * which seem to be problematic (compared to defs) in resolving conflicts
 */

trait TestEnvironmentBase {
  // get the name of the class which mixes in this trait
  val name = this.getClass.getName

  // a hadoop configuration
  val conf = SparkUtils.createHadoopConfiguration

  // cache the local file system, no tests should have to call getFileSystem
  val localFS = getLocalFS

  // e.g., root directory on local file system for source data (e.g., tiffs)
  // localFS.getWorkingDirectory is for e.g., /home/jdoe/git/geotrellis
  val inputHome = new Path(localFS.getWorkingDirectory, "geotrellis-spark/src/test/resources")

  // test directory paths on local and hdfs 
  // outputHomeLocal - root directory of all tests on the local file system (e.g., file:///tmp/testFiles)
  // outputHomeHdfs - root directory of all tests on hdfs (e.g., hdfs:///tmp)
  // outputLocal - directory of this particular test (e.g., file:///tmp/testFiles/geotrellis.spark.cmd.IngestSpec)
  val (outputHomeLocal, outputHomeHdfs, outputLocal) = setupTestDirs

  // clean up the test directory after the test
  // note that this afterAll is not inherited from BeforeAndAfterAll, its callers are
  protected def afterAll() = FileUtils.deleteDirectory(new File(outputLocal.toUri()))

  // root directory name on both local file system and hdfs for all tests
  private final val outputHome = "testFiles"

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

trait TestEnvironment extends TestEnvironmentBase with FunSpec with BeforeAndAfterAll {

  // clean up the test directory after the test
  override def afterAll() = super[TestEnvironmentBase].afterAll()

}

trait TestEnvironmentFixture extends TestEnvironmentBase with fixture.FunSpec with BeforeAndAfterAll {

  type FixtureParam = SparkContext

  def withFixture(test: OneArgTest) {
    // we don't call the version in SparkUtils as that moves the jar file dependency around
    // and that is not needed for local spark context
    System.setProperty("spark.master.port", "0")
    val sc = new SparkContext("local",test.name)
    try {
      test(sc)
    } finally {
      sc.stop
      // To avoid Akka rebinding to the same port, since it doesn't unbind immediately on shutdown
      System.clearProperty("spark.driver.port")
    }
  }

  // clean up the test directory after the test
  override def afterAll() = super[TestEnvironmentBase].afterAll()
}