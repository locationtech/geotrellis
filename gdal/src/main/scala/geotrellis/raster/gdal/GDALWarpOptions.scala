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
import geotrellis.raster.{ConvertTargetCellType, TargetCellType}
import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.raster.io.geotiff.OverviewStrategy
import geotrellis.proj4.CRS
import geotrellis.vector.Extent

import cats.Monad
import cats.instances.option._
import cats.syntax.option._


/**
  * GDALWarpOptions basically should cover https://www.gdal.org/gdalwarp.html
  */
case class GDALWarpOptions(
  /** -of, Select the output format. The default is GeoTIFF (GTiff). Use the short format name. */
  outputFormat: Option[String] = Some("VRT"),
  /** -r, Resampling method to use, visit https://www.gdal.org/gdalwarp.html for details. */
  resampleMethod: Option[ResampleMethod] = None,
  /** -et, error threshold for transformation approximation */
  errorThreshold: Option[Double] = None,
  /** -tr, set output file resolution (in target georeferenced units) */
  cellSize: Option[CellSize] = None,
  /** -tap, aligns the coordinates of the extent of the output file to the values of the target resolution,
    * such that the aligned extent includes the minimum extent.
    *
    * In terms of GeoTrellis it's very similar to [[GridExtent]].createAlignedGridExtent:
    *
    * newMinX = floor(envelop.minX / xRes) * xRes
    * newMaxX = ceil(envelop.maxX / xRes) * xRes
    * newMinY = floor(envelop.minY / yRes) * yRes
    * newMaxY = ceil(envelop.maxY / yRes) * yRes
    *
    * if (xRes == 0) || (yRes == 0) than GDAL calculates it using the extent and the cellSize
    * xRes = (maxX - minX) / cellSize.width
    * yRes = (maxY - minY) / cellSize.height
    *
    * If -tap parameter is NOT set, GDAL increases extent by a half of a pixel, to avoid missing points on the border.
    *
    * The actual code reference: https://github.com/OSGeo/gdal/blob/v2.3.2/gdal/apps/gdal_rasterize_lib.cpp#L402-L461
    * The actual part with the -tap logic: https://github.com/OSGeo/gdal/blob/v2.3.2/gdal/apps/gdal_rasterize_lib.cpp#L455-L461
    *
    * The initial PR that introduced that feature in GDAL 1.8.0: https://trac.osgeo.org/gdal/attachment/ticket/3772/gdal_tap.patch
    * A discussion thread related to it: https://lists.osgeo.org/pipermail/gdal-dev/2010-October/thread.html#26209
    *
    */
  alignTargetPixels: Boolean = true,
  dimensions: Option[(Int, Int)] = None, // -ts
  /** -s_srs, source spatial reference set */
  sourceCRS: Option[CRS] = None,
  /** -t_srs, target spatial reference set */
  targetCRS: Option[CRS] = None,
  /** -te, set georeferenced extents of output file to be created (with a CRS specified) */
  te: Option[Extent] = None,
  teCRS: Option[CRS] = None,
  /** -srcnodata, set nodata masking values for input bands (different values can be supplied for each band) */
  srcNoData: List[String] = Nil,
  /** -dstnodata, set nodata masking values for output bands (different values can be supplied for each band) */
  dstNoData: List[String] = Nil,
  /** -ovr,  To specify which overview level of source files must be used.
    *        The default choice, AUTO, will select the overview level whose resolution is the closest to the target resolution.
    *        Specify an integer value (0-based, i.e. 0=1st overview level) to select a particular level.
    *        Specify AUTO-n where n is an integer greater or equal to 1, to select an overview level below the AUTO one.
    *        Or specify NONE to force the base resolution to be used (can be useful if overviews have been generated with a low quality resampling method, and the warping is done using a higher quality resampling method).
    */
  ovr: Option[OverviewStrategy] = Some(OverviewStrategy.DEFAULT),
  /** -to, set a transformer option suitable to pass to [GDALCreateGenImgProjTransformer2()](https://www.gdal.org/gdal__alg_8h.html#a94cd172f78dbc41d6f407d662914f2e3) */
  to: List[(String, String)] = Nil,
  /** -novshiftgrid, Disable the use of vertical datum shift grids when one of the source or target SRS has an explicit vertical datum, and the input dataset is a single band dataset. */
  novShiftGrid: Boolean = false,
  /** -order n, order of polynomial used for warping (1 to 3). The default is to select a polynomial order based on the number of GCPs.*/
  order: Option[Int] = None,
  /** -tps, force use of thin plate spline transformer based on available GCPs. */
  tps: Boolean = false,
  /** -rps, force use of RPCs. */
  rps: Boolean = false,
  /** -geoloc, force use of Geolocation Arrays. */
  geoloc: Boolean = false,
  /** -refine_gcps, refines the GCPs by automatically eliminating outliers. Outliers will be eliminated until minimum_gcps
    * are left or when no outliers can be detected. The tolerance is passed to adjust when a GCP will be eliminated.
    * Not that GCP refinement only works with polynomial interpolation. The tolerance is in pixel units if no projection is available,
    * otherwise it is in SRS units. If minimum_gcps is not provided, the minimum GCPs according to the polynomial model is used.
    */
  refineGCPs: Option[(Double, Int)] = None,
  /** -wo, set a warp option. The GDALWarpOptions::papszWarpOptions docs show all options. Multiple -wo options may be listed. */
  wo: List[(String, String)] = Nil,
  /** -ot, for the output bands to be of the indicated data type. */
  outputType: Option[String] = None,
  /** -wt, working pixel data type. The data type of pixels in the source image and destination image buffers. */
  wt: Option[String] = None,
  /** -srcalpha, force the last band of a source image to be considered as a source alpha band. */
  srcAlpha: Boolean = false,
  /** -nosrcalpha, prevent the alpha band of a source image to be considered as such (it will be warped as a regular band). */
  noSrcAlpha: Boolean = false,
  /** -dstalpha, create an output alpha band to identify nodata (unset/transparent) pixels. */
  dstAlpha: Boolean = false,
  /** -wm, set the amount of memory (in megabytes) that the warp API is allowed to use for caching. */
  wm: Option[Int] = None,
  /** -multi, Use multithreaded warping implementation. Two threads will be used to process chunks of image and perform input/output operation simultaneously.
    * Note that computation is not multithreaded itself. To do that, you can use the -wo NUM_THREADS=val/ALL_CPUS option, which can be combined with -multi.
    */
  multi: Boolean = false,
  /** -q, be quiet. */
  q: Boolean = false,
  /** -co, passes a creation option to the output format driver. Multiple -co options may be listed. https://www.gdal.org/formats_list.html */
  co: List[(String, String)] = Nil,
  /** -cutline, enable use of a blend cutline from the name OGR support datasource. */
  cutline: Option[String] = None,
  /** -cl, select the named layer from the cutline datasource. */
  cl: Option[String] = None,
  /** -cwhere, restrict desired cutline features based on attribute query. */
  cwhere: Option[String] = None,
  /** -csql, select cutline features using an SQL query instead of from a layer with -cl. */
  csql: Option[String] = None,
  /** -cblend, set a blend distance to use to blend over cutlines (in pixels). */
  cblend: Option[String] = None,
  /** -crop_to_cutline, crop the extent of the target dataset to the extent of the cutline. */
  cropToCutline: Boolean = false,
  /** -overwrite, overwrite the target dataset if it already exists. */
  overwrite: Boolean = false,
  /** -nomd, do not copy metadata. Without this option, dataset and band metadata (as well as some band information)
    * will be copied from the first source dataset. Items that differ between source datasets will be set to * (see -cvmd option).
    */
  nomd: Boolean = false,
  /** -cvmd, value to set metadata items that conflict between source datasets (default is "*"). Use "" to remove conflicting items. */
  cvmd: Option[String] = None,
  /** -setci, set the color interpretation of the bands of the target dataset from the source dataset. */
  setci: Boolean = false,
  /** -oo, dataset open option (format specific). */
  oo: List[(String, String)] = Nil,
  /** -doo, output dataset open option (format specific). */
  doo: List[(String, String)] = Nil,
  /** -srcfile, the source file name(s). */
  srcFile: List[String] = Nil,
  /** -dstfile, the destination file name. */
  dstFile: Option[String] = None
) {
  lazy val name: String = toWarpOptionsList.map(_.toLowerCase).mkString("_")

  def toWarpOptionsList: List[String] = {
    outputFormat.toList.flatMap { of => List("-of", of) } :::
    resampleMethod.toList.flatMap { method => List("-r", s"${GDALUtils.deriveResampleMethodString(method)}") } :::
    errorThreshold.toList.flatMap { et => List("-et", s"${et}") } :::
    cellSize.toList.flatMap { cz =>
      // the -tap parameter can only be set if -tr is set as well
      val tr = List("-tr", s"${cz.width}", s"${cz.height}")
      if (alignTargetPixels) "-tap" +: tr else tr
    } ::: dimensions.toList.flatMap { case (c, r) => List("-ts", s"$c", s"$r") } :::
    sourceCRS.toList.flatMap { source => List("-s_srs", source.toProj4String) } :::
    targetCRS.toList.flatMap { target => List("-t_srs", target.toProj4String) } :::
    ovr.toList.flatMap { o => List("-ovr", GDALUtils.deriveOverviewStrategyString(o)) } :::
    te.toList.flatMap { ext =>
      List("-te", s"${ext.xmin}", s"${ext.ymin}", s"${ext.xmax}", s"${ext.ymax}") :::
        teCRS.orElse(targetCRS).toList.flatMap { tcrs => List("-te_srs", s"${tcrs.toProj4String}") }
    } ::: { if(srcNoData.nonEmpty) "-srcnodata" +: srcNoData else Nil } :::
    { if(dstNoData.nonEmpty) "-dstnodata" +: dstNoData else Nil } :::
    { if(to.nonEmpty) { "-to" +: to.map { case (k, v) => s"$k=$v" } } else Nil } :::
    { if(novShiftGrid) List("-novshiftgrid") else Nil } :::
    order.toList.flatMap { n => List("-order", s"$n") } :::
    { if(tps) List("-tps") else Nil } ::: { if(rps) List("-rps") else Nil } :::
    { if(geoloc) List("-geoloc") else Nil } ::: refineGCPs.toList.flatMap { case (tolerance, minimumGCPs) =>
      List("-refine_gcps", s"$tolerance", s"$minimumGCPs")
    } ::: { if(wo.nonEmpty) { "-wo" +: wo.map { case (k, v) => s"$k=$v" } } else Nil } :::
    outputType.toList.flatMap { ot => List("-ot", s"$ot") } ::: wt.toList.flatMap { wt => List("-wt", s"$wt") } :::
    { if(srcAlpha) List("-srcalpha") else Nil } ::: { if(noSrcAlpha) List("-nosrcalpha") else Nil } :::
    { if(dstAlpha) List("-dstalpha") else Nil } ::: wm.toList.flatMap { wm => List("-wm", s"$wm") } :::
    { if(multi) List("-multi") else Nil } ::: { if(q) List("-q") else Nil } :::
    { if(co.nonEmpty) { "-co" +: co.map { case (k, v) => s"$k=$v" } } else Nil } :::
    cutline.toList.flatMap { cutline => List("-cutline", s"$cutline") } :::
    cl.toList.flatMap { cl => List("-cl", s"$cl") } ::: cwhere.toList.flatMap { cw => List("-cwhere", s"$cw") } :::
    csql.toList.flatMap { csql => List("-csql", s"$csql") } ::: cblend.toList.flatMap { cblend => List("-cblend", s"$cblend") } :::
    { if(cropToCutline) List("-crop_to_cutline") else Nil } ::: { if(overwrite) List("-overwrite") else Nil } :::
    { if(nomd) List("-nomd") else Nil } ::: cvmd.toList.flatMap { cvmd => List("-cvmd", s"$cvmd") } :::
    { if(setci) List("-setci") else Nil } ::: { if(oo.nonEmpty) { "-oo" +: oo.map { case (k, v) => s"$k=$v" } } else Nil } :::
    { if(doo.nonEmpty) { "-doo" +: doo.map { case (k, v) => s"$k=$v" } } else Nil } :::
    { if(srcFile.nonEmpty) { "-srcfile" +: srcFile } else Nil } ::: dstFile.toList.flatMap { df => List("-dstfile", s"$df") }
  }

  def combine(that: GDALWarpOptions): GDALWarpOptions = {
    if (that == this) this
    else this.copy(
      outputFormat orElse that.outputFormat,
      resampleMethod orElse that.resampleMethod,
      errorThreshold orElse that.errorThreshold,
      cellSize orElse that.cellSize,
      alignTargetPixels,
      dimensions orElse that.dimensions,
      sourceCRS orElse that.sourceCRS,
      targetCRS orElse that.targetCRS,
      te orElse that.te,
      teCRS orElse that.teCRS,
      { if(srcNoData.isEmpty) that.srcNoData else srcNoData },
      { if(dstNoData.isEmpty) that.dstNoData else dstNoData },
      ovr orElse that.ovr,
      { if (to.isEmpty) that.to else to },
      novShiftGrid,
      order orElse that.order,
      tps,
      rps,
      geoloc,
      refineGCPs orElse that.refineGCPs,
      { if(wo.isEmpty) that.wo else wo },
      outputType orElse that.outputType,
      wt orElse that.wt,
      srcAlpha,
      noSrcAlpha,
      dstAlpha,
      wm orElse that.wm,
      multi,
      q,
      { if(co.isEmpty) that.co else co },
      cutline orElse that.cutline,
      cl orElse that.cl,
      cwhere orElse that.cwhere,
      csql orElse that.csql,
      cblend orElse that.cblend,
      cropToCutline,
      overwrite,
      nomd,
      cvmd orElse that.cvmd,
      setci,
      { if(oo.isEmpty) that.oo else oo },
      { if(doo.isEmpty) that.doo else doo },
      { if(srcFile.isEmpty) that.srcFile else srcFile },
      dstFile orElse dstFile
    )
  }

  def isEmpty: Boolean = this == GDALWarpOptions.EMPTY
  def datasetType: DatasetType = if(isEmpty) GDALDataset.SOURCE else GDALDataset.WARPED

  /** Adjust GDAL options to represents reprojection with following parameters.
   * This call matches semantics and arguments of {@see RasterSource#reproject}
   */
  def reproject(rasterExtent: GridExtent[Long], sourceCRS: CRS, targetCRS: CRS, resampleTarget: ResampleTarget = DefaultTarget, resampleMethod: ResampleMethod = NearestNeighbor): GDALWarpOptions = {
    val reprojectOptions = ResampleTarget.toReprojectOptions(rasterExtent, resampleTarget, resampleMethod)
    val reprojectedRasterExtent = rasterExtent.reproject(sourceCRS, targetCRS, reprojectOptions)

    resampleTarget match {
      case TargetDimensions(cols, rows) =>
        this.copy(
          te             = reprojectedRasterExtent.extent.some,
          cellSize       = None,
          targetCRS      = targetCRS.some,
          sourceCRS      = this.sourceCRS orElse sourceCRS.some,
          resampleMethod = reprojectOptions.method.some,
          dimensions     = (cols.toInt, rows.toInt).some
        )
      case _ =>
        val re = {
          val targetRasterExtent = resampleTarget(reprojectedRasterExtent).toRasterExtent
          if(this.alignTargetPixels) targetRasterExtent.alignTargetPixels else targetRasterExtent
        }

        this.copy(
          cellSize       = re.cellSize.some,
          te             = re.extent.some,
          targetCRS      = targetCRS.some,
          sourceCRS      = this.sourceCRS orElse sourceCRS.some,
          resampleMethod = reprojectOptions.method.some
        )
    }
  }

  /** Adjust GDAL options to represents resampling with following parameters .
   * This call matches semantics and arguments of {@see RasterSource#resample}
   */
  def resample(gridExtent: => GridExtent[Long], resampleTarget: ResampleTarget): GDALWarpOptions =
    resampleTarget match {
      case TargetDimensions(cols, rows) =>
        this.copy(te = gridExtent.extent.some, cellSize = None, dimensions = (cols.toInt, rows.toInt).some)

      case _ =>
        val re = {
          val targetRasterExtent = resampleTarget(gridExtent).toRasterExtent
          if(this.alignTargetPixels) targetRasterExtent.alignTargetPixels else targetRasterExtent
        }

        this.copy(te = re.extent.some, cellSize = re.cellSize.some)
    }

  /** Adjust GDAL options to represents conversion to desired cell type.
   * This call matches semantics and arguments of {@see RasterSource#convert}
   */
  def convert(targetCellType: TargetCellType, noDataValue: Option[Double], dimensions: Option[(Int, Int)]): GDALWarpOptions = {
    val convertOptions =
      GDALWarpOptions
        .createConvertOptions(targetCellType, noDataValue)
        .map(_.copy(dimensions = this.cellSize.fold(dimensions)(_ => None)))
        .toList

    (convertOptions :+ this).reduce(_ combine _)
  }

  override def toString: String = s"GDALWarpOptions(${toWarpOptionsList.mkString(" ")})"
}

