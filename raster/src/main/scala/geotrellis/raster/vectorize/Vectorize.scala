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

package geotrellis.raster.vectorize

import geotrellis.raster._
import geotrellis.raster.rasterize.polygon.PolygonRasterizer
import geotrellis.raster.regiongroup.{RegionGroup, RegionGroupOptions}
import geotrellis.vector._

import org.locationtech.jts.geom

import scala.collection.mutable


object Vectorize {
  def apply(
    tile: Tile,
    extent: Extent,
    regionConnectivity: Connectivity = RegionGroupOptions.default.connectivity
  ): List[PolygonFeature[Int]] = {
    class ToVectorCallback(
      val polyizer: Polygonizer,
      val r: Tile,
      val v: Int
    ) extends ((Int, Int) => Unit) {
      private val innerStarts = mutable.Map[Int, (Int, Int)]()

      def linearRings =
        for(k <- innerStarts.keys) yield {
          polyizer.getLinearRing(k, innerStarts(k))
        }

      def apply(col: Int, row: Int) = {
        val innerV = r.get(col, row)
        if(innerV != v) {
          if(innerStarts.contains(innerV)) {
            val (pcol, prow) = innerStarts(innerV)
            if(col < pcol || ((col == pcol) && row < prow)) {
              innerStarts(innerV) = (col, row)
            }
          } else {
            innerStarts(innerV) = (col, row)
          }
        }
      }
    }

    val regionGroupOptions =
      RegionGroupOptions(
        connectivity = regionConnectivity,
        ignoreNoData = false
      )
    val rgr = RegionGroup(tile, regionGroupOptions)

    val r = rgr.tile

    val regionMap = rgr.regionMap
    val rasterExtent = RasterExtent(extent, r.cols, r.rows)
    val polyizer = new Polygonizer(r, rasterExtent)

    val processedValues = mutable.Set[Int]()
    val polygons = mutable.Set[PolygonFeature[Int]]()
    val jtsFactory = new geom.GeometryFactory()

    var col = 0
    while(col < r.cols) {
      var row = 0
      while(row < r.rows) {
        val v = r.get(col, row)
        if(isData(regionMap(v))) {
          if(!processedValues.contains(v)) {
            val shell = {
              val lr = polyizer.getLinearRing(v, (col, row))
              if(!lr.isValid) {
                val coords: Array[(Double, Double)] = lr.getCoordinates.map { coord =>
                  // Need to round decimal places or else JTS invents self-intersections
                  val x = BigDecimal(coord.x).setScale(12, BigDecimal.RoundingMode.HALF_UP).toDouble
                  val y = BigDecimal(coord.y).setScale(12, BigDecimal.RoundingMode.HALF_UP).toDouble
                  (x, y)
                }
                val coordsToLastIndex = coords.zipWithIndex.toMap
                var i = 1
                val len = coords.size
                val adjusted = mutable.ListBuffer[(Double, Double)](coords(0))
                while(i < len) {
                  val p = coords(i)
                  // Jump to the index of last occurance of this point to remove intersection.
                  i = coordsToLastIndex(p)
                  adjusted += p
                  i += 1
                }
                val l = jtsFactory.createLinearRing(adjusted.map { case (x, y) => new geom.Coordinate(x, y) }.toArray)
                l
              } else { lr }
            }

            val shellPoly = PolygonFeature(
              Polygon(shell),
              rgr.regionMap(v)
            )

            val callback = new ToVectorCallback(polyizer, r, v)

            shellPoly.geom.foreach(rasterExtent)(callback)

            val holes = {
              val rings = callback.linearRings.map(Line.apply)
              if(rings.size > 1) {
                // We need to get rid of intersecting holes.
                rings.map(Polygon(_).buffer(0)).unionGeometries.asMultiPolygon match {
                  case Some(mp) => mp.polygons.map(_.exterior).toSet
                  case None => sys.error(s"Invalid geometries returned by polygon holes: ${rings.map(Polygon.apply).unionGeometries}")
                }
              } else {
                rings.toSet
              }
            }

            polygons += PolygonFeature(
              Polygon(Line(shell), holes),
              rgr.regionMap(v)
            )

            processedValues += v
          }
        }
        row += 1
      }
      col += 1
    }

    polygons.toList
  }
}
