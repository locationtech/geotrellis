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

import java.util.Arrays.sort


object IterativeViewshed {

  val logger = Logger.getLogger(IterativeViewshed.getClass)

  type Message = (SpatialKey, From, Ray)
  type Messages = mutable.ArrayBuffer[Message]
  type Rays = (Array[Ray], Array[Ray])
  // type Coordinates = (Int, Int, Int, Int)

  // class PointFinder extends AccumulatorV2[Coordinates, Coordinates] {
  //   private var coordinates: Coordinates = null
  //   def copy PointFinder = {
  //     val other = new PointFinder
  //     other.merge(this)
  //     other
  //   }
  //   def add(_coordinates: Coordinates): Unit = this.synchronized { coordinates = _coordinates }
  //   def isZero: Boolean = (coordinates == null)
  //   def merge(other: AccumulatorV2[Coordinates, Coordinates]): Unit = this.synchronized { coordinates = other.value }
  //   def reset: Unit = this.synchronized { coordinates = null }
  //   def value: Coordinates = coordinates.copy
  // }

  class RayCatcher extends AccumulatorV2[Message, Messages] {
    private val messages: Messages = mutable.ArrayBuffer.empty
    def copy: RayCatcher = {
      val other = new RayCatcher
      other.merge(this)
      other
    }
    def add(message: Message): Unit = this.synchronized { messages.append(message) }
    def isZero: Boolean = messages.isEmpty
    def merge(other: AccumulatorV2[Message, Messages]): Unit = this.synchronized { messages ++= other.value }
    def reset: Unit = this.synchronized { messages.clear }
    def value: Messages = messages
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

    val bounds = md.bounds.asInstanceOf[KeyBounds[K]]
    val minKey = implicitly[SpatialKey](bounds.minKey)
    val minKeyCol = minKey._1
    val minKeyRow = minKey._2
    val maxKey = implicitly[SpatialKey](bounds.maxKey)
    val maxKeyCol = maxKey._1
    val maxKeyRow = maxKey._2

    val rays = new RayCatcher; sc.register(rays)
    val pointKeyCol = sc.longAccumulator
    val pointKeyRow = sc.longAccumulator
    val pointCol = sc.longAccumulator
    val pointRow = sc.longAccumulator
    val pointHeight = sc.doubleAccumulator

    def rayCatcherFn(key: SpatialKey)(ray: Ray, from: From): Unit = {
      val key2 = from match {
        case _: FromSouth => SpatialKey(key.col + 0, key.row + 1)
        case _: FromWest =>  SpatialKey(key.col + 1, key.row + 0)
        case _: FromNorth => SpatialKey(key.col + 0, key.row - 1)
        case _: FromEast =>  SpatialKey(key.col - 1, key.row + 0)
        case _: FromInside => throw new Exception
      }
      if (minKeyCol <= key2.col && key2.col <= maxKeyCol && minKeyRow <= key2.row && key2.row <= maxKeyRow) {
        val message = (key2, from, ray)
        rays.add(message)
      }
    }

    // Create RDD of viewsheds; the tile containing the starting point
    // is  complete and  the accumulator  contains the  rays emanating
    // from that.
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
        pointHeight.reset ; pointHeight.add(0.0) // XXX
        Rasterizer
          .foreachCellByGeometry(point, rasterExtent, options)({ (col, row) =>
            pointKeyCol.reset ; pointKeyCol.add(key.col)
            pointKeyRow.reset ; pointKeyRow.add(key.row)
            pointCol.reset ; pointCol.add(col)
            pointRow.reset ; pointRow.add(row)

            R2Viewshed.compute(
              tile, shed,
              col, row, pointHeight.value, resolution,
              FromInside(),
              null,
              rayCatcherFn(key)
            )
          })
      }

      (k, v, shed)
    }).persist(StorageLevel.MEMORY_AND_DISK_SER)
    sheds.count

    val _details = (pointKeyCol.value, pointKeyRow.value, pointCol.value, pointRow.value, pointHeight.value)
    val details = sc.broadcast(_details)

    // Repeatedly  map over the RDD  of viewshed tiles until  all rays
    // have reached the periphery.
    do {
      val _changes: Map[SpatialKey, Seq[(From, Ray)]] =
        rays.value
          .groupBy(_._1)
          .map({ case (k, list) => (k, list.map({ case (_, from, ray) => (from, ray) })) })
          .toMap
      val changes = sc.broadcast(_changes)

      val oldSheds = sheds
      rays.reset
      sheds = oldSheds.map({ case (k, v, shed) =>
        val key = implicitly[SpatialKey](k)
        val elevationTile = implicitly[Tile](v)
        val cols = elevationTile.cols
        val rows = elevationTile.rows
        val localChanges: Option[Seq[(From, Ray)]] = changes.value.get(key)
        val (pointKeyCol, pointKeyRow, pointCol, pointRow, viewHeight) = details.value

        localChanges match {
          case Some(localChanges) => {
            val packets: List[(From, Seq[Ray])] =
              localChanges
                .groupBy(_._1)
                .map({ case (from, list) => (from, list.map({ case (_, ray) => ray })) })
                .toList

            packets.foreach({ case (from, rays) =>
              val startCol = (pointKeyCol - key.col) * cols + pointCol
              val startRow = (pointKeyRow - key.row) * rows + pointRow

              R2Viewshed.compute(
                elevationTile, shed,
                startCol.toInt, startRow.toInt, viewHeight, resolution,
                from,
                rays.sortBy({ _.theta }).toArray,
                rayCatcherFn(key)
              )
            })
          }
          case None =>
        }
        (k, v, shed)
      }).persist(StorageLevel.MEMORY_AND_DISK_SER)

      sheds.count
      oldSheds.unpersist()
    } while (rays.value.size > 0)

    // println(s"XXX $bounds")
    // rays.value.foreach({ case (key, from, ray) =>
    //   println(s"XXX $key $from $ray")
    // })
  }

}
