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

import geotrellis.layers.{Metadata, TileLayerMetadata}
import geotrellis.proj4.LatLng
import geotrellis.raster._
import geotrellis.raster.rasterize.Rasterizer
import geotrellis.raster.viewshed.R2Viewshed
import geotrellis.raster.viewshed.R2Viewshed._
import geotrellis.tiling.{KeyBounds, SpatialKey}
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.util._
import geotrellis.vector._
import org.locationtech.jts.{geom => jts}
import org.apache.log4j.Logger
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.storage.StorageLevel
import org.apache.spark.util.AccumulatorV2

import scala.collection.mutable
import scala.reflect.ClassTag

/**
  * @param x:           x-coordinate (in the units used by the layer)
  * @param y:           y-coordinate (in the units used by the layer)
  * @param viewHeight:  view height (in units of "meters") if positive then height above the surface, if negative then absolute height
  * @param angle:       the angle in radians (about the z-axis) of the "camera"
  * @param fieldOfView: the field of view of the "camera" in radians
  * @param altitude:    the absolute altitude to query; if -∞ then use the terrain height
  */
case class Viewpoint(
  x: Double,
  y: Double,
  viewHeight: Double,
  angle: Double,
  fieldOfView: Double,
  altitude: Double
)

/**
  * A Spark-enabled implementation of R2 [1] viewshed.
  *
  * 1. Franklin, Wm Randolph, and Clark Ray.
  *    "Higher isn’t necessarily better: Visibility algorithms and experiments."
  *    Advances in GIS research: sixth international symposium on spatial data handling. Vol. 2.
  *    Taylor & Francis Edinburgh, 1994.
  *
  * @author James McClain
  */
object IterativeViewshed {

  implicit def coordinatesToPoints(points: Seq[jts.Coordinate]): Seq[Viewpoint] =
    points.map({ p => Viewpoint(p.getX, p.getY, p.getZ, 0, -1.0, Double.NegativeInfinity) })

  private val logger = Logger.getLogger(IterativeViewshed.getClass)

  private case class Message(
    target: SpatialKey,
    index: Int,
    from: From,
    rays: mutable.ArrayBuffer[Ray]
  )

  private type Messages = Map[SpatialKey, List[Message]]

  /**
    * A Spark AccumulatorV2 to catch packets of rays as they cross the
    * boundaries between tiles so that they can be forwarded to
    * adjacent tiles.
    */
  private class RayCatcher extends AccumulatorV2[Message, Messages] {
    private val messages = mutable.Map.empty[SpatialKey, List[Message]]

    def copy: RayCatcher = {
      val other = new RayCatcher
      other.merge(this)
      other
    }

    def add(message: Message): Unit = this.synchronized {
      if (messages contains message.target) {
        val target = message.target
        val list = messages.getOrElse(target, throw new Exception) :+ message
        messages(message.target) = list
      }
      else {
        messages += (message.target -> List(message))
      }
    }

    def isZero: Boolean = messages.isEmpty

    def merge(other: AccumulatorV2[Message, Messages]): Unit =
      this.synchronized {
        val newMessages = (messages.toList ++ other.value.toList)
          .groupBy(_._1)
          .map({ case (key: SpatialKey, lists: List[(SpatialKey, List[Message])]) =>
            val value = lists.map({ case (_, m) => m })
            key -> lists.map({ case (_, m) => m }).reduce(_ ++ _)
          })

        messages.clear
        messages ++= newMessages
     }

    def reset: Unit = this.synchronized { messages.clear }

    def value: Messages = messages.toMap
  }

  /**
    * Compute the resolution (in meters per pixel) of a layer.
    */
  private def computeResolution[K: (? => SpatialKey), V: (? => Tile)](
    elevation: RDD[(K, V)] with Metadata[TileLayerMetadata[K]]
  ) = {
    val md = elevation.metadata
    val mt = md.mapTransform
    val key: SpatialKey = md.bounds.get.minKey
    val extent = mt(key).reproject(md.crs, LatLng)
    val degrees = extent.xmax - extent.xmin
    val meters = degrees * (6378137 * 2.0 * math.Pi) / 360.0
    val pixels = md.layout.tileCols
    math.abs(meters / pixels)
  }

  private case class PointInfo(
    index: Int,
    key: SpatialKey,
    col: Int,
    row: Int,
    viewHeight: Double,
    angle: Double,
    fov: Double,
    alt: Double
  )

