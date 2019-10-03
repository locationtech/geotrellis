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

package geotrellis.raster.gdal

import geotrellis.raster.gdal.GDALDataset.DatasetType
import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.{AutoHigherResolution, OverviewStrategy}
import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.vector._

class GDALRasterSource(
  val dataPath: GDALPath,
  val options: GDALWarpOptions = GDALWarpOptions.EMPTY,
  private[raster] val targetCellType: Option[TargetCellType] = None
) extends RasterSource {

  /**
    * All the information received from the JNI side should be cached on the JVM side,
    * to minimize JNI calls. Too aggressive JNI functions usage can lead to a significant performance regression
    * as the result of the often memory copy.
    *
    * For the each JNI call the proxy function will send arguments into the C bindings,
    * on the C side the result would be computed and sent to the JVM side (memory copy happened two times).
    *
    * Since it would happen for each call, much safer and faster would be to remember once computed value in a JVM memory
    * and interact only with it: it will allow to minimize JNI calls, speed up function calls and will allow to memoize some
    * potentially sensitive data.
    *
    */

  def name: GDALPath = dataPath
  val path: String = dataPath.value

  lazy val datasetType: DatasetType = options.datasetType

  // current dataset
  @transient lazy val dataset: GDALDataset =
    GDALDataset(path, options.toWarpOptionsList.toArray)

  /**
    * Fetches a default metadata from the default domain.
    * If there is a need in some custom domain, use the metadataForDomain function.
    */
  lazy val metadata: GDALMetadata = GDALMetadata(this, dataset, DefaultDomain :: Nil)

  /**
    * Return the "base" metadata, usually it is a zero band metadata,
    * a metadata that is valid for the entire source and for the zero band
    */
  def attributes: Map[String, String] = metadata.attributes

  /**
    * Return a per band metadata
    */
  def attributesForBand(band: Int): Map[String, String] = metadata.attributesForBand(band)

  /**
    * Fetches a metadata from the specified [[GDALMetadataDomain]] list.
    */
  def metadataForDomain(domainList: List[GDALMetadataDomain]): GDALMetadata = GDALMetadata(this, dataset, domainList)

  /**
    * Fetches a metadata from all domains.
    */
  def metadataForAllDomains: GDALMetadata = GDALMetadata(this, dataset)

  lazy val bandCount: Int = dataset.bandCount

  lazy val crs: CRS = dataset.crs

  // noDataValue from the previous step
  lazy val noDataValue: Option[Double] = dataset.noDataValue(GDALDataset.SOURCE)

  lazy val dataType: Int = dataset.dataType

  lazy val cellType: CellType = dstCellType.getOrElse(dataset.cellType)

  lazy val gridExtent: GridExtent[Long] = dataset.rasterExtent(datasetType).toGridType[Long]

  /** Resolutions of available overviews in GDAL Dataset
    *
    * These resolutions could represent actual overview as seen in source file
    * or overviews of VRT that was created as result of resample operations.
    */
  lazy val resolutions: List[CellSize] = dataset.resolutions(datasetType).map(_.toGridType[Long])

  override def readBounds(bounds: Traversable[GridBounds[Long]], bands: Seq[Int]): Iterator[Raster[MultibandTile]] = {
    bounds
      .toIterator
      .flatMap { gb => gridBounds.intersection(gb) }
      .map { gb =>
        val tile = dataset.readMultibandTile(gb.toGridType[Int], bands.map(_ + 1), datasetType)
        val extent = this.gridExtent.extentFor(gb)
        convertRaster(Raster(tile, extent))
      }
  }

  def reprojection(targetCRS: CRS, resampleTarget: ResampleTarget = DefaultTarget, method: ResampleMethod = NearestNeighbor, strategy: OverviewStrategy = AutoHigherResolution): RasterSource =
    new GDALRasterSource(dataPath, options.reproject(gridExtent, crs, targetCRS, resampleTarget, method))

  def resample(resampleTarget: ResampleTarget, method: ResampleMethod, strategy: OverviewStrategy): RasterSource =
    new GDALRasterSource(dataPath, options.resample(gridExtent, resampleTarget))

  /** Converts the contents of the GDALRasterSource to the [[TargetCellType]].
   *
   *  Note:
   *
   *  GDAL handles Byte data differently than GeoTrellis. Unlike GeoTrellis,
   *  GDAL treats all Byte data as Unsigned Bytes. Thus, the output from
   *  converting to a Signed Byte CellType can result in unexpected results.
   *  When given values to convert to Byte, GDAL takes the following steps:
   *
   *  1. Checks to see if the values falls in [0, 255].
   *  2. If the value falls outside of that range, it'll clamp it so that
   *  it falls within it. For example: -1 would become 0 and 275 would turn
   *  into 255.
   *  3. If the value falls within that range and is a floating point, then
   *  GDAL will round it up. For example: 122.492 would become 122 and 64.1
   *  would become 64.
   *
   *  Thus, it is recommended that one avoids converting to Byte without first
   *  ensuring that no data will be lost.
   *
   *  Note:
   *
   *  It is not currently possible to convert to the [[BitCellType]] using GDAL.
   *  @group convert
   */
  def convert(targetCellType: TargetCellType): RasterSource = {
    /** To avoid incorrect warp cellSize transformation, we need explicitly set target dimensions. */
    new GDALRasterSource(dataPath, options.convert(targetCellType, noDataValue, Some(cols.toInt -> rows.toInt)), Some(targetCellType))
  }

  def read(extent: Extent, bands: Seq[Int]): Option[Raster[MultibandTile]] = {
    val bounds = gridExtent.gridBoundsFor(extent.buffer(- cellSize.width / 2, - cellSize.height / 2), clamp = false)
    read(bounds, bands)
  }

  def read(bounds: GridBounds[Long], bands: Seq[Int]): Option[Raster[MultibandTile]] = {
    val it = readBounds(List(bounds).flatMap(_.intersection(this.gridBounds)), bands)
    if (it.hasNext) Some(it.next) else None
  }

  override def readExtents(extents: Traversable[Extent]): Iterator[Raster[MultibandTile]] = {
    val bounds = extents.map(gridExtent.gridBoundsFor(_, clamp = false))
    readBounds(bounds, 0 until bandCount)
  }

  override def toString: String = {
    s"GDALRasterSource(${dataPath.value},$options)"
  }

  override def equals(other: Any): Boolean = {
    other match {
      case that: GDALRasterSource =>
        this.dataPath == that.dataPath && this.options == that.options && this.targetCellType == that.targetCellType
      case _ => false
    }
  }

  override def hashCode(): Int = java.util.Objects.hash(dataPath, options, targetCellType)
}

object GDALRasterSource {
  def apply(dataPath: GDALPath, options: GDALWarpOptions = GDALWarpOptions.EMPTY, targetCellType: Option[TargetCellType] = None): GDALRasterSource =
    new GDALRasterSource(dataPath, options, targetCellType)
}
