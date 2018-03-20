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

package geotrellis.tiling

import geotrellis.proj4._
import geotrellis.proj4.util.UTM
import geotrellis.raster._
import geotrellis.vector._
import geotrellis.util.Haversine

object ZoomedLayoutScheme {
  val EARTH_CIRCUMFERENCE = 2 * math.Pi * Haversine.EARTH_RADIUS

  val DEFAULT_TILE_SIZE = 256
  val DEFAULT_RESOLUTION_THRESHOLD = 0.1

  def layoutColsForZoom(level: Int): Int = math.pow(2, level).toInt
  def layoutRowsForZoom(level: Int): Int = math.pow(2, level).toInt


  def apply(crs: CRS, tileSize: Int = DEFAULT_TILE_SIZE, resolutionThreshold: Double = DEFAULT_RESOLUTION_THRESHOLD) =
    new ZoomedLayoutScheme(crs, tileSize, resolutionThreshold)

  def layoutForZoom(zoom: Int, layoutExtent: Extent, tileSize: Int = DEFAULT_TILE_SIZE): LayoutDefinition = {
    if(zoom < 0)
      sys.error("TMS Tiling scheme does not have levels below 0")
    LayoutDefinition(layoutExtent, TileLayout(layoutColsForZoom(zoom), layoutRowsForZoom(zoom), tileSize, tileSize))
  }
}

/** Layout for zoom levels based off of a power-of-2 scheme,
  * used in Leaflet et al.
  *
  * @param  crs                      The CRS this zoomed layout scheme will be using
  * @param  tileSize                 The size of each tile in this layout scheme
  * @param  resolutionThreshold      The percentage difference between a cell size and a zoom level
  *                                  and the resolution difference between that zoom level and the next
  *                                  that is tolerated to snap to the lower-resolution zoom level.
  *                                  For example, if this paramter is 0.1, that means we're willing to downsample
  *                                  rasters with a higher resolution in order to fit them to some zoom level Z,
  *                                  if the difference is resolution is less than or equal to 10% the difference
  *                                  between the resolutions of zoom level Z and zoom level Z+1.
  * */
class ZoomedLayoutScheme(val crs: CRS, val tileSize: Int, val resolutionThreshold: Double) extends LayoutScheme {
  import ZoomedLayoutScheme.EARTH_CIRCUMFERENCE
  import ZoomedLayoutScheme.{layoutColsForZoom, layoutRowsForZoom}

  /** This will calcluate the closest zoom level based on the resolution in a UTM zone containing the point.
    * The calculated zoom level is up to some percentage (determined by the resolutionThreshold) less resolute then the cellSize.
    * If the cellSize is more resolute than that threshold's allowance, this will return the next zoom level up.
    */
  def zoom(x: Double, y: Double, cellSize: CellSize): Int = {
    val ll1 = Point(x + cellSize.width, y + cellSize.height).reproject(crs, LatLng)
    val ll2 = Point(x, y).reproject(crs, LatLng)
    // Try UTM zone, if not, use Haversine distance formula
    val dist: Double =
      if(UTM.inValidZone(ll1.y)) {
        val utmCrs = UTM.getZoneCrs(ll1.x, ll1.y)
        val (p1, p2) = (ll1.reproject(LatLng, utmCrs), ll2.reproject(LatLng, utmCrs))

        math.max(math.abs(p1.x - p2.x), math.abs(p1.y - p2.y))
      } else {
        Haversine(ll1.x, ll1.y, ll2.x, ll2.y)
      }
    val z = (math.log(EARTH_CIRCUMFERENCE / (dist * tileSize)) / math.log(2)).toInt
    val zRes = EARTH_CIRCUMFERENCE / (math.pow(2, z) * tileSize)
    val nextZRes = EARTH_CIRCUMFERENCE / (math.pow(2, z + 1) * tileSize)
    val delta = zRes - nextZRes
    val diff = zRes - dist

    val zoom =
      if(diff / delta > resolutionThreshold) {
        z.toInt + 1
      } else {
        z.toInt
      }

    zoom
  }

  def levelFor(extent: Extent, cellSize: CellSize): LayoutLevel = {
    val worldExtent = crs.worldExtent
    val l =
      zoom(extent.xmin, extent.ymin, cellSize)

    levelForZoom(worldExtent, l)
  }

  def levelForZoom(id: Int): LayoutLevel =
    levelForZoom(crs.worldExtent, id)

  def levelForZoom(worldExtent: Extent, id: Int): LayoutLevel = {
    if(id < 0)
      sys.error("TMS Tiling scheme does not have levels below 0")
    LayoutLevel(id, LayoutDefinition(worldExtent, TileLayout(layoutColsForZoom(id), layoutRowsForZoom(id), tileSize, tileSize)))
  }

  def zoomOut(level: LayoutLevel) = {
    val layout = level.layout
    val newZoom = level.zoom - 1
    val newSize = math.pow(2, newZoom).toInt
    new LayoutLevel(
      zoom = newZoom,
      layout = LayoutDefinition(
        extent = layout.extent,
        tileLayout = TileLayout(
          newSize,
          newSize,
          layout.tileCols,
          layout.tileRows
        )
      )
    )
  }

  def zoomIn(level: LayoutLevel) = {
    val layout = level.layout
    val newZoom = level.zoom + 1
    val newSize = math.pow(2, newZoom).toInt
    new LayoutLevel(
      zoom = newZoom,
      layout = LayoutDefinition(
        extent = layout.extent,
        tileLayout = TileLayout(
          newSize,
          newSize,
          layout.tileCols,
          layout.tileRows
        )
      )
    )
  }
}