  /**
    * Elaborate a point with information from the layer.
    */
  private def pointInfo[K: (? => SpatialKey), V: (? => Tile)](
    rdd: RDD[(K, V)] with Metadata[TileLayerMetadata[K]])(
    pi: (Viewpoint, Int)
  )= {
    val (p, index) = pi
    val md = rdd.metadata

    val cols = md.layout.tileCols
    val rows = md.layout.tileRows
    val key = md.layout.mapTransform(Point(p.x, p.y))
    val extent = md.mapTransform(key)
    val re = RasterExtent(extent, cols, rows)
    val col = re.mapXToGrid(p.x)
    val row = re.mapYToGrid(p.y)
    val viewHeight = p.viewHeight

    PointInfo(
      index = index,
      key = key,
      col = col,
      row = row,
      viewHeight = viewHeight,
      angle = p.angle,
      fov = p.fieldOfView,
      alt = p.altitude
    )
  }

  /**
    * The main entry-point for the iterative viewshed implementation.
    * Takes a layer, some source points, and other ancillary
    * information and produces a viewshed layer.
    *
    * Parameters given in units of meters interact with the layer
    * (approximate) layer resolution, which is computed in this
    * function.  That approximation is done by reprojecting keys to
    * EPSG:4326, then converting degrees to meters, then dividing the
    * number of meters by the number of pixels.
    *
    * @param  elevation    The elevation layer; pixel values are interpreted as being in units of "meters"
    * @param  ps           Viewshed source points; their construction and interpretation is described above
    * @param  maxDistance  The maximum distance that rays are allowed to travel; a lower number reduces computational cost.  This is given in units of meters.
    * @param  curvature    Whether or not to take the curvature of the Earth into account
    * @param  operator     The aggregation operator to use (e.g. Or)
    * @param  epsilon      Rays within this many radians of horizontal (vertical) are considered to be horizontal (vertical)
    */
  def apply[K: (? => SpatialKey): ClassTag, V: (? => Tile)](
    elevation: RDD[(K, V)] with Metadata[TileLayerMetadata[K]],
    ps: Seq[Viewpoint],
    maxDistance: Double,
    curvature: Boolean = true,
    operator: AggregationOperator = Or,
    epsilon: Double = (1/math.Pi)
  ): RDD[(K, Tile)] with Metadata[TileLayerMetadata[K]] = {

    val sparkContext = elevation.sparkContext
    val md = elevation.metadata
    val mt = md.mapTransform

    val resolution = computeResolution(elevation)
    logger.debug(s"Computed resolution: $resolution meters/pixel")

    val bounds = md.bounds match {
      case b: KeyBounds[K] => b
      case _ => throw new EmptyBoundsError
    }
    val minKey: SpatialKey = bounds.minKey
    val minKeyCol = minKey.col
    val minKeyRow = minKey.row
    val maxKey: SpatialKey = bounds.maxKey
    val maxKeyCol = maxKey.col
    val maxKeyRow = maxKey.row

    val rays = new RayCatcher; sparkContext.register(rays)

    def validKey(key: SpatialKey): Boolean = {
      ((minKeyCol <= key.col && key.col <= maxKeyCol) &&
       (minKeyRow <= key.row && key.row <= maxKeyRow))
    }

    /**
      * This function is used to create the `tileCallback` function
      * that `R2Viewshed.compute` expects.  It receives packets of
      * rays generated within the single-tile viewshed code and gives
      * those to a Spark AccumulatorV2 so that they can be forwarded
      * to interested neighbors.
      */
    def rayCatcherFn(key: SpatialKey, index: Int)(bundle: Bundle): Unit = {
      val toTheNorth = SpatialKey(key.col + 0, key.row + 1) // key to the North of the present key
      val toTheEast = SpatialKey(key.col + 1, key.row + 0)
      val toTheSouth = SpatialKey(key.col + 0, key.row - 1)
      val toTheWest = SpatialKey(key.col - 1, key.row + 0)

      Map(
        FromSouth -> toTheNorth, // key is to the North, so rays arrive from the South
        FromWest -> toTheEast,
        FromNorth -> toTheSouth,
        FromEast -> toTheWest
      ).foreach({ case (dir, key) =>
        if (validKey(key)) {
          val rs = bundle.getOrElse(dir, throw new Exception)
          if (rs.length > 0) {
            val message = Message(key, index, dir, rs)
            rays.add(message)
          }
        }
      })
    }

    val info: Seq[PointInfo] = {
      val fn = pointInfo(elevation)_
      ps.zipWithIndex.map(fn)
    }

    val _pointsByKey: Map[SpatialKey, Seq[PointInfo]] =
      info
        .groupBy(_.key)
        .toMap
    val pointsByKey = sparkContext.broadcast(_pointsByKey)

    val _pointsByIndex: Array[PointInfo] = info.toArray
    val pointsByIndex = sparkContext.broadcast(_pointsByIndex)

    val _heightsByIndex: Map[Int, Double] = // index -> height
      elevation
        .flatMap({ case (k, v) =>
          val key = implicitly[SpatialKey](k)
          val tile = implicitly[Tile](v)

          pointsByKey.value.get(key) match {
            case Some(list) =>
              list.map({ case PointInfo(index, _, col, row, viewHeight0, _, _, _) =>
                val viewHeight =
                  if (viewHeight0 >= 0.0) tile.getDouble(col, row) + viewHeight0 ; else -viewHeight0
                (index, viewHeight)
              })
            case None => Seq.empty[(Int, Double)]
          }
        })
        .collect
        .toMap
    val heightsByIndex = sparkContext.broadcast(_heightsByIndex)

    // Create RDD  of viewsheds; after this,  the accumulator contains
    // the rays emanating from the starting points.
    var sheds: RDD[(K, V, MutableArrayTile)] = elevation.map({ case (k, v) =>
      val key = implicitly[SpatialKey](k)
      val tile = implicitly[Tile](v)
      val shed = R2Viewshed.generateEmptyViewshedTile(tile.cols, tile.rows)

      pointsByKey.value.get(key) match {
        case Some(list) =>
          list.foreach({ case PointInfo(index, _, col, row, _, ang, fov, alt) =>
            val viewHeight = heightsByIndex.value.getOrElse(index, throw new Exception)

            R2Viewshed.compute(
              tile, shed,
              col, row, viewHeight,
              FromInside,
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

    // Repeatedly map over the RDD of viewshed tiles until all rays
    // have reached the periphery of the layer.
    do {
      val _changes: Map[SpatialKey, Seq[(Int, From, mutable.ArrayBuffer[Ray])]] =
        rays.value
          .map({ case (k, list) =>
            (k, list.map({ case Message(_, index, from, rs) => (index, from, rs) }))
          })
          .toMap
      val changes = sparkContext.broadcast(_changes)

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
            val indexed: Array[(Int, Seq[(From, mutable.ArrayBuffer[Ray])])] = // an array of <index, <from, rays>> pairs
              localChanges
                .groupBy(_._1)
                .map({ case (index, list) =>
                  (index, list.map({ case (_, from, rs) => (from, rs) })) })
                .toArray

            var j = 0; while (j < indexed.length) { // for all <from, rays> pairs generated by this point (this index)
              val (index, list) = indexed(j)
              val PointInfo(_, pointKey, col, row, _, angle, fov, alt) = pointsByIndex.value(index)
              val startCol = (pointKey.col - key.col) * cols + col
              val startRow = (pointKey.row - key.row) * rows + row
              val viewHeight = heightsByIndex.value.getOrElse(index, throw new Exception)
              val packets: Array[(From, Array[Ray])] =
                list
                  .groupBy(_._1)
                  .mapValues({ case rss =>
                    rss
                      .map({ case (_, rs) => rs })
                      .foldLeft(mutable.ArrayBuffer.empty[Ray])(_ ++ _) })
                  .mapValues({ rs => rs.sortBy(_.theta).toArray })
                  .toArray

              var i = 0; while (i < packets.length) { // for each <direction, packet> pair, evolve the tile
                val (from, rays) = packets(i)
                val sortedRays = rays.toArray
                if (rays.length > 0) {
                  R2Viewshed.compute(
                    elevationTile, shed,
                    startCol, startRow, viewHeight,
                    from,
                    sortedRays,
                    rayCatcherFn(key, index),
                    resolution = resolution,
                    maxDistance = maxDistance,
                    curvature = curvature,
                    operator = operator,
                    altitude = alt,
                    cameraDirection = angle,
                    cameraFOV = fov,
                    epsilon = epsilon
                  )
                }
                i += 1;
              }
              j += 1;
            }
          }
          case None =>
        }
        (k, v, shed)
      }).persist(StorageLevel.MEMORY_AND_DISK_SER)
      sheds.count
      oldSheds.unpersist()

    } while (rays.value.nonEmpty)

    // Return the computed viewshed layer
    val metadata = TileLayerMetadata(IntConstantNoDataCellType, md.layout, md.extent, md.crs, md.bounds)
    val rdd = sheds.map({ case (k, _, viewshed) => (k, viewshed: Tile) })
    ContextRDD(rdd, metadata)
  }

}
