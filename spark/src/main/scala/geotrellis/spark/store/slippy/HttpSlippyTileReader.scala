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

package geotrellis.spark.store.slippy

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.tiling.SpatialKey
import geotrellis.spark._
import geotrellis.spark.store.slippy._
import geotrellis.vector._

import org.apache.commons.io.IOUtils._
import org.apache.spark._
import org.apache.spark.rdd._

import java.io._
import java.net._

class HttpSlippyTileReader[T](pathTemplate: String)(fromBytes: (SpatialKey, Array[Byte]) => T) extends SlippyTileReader[T] {
  import SlippyTileReader.TilePath

  def getURL(z: Int, x: Int, y: Int): String =
    pathTemplate.replace("{z}", z.toString)
      .replace("{x}", x.toString)
      .replace("{y}", y.toString)

  def getURLs(z: Int): Seq[String] =
    for (x <- 0 until math.pow(2,z).toInt;
         y <- 0 until math.pow(2,z).toInt) yield getURL(z, x, y)

  private def getByteArray(url: String): Array[Byte] = {
    val inStream = new URL(url).openStream()

    try {
      toByteArray(inStream)
    } finally {
      inStream.close()
    }
  }

  def read(zoom: Int, key: SpatialKey): T = {
    val url = getURL(zoom, key.col, key.row)

    getByteArray(url) match {
      case Array() => throw new FileNotFoundException(s"$url")
      case Array(tile) => fromBytes(key, Array(tile))
    }
  }

  def read(zoom: Int)(implicit sc: SparkContext): RDD[(SpatialKey, T)] = {
    val urls = {
      getURLs(zoom)
        .flatMap { url =>
          url match {
            case TilePath(x, y) => Some((SpatialKey(x.toInt, y.toInt), url))
            case _ => None
          }
      }
    }

    val numPartitions = math.min(urls.size, math.max(urls.size / 10, 50)).toInt
    sc.parallelize(urls.toSeq)
      .partitionBy(new HashPartitioner(numPartitions))
      .mapPartitions({ partition =>
        partition.map { case (key, url) =>

          val tile = getByteArray(url) match {
            case Array(tile) => fromBytes(key, Array(tile))
            case _ => fromBytes(key, Array())
          }

          (key, tile)
        }
      }, true)
  }
}