object GDALWarpOptions {
  val EMPTY = GDALWarpOptions()

  implicit def lift2Monad[F[_]: Monad](options: GDALWarpOptions): F[GDALWarpOptions] = Monad[F].pure(options)

  def createConvertOptions(targetCellType: TargetCellType, noDataValue: Option[Double]): Option[GDALWarpOptions] = targetCellType match {
    case ConvertTargetCellType(target) =>
      target match {
        case BitCellType => throw new Exception("Cannot convert GDALRasterSource to the BitCellType")
        case ByteConstantNoDataCellType =>
          GDALWarpOptions(
            outputType = Some("Byte"),
            dstNoData = List(Byte.MinValue.toString),
            srcNoData = noDataValue.map(_.toString).toList
          ).some
        case ByteCellType =>
          GDALWarpOptions(
            outputType = Some("Byte"),
            dstNoData = List("None"),
            srcNoData = noDataValue.map(_.toString).toList
          ).some
        case ByteUserDefinedNoDataCellType(value) =>
          GDALWarpOptions(
            outputType = Some("Byte"),
            dstNoData = List(value.toString),
            srcNoData = noDataValue.map(_.toString).toList
          ).some

        case UByteConstantNoDataCellType =>
          GDALWarpOptions(
            outputType = Some("Byte"),
            dstNoData = List(0.toString),
            srcNoData = noDataValue.map(_.toString).toList
          ).some
        case UByteCellType =>
          GDALWarpOptions(
            outputType = Some("Byte"),
            dstNoData = List("none"),
            srcNoData = noDataValue.map(_.toString).toList
          ).some
        case UByteUserDefinedNoDataCellType(value) =>
          GDALWarpOptions(
            outputType = Some("Byte"),
            dstNoData = List(value.toString),
            srcNoData = noDataValue.map(_.toString).toList
          ).some

        case ShortConstantNoDataCellType =>
          GDALWarpOptions(
            outputType = Some("Int16"),
            dstNoData = List(Short.MinValue.toString),
            srcNoData = noDataValue.map(_.toString).toList
          ).some
        case ShortCellType =>
          GDALWarpOptions(
            outputType = Some("Int16"),
            dstNoData = List("None"),
            srcNoData = noDataValue.map(_.toString).toList
          ).some
        case ShortUserDefinedNoDataCellType(value) =>
          GDALWarpOptions(
            outputType = Some("Int16"),
            dstNoData = List(value.toString),
            srcNoData = noDataValue.map(_.toString).toList
          ).some

        case UShortConstantNoDataCellType =>
          GDALWarpOptions(
            outputType = Some("UInt16"),
            dstNoData = List(0.toString),
            srcNoData = noDataValue.map(_.toString).toList
          ).some
        case UShortCellType =>
          GDALWarpOptions(
            outputType = Some("UInt16"),
            dstNoData = List("None"),
            srcNoData = noDataValue.map(_.toString).toList
          ).some
        case UShortUserDefinedNoDataCellType(value) =>
          GDALWarpOptions(
            outputType = Some("UInt16"),
            dstNoData = List(value.toString),
            srcNoData = noDataValue.map(_.toString).toList
          ).some

        case IntConstantNoDataCellType =>
          Option(GDALWarpOptions(
            outputType = Some("Int32"),
            dstNoData = List(Int.MinValue.toString),
            srcNoData = noDataValue.map(_.toString).toList
          ))
        case IntCellType =>
          GDALWarpOptions(
            outputType = Some("Int32"),
            dstNoData = List("None"),
            srcNoData = noDataValue.map(_.toString).toList
          ).some
        case IntUserDefinedNoDataCellType(value) =>
          GDALWarpOptions(
            outputType = Some("Int32"),
            dstNoData = List(value.toString),
            srcNoData = noDataValue.map(_.toString).toList
          ).some

        case FloatConstantNoDataCellType =>
          GDALWarpOptions(
            outputType = Some("Float32"),
            dstNoData = List(Float.NaN.toString),
            srcNoData = noDataValue.map(_.toString).toList
          ).some
        case FloatCellType =>
          GDALWarpOptions(
            outputType = Some("Float32"),
            dstNoData = List("NaN"),
            srcNoData = noDataValue.map(_.toString).toList
          ).some
        case FloatUserDefinedNoDataCellType(value) =>
          GDALWarpOptions(
            outputType = Some("Float32"),
            dstNoData = List(value.toString),
            srcNoData = noDataValue.map(_.toString).toList
          ).some

        case DoubleConstantNoDataCellType =>
          GDALWarpOptions(
            outputType = Some("Float64"),
            dstNoData = List(Double.NaN.toString),
            srcNoData = noDataValue.map(_.toString).toList
          ).some
        case DoubleCellType =>
          GDALWarpOptions(
            outputType = Some("Float64"),
            dstNoData = List("NaN"),
            srcNoData = noDataValue.map(_.toString).toList
          ).some
        case DoubleUserDefinedNoDataCellType(value) =>
          GDALWarpOptions(
            outputType = Some("Float64"),
            dstNoData = List(value.toString),
            srcNoData = noDataValue.map(_.toString).toList
          ).some
      }
    case _ => None
  }
}
