/*
 * Copyright 2019 Azavea
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

package geotrellis.spark

import geotrellis.proj4.CRS
import geotrellis.raster._
import geotrellis.layer._
import geotrellis.vector.Extent
import geotrellis.util._

import org.apache.spark.rdd.RDD
import org.log4s._

case class RasterSummary[M](
  crs: CRS,
  cellType: CellType,
  cellSize: CellSize,
  extent: Extent,
  cells: Long,
  count: Long,
  // for the internal usage only, required to collect non spatial bounds
  bounds: Bounds[M]
) extends Serializable {
  @transient private[this] lazy val logger = getLogger

  def estimatePartitionsNumber: Int = {
    import squants.information._
    val bytes = Bytes(cellType.bytes * cells)
    val numPartitions: Int = math.max((bytes / Megabytes(64)).toInt, 1)
    logger.info(s"Using $numPartitions partitions for ${bytes.toString(Gigabytes)}")
    numPartitions
  }

  def levelFor(layoutScheme: LayoutScheme): LayoutLevel =
    layoutScheme.levelFor(extent, cellSize)

  def toGridExtent: GridExtent[Long] =
    new GridExtent[Long](extent, cellSize)

  def layoutDefinition(scheme: LayoutScheme): LayoutDefinition =
    scheme.levelFor(extent, cellSize).layout

  def combine(other: RasterSummary[M])(implicit ev: Boundable[M]): RasterSummary[M] = {
    require(other.crs == crs, s"Can't combine LayerExtent for different CRS: $crs, ${other.crs}")
    val smallestCellSize = if (cellSize.resolution < other.cellSize.resolution) cellSize else other.cellSize
    RasterSummary(
      crs,
      cellType.union(other.cellType),
      smallestCellSize,
      extent.combine(other.extent),
      cells + other.cells,
      count + other.count,
      bounds.combine(other.bounds)
    )
  }

  def toTileLayerMetadata[K: SpatialComponent](layoutDefinition: LayoutDefinition, keyTransform: (M, SpatialKey) => K): TileLayerMetadata[K] = {
    // We want to align the data extent to pixel layout of layoutDefinition
    val layerGridExtent = layoutDefinition.createAlignedGridExtent(extent)
    val keyBounds = bounds match {
      case KeyBounds(minDim, maxDim) =>
        val KeyBounds(minSpatialKey, maxSpatialKey) = KeyBounds(layoutDefinition.mapTransform.extentToBounds(layerGridExtent.extent))
        KeyBounds(keyTransform(minDim, minSpatialKey), keyTransform(maxDim, maxSpatialKey))
      case EmptyBounds => EmptyBounds
    }
    TileLayerMetadata(cellType, layoutDefinition, layerGridExtent.extent, crs, keyBounds)
  }

  def toTileLayerMetadata(layoutDefinition: LayoutDefinition): TileLayerMetadata[SpatialKey] =
    toTileLayerMetadata(layoutDefinition, (_, sk) => sk)

  def toTileLayerMetadata(layoutType: LayoutType): TileLayerMetadata[SpatialKey] =
    toTileLayerMetadata(layoutType.layoutDefinitionWithZoom(crs, extent, cellSize)._1, (_, sk) => sk)

  // TODO: probably this function should be removed in the future
  def resample(resampleTarget: ResampleTarget): RasterSummary[M] = {
    val re = resampleTarget(toGridExtent)
    RasterSummary(
      crs      = crs,
      cellType = cellType,
      cellSize = re.cellSize,
      extent   = re.extent,
      cells    = re.size,
      count    = count,
      bounds   = bounds // do nothing with it, since it contains non spatial information
    )
  }
}

object RasterSummary {
  def collect[M: Boundable](rdd: RDD[RasterSource], getDimension: RasterMetadata => M): Seq[RasterSummary[M]] = {
    rdd
      .map { rs =>
        val extent = rs.extent
        val crs    = rs.crs
        val dim    = getDimension(rs)
        (crs, RasterSummary(crs, rs.cellType, rs.cellSize, extent, rs.size, 1, KeyBounds(dim, dim)))
      }
      .reduceByKey { _ combine _ }
      .values
      .collect
      .toSeq
  }

  // TODO: should be refactored, we need to reproject all metadata into a common CRS. This code is for the current code simplification
  def fromRDD[M: Boundable](rdd: RDD[RasterSource], getDimension: RasterMetadata => M): RasterSummary[M] = {
    /* Notes on why this is awful:
    - scalac can't infer both GetComponent and type V as CellGrid[N]
    - very ad-hoc, why type constraint and lense?
    - might be as simple as HasRasterSummary[V] in the first place
    */
    val all = collect[M](rdd, getDimension)
    require(all.size == 1, "multiple CRSs detected") // what to do in this case?
    all.head
  }

  def fromRDD(rdd: RDD[RasterSource]): RasterSummary[Unit] = {
    val all = collect(rdd, _ => ())
    require(all.size == 1, "multiple CRSs detected")
    all.head
  }

  def fromSeq[M: Boundable](seq: Seq[RasterSource], getDimension: RasterMetadata => M): RasterSummary[M] = {
    val all =
      seq
        .map { rs =>
          val extent = rs.extent
          val crs    = rs.crs
          val dim    = getDimension(rs)
          (crs, RasterSummary(crs, rs.cellType, rs.cellSize, extent, rs.size, 1, KeyBounds(dim, dim)))
        }
        .groupBy(_._1)
        .map { case (_, v) => v.map(_._2).reduce(_ combine _) }

    require(all.size == 1, "multiple CRSs detected") // what to do in this case?
    all.head
  }

  def fromSeq(seq: Seq[RasterSource]): RasterSummary[Unit] = fromSeq(seq, _ => ())
}
