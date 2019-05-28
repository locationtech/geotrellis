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

package geotrellis.spark.etl.hadoop

import java.math.BigInteger

import geotrellis.tiling._
import geotrellis.raster.Tile
import geotrellis.raster.render._
import geotrellis.layers.{LayerId, Metadata, TileLayerMetadata}
import geotrellis.layers.io.index.KeyIndexMethod
import geotrellis.spark.etl.OutputPlugin
import geotrellis.spark._
import geotrellis.spark.etl.Etl
import geotrellis.spark.etl.config.{Backend, EtlConf, HadoopPath, UserDefinedPath}
import geotrellis.spark.render._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.s3._
import org.apache.hadoop.conf.ConfServlet.BadFormatException
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.reflect._

class SpatialRenderOutput extends OutputPlugin[SpatialKey, Tile, TileLayerMetadata[SpatialKey]] {
  import Etl.SaveAction

  def name = "render"
  def key = classTag[SpatialKey]
  def attributes(conf: EtlConf) = null
  /**
   * Parses to a ColorMap a string of limits and their colors in hex RGBA
   * Only used for rendering PNGs
   *
   * @param color map in "BREAK:COLOR" format, ex: "23:cc00ccff;30:aa00aaff;120->ff0000ff"
   * @return ColorMap
   */
  def parseColorMaps(classifications: Option[String]): Option[ColorMap] = {
    classifications.map { blob =>
      try {
        val split = blob.split(";").map(_.trim.split(":"))
        val limits = split.map(pair => Integer.parseInt(pair(0)))
        val colors = split.map(pair => new BigInteger(pair(1), 16).intValue())
        ColorMap(limits, colors)
      } catch {
        case e: Exception =>
          throw new BadFormatException(s"Unable to parse classifications, expected '{limit}:{RGBA};{limit}:{RGBA};...' got: '$blob'")
      }
    }
  }

  override def apply(
    id: LayerId,
    rdd: TileLayerRDD[SpatialKey],
    conf: EtlConf,
    saveAction: SaveAction[SpatialKey, Tile, TileLayerMetadata[SpatialKey]] = SaveAction.DEFAULT[SpatialKey, Tile, TileLayerMetadata[SpatialKey]]
  ): Unit = {
    val outputUri = conf.output.backend.path match {
      case HadoopPath(p) => p
      case UserDefinedPath(p) => p
      case path => throw new IllegalArgumentException(s"Can't handle $path for render")
    }
    val useS3 = outputUri.take(5) == "s3://"
    val images =
      conf.output.encoding.get.toLowerCase match {
          case "png" =>
            parseColorMaps(conf.output.breaks) match {
              case Some(colorMap) =>
                rdd.renderPng(colorMap).mapValues(_.bytes)
              case None =>
                rdd.renderPng().mapValues(_.bytes)
            }
          case "jpg" =>
            parseColorMaps(conf.output.breaks) match {
              case Some(colorMap) =>
                rdd.renderJpg(colorMap).mapValues(_.bytes)
              case None =>
                rdd.renderJpg().mapValues(_.bytes)
            }
          case "geotiff" =>
            rdd.asInstanceOf[RDD[(SpatialKey, Tile)] with Metadata[TileLayerMetadata[SpatialKey]]].renderGeoTiff().mapValues(_.toByteArray)
        }

    if (useS3) {
      val keyToPath = SaveToS3.spatialKeyToPath(id, outputUri)
      images.saveToS3(keyToPath)
    }
    else {
      val keyToPath = SaveToHadoop.spatialKeyToPath(id, outputUri)
      images.saveToHadoop(keyToPath)
    }
  }

  // TODO: ??? means that the hierarchy is broke. Pipelining improvements should fix this.
  def writer(conf: EtlConf)(implicit sc: SparkContext) = ???
}
