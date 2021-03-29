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
import cats.syntax.option._

case class GDALDataset(token: Long) extends AnyVal {
  def getAllMetadataFlatten: Map[String, String] = getAllMetadataFlatten(GDALDataset.SOURCE)

  def getAllMetadataFlatten(datasetType: DatasetType): Map[String, String] =
    (0 until bandCount(datasetType)).map(getAllMetadata(datasetType, _).flatMap(_._2)).reduce(_ ++ _)

  def getAllMetadata(band: Int): Map[GDALMetadataDomain, Map[String, String]] =
    getAllMetadata(GDALDataset.SOURCE, band)

  def getAllMetadata(datasetType: DatasetType, band: Int): Map[GDALMetadataDomain, Map[String, String]] =
    getMetadataDomainList(datasetType.value).map { domain => domain -> getMetadata(domain, band) }.filter(_._2.nonEmpty).toMap

  def getMetadataDomainList(band: Int): List[GDALMetadataDomain] = getMetadataDomainList(GDALDataset.SOURCE, band)

  /** https://github.com/OSGeo/gdal/blob/b1c9c12ad373e40b955162b45d704070d4ebf7b0/gdal/doc/source/development/rfc/rfc43_getmetadatadomainlist.rst */
  def getMetadataDomainList(datasetType: DatasetType, band: Int): List[GDALMetadataDomain] = {
    require(acceptableDatasets contains datasetType)

    val arr = Array.ofDim[Byte](100, 1 << 10)
    val returnValue = GDALWarp.get_metadata_domain_list(token, datasetType.value, numberOfAttempts, band, arr)

    errorHandler(returnValue, { positiveValue =>
      new MalformedDataException(
        s"Unable to get the metadata domain list. GDAL Error Code: $positiveValue",
        positiveValue
      )
    })

    (GDALMetadataDomain.ALL ++ arr.map(new String(_, "UTF-8").trim).filter(_.nonEmpty).toList.map(UserDefinedDomain)).distinct
  }

  def getMetadata(domains: List[GDALMetadataDomain], band: Int): Map[GDALMetadataDomain, Map[String, String]] =
    getMetadata(GDALDataset.SOURCE, domains, band)

  def getMetadata(datasetType: DatasetType, domains: List[GDALMetadataDomain], band: Int): Map[GDALMetadataDomain, Map[String, String]] =
    domains.map(domain => domain -> getMetadata(datasetType, domain, band)).filter(_._2.nonEmpty).toMap

  def getMetadata(domain: GDALMetadataDomain, band: Int): Map[String, String] = getMetadata(GDALDataset.SOURCE, domain, band)

  def getMetadata(datasetType: DatasetType, domain: GDALMetadataDomain, band: Int): Map[String, String] = {
    require(acceptableDatasets contains datasetType)

    val arr = Array.ofDim[Byte](100, 1 << 10)
    val returnValue = GDALWarp.get_metadata(token, datasetType.value, numberOfAttempts, band, domain.name, arr)

    errorHandler(returnValue, { positiveValue =>
      new MalformedDataException(
        s"Unable to get the metadata domain list. GDAL Error Code: $positiveValue",
        positiveValue
      )
    })

    arr
      .map(new String(_, "UTF-8").trim)
      .filter(_.nonEmpty)
      .flatMap { str =>
        val arr = str.split("=")
        if(arr.length == 2) {
          val Array(key, value) = str.split("=")
          (key, value).some
        } else ("", str).some
      }.toMap
  }

  def getMetadataItem(key: String, domain: GDALMetadataDomain, band: Int): String = getMetadataItem(GDALDataset.WARPED, key, domain, band)

  def getMetadataItem(datasetType: DatasetType, key: String, domain: GDALMetadataDomain, band: Int): String = {
    require(acceptableDatasets contains datasetType)

    val arr = Array.ofDim[Byte](1 << 10)
    val returnValue = GDALWarp.get_metadata_item(token, datasetType.value, numberOfAttempts, band, key, domain.name, arr)

    errorHandler(returnValue, { positiveValue =>
      new MalformedDataException(
        s"Unable to get the metadata item. GDAL Error Code: $positiveValue",
        positiveValue
      )
    })

    new String(arr, "UTF-8").trim
  }

  def getProjection: Option[String] = getProjection(GDALDataset.WARPED)

  def getProjection(datasetType: DatasetType): Option[String] = {
    require(acceptableDatasets contains datasetType)

    val crs = Array.ofDim[Byte](1 << 10)
    val returnValue = GDALWarp.get_crs_wkt(token, datasetType.value, numberOfAttempts, crs)

    errorHandler(returnValue, { positiveValue =>
      new MalformedProjectionException(
        s"Unable to parse projection as WKT String. GDAL Error Code: $positiveValue",
        positiveValue
      )
    })

    new String(crs, "UTF-8").some
  }

  def overviewDimensions: List[Dimensions[Int]] = overviewDimensions(GDALDataset.WARPED)

  def overviewDimensions(datasetType: DatasetType): List[Dimensions[Int]] = {
    require(acceptableDatasets contains datasetType)

    val N = 1 << 8
    val widths = Array.ofDim[Int](N)
    val heights = Array.ofDim[Int](N)

    val returnValue = GDALWarp.get_overview_widths_heights(token, datasetType.value, numberOfAttempts, 1, widths, heights)

    errorHandler(returnValue, { positiveValue =>
      new MalformedDataException(
        s"Unable to construct overview dimensions. GDAL Error Code: $positiveValue",
        positiveValue
      )
    })

    widths.zip(heights).flatMap({ case (w, h) => if (w > 0 && h > 0) Dimensions(cols = w, rows = h).some else None }).toList
  }

  def dimensions: Dimensions[Int] = dimensions(GDALDataset.WARPED)

  def dimensions(datasetType: DatasetType): Dimensions[Int] = {
    require(acceptableDatasets contains datasetType)

    val dimensions = Array.ofDim[Int](2)
    val returnValue = GDALWarp.get_width_height(token, datasetType.value, numberOfAttempts, dimensions)

    errorHandler(returnValue, { positiveValue =>
      new MalformedDataException(
        s"Unable to construct dataset dimensions. GDAL Error Code: $positiveValue",
        positiveValue
      )
    })

    Dimensions(dimensions(0), dimensions(1))
  }

  def getTransform: Array[Double] = getTransform(GDALDataset.WARPED)

  def getTransform(datasetType: DatasetType): Array[Double] = {
    require(acceptableDatasets contains datasetType)

    val transform = Array.ofDim[Double](6)
    val returnValue = GDALWarp.get_transform(token, datasetType.value, numberOfAttempts, transform)

    errorHandler(returnValue, { positiveValue =>
      new MalformedDataException(
        s"Unable to construct a RasterExtent from the Transformation given. GDAL Error Code: $positiveValue",
        positiveValue
      )
    })

    transform
  }

  def rasterExtent: RasterExtent = rasterExtent(GDALDataset.WARPED)

  /**
    * Even though GeoTrellis doesn't support rotation fully, we still can properly compute the Dataset Extent.
    *
    * https://gdal.org/user/raster_data_model.html#affine-geotransform
    * https://github.com/mapbox/rasterio/blob/1.2b1/rasterio/_base.pyx#L865-L895
    * Previous revision code reference: https://github.com/locationtech/geotrellis/blob/v3.5.1/gdal/src/main/scala/geotrellis/raster/gdal/GDALDataset.scala#L128
    * */
  def rasterExtent(datasetType: DatasetType): RasterExtent = {
    require(acceptableDatasets contains datasetType)

    val Dimensions(cols, rows) = dimensions(datasetType)
    val transform @ Array(_, _, xskew, _, yskew, _) = getTransform(datasetType)
    val ext = extent(transform, cols, rows)

    /**
      * RasterExtent doesn't support rotated AffineTransform.
      * In case of rotation, it would throw since the cellSize would not match RasterExtent expectations.
      *
      * This if-statement aligns GDAL RasterExtent computation behavior with the Java RasterExtent computation.
      *
      * TODO: fix rotated rasters support: https://github.com/locationtech/geotrellis/issues/3332
      */
    if(xskew == 0d && yskew == 0d) {
      val cs = cellSize(transform)
      RasterExtent(ext, cs.width, cs.height, cols, rows)
    } else RasterExtent(ext, cols, rows)
  }

  def resolutions: List[RasterExtent] = resolutions(GDALDataset.WARPED)

  def resolutions(datasetType: DatasetType): List[RasterExtent] = {
    require(acceptableDatasets contains datasetType)

    val ext = extent(datasetType)
    overviewDimensions(datasetType).map { case Dimensions(cols, rows) => RasterExtent(ext, cols, rows) }
  }

  def extent: Extent = extent(GDALDataset.WARPED)

  def extent(datasetType: DatasetType): Extent = {
    require(acceptableDatasets contains datasetType)
    rasterExtent(datasetType).extent
  }

  /**
    * Compute extent, takes into account a possible rotation.
    *
    * https://github.com/mapbox/rasterio/blob/1.2b1/rasterio/_base.pyx#L865-L895
    *
    * Note for a reader:
    * affine.Affine(a, b, c, d, e, f)
    * GDALTransform(c, a, b, f, d, e)
    *
    * https://gdal.org/user/raster_data_model.html#affine-geotransform
    * https://github.com/mapbox/rasterio/blob/1.2b1/rasterio/_base.pyx#L865-L895
    */
  def extent(transform: Array[Double], cols: Int, rows: Int): Extent = {
    val Array(upx, xres, xskew, upy, yskew, yres) = transform

    val ulx = upx + 0 * xres + 0 * xskew
    val uly = upy + 0 * yskew + 0 * yres

    val llx = upx + 0 * xres + rows * xskew
    val lly = upy + 0 * yskew + rows * yres

    val lrx = upx + cols * xres + rows * xskew
    val lry = upy + cols * yskew + rows * yres

    val urx = upx + cols * xres + 0 * xskew
    val ury = upy + cols * yskew + 0 * yres

    val xs = List(ulx, llx, lrx, urx)
    val ys = List(uly, lly, lry, ury)

    Extent(xs.min, ys.min, xs.max, ys.max)
  }

  def bandCount: Int = bandCount(GDALDataset.WARPED)

  def bandCount(datasetType: DatasetType): Int = {
    require(acceptableDatasets contains datasetType)

    val count = Array.ofDim[Int](1)
    val returnValue = GDALWarp.get_band_count(token, datasetType.value, numberOfAttempts, count)

    errorHandler(returnValue, { positiveValue =>
      new MalformedDataException(
        s"A bandCount of <= 0 was found. GDAL Error Code: $positiveValue",
        positiveValue
      )
    })

    count(0)
  }

  def crs: CRS = crs(GDALDataset.WARPED)

  def crs(datasetType: DatasetType): CRS = {
    require(acceptableDatasets contains datasetType)

    val crs = Array.ofDim[Byte](1 << 16)
    val returnValue = GDALWarp.get_crs_proj4(token, datasetType.value, numberOfAttempts, crs)

    errorHandler(returnValue, { positiveValue =>
      new MalformedProjectionException(
        s"Unable to parse projection as CRS. GDAL Error Code: $positiveValue",
        positiveValue
      )
    })

    val proj4String = new String(crs, "UTF-8").trim
    if (proj4String.nonEmpty) CRS.fromString(proj4String.trim) else LatLng
  }

  def noDataValue: Option[Double] = noDataValue(GDALDataset.WARPED)

  def noDataValue(datasetType: DatasetType): Option[Double] = {
    require(acceptableDatasets contains datasetType)

    val nodata = Array.ofDim[Double](1)
    val success = Array.ofDim[Int](1)
    val returnValue = GDALWarp.get_band_nodata(token, datasetType.value, numberOfAttempts, 1, nodata, success)

    errorHandler(returnValue, { positiveValue =>
      new MalformedDataTypeException(
        s"Unable to determine NoData value. GDAL Exception Code: $positiveValue",
        positiveValue
      )
    })

    if (success(0) == 0) None else nodata(0).some
  }

  def dataType: Int = dataType(GDALDataset.WARPED)

  def dataType(datasetType: DatasetType): Int = {
    require(acceptableDatasets contains datasetType)

    val dataType = Array.ofDim[Int](1)
    val returnValue = GDALWarp.get_band_data_type(token, datasetType.value, numberOfAttempts, 1, dataType)

    errorHandler(returnValue, { positiveValue =>
      new MalformedDataTypeException(
        s"Unable to determine DataType. GDAL Error Code: $positiveValue",
        positiveValue
      )
    })

    dataType(0)
  }

  def cellSize: CellSize = cellSize(GDALDataset.WARPED)

  /**
    * https://github.com/mapbox/rasterio/blob/1.2b1/rasterio/_base.pyx#L865-L895
    *
    * Note for a reader:
    * affine.Affine(a, b, c, d, e, f)
    * GDALTransform(c, a, b, f, d, e)
    *
    * See https://rasterio.readthedocs.io/en/latest/topics/migrating-to-v1.html?highlight=Affine#affine-affine-vs-gdal-style-geotransforms
    * */
  def cellSize(datasetType: DatasetType): CellSize = {
    require(acceptableDatasets contains datasetType)
    cellSize(getTransform(datasetType))
  }

  def cellSize(transform: Array[Double]): CellSize = {
    val Array(_, xres, xskew, _, yskew, yres) = transform

    if(xskew == 0d && yskew == 0d) CellSize(math.abs(xres), math.abs(yres))
    else CellSize(math.sqrt(xres * xres + yskew * yskew), math.sqrt(xskew * xskew + yres * yres))
  }

  def cellType: CellType = cellType(GDALDataset.WARPED)

  def cellType(datasetType: DatasetType): CellType = {
    require(acceptableDatasets contains datasetType)

    val nd = noDataValue(datasetType)
    val dt = GDALDataType.intToGDALDataType(dataType(datasetType))
    /** The necessary metadata about the [[CellType]] is available only on the RasterBand metadata level, that is why band = 1 here. */
    lazy val md = getMetadata(ImageStructureDomain, 1)
    /** To handle the [[BitCellType]] it is possible to fetch NBITS information from the RasterBand metadata, **/
    lazy val bitsPerSample = md.get("NBITS").map(_.toInt)
    /** To handle the [[ByteCellType]] it is possible to fetch information about the sampleFormat from the RasterBand metadata. **/
    lazy val signedByte = md.get("PIXELTYPE").contains("SIGNEDBYTE")
    GDALUtils.dataTypeToCellType(datatype = dt, noDataValue = nd, typeSizeInBits = bitsPerSample, signedByte = signedByte)
  }

  def readTile(gb: GridBounds[Int] = GridBounds(dimensions), band: Int, datasetType: DatasetType = GDALDataset.WARPED): Tile = {
    require(acceptableDatasets contains datasetType)

    val GridBounds(xmin, ymin, xmax, ymax) = gb
    val srcWindow: Array[Int] = Array(xmin, ymin, xmax - xmin + 1, ymax - ymin + 1)
    val dstWindow: Array[Int] = Array(srcWindow(2), srcWindow(3))
    val ct = cellType(datasetType)
    val dt = dataType(datasetType)

    /**
      * GDAL allows [[BitCellType]] reads only as [[GDALWarp.GDT_Byte]].
      * In this case we need to read data as [[ByteCellType]] and to covert it later into the [[BitCellType]].
      */
    val rct = ct match {
      case BitCellType => ByteCellType
      case _           => ct
    }

    /** Allocation should rely on the underlying GDAL data type. */
    val bytes = Array.ofDim[Byte](dstWindow(0) * dstWindow(1) * rct.bytes)

    val returnValue = GDALWarp.get_data(token, datasetType.value, numberOfAttempts, srcWindow, dstWindow, band, dt, bytes)

    errorHandler(returnValue, { positiveValue =>
      new GDALIOException(
        s"Unable to read in data. GDAL Error Code: $positiveValue",
        positiveValue
      )
    })

    val tile = ArrayTile.fromBytes(bytes, rct, dstWindow(0), dstWindow(1))

    ct match {
      case BitCellType => tile.convert(ct)
      case _           => tile
    }
  }

  def readMultibandTile(gb: GridBounds[Int] = GridBounds(dimensions), bands: Seq[Int] = 1 to bandCount, datasetType: DatasetType = GDALDataset.WARPED): MultibandTile =
    MultibandTile(bands.map { readTile(gb, _, datasetType) })

  def readMultibandRaster(gb: GridBounds[Int] = GridBounds(dimensions), bands: Seq[Int] = 1 to bandCount, datasetType: DatasetType = GDALDataset.WARPED): Raster[MultibandTile] =
    Raster(readMultibandTile(gb, bands, datasetType), rasterExtent.rasterExtentFor(gb).extent)

  private def errorHandler(returnValue: Int, exception: Int => Throwable): Unit =
    if (returnValue <= 0) throw exception(math.abs(returnValue))
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
