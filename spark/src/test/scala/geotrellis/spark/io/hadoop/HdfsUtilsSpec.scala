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

package geotrellis.spark.io.hadoop

import geotrellis.layers.hadoop.HdfsUtils
import geotrellis.spark.testkit._
import geotrellis.util.Filesystem

import java.io.IOException

import org.scalatest._
import org.apache.hadoop.fs.Path


class HdfsUtilsSpec extends FunSpec with Matchers with TestEnvironment {
  describe("HdfsUtils") {

    def testArrays[T](arr1: Array[T], arr2: Array[T]): Array[(T, T)] = {
      val zipped = arr1.zip(arr2)
      zipped.filter(x => x._1 != x._2)
    }

    val path = "spark/src/test/resources/all-ones.tif"
    val hdfsFile = new Path(path)
    val array = Filesystem.slurp(path)

    it("should not crash with unuseful error messages when no files match listFiles") {
      val path = new Path("/this/does/not/exist") // Would be really weird if this did.
      an[IOException] should be thrownBy { HdfsUtils.listFiles(path, conf) }
    }

    it("should read the wole file if given whole file length") {
      val actual = HdfsUtils.readRange(hdfsFile, 0, array.length, conf)

      val result = testArrays(array, actual)

      result.length should be (0)
    }

    it("should return an Array[Byte] of the correct size") {
      val actual = HdfsUtils.readRange(hdfsFile, 500, 500, conf)

      actual.length should be (500)
    }

    it("should read the correct range of bytes from a file") {
      val expected = Array.ofDim[Byte](500)
      val actual = HdfsUtils.readRange(hdfsFile, 500, 500, conf)

      System.arraycopy(array, 500, expected, 0, expected.length)

      val result = testArrays(expected, actual)

      result.length should be (0)
    }
  }
}
