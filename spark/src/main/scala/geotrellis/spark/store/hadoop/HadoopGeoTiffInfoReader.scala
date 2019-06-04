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

package geotrellis.spark.store.hadoop

import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.reader.GeoTiffReader.GeoTiffInfo
import geotrellis.raster.io.geotiff.tags.TiffTags
import geotrellis.layers.hadoop.{HdfsUtils, HdfsRangeReader, SerializableConfiguration}
import geotrellis.spark.store._
import geotrellis.spark.store.hadoop._
import geotrellis.util.ByteReader

import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD


case class HadoopGeoTiffInfoReader(
  path: String,
  config: SerializableConfiguration,
  tiffExtensions: Seq[String] = HadoopGeoTiffRDD.Options.DEFAULT.tiffExtensions,
  streaming: Boolean = true
) extends GeoTiffInfoReader {

  /** Returns RDD of URIs to tiffs as GeoTiffInfo is not serializable. */
  def geoTiffInfoRDD(implicit sc: SparkContext): RDD[String] = {
    sc.parallelize(
      HdfsUtils
        .listFiles(new Path(path), config.value)
        .map({ path => path.toString })
        .filter({ path => tiffExtensions.exists({ e => path.endsWith(e) }) })
    )
  }

  def getGeoTiffInfo(uri: String): GeoTiffInfo = {
    val rr = HdfsRangeReader(new Path(uri), config.value)
    val ovrPath = new Path(s"${uri}.ovr")
    val ovrReader: Option[ByteReader] =
      if (HdfsUtils.pathExists(ovrPath, config.value)) Some(HdfsRangeReader(ovrPath, config.value)) else None

    GeoTiffReader.readGeoTiffInfo(rr, streaming, true, ovrReader)
  }
}
