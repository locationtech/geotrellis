/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.testkit

import geotrellis.layers._
import geotrellis.layers.hadoop.HdfsUtils
import geotrellis.spark.util.SparkUtils
import geotrellis.spark.store.kryo.KryoRegistrator

import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.FileUtil
import org.apache.hadoop.fs.Path
import org.apache.hadoop.conf.Configuration
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SparkSession
import org.apache.spark.serializer.KryoSerializer
import org.scalatest._
import org.scalatest.BeforeAndAfterAll

import java.io.File
import scala.collection.mutable
import scala.util.Properties

object TestEnvironment {
  def getLocalFS(conf: Configuration): FileSystem = new Path(System.getProperty("java.io.tmpdir")).getFileSystem(conf)

  def inputHome: Path = {
    val conf = SparkUtils.hadoopConfiguration
    val localFS = getLocalFS(conf)
    new Path(localFS.getWorkingDirectory, "spark/src/test/resources/")
  }
}

/*
 * These set of traits handle the creation and deletion of test directories on the local fs and hdfs,
 * It uses commons-io in at least one case (recursive directory deletion)
 */
trait TestEnvironment extends BeforeAndAfterAll
  with TileLayerRDDBuilders
  with TileLayerRDDMatchers
  with OpAsserter
{ self: Suite with BeforeAndAfterAll =>

  /** Subclasses can override this to change the Spark master specification */
  def sparkMaster = "local[2]"

  private lazy val afterAlls = mutable.ListBuffer[() => Unit]()
  def registerAfterAll(f: () => Unit): Unit =
    afterAlls += f

  def setKryoRegistrator(conf: SparkConf): Unit =
    conf.set("spark.kryo.registrator", classOf[KryoRegistrator].getName)

  lazy val _ssc: SparkSession = {
    System.setProperty("spark.driver.port", "0")
    System.setProperty("spark.hostPort", "0")
    System.setProperty("spark.ui.enabled", "false")

    val conf = new SparkConf()
    conf
      .setMaster(sparkMaster)
      .setAppName("Test Context")
      .set("spark.default.parallelism", "4")

    // Shortcut out of using Kryo serialization if we want to test against
    // java serialization.
    if(Properties.envOrNone("GEOTRELLIS_USE_JAVA_SER").isEmpty) {
      conf
        .set("spark.serializer", classOf[KryoSerializer].getName)
        .set("spark.kryoserializer.buffer.max", "500m")
        .set("spark.kryo.registrationRequired", "false")
      setKryoRegistrator(conf)
    }

    val sparkContext = SparkSession.builder().config(conf).getOrCreate()

    System.clearProperty("spark.driver.port")
    System.clearProperty("spark.hostPort")
    System.clearProperty("spark.ui.enabled")

    sparkContext
  }

  lazy val _sc: SparkContext = _ssc.sparkContext

  implicit def ssc: SparkSession = _ssc
  implicit def sc: SparkContext = _sc

  // get the name of the class which mixes in this trait
  val name = this.getClass.getName

  // a hadoop configuration
  val conf = SparkUtils.hadoopConfiguration

  // cache the local file system, no tests should have to call getFileSystem
  val localFS = TestEnvironment.getLocalFS(conf)

  // e.g., root directory on local file system for source data (e.g., tiffs)
  // localFS.getWorkingDirectory is for e.g., /home/jdoe/git/geotrellis
  val inputHome = TestEnvironment.inputHome
  val inputHomeLocalPath = inputHome.toUri.getPath

  // test directory paths on local and hdfs
  // outputHomeLocal - root directory of all tests on the local file system (e.g., file:///tmp/testFiles)
  // outputHomeHdfs - root directory of all tests on hdfs (e.g., hdfs:///tmp)
  // outputLocal - directory of this particular test (e.g., file:///tmp/testFiles/geotrellis.spark.cmd.IngestSpec)
  // outputLocalPath - Local file path of directory of this particular test (e.g., /tmp/testFiles/geotrellis.spark.cmd.IngestSpec)
  val (outputHomeLocal, outputHomeHdfs, outputLocal, outputLocalPath) = {
    val tmpDir = System.getProperty("java.io.tmpdir")

    val outputHomeLocalHandle = new File(tmpDir, outputHome)
    if (!outputHomeLocalHandle.exists)
      outputHomeLocalHandle.mkdirs()

    val hadoopTmpDir = HdfsUtils.getTempDir(conf)

    // file handle to the test directory on local file system
    val outputLocalHandle = new File(outputHomeLocalHandle.toString, name)
    if (!outputLocalHandle.exists)
      outputLocalHandle.mkdirs()
    (new Path(outputHomeLocalHandle.toURI), new Path(hadoopTmpDir), new Path(outputLocalHandle.toURI), outputLocalHandle.getAbsolutePath)
  }


  /*
   * Makes directory given a path. The parent directory is expected to exist
   * e.g., to make directory bar under /tmp/foo, call mkdir(new Path("/tmp/foo/bar"))
   * The parent directory is assumed to exist
   */
  def mkdir(dir: Path): Unit = {
   val handle = new File(dir.toUri)
    if (!handle.exists)
      handle.mkdirs()
  }

  def clearTestDirectory() = FileUtil.fullyDelete(new File(outputLocal.toUri))

  // clean up the test directory after the test
  // note that this afterAll is not inherited from BeforeAndAfterAll, its callers are
  override def afterAll() = {
    FileUtil.fullyDelete(new File(outputLocal.toUri))
    sc.stop()
    if(afterAlls != null) {
      for(f <- afterAlls) { f() }
    }
  }

  // root directory name on both local file system and hdfs for all tests
  private final val outputHome = "testFiles"

  private def getLocalFS: FileSystem = new Path(System.getProperty("java.io.tmpdir")).getFileSystem(conf)
}
