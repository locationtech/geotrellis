/*
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
 */

package geotrellis.spark.cmd

import geotrellis.spark.metadata.PyramidMetadata
import geotrellis.spark._
import org.apache.hadoop.fs.FileUtil
import org.apache.hadoop.fs.Path
import org.scalatest.Suite
import org.scalatest.FunSpec

//abstract class BuildPyramidSetup extends TestEnvironment with SharedSparkContext { self: Suite =>
// class BuildPyramidSpec extends FunSpec 
//                           with RasterVerifyMethods 
//                           with TestEnvironment 
//                           with SharedSparkContext 
//                           with OnlyIfCanRunSpark {
//   val allOnesOrig = new Path(inputHome, "all-ones")
//   val allOnes = new Path(outputLocal, "all-ones")
  
//   // delete may seem redundant with the "overwrite" flag in FileUtil.copy but 
//   // apparently it is not, since it throws an exception if it finds the directory pre-existing
//   localFS.delete(allOnes, true)
//   FileUtil.copy(localFS, allOnesOrig, localFS, allOnes.getParent(),
//     false /* deleteSource */ ,
//     true /* overwrite */ ,
//     conf)

//   // lazy so runs only after the BuildPyramid.build runs
//   lazy val meta = PyramidMetadata(allOnes, conf)
  
//   override def beforeAll() {
//     super.beforeAll()
//     BuildPyramid.build(sc, allOnes, conf)
//   }

//   describe("Build Pyramid") {
//     ifCanRunSpark {
//       it("should create the correct metadata") {

//         // first lets make sure that the base attributes (everything but the zoom levels in rasterMetadata)
//         // has not changed
//         val actualMetaBase = meta.copy(
//           rasterMetadata = meta.rasterMetadata.filterKeys(_ == meta.maxZoomLevel.toString))
//         val expectedMetaBase = PyramidMetadata(allOnesOrig, conf)
//         actualMetaBase should be(expectedMetaBase)

//         // now lets make sure that the metadata has all the zoom levels in the rasterMetadata
//         meta.rasterMetadata.keys.toList.map(_.toInt).sorted should be((1 to meta.maxZoomLevel).toList)
//       }

//       it("should have the right zoom level directory") {
//         for (z <- 1 until meta.maxZoomLevel) {
//           verifyZoomLevelDirectory(new Path(allOnes, z.toString))
//         }
//       }

//       it("should have the right number of splits for each of the zoom levels") {
//         for (z <- 1 until meta.maxZoomLevel) {
//           verifyPartitions(new Path(allOnes, z.toString))
//         }
//       }

//       it("should have the correct tiles (checking tileIds)") {
//         for (z <- 1 until meta.maxZoomLevel) {
//           verifyTiles(new Path(allOnes, z.toString), meta)
//         }
//       }

//       it("should have its data files compressed") {
//         for (z <- 1 until meta.maxZoomLevel) {
//           verifyCompression(new Path(allOnes, z.toString))
//         }
//       }

//       it("should have its block size set correctly") {
//         for (z <- 1 until meta.maxZoomLevel) {
//           verifyBlockSize(new Path(allOnes, z.toString))
//         }
//       }
//     }
//   }
// }
