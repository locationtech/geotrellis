/*
 * Copyright 2017 Azavea
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

package geotrellis.spark.viewshed

import geotrellis.proj4.LatLng
import geotrellis.raster._
import geotrellis.raster.rasterize.Rasterizer
import geotrellis.raster.viewshed.R2Viewshed
import geotrellis.raster.viewshed.R2Viewshed._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.util._
import geotrellis.vector._

import org.apache.log4j.Logger
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.storage.StorageLevel
import org.apache.spark.util.AccumulatorV2

import scala.collection.mutable


object IterativeViewshed {

  type SpatialKeyFrom = (SpatialKey, From)
  type Message = (SpatialKeyFrom, Ray)
  type Rays = (Array[Ray], Array[Ray])
  type Messages = mutable.ArrayBuffer[Message]

  val logger = Logger.getLogger(IterativeViewshed.getClass)

  class RayCatcher extends AccumulatorV2[Message, Messages] {
    private val messages: Messages = mutable.ArrayBuffer.empty

    def copy: RayCatcher = {
      val other = new RayCatcher
      other.merge(this)
      other
    }

    def add(message: Message): Unit =
      this.synchronized { messages.append(message) }

    def isZero: Boolean = messages.isEmpty

    def merge(other: AccumulatorV2[Message, Messages]): Unit =
      this.synchronized { messages ++= other.value }

    def reset: Unit =
      this.synchronized { messages.clear }

    def value: Messages =
      messages
  }

  def computeResolution[K: (? => SpatialKey), V: (? => Tile)](
    elevation: RDD[(K, V)] with Metadata[TileLayerMetadata[K]]
  ) = {
    val md = elevation.metadata
    val mt = md.mapTransform
    val kv = elevation.first
    val key = implicitly[SpatialKey](kv._1)
    val tile = implicitly[Tile](kv._2)
    val extent = mt(key).reproject(md.crs, LatLng)
    val degrees = extent.xmax - extent.xmin
    val meters = degrees * (6378137 * 2.0 * math.Pi) / 360.0
    val pixels = tile.cols
    math.abs(meters / pixels)
  }

  def apply[K: (? => SpatialKey), V: (? => Tile)](
    elevation: RDD[(K, V)] with Metadata[TileLayerMetadata[K]],
    point: Point
  )(implicit sc: SparkContext)/*: RDD[(K, Tile)] with Metadata[TileLayerMetadata[K]]*/= {

    val md = elevation.metadata
    val mt = md.mapTransform
    val resolution = computeResolution(elevation)
    logger.debug(s"Computed resolution: $resolution meters/pixel")

    val rays = new RayCatcher
    sc.register(rays)

    def rayCatcherFn(key: SpatialKey)(ray: Ray, from: From): Unit = {
      val skf = from match {
        case _: FromSouth => (SpatialKey(key.col + 0, key.row - 1), from)
        case _: FromWest =>  (SpatialKey(key.col + 1, key.row + 0), from)
        case _: FromNorth => (SpatialKey(key.col + 0, key.row + 1), from)
        case _: FromEast =>  (SpatialKey(key.col - 1, key.row + 0), from)
        case _: FromInside => throw new Exception
      }
      val message = (skf, ray)
      rays.add(message)
    }

    var sheds: RDD[(K, V, MutableArrayTile)] = elevation.map({ case (k, v) =>
      val key = implicitly[SpatialKey](k)
      val tile = implicitly[Tile](v)
      val cols = tile.cols
      val rows = tile.rows
      val extent = mt(key)
      val rasterExtent = RasterExtent(extent, cols, rows)
      val options = Rasterizer.Options.DEFAULT
      val shed = R2Viewshed.generateEmptyViewshedTile(cols, rows)

      if (extent.contains(point)) {
        Rasterizer
          .foreachCellByGeometry(point, rasterExtent, options)({ (col, row) =>
            R2Viewshed.compute(
              tile, shed,
              col, row, 0.0, resolution,
              FromInside(), null, null,
              rayCatcherFn(key)
            )
          })
      }

      (k, v, shed)
    }).persist(StorageLevel.MEMORY_AND_DISK_SER)
    sheds.count

    rays.value.foreach({ case ((key, from), ray) =>
      println(s"XXX $key $from $ray")
    })
  }

}
