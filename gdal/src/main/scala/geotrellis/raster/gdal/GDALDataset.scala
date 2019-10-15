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

import geotrellis.raster.gdal.config.GDALOptionsConfig
import geotrellis.raster.gdal.GDALDataset.DatasetType
import geotrellis.raster._
import geotrellis.proj4.{CRS, LatLng}
import geotrellis.vector.Extent

import com.azavea.gdal.GDALWarp

case class GDALDataset(token: Long) extends AnyVal {
  def getAllMetadataFlatten(): Map[String, String] = getAllMetadataFlatten(GDALDataset.SOURCE)

  def getAllMetadataFlatten(datasetType: DatasetType): Map[String, String] =
    (0 until bandCount(datasetType)).map(getAllMetadata(datasetType, _).flatMap(_._2)).reduce(_ ++ _)

  def getAllMetadata(band: Int): Map[GDALMetadataDomain, Map[String, String]] =
    getAllMetadata(GDALDataset.SOURCE, band)

  def getAllMetadata(datasetType: DatasetType, band: Int): Map[GDALMetadataDomain, Map[String, String]] =
    getMetadataDomainList(datasetType.value).map { domain => domain -> getMetadata(domain, band) }.filter(_._2.nonEmpty).toMap

  def getMetadataDomainList(band: Int): List[GDALMetadataDomain] = getMetadataDomainList(GDALDataset.SOURCE, band)

  /** https://github.com/OSGeo/gdal/blob/b1c9c12ad373e40b955162b45d704070d4ebf7b0/gdal/doc/source/development/rfc/rfc43_getmetadatadomainlist.rst */
  def getMetadataDomainList(datasetType: DatasetType, band: Int): List[GDALMetadataDomain] = {
    val arr = Array.ofDim[Byte](100, 1 << 10)
    val returnValue = GDALWarp.get_metadata_domain_list(token, datasetType.value, numberOfAttempts, band, arr)

    if (returnValue <= 0) {
      val positiveValue = math.abs(returnValue)
      throw new MalformedProjectionException(
        s"Unable to get the metadata domain list. GDAL Error Code: $positiveValue",
        positiveValue
      )
    }

    (GDALMetadataDomain.ALL ++ arr.map(new String(_, "UTF-8").trim).filter(_.nonEmpty).toList.map(UserDefinedDomain)).distinct
  }

  def getMetadata(domains: List[GDALMetadataDomain], band: Int): Map[GDALMetadataDomain, Map[String, String]] =
    getMetadata(GDALDataset.SOURCE, domains, band)

  def getMetadata(datasetType: DatasetType, domains: List[GDALMetadataDomain], band: Int): Map[GDALMetadataDomain, Map[String, String]] =
    domains.map(domain => domain -> getMetadata(datasetType, domain, band)).filter(_._2.nonEmpty).toMap

  def getMetadata(domain: GDALMetadataDomain, band: Int): Map[String, String] = getMetadata(GDALDataset.SOURCE, domain, band)

  def getMetadata(datasetType: DatasetType, domain: GDALMetadataDomain, band: Int): Map[String, String] = {
    val arr = Array.ofDim[Byte](100, 1 << 10)
    val returnValue = GDALWarp.get_metadata(token, datasetType.value, numberOfAttempts, band, domain.name, arr)

    if (returnValue <= 0) {
      val positiveValue = math.abs(returnValue)
      throw new MalformedProjectionException(
        s"Unable to get the metadata. GDAL Error Code: $positiveValue",
        positiveValue
      )
    }

    arr
      .map(new String(_, "UTF-8").trim)
      .filter(_.nonEmpty)
      .flatMap { str =>
        val arr = str.split("=")
        if(arr.length == 2) {
          val Array(key, value) = str.split("=")
          Some(key -> value)
        } else Some("" -> str)
      }.toMap
  }

  def getMetadataItem(key: String, domain: GDALMetadataDomain, band: Int): String = getMetadataItem(GDALDataset.WARPED, key, domain, band)

  def getMetadataItem(datasetType: DatasetType, key: String, domain: GDALMetadataDomain, band: Int): String = {
    val arr = Array.ofDim[Byte](1 << 10)
    val returnValue = GDALWarp.get_metadata_item(token, datasetType.value, numberOfAttempts, band, key, domain.name, arr)

    if (returnValue <= 0) {
      val positiveValue = math.abs(returnValue)
      throw new MalformedProjectionException(
        s"Unable to get the metadata item. GDAL Error Code: $positiveValue",
        positiveValue
      )
    }

    new String(arr, "UTF-8").trim
  }

  def getProjection: Option[String] = getProjection(GDALDataset.WARPED)

  def getProjection(datasetType: DatasetType): Option[String] = {
    require(acceptableDatasets contains datasetType)
    val crs = Array.ofDim[Byte](1 << 10)
    val returnValue = GDALWarp.get_crs_wkt(token, datasetType.value, numberOfAttempts, crs)

    if (returnValue <= 0) {
      val positiveValue = math.abs(returnValue)
      throw new MalformedProjectionException(
        s"Unable to parse projection as WKT String. GDAL Error Code: $positiveValue",
        positiveValue
      )
    }

    Some(new String(crs, "UTF-8"))
  }

  def rasterExtent: RasterExtent = rasterExtent(GDALDataset.WARPED)

  def dimensions: Dimensions[Int] = rasterExtent.dimensions

  def rasterExtent(datasetType: DatasetType): RasterExtent = {
    require(acceptableDatasets contains datasetType)
    val transform = Array.ofDim[Double](6)
    val width_height = Array.ofDim[Int](2)

    val transformReturnValue = GDALWarp.get_transform(token, datasetType.value, numberOfAttempts, transform)
    val dimensionReturnValue = GDALWarp.get_width_height(token, datasetType.value, numberOfAttempts, width_height)

    val returnValue =
      if (transformReturnValue < 0) transformReturnValue
      else if (dimensionReturnValue < 0) dimensionReturnValue
      else 0

    if (returnValue < 0) {
      val positiveValue = math.abs(returnValue)
      throw new MalformedDataException(
        s"Unable to construct a RasterExtent from the Transformation given. GDAL Error Code: $positiveValue",
        positiveValue
      )
    }

    val x1 = transform(0)
    val y1 = transform(3)
    val x2 = x1 + transform(1) * width_height(0)
    val y2 = y1 + transform(5) * width_height(1)
    val e = Extent(
      math.min(x1, x2),
      math.min(y1, y2),
      math.max(x1, x2),
      math.max(y1, y2)
    )

    RasterExtent(e, math.abs(transform(1)), math.abs(transform(5)), width_height(0), width_height(1))
  }

  def resolutions(): List[RasterExtent] = resolutions(GDALDataset.WARPED)

  def resolutions(datasetType: DatasetType): List[RasterExtent] = {
    require(acceptableDatasets contains datasetType)
    val N = 1 << 8
    val widths = Array.ofDim[Int](N)
    val heights = Array.ofDim[Int](N)
    val extent = this.extent(datasetType)

    val returnValue =
      GDALWarp.get_overview_widths_heights(token, datasetType.value, numberOfAttempts, 1, widths, heights)

    if (returnValue <= 0) {
      val positiveValue = math.abs(returnValue)
      throw new MalformedDataException(
        s"Unable to construct the overview RasterExtents for the resample. GDAL Error Code: $positiveValue",
        positiveValue
      )
    }

    widths.zip(heights).flatMap({ case (w, h) =>
      if (w > 0 && h > 0) Some(RasterExtent(extent, cols = w, rows = h))
      else None
    }).toList
  }

  def extent: Extent = extent(GDALDataset.WARPED)

  def extent(datasetType: DatasetType): Extent = {
    require(acceptableDatasets contains datasetType)
    this.rasterExtent(datasetType).extent
  }

  def bandCount: Int = bandCount(GDALDataset.WARPED)

  def bandCount(datasetType: DatasetType): Int = {
    require(acceptableDatasets contains datasetType)
    val count = Array.ofDim[Int](1)

    val returnValue = GDALWarp.get_band_count(token, datasetType.value, numberOfAttempts, count)

    if (returnValue <= 0) {
      val positiveValue = math.abs(returnValue)
      throw new MalformedDataException(
        s"A bandCount of <= 0 was found. GDAL Error Code: $positiveValue",
        positiveValue
      )
    }

    count(0)
  }

  def crs: CRS = crs(GDALDataset.WARPED)

  def crs(datasetType: DatasetType): CRS = {
    require(acceptableDatasets contains datasetType)
    val crs = Array.ofDim[Byte](1 << 16)

    val returnValue = GDALWarp.get_crs_proj4(token, datasetType.value, numberOfAttempts, crs)

    if (returnValue <= 0) {
      val positiveValue = math.abs(returnValue)
      throw new MalformedProjectionException(
        s"Unable to parse projection as CRS. GDAL Error Code: $positiveValue",
        positiveValue
      )
    }

    val proj4String: String = new String(crs, "UTF-8").trim
    if (proj4String.length > 0) CRS.fromString(proj4String.trim)
    else LatLng
  }

  def noDataValue: Option[Double] = noDataValue(GDALDataset.WARPED)

  def noDataValue(datasetType: DatasetType): Option[Double] = {
    require(acceptableDatasets contains datasetType)
    val nodata = Array.ofDim[Double](1)
    val success = Array.ofDim[Int](1)

    val returnValue = GDALWarp.get_band_nodata(token, datasetType.value, numberOfAttempts, 1, nodata, success)

    if (returnValue <= 0) {
      val positiveValue = math.abs(returnValue)
      throw new MalformedDataTypeException(
        s"Unable to determine NoData value. GDAL Exception Code: $positiveValue",
        positiveValue
      )
    }

    if (success(0) == 0) None
    else Some(nodata(0))
  }

  def dataType: Int = dataType(GDALDataset.WARPED)

  def dataType(datasetType: DatasetType): Int = {
    require(acceptableDatasets contains datasetType)
    val dataType = Array.ofDim[Int](1)

    val returnValue = GDALWarp.get_band_data_type(token, datasetType.value, numberOfAttempts, 1, dataType)

    if (returnValue <= 0) {
      val positiveValue = math.abs(returnValue)
      throw new MalformedDataTypeException(
        s"Unable to determine DataType. GDAL Error Code: $positiveValue",
        positiveValue
      )
    }

    dataType(0)
  }

  def cellSize: CellSize = cellSize(GDALDataset.WARPED)

  def cellSize(datasetType: DatasetType): CellSize = {
    require(acceptableDatasets contains datasetType)
    val transform = Array.ofDim[Double](6)
    GDALWarp.get_transform(token, datasetType.value, numberOfAttempts, transform)
    CellSize(transform(1), transform(5))
  }

  def cellType: CellType = cellType(GDALDataset.WARPED)

  def cellType(datasetType: DatasetType): CellType = {
    require(acceptableDatasets contains datasetType)
    val nd = noDataValue(datasetType)
    val dt = GDALDataType.intToGDALDataType(this.dataType(datasetType))
    val mm = {
      val minmax = Array.ofDim[Double](2)
      val success = Array.ofDim[Int](1)

      val returnValue = GDALWarp.get_band_min_max(token, datasetType.value, numberOfAttempts, 1, true, minmax, success)

      if (returnValue <= 0) {
        val positiveValue = math.abs(returnValue)
        throw new MalformedDataTypeException(
          s"Unable to deterime the min/max values in order to calculate CellType. GDAL Error Code: $positiveValue",
          positiveValue
        )
      }
      if (success(0) != 0) Some(minmax(0), minmax(1))
      else None
    }
    GDALUtils.dataTypeToCellType(datatype = dt, noDataValue = nd, minMaxValues = mm)
  }

  def readTile(gb: GridBounds[Int] = GridBounds(dimensions), band: Int, datasetType: DatasetType = GDALDataset.WARPED): Tile = {
    require(acceptableDatasets contains datasetType)
    val GridBounds(xmin, ymin, xmax, ymax) = gb
    val srcWindow: Array[Int] = Array(xmin, ymin, xmax - xmin + 1, ymax - ymin + 1)
    val dstWindow: Array[Int] = Array(srcWindow(2), srcWindow(3))
    val ct = this.cellType(datasetType)
    val dt = this.dataType(datasetType)
    val bytes = Array.ofDim[Byte](dstWindow(0) * dstWindow(1) * ct.bytes)

    val returnValue = GDALWarp.get_data(token, datasetType.value, numberOfAttempts, srcWindow, dstWindow, band, dt, bytes)

    if (returnValue <= 0) {
      val positiveValue = math.abs(returnValue)
      throw new GDALIOException(
        s"Unable to read in data. GDAL Error Code: $positiveValue",
        positiveValue
      )
    }

    ArrayTile.fromBytes(bytes, ct, dstWindow(0), dstWindow(1))
  }

  def readMultibandTile(gb: GridBounds[Int] = GridBounds(dimensions), bands: Seq[Int] = 1 to bandCount, datasetType: DatasetType = GDALDataset.WARPED): MultibandTile =
    MultibandTile(bands.map { readTile(gb, _, datasetType) })

  def readMultibandRaster(gb: GridBounds[Int] = GridBounds(dimensions), bands: Seq[Int] = 1 to bandCount, datasetType: DatasetType = GDALDataset.WARPED): Raster[MultibandTile] =
    Raster(readMultibandTile(gb, bands, datasetType), rasterExtent.rasterExtentFor(gb).extent)
}

object GDALDataset {
  /** ADTs to encode the Dataset source type. */
  sealed trait DatasetType { def value: Int }
  /** [[SOURCE]] allows to access the source dataset of the warped dataset. */
  case object SOURCE extends DatasetType { val value: Int = GDALWarp.SOURCE }
  /** [[WARPED]] allows to access the warped dataset. */
  case object WARPED extends DatasetType { val value: Int = GDALWarp.WARPED }

  GDALOptionsConfig.setOptions
  def apply(uri: String, options: Array[String]): GDALDataset = GDALDataset(GDALWarp.get_token(uri, options))
  def apply(uri: String): GDALDataset = apply(uri, Array())
}
