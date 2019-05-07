/*
 * Copyright 2018 Azavea
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

package geotrellis.layers.cog

import geotrellis.proj4.CRS
import geotrellis.vector.Extent
import geotrellis.tiling._
import geotrellis.raster._
import geotrellis.raster.io._
import geotrellis.layers.TileLayerMetadata
import geotrellis.layers._
import geotrellis.util._

import cats.instances.either._
import cats.instances.stream._
import cats.syntax.foldable._

import spray.json._
import spray.json.DefaultJsonProtocol._


case class COGLayerMetadata[K: SpatialComponent](
  cellType: CellType,
  zoomRangeInfos: Vector[(ZoomRange, KeyBounds[K])], // KeyBounds is for a minZoom in this ranges
  layoutScheme: ZoomedLayoutScheme,
  extent: Extent,
  crs: CRS
) {
  def combine(other: COGLayerMetadata[K])(implicit ev: Boundable[K]): COGLayerMetadata[K] = {
    val combinedZoomRangeInfos =
      (zoomRangeInfos ++ other.zoomRangeInfos)
        .groupBy(_._1)
        .map { case (key, bounds) => key -> bounds.map(_._2).reduce(_ combine _) }
        .toVector

    val combinedExtent = extent.combine(other.extent)

    COGLayerMetadata(
      cellType,
      combinedZoomRangeInfos,
      layoutScheme,
      combinedExtent,
      crs
    )
  }

  private val maxZooms =
    zoomRangeInfos.map(_._1.maxZoom).toArray

  def zoomRanges: Vector[ZoomRange] =
    zoomRangeInfos.map(_._1)

  def zoomRangeFor(zoom: Int): ZoomRange =
    zoomRangeInfoFor(zoom)._1

  def zoomRangeInfoFor(zoom: Int): (ZoomRange, KeyBounds[K]) = {
    val i = java.util.Arrays.binarySearch(maxZooms, zoom)
    val idx =
      if(i >= 0) { i }
      else {
        ~i //- 1
      }

    zoomRangeInfos(idx)
  }


  def layoutForZoom(z: Int): LayoutDefinition =
    layoutScheme.levelForZoom(z).layout

  def keyBoundsForZoom(zoom: Int): KeyBounds[K] = {
    /** "Base" in this function means min zoom level and NOT the highest resolution */
    val (ZoomRange(minZoom, _), baseKeyBounds) = zoomRangeInfoFor(zoom)
    if(minZoom == zoom) baseKeyBounds
    else {
      val (baseLayout, layout) = layoutForZoom(minZoom) -> layoutForZoom(zoom)
      val KeyBounds(baseMinKey, baseMaxKey) = baseKeyBounds

      val extentGridBounds =
        layout
          .mapTransform
          .extentToBounds(extent)

      val gridBounds =
        layout
          .mapTransform.extentToBounds(
            baseLayout
              .mapTransform
              .boundsToExtent(baseKeyBounds.toGridBounds())
              .bufferByLayout(layout)
        )

      val GridBounds(colMin, rowMin, colMax, rowMax) =
        extentGridBounds
          .intersection(gridBounds)
          .getOrElse(
            throw new Exception(
              s"Entire layout grid bounds $extentGridBounds have no intersections to layer grid bounds $gridBounds"
            )
          )

      KeyBounds(
        baseMinKey.setComponent(SpatialKey(colMin, rowMin)),
        baseMaxKey.setComponent(SpatialKey(colMax, rowMax))
      )
    }
  }

  def tileLayerMetadata(zoom: Int): TileLayerMetadata[K] =
    TileLayerMetadata[K](
      cellType,
      layoutScheme.levelForZoom(zoom).layout,
      extent,
      crs,
      keyBoundsForZoom(zoom)
    )

  def getReadDefinitions(queryKeyBounds: Seq[KeyBounds[K]], zoom: Int): Seq[(ZoomRange, Seq[(SpatialKey, Int, TileBounds, Seq[(GridBounds[Int], SpatialKey)])])] =
    queryKeyBounds
      .map { _.toSpatial }
      .distinct // to avoid duplications because of the temporal component
      .map { getReadDefinitions(_, zoom) }

  /**
    * Returns the ZoomRange to read, and a Sequence of SpatialKey COGs to read, the total
    * GridBounds to read from that COG, and the sequence of GridBounds -> Keys that that
    * file should be cropped by
    */
  private [layers] def getReadDefinitions(queryKeyBounds: KeyBounds[SpatialKey], zoom: Int): (ZoomRange, Seq[(SpatialKey, Int, TileBounds, Seq[(GridBounds[Int], SpatialKey)])]) = {
    /** "Base" in this function means min zoom level and NOT the highest resolution */
    val zoomRange @ ZoomRange(minZoom, maxZoom) = zoomRangeFor(zoom)
    val (baseLayout, layout) = layoutForZoom(minZoom) -> layoutForZoom(zoom)
    val overviewIdx = maxZoom - zoom - 1
    val queryTileBounds = queryKeyBounds.toGridBounds()

    // queryTileBounds converted on a base zoom level
    val baseQueryTileBounds = {
      val extentGridBounds =
        baseLayout
          .mapTransform
          .extentToBounds(extent)

      val tileBounds =
        baseLayout
          .mapTransform.extentToBounds(
            layout
              .mapTransform
              .boundsToExtent(queryTileBounds)
              .bufferByLayout(layout)
          )

      extentGridBounds
        .intersection(tileBounds)
        .getOrElse(
          throw new Exception(
            s"Entire layout grid bounds $extentGridBounds have no intersections to layer grid bounds $tileBounds"
          )
        )
    }

    val GridBounds(colMin, rowMin, colMax, rowMax) = baseQueryTileBounds

    val seq =
      for {
        col <- colMin to colMax
        row <- rowMin to rowMax
      } yield {
        val queryKey = SpatialKey(col, row)
        val layoutGridBounds =
          layout
            .mapTransform
            .extentToBounds(queryKey.extent(baseLayout).bufferByLayout(layout))

        val seq = queryTileBounds.intersection(layoutGridBounds) match {
          case Some(GridBounds(queryMinKeyCol, queryMinKeyRow, queryMaxKeyCol, queryMaxKeyRow)) =>
            for {
              qcol <- queryMinKeyCol to queryMaxKeyCol
              qrow <- queryMinKeyRow to queryMaxKeyRow
            } yield {
              val key = SpatialKey(qcol, qrow)

              val (minCol, minRow) = ((key.col - layoutGridBounds.colMin) * layout.tileCols, (key.row - layoutGridBounds.rowMin) * layout.tileRows)
              val (maxCol, maxRow) = (minCol + layout.tileCols - 1, minRow + layout.tileRows - 1)
              (GridBounds(minCol, minRow, maxCol, maxRow), key)
            }
          case _ => Nil
        }

        if(seq.nonEmpty) {
          val combinedGridBounds = seq.map(_._1).reduce(_ combine _)
          Some((queryKey, overviewIdx, combinedGridBounds, seq))
        } else None
      }

    (zoomRange, seq.flatten)
  }

  /** Returns the ZoomRange and SpatialKey of the COG to be read for this key, index of overview, as well as the GridBounds to crop
    * that COG to */
  private [layers] def getReadDefinition(key: SpatialKey, zoom: Int): (ZoomRange, SpatialKey, Int, TileBounds) = {
    val zoomRange @ ZoomRange(minZoom, maxZoom) = zoomRangeFor(zoom)
    val overviewIdx = maxZoom - zoom - 1

    val baseLayout = layoutForZoom(minZoom)
    val layout = layoutForZoom(zoom)

    val baseKey =
      baseLayout
        .mapTransform
        .pointToKey(
          layout
            .mapTransform
            .keyToExtent(key)
            .center
        )

    val layoutGridBounds = layout.mapTransform(baseKey.extent(baseLayout).bufferByLayout(layout))

    val gridBounds = {
      val (minCol, minRow) = ((key.col - layoutGridBounds.colMin) * layout.tileCols, (key.row - layoutGridBounds.rowMin) * layout.tileRows)
      val (maxCol, maxRow) = (minCol + layout.tileCols - 1, minRow + layout.tileRows - 1)
      GridBounds(minCol, minRow, maxCol, maxRow)
    }

    (zoomRange, baseKey, overviewIdx, gridBounds)
  }
}

