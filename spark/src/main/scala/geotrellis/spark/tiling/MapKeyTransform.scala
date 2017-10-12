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

package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.raster.rasterize.{Rasterizer, Callback}
import geotrellis.vector._
import geotrellis.proj4._
import geotrellis.util._

import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConverters._

object MapKeyTransform {
  def apply(crs: CRS, level: LayoutLevel): MapKeyTransform =
    apply(crs.worldExtent, level.layout.layoutCols, level.layout.layoutRows)

  def apply(crs: CRS, layoutDimensions: (Int, Int)): MapKeyTransform =
    apply(crs.worldExtent, layoutDimensions)

  def apply(crs: CRS, layoutCols: Int, layoutRows: Int): MapKeyTransform =
    apply(crs.worldExtent, layoutCols, layoutRows)

  def apply(extent: Extent, layoutDimensions: (Int, Int)): MapKeyTransform =
    apply(extent, layoutDimensions._1, layoutDimensions._2)

  def apply(extent: Extent, layoutCols: Int, layoutRows: Int): MapKeyTransform =
    new MapKeyTransform(extent, layoutCols, layoutRows)
}

/**
  * Transforms between geographic map coordinates and spatial keys.
  * Since geographic point can only be mapped to a grid tile that contains that point,
  * transformation from Extent to GridBounds to Extent will likely not
  * produce the original geographic extent, but a larger one.
  */
class MapKeyTransform(val extent: Extent, val layoutCols: Int, val layoutRows: Int) extends Serializable {
  lazy val tileWidth: Double = extent.width / layoutCols
  lazy val tileHeight: Double = extent.height / layoutRows

  def extentToBounds(otherExtent: Extent): GridBounds = apply(otherExtent)

  def apply(otherExtent: Extent): GridBounds = {
    val SpatialKey(colMin, rowMin) = apply(otherExtent.xmin, otherExtent.ymax)

    // For calculating GridBounds, the extent parameter is considered
    // inclusive on it's north and west borders, and execlusive on
    // it's east and south borders.
    // If the Extent has xmin == xmax and/or ymin == ymax, then consider
    // those zero length dimensions to represent the west and/or east
    // borders (so they are inclusive). In this case, the tiles returned
    // will be south and/or east of the line or point.
    val colMax = {
      val d = (otherExtent.xmax - extent.xmin) / (extent.width / layoutCols)

      if(d == math.floor(d) && d != colMin) { d.toInt - 1 }
      else { d.toInt }
    }

    val rowMax = {
      val d = (extent.ymax - otherExtent.ymin) / (extent.height / layoutRows)

      if(d == math.floor(d) && d != rowMin) { d.toInt - 1 }
      else { d.toInt }
    }

    GridBounds(colMin, rowMin, colMax, rowMax)
  }

  def boundsToExtent(gridBounds: GridBounds): Extent = apply(gridBounds)

  def apply(gridBounds: GridBounds): Extent = {
    val e1 = apply(gridBounds.colMin, gridBounds.rowMin)
    val e2 = apply(gridBounds.colMax, gridBounds.rowMax)
    e1.expandToInclude(e2)
  }

  /** Fetch the [[SpatialKey]] that corresponds to some coordinates in some CRS on the Earth. */
  def pointToKey(p: Point): SpatialKey = apply(p)

  def apply(p: Point): SpatialKey = apply(p.x, p.y)

  /** Fetch the [[SpatialKey]] that corresponds to some coordinates in some CRS on the Earth. */
  def pointToKey(x: Double, y: Double): SpatialKey = apply(x, y)

  def apply(x: Double, y: Double): SpatialKey = {
    val tcol =
      ((x - extent.xmin) / extent.width) * layoutCols

    val trow =
      ((extent.ymax - y) / extent.height) * layoutRows

    (tcol.floor.toInt, trow.floor.toInt)
  }

  @deprecated("Use this instead: MapKeyTransform.keyToExtent(key.getComponent[SpatialKey])", "1.2")
  def apply[K: SpatialComponent](key: K): Extent = apply(key.getComponent[SpatialKey])

  /** Get the [[Extent]] corresponding to a [[SpatialKey]] in some zoom level. */
  def keyToExtent(key: SpatialKey): Extent = apply(key)

  def apply(key: SpatialKey): Extent = apply(key.col, key.row)

  /** 'col' and 'row' correspond to a [[SpatialKey]] column and row in some grid. */
  def keyToExtent(col: Int, row: Int): Extent = apply(col, row)

  def apply(col: Int, row: Int): Extent =
    Extent(
      extent.xmin + col * tileWidth,
      extent.ymax - (row + 1) * tileHeight,
      extent.xmin + (col + 1) * tileWidth,
      extent.ymax - row * tileHeight
    )

  def multiLineToKeys(multiLine: MultiLine): Iterator[SpatialKey] = {
    val extent = multiLine.envelope
    val bounds: GridBounds = extentToBounds(extent)
    val options = Rasterizer.Options(includePartial=true, sampleType=PixelIsArea)

    val boundsExtent: Extent = boundsToExtent(bounds)
    val rasterExtent = RasterExtent(boundsExtent, bounds.width, bounds.height)

    /*
     * Use the Rasterizer to construct  a list of tiles which meet
     * the  query polygon.   That list  of tiles  is stored  as an
     * array of  tuples which  is then  mapped-over to  produce an
     * array of KeyBounds.
     */
    val tiles = new ConcurrentHashMap[(Int,Int), Unit]
    val fn = new Callback {
      def apply(col : Int, row : Int): Unit = {
        val tile : (Int, Int) = (bounds.colMin + col, bounds.rowMin + row)
        tiles.put(tile, Unit)
      }
    }

    multiLine.foreach(rasterExtent, options)(fn)
    tiles.keys.asScala.map { case (col, row) => SpatialKey(col, row) }
  }

  def multiPolygonToKeys(multiPolygon: MultiPolygon): Iterator[SpatialKey] = {
    val extent = multiPolygon.envelope
    val bounds: GridBounds = extentToBounds(extent)
    val options = Rasterizer.Options(includePartial=true, sampleType=PixelIsArea)
    val boundsExtent: Extent = boundsToExtent(bounds)
    val rasterExtent = RasterExtent(boundsExtent, bounds.width, bounds.height)

    /*
     * Use the Rasterizer to construct  a list of tiles which meet
     * the  query polygon.   That list  of tiles  is stored  as an
     * array of  tuples which  is then  mapped-over to  produce an
     * array of KeyBounds.
     */
    val tiles = new ConcurrentHashMap[(Int,Int), Unit]
    val fn = new Callback {
      def apply(col : Int, row : Int): Unit = {
        val tile : (Int, Int) = (bounds.colMin + col, bounds.rowMin + row)
        tiles.put(tile, Unit)
      }
    }

    multiPolygon.foreach(rasterExtent, options)(fn)
    tiles.keys.asScala.map { case (col, row) => SpatialKey(col, row) }
  }
}
