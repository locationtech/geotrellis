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

import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.reader.GeoTiffReader.GeoTiffInfo
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD


case class HadoopGeoTiffInfoReader(
  path: String,
  config: SerializableConfiguration,
  extensions: Seq[String],
  decompress: Boolean = false,
  streaming: Boolean = true
) extends GeoTiffInfoReader {

  lazy val geoTiffInfo: List[(String, GeoTiffInfo)] = {
    HdfsUtils
      .listFiles(new Path(path), config.value)
      .map({ path => path.toString })
      .filter({ path => extensions.exists({ e => path.endsWith(e) }) })
      .map({ uri => (uri, getGeoTiffInfo(uri)) })
  }

  /** Returns RDD of URIs to tiffs as GeoTiffInfo is not serializable. */
  def geoTiffInfoRdd(implicit sc: SparkContext): RDD[String] = {
    sc.parallelize(
      HdfsUtils
        .listFiles(new Path(path), config.value)
        .map({ path => path.toString })
        .filter({ path => extensions.exists({ e => path.endsWith(e) }) })
    )
  }

  def getGeoTiffInfo(uri: String): GeoTiffInfo = {
    val path = new Path(uri)
    val rr = HdfsRangeReader(path, config.value)
    GeoTiffReader.readGeoTiffInfo(rr, decompress, streaming)
  }
}