object COGLayerMetadata {
  /** Constructs a COGLayerMetadata
    *
    * @param cellType: CellType of layer.
    * @param extent: The extent of the layer.
    * @param crs: CRS of layer.
    * @param keyBounds: KeyBounds of the base zoom level for the layer.
    * @param layoutScheme: The ZoomedLayoutScheme of this layer.
    * @param maxZoom: The maximum zoom level for this tile
    * @param minZoom: Minimum zoom level. Defaults to 0.
    * @param maxTileSize: The maximum tile size for any one COG file for this layer.
    *                     For instance, if 1024, no COG in the layer will have a greater
    *                     width or height than 1024. Defaults to 4096.
    */
  def apply[K: SpatialComponent](
    cellType: CellType,
    extent: Extent,
    crs: CRS,
    keyBounds: KeyBounds[K],
    layoutScheme: ZoomedLayoutScheme,
    maxZoom: Int,
    minZoom: Int = 0,
    maxTileSize: Int = 4096
  ): COGLayerMetadata[K] = {

    val baseLayout = layoutScheme.levelForZoom(maxZoom).layout

    val pmin =
      baseLayout
        .mapTransform
        .keyToExtent(keyBounds.minKey.getComponent[SpatialKey])
        .center

    val pmax =
      baseLayout
        .mapTransform
        .keyToExtent(keyBounds.maxKey.getComponent[SpatialKey])
        .center

    def getKeyBounds(layout: LayoutDefinition): KeyBounds[K] = {
      val (skMin, skMax) =
        (layout.mapTransform.pointToKey(pmin), layout.mapTransform.pointToKey(pmax))
      KeyBounds(
        keyBounds.minKey.setComponent[SpatialKey](skMin),
        keyBounds.maxKey.setComponent[SpatialKey](skMax)
      )
    }

    /**
      * List of ranges, the current maximum zoom for the next range, the current tile size, isLowLevel, fitsZoomRange.
      * - fitsZoomRange is required for the case when the min zoom level is not the low zoom level of the partial pyramid
      *   but fits some partial pyramid.
      * - for the range, and a flag for whether or not we've gotten to a zoom level that
      *   has 4 or less tiles contain the extent.
      */
    val accSeed = (List[(ZoomRange, KeyBounds[K])](), maxZoom, baseLayout.tileRows, false, true)
    def validZoomRange(zr: ZoomRange): Boolean = zr.zoomInRange(minZoom) || (zr.minZoom >= minZoom)

    val (zoomRanges, _, _, _, _) = {
      // generate a stream from the max zoom level by -1, we want to perform a lazy fold with a conditional break
      val Left(res) =
        Stream.from(maxZoom, -1).foldLeftM(accSeed) { case (prod@(acc, currMaxZoom, currTileSize, isLowLevel, fitsZoomRange), z) =>
          // TMS doesn't support zoom levels below 0
          if (z < 0 || !fitsZoomRange) {
            Left(prod)
          } else {
            if (isLowLevel) {
              val thisLayout = layoutScheme.levelForZoom(z).layout
              val zr = ZoomRange(z, currMaxZoom)
              val fits = validZoomRange(zr)
              // a border case where we need to stop and not to push data to the next fold step
              if (fits) Right(((zr, getKeyBounds(thisLayout)) :: acc, z - 1, currTileSize, isLowLevel, fits))
              else Left(prod)
            } else {
              val thisLayout = layoutScheme.levelForZoom(z).layout
              val thisTileSize =
                if (currMaxZoom == z) {
                  // Starting a fresh range
                  thisLayout.tileRows
                } else {
                  currTileSize * 2
                }

              val thisIsLowLevel = {
                val SpatialKey(colMin, rowMin) = thisLayout.mapTransform.pointToKey(extent.xmin, extent.ymax)
                val SpatialKey(colMax, rowMax) = thisLayout.mapTransform.pointToKey(extent.xmax, extent.ymin)
                rowMax - rowMin < 2 || colMax - colMin < 2
              }

              if (thisIsLowLevel || thisTileSize >= maxTileSize) {
                val zr = ZoomRange(z, currMaxZoom)
                // thisTileSize is ignored next round
                Right(((zr, getKeyBounds(thisLayout)) :: acc, z - 1, thisTileSize, thisIsLowLevel, validZoomRange(zr)))
              } else {
                Right((acc, currMaxZoom, thisTileSize, thisIsLowLevel, fitsZoomRange))
              }
            }
          }
        }

      res
    }

    COGLayerMetadata(
      cellType,
      zoomRanges.toVector,
      layoutScheme,
      extent,
      crs
    )
  }

  implicit def cogLayerMetadataFormat[K: SpatialComponent: JsonFormat] =
    new RootJsonFormat[COGLayerMetadata[K]] {
      def write(metadata: COGLayerMetadata[K]) =
        JsObject(
          "cellType" -> metadata.cellType.toJson,
          "zoomRangesInfos" -> metadata.zoomRangeInfos.toJson,
          "layoutScheme" -> metadata.layoutScheme.toJson,
          "extent" -> metadata.extent.toJson,
          "crs" -> metadata.crs.toJson
        )

      def read(value: JsValue): COGLayerMetadata[K] =
        value.asJsObject.getFields("cellType", "zoomRangesInfos", "layoutScheme", "extent", "crs") match {
          case Seq(cellType, JsArray(zoomRanges), layoutScheme, extent, crs) =>
            COGLayerMetadata(
              cellType.convertTo[CellType],
              zoomRanges.map(_.convertTo[(ZoomRange, KeyBounds[K])]),
              layoutScheme.convertTo[ZoomedLayoutScheme],
              extent.convertTo[Extent],
              crs.convertTo[CRS]
            )
          case v =>
            throw new DeserializationException(s"COGLayerMetadata expected, got $v")
        }
    }
}
