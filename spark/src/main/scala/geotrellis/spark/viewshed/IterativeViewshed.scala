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

import com.vividsolutions.jts.{ geom => jts }
import org.apache.log4j.Logger
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.storage.StorageLevel
import org.apache.spark.util.AccumulatorV2

import scala.collection.mutable
import scala.reflect.ClassTag


object IterativeViewshed {

  type Point6D = Array[Double]

  implicit def coordinatesToPoints(points: Seq[jts.Coordinate]): Seq[Point6D] =
    points.map({ p => Array(p.x, p.y, p.z, 0, -1.0, Double.NegativeInfinity) })

  private val logger = Logger.getLogger(IterativeViewshed.getClass)

  private type Message = (SpatialKey, Int, From, mutable.ArrayBuffer[Ray]) // key, point index, direction, ray
  private type Messages = mutable.ArrayBuffer[Message]

  private class RayCatcher extends AccumulatorV2[Message, Messages] {
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

  private def computeResolution[K: (? => SpatialKey): ClassTag, V: (? => Tile)](
    elevation: RDD[(K, V)] with Metadata[TileLayerMetadata[K]]
  ) = {
    val md = elevation.metadata
    val mt = md.mapTransform
    val key = implicitly[SpatialKey](elevation.map(_._1).first)
    val extent = mt(key).reproject(md.crs, LatLng)
    val degrees = extent.xmax - extent.xmin
    val meters = degrees * (6378137 * 2.0 * math.Pi) / 360.0
    val pixels = md.layout.tileCols
    math.abs(meters / pixels)
  }

  private def pointInfo[K: (? => SpatialKey)](md: TileLayerMetadata[K])(
    pi: (Array[Double], Int)
  )= {
    val (p, index) = pi
    val p2 = new jts.Coordinate(p(0),p(1),p(2))
    val bounds = md.layout.mapTransform(p2.envelope)
    require(bounds.colMin == bounds.colMax)
    require(bounds.rowMin == bounds.rowMax)

    val cols = md.layout.tileCols
    val rows = md.layout.tileRows
    val key = SpatialKey(bounds.colMin, bounds.rowMin)
    val extent = md.mapTransform(key)
    val re = RasterExtent(extent, cols, rows)
    val col = re.mapXToGrid(p2.x)
    val row = re.mapYToGrid(p2.y)

    (key, (index, col, row, p(2), p(3), p(4), p(5)))
  }

