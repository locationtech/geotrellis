package geotrellis.spark
import geotrellis.spark.utils.SparkUtils

import org.apache.commons.io.FileUtils
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.scalatest._
import org.scalatest.BeforeAndAfterAll

import java.io.File

/*
 * These set of traits handle the creation and deletion of test directories on the local fs and hdfs,
 * It uses commons-io in at least one case (recursive directory deletion)
 */
trait TestEnvironment extends BeforeAndAfterAll {self: Suite =>
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


  /* 
   * Makes directory given a path. The parent directory is expected to exist
   * e.g., to make directory bar under /tmp/foo, call mkdir(new Path("/tmp/foo/bar"))
   * The parent directory is assumed to exist
   */
  def mkdir(dir: Path): Unit = {
   val handle = new File(dir.toUri())
    if (!handle.exists)
      handle.mkdirs()    
  }
  
  // clean up the test directory after the test
  // note that this afterAll is not inherited from BeforeAndAfterAll, its callers are
  override def afterAll() = FileUtils.deleteDirectory(new File(outputLocal.toUri()))

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