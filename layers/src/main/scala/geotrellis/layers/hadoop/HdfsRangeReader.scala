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

package geotrellis.layers.hadoop

import geotrellis.util.RangeReader

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path


/**
 * This class extends [[RangeReader]] by reading chunks out of a GeoTiff on HDFS.
 *
 * @param request: A [[GetObjectRequest]] of the desired GeoTiff.
 * @param client: The [[S3Client]] that retrieves the data.
 * @return A new instance of S3RangeReader.
 */
class HdfsRangeReader(
  path: Path,
  conf: Configuration) extends RangeReader {

  val totalLength: Long =
    path.getFileSystem(conf).getFileStatus(path).getLen

  def readClippedRange(start: Long, length: Int): Array[Byte] =
    HdfsUtils.readRange(path, start, length, conf)
}

object HdfsRangeReader {
  /**
    * Returns a new instance of HdfsRangeReader.
    *
    * @param path: Path to file
    * @param conf: Hadoop configuration
    *
    * @return A new instance of HdfsRangeReader.
    */
  def apply(path: Path, conf: Configuration): HdfsRangeReader =
    new HdfsRangeReader(path, conf)

}
