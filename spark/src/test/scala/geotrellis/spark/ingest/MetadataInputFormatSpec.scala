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

package geotrellis.spark.ingest

import geotrellis.vector.Extent
import geotrellis.spark.TestEnvironment
import geotrellis.spark.metadata.PyramidMetadata
import geotrellis.spark.metadata.RasterMetadata
import geotrellis.spark.rdd.TileIdPartitioner
import geotrellis.spark.storage.RasterReader
import geotrellis.spark.tiling.PixelExtent
import geotrellis.spark.tiling.TileExtent
import geotrellis.spark.tiling.TmsTiling

import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.SequenceFile
import org.apache.hadoop.mapreduce._
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat

import org.scalatest._
import scala.collection.JavaConversions._

import java.awt.image.DataBuffer

/*
 * Tests both local and spark ingest mode
 */
class MetadataInputFormatSpec extends FunSpec 
                                 with Matchers
                                 with TestEnvironment {
  describe("MetadataInputFormat") {
    it("should handle tiff file") {
      val allOnes = new Path(inputHome, "all-ones.tif")
      val job = new Job(conf)
      FileInputFormat.setInputPaths(job, allOnes.toString)

      val mif = new MetadataInputFormat
      val split = mif.getSplits(job).head

      val id = new TaskAttemptID("foo", 0, false, 1, 2)
      val tc = new TaskAttemptContext(conf, id)
      val rr = mif.createRecordReader(split, tc)

      rr.initialize(split, tc)
      rr.nextKeyValue should be (true)
      rr.getCurrentValue should not be (None)
    }

    it("should handle non-tiff file") {
      val allOnes = new Path(inputHome, "all-ones/metadata")
      val job = new Job(conf)
      FileInputFormat.setInputPaths(job, allOnes.toString)

      val mif = new MetadataInputFormat
      val split = mif.getSplits(job).head

      val id = new TaskAttemptID("foo", 0, false, 1, 2)
      val tc = new TaskAttemptContext(conf, id)
      val rr = mif.createRecordReader(split, tc)

      rr.initialize(split, tc)
      rr.nextKeyValue should be (true)
      rr.getCurrentValue should be (None)
    }

  }
}
