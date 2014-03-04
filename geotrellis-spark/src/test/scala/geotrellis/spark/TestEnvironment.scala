/*******************************************************************************
 * Copyright (c) 2014 DigitalGlobe.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package geotrellis.spark
import org.apache.commons.io.FileUtils
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSpec
import java.io.File
import java.nio.file.FileSystems

/*
 * This trait sets up the test directories on the local fs and hdfs 
 * 
 * It uses commons-io in at least one case (recursive directory deletion)
 */
trait TestEnvironment extends FunSpec with BeforeAndAfterAll {
  // get the name of the class which mixes in this trait
  val name = this.getClass.getName

  // root directory on local file system for source data (e.g., tiffs)
  final val TestSourceRoot = "geotrellis-spark/src/test/resources"
  
  // make a fully qualified (including scheme) path given a directory and either a file or directory    
  def makeQualified(prefix: String, suffix: String) = 
    FileSystems.getDefault().getPath(prefix, suffix).toUri().toString()
    
  // root directory name on both local file system and hdfs for all tests
  final val RootName = "testFiles"

  // root directory paths on both local file system and hdfs for all tests  
  val (rootLocalDir, rootHdfsDir) = setupRootDirs

  // file handle to the test directory on local file system
  val testLocalHandle = new File(rootLocalDir, name)
  if (!testLocalHandle.exists)
    testLocalHandle.mkdirs()

  // test directory on local file system
  val testLocalDir = testLocalHandle.toURI.toString()

  //override def beforeAll {
  //}

  override def afterAll =
    FileUtils.deleteDirectory(testLocalHandle)

  private def setupRootDirs: Tuple2[String, String] = {
    val tmpDir = System.getProperty("java.io.tmpdir")

    val rootLocalHandle = new File(tmpDir, RootName)
    if (!rootLocalHandle.exists)
      rootLocalHandle.mkdirs()
    (rootLocalHandle.getAbsolutePath(), "")
  }
}