  /**
    *
    */
  def apply[K: (? => SpatialKey): ClassTag, V: (? => Tile)](
    elevation: RDD[(K, V)] with Metadata[TileLayerMetadata[K]],
    ps: Seq[Array[Double]],
    maxDistance: Double,
    curvature: Boolean = true,
    operator: AggregationOperator = Or(),
    touched: mutable.Set[SpatialKey] = null
  )(implicit sc: SparkContext): RDD[(K, Tile)] with Metadata[TileLayerMetadata[K]] = {

    ps.foreach({ p => require(p.length == 6) })

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

    def validKey(key: SpatialKey): Boolean = {
      ((minKeyCol <= key.col && key.col <= maxKeyCol) &&
       (minKeyRow <= key.row && key.row <= maxKeyRow))
    }

    def rayCatcherFn(key: SpatialKey, index: Int)(bundle: Bundle): Unit = {
      val southKey = SpatialKey(key.col + 0, key.row + 1)
      val westKey = SpatialKey(key.col + 1, key.row + 0)
      val northKey = SpatialKey(key.col + 0, key.row - 1)
      val eastKey = SpatialKey(key.col - 1, key.row + 0)

      Map(FromSouth() -> southKey, FromWest() -> westKey, FromNorth() -> northKey, FromEast() -> eastKey)
        .foreach({ case (dir, key) =>
          if (validKey(key)) {
            val rs = bundle.getOrElse(dir, throw new Exception)
            if (rs.length > 0) {
              val message = (key, index, dir, rs)
              rays.add(message)
            }
          }
        })
    }

    val info: Seq[(SpatialKey, (Int, Int, Int, Double, Double, Double, Double))] = { // inner tuple: index, col, row, z, angle, fov, altitude
      val fn = pointInfo(md)_
      ps.zipWithIndex.map(fn)
    }

    val _pointsByKey: Map[SpatialKey, Seq[(Int, Int, Int, Double, Double, Double, Double)]] = // value: index, col, row, z, angle, fov, altitude
      info
        .groupBy(_._1)
        .mapValues({ list => list.map({ case (_, v) => v }) })
        .toMap
    val pointsByKey = sc.broadcast(_pointsByKey)
    if (touched != null) touched ++= _pointsByKey.keys

    val _pointsByIndex: Map[Int, (SpatialKey, Int, Int, Double, Double, Double)] = // value: key, col, angle, fov, altitude
      info
        .groupBy(_._2._1)
        .mapValues({ list => list.map({ case (key, (index, col, row, z, angle, fov, alt)) =>
          (key, col, row, angle, fov, alt) }) })
        .mapValues({ list => list.head })
        .toMap
    val pointsByIndex = sc.broadcast(_pointsByIndex)

    val _heights: Map[Int, Double] = // index -> height
      elevation
        .flatMap({ case (k, v) =>
          val key = implicitly[SpatialKey](k)
          val tile = implicitly[Tile](v)

          pointsByKey.value.get(key) match {
            case Some(list) =>
              list.map({ case (index: Int, col: Int, row: Int, z: Double, _, _, _) =>
                val height = if (z >= 0.0) tile.getDouble(col, row) + z ; else -z
                (index, height)
              })
            case None => Seq.empty[(Int, Double)]
          }
        })
        .collect
        .toMap
    val heights = sc.broadcast(_heights)

    // Create RDD  of viewsheds; after this,  the accumulator contains
    // the rays emanating from the starting points.
    var sheds: RDD[(K, V, MutableArrayTile)] = elevation.map({ case (k, v) =>
      val key = implicitly[SpatialKey](k)
      val tile = implicitly[Tile](v)
      val shed = R2Viewshed.generateEmptyViewshedTile(tile.cols, tile.rows)

      pointsByKey.value.get(key) match {
        case Some(list) =>
          list.foreach({ case (index: Int, col: Int, row: Int, z: Double, ang: Double, fov: Double, alt: Double) =>
            val height = heights.value.getOrElse(index, throw new Exception)

            R2Viewshed.compute(
              tile, shed,
              col, row, height,
              FromInside(),
              null,
              rayCatcherFn(key, index),
              resolution = resolution,
              maxDistance = maxDistance,
              curvature = curvature,
              altitude = alt,
              operator = operator,
              cameraDirection = ang,
              cameraFOV = fov
            )
          })
        case None =>
      }

      (k, v, shed)
    }).persist(StorageLevel.MEMORY_AND_DISK_SER)
    sheds.count // make sheds materialize

    // Repeatedly  map over the RDD  of viewshed tiles until  all rays
    // have reached the periphery of the layer.
    do {
      val _changes: Map[SpatialKey, Seq[(Int, From, mutable.ArrayBuffer[Ray])]] =
        rays.value
          .groupBy(_._1)
          .map({ case (k, list) =>
            (k, list.map({ case (_, index, from, rs) => (index, from, rs) }))
          })
          .toMap
      val changes = sc.broadcast(_changes)

      if (touched != null) touched ++= _changes.keys
      rays.reset
      logger.debug(s"≥ ${changes.value.size} tiles in motion")

      val oldSheds = sheds
      sheds = oldSheds.map({ case (k, v, shed) =>
        val key = implicitly[SpatialKey](k)
        val elevationTile = implicitly[Tile](v)
        val cols = elevationTile.cols
        val rows = elevationTile.rows

        changes.value.get(key) match {
          case Some(localChanges: Seq[(Int, From, mutable.ArrayBuffer[Ray])]) => { // sequence of <index, from, rays> triples for this key
            val indexed: Map[Int, Seq[(From, mutable.ArrayBuffer[Ray])]] = // a map from an index to a sequence of <from, rays> pairs
              localChanges
                .groupBy(_._1)
                .map({ case (index, list) =>
                  (index, list.map({ case (_, from, rs) => (from, rs) }))
                })

            indexed.foreach({ case (index, list) => // for all <from, rays> pairs generated by this point (this index)
              val (pointKey, col, row, angle, fov, alt) = pointsByIndex.value.getOrElse(index, throw new Exception)
              val startCol = (pointKey.col - key.col) * cols + col
              val startRow = (pointKey.row - key.row) * rows + row
              val height = heights.value.getOrElse(index, throw new Exception)
              val packets: Map[From, Array[Ray]] = list
                .groupBy(_._1)
                .mapValues({ case rss =>
                  rss
                    .map({ case (_, rs) => rs })
                    .foldLeft(mutable.ArrayBuffer.empty[Ray])(_ ++ _) })
                .mapValues({ rs => rs.sortBy(_.theta).toArray })

              packets.foreach({ case (from, rays) => // for each <direction, packet> pair, evolve the tile
                val sortedRays = rays.toArray.sortBy(_.theta)
                if (rays.length > 0) {
                  R2Viewshed.compute(
                    elevationTile, shed,
                    startCol, startRow, height,
                    from,
                    sortedRays,
                    rayCatcherFn(key, index),
                    resolution = resolution,
                    maxDistance = maxDistance,
                    curvature = curvature,
                    operator = operator,
                    altitude = alt,
                    cameraDirection = angle,
                    cameraFOV = fov
                  )
                }
              })
            })

          }
          case None =>
        }
        (k, v, shed)
      }).persist(StorageLevel.MEMORY_AND_DISK_SER)
      sheds.count
      oldSheds.unpersist()

    } while (rays.value.size > 0)

    // Return the computed viewshed layer
    val metadata = TileLayerMetadata(IntConstantNoDataCellType, md.layout, md.extent, md.crs, md.bounds)
    val rdd = sheds.map({ case (k, _, v) => (k, v.asInstanceOf[Tile]) })
    ContextRDD(rdd, metadata)
  }

}
