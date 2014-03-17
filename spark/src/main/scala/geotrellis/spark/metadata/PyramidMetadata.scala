/**************************************************************************
 * Copyright (c) 2014 DigitalGlobe.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **************************************************************************/

package geotrellis.spark.metadata
import geotrellis._
import geotrellis.RasterType
import geotrellis.data.GeoTiff
import geotrellis.data.GeoTiff.Metadata
import geotrellis.spark.cmd.Ingest
import geotrellis.spark.tiling.PixelExtent
import geotrellis.spark.tiling.TileExtent
import geotrellis.spark.tiling.TmsTiling
import geotrellis.spark.utils.HdfsUtils
import geotrellis.spark.utils.SparkUtils

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

import java.io.PrintWriter
import java.net.URL

/**
 * @author akini
 *
 * PyramidMetadata encapsulates the metadata for a pyramid. The on-disk representation is JSON
 * and this class provides utilities to read/save from/to JSON.
 *
 * The metadata has two types of attributes - pyramid level and raster level
 * Pyramid Level attributes - everything except rasterMetadata
 * Raster Level attributes - rasterMetadata
 *
 */
case class RasterMetadata(pixelExtent: PixelExtent, tileExtent: TileExtent)

/* ------Note to self------- 
 * Three workarounds that'd be good to resolve eventually:
 * 
 * Using Int instead of RasterType for rasterType (issue with nested case classes that take 
 * constructor arguments)
 * Using Map[String,..] instead of Map[Int,..] for rasterMetadata 
 * Right now I'm using Double for nodata because it's not clear whether we need an array or not. 
 * But before that, I had List[Float] instead of Array[Float], as I ran into an issue with using 
 * jackson-module-scala with nested collections, in particular nested Arrays. 
 * If I use Array[Float], I get back a json string that looks like an object reference instead 
 * of the array values themselves. I tried adding the @JsonDeserialize as suggested here: 
 * https://github.com/FasterXML/jackson-module-scala/wiki/FAQ
 * like so, @JsonDeserialize(contentAs = classOf[java.lang.Float]) defaultValues: Array[Float]
 * but still see the exception. The closest issue I saw to this is 
 * https://github.com/FasterXML/jackson-module-scala/issues/48
 * and the fix seems to be in 2.1.2 (I'm using 2.3.0) but doesn't seem to fix this problem
 */
case class PyramidMetadata(
  extent: Extent,
  tileSize: Int,
  bands: Int,
  nodata: Double,
  awtRasterType: Int,
  maxZoomLevel: Int,
  rasterMetadata: Map[String, RasterMetadata]) {

  def rasterType: RasterType = RasterType.fromAwtType(awtRasterType)

  def save(pyramid: Path, conf: Configuration) = {
    val metaPath = new Path(pyramid, PyramidMetadata.MetaFile)
    val fs = metaPath.getFileSystem(conf)
    val fdos = fs.create(metaPath)
    val out = new PrintWriter(fdos)
    out.println(JacksonWrapper.prettyPrint(this))
    out.close()
    fdos.close()
  }

  def metadataForBaseZoom: RasterMetadata = rasterMetadata(maxZoomLevel.toString)
  
  override def equals(that: Any): Boolean =
    that match {
      case other: PyramidMetadata => {
        (this.extent == other.extent &&
          this.tileSize == other.tileSize &&
          this.bands == other.bands &&
          ((isNoData(this.nodata) && isNoData(other.nodata)) ||
            (this.nodata == other.nodata)) &&
            this.awtRasterType == other.awtRasterType &&
            this.maxZoomLevel == other.maxZoomLevel &&
            this.rasterMetadata == other.rasterMetadata)
      }
      case _ => false
    }

  override def hashCode: Int =
    41 * (
      41 * (
        41 * (
          41 * (
            41 * (
              41 + extent.hashCode)
              + tileSize.hashCode)
              + nodata.hashCode)
              + rasterType.hashCode)
              + maxZoomLevel.hashCode)
  +rasterMetadata.hashCode

  override def toString = JacksonWrapper.prettyPrint(this)
}

object PyramidMetadata {
  final val MetaFile = "metadata"

  // currently we only support single band data
  final val MaxBands = 1
  
  /*
   * Reads the raster's metadata 
   * 
   * pyramid - A path to the pyramid 
   */
  def apply(pyramid: Path, conf: Configuration) = {
    val metaPath = new Path(pyramid, MetaFile)

    val txt = HdfsUtils.getLineScanner(metaPath, conf) match {
      case Some(in) =>
        try {
          in.mkString
        } finally {
          in.close
        }
      case None =>
        sys.error(s"oops - couldn't find metadata here - ${metaPath.toUri.toString}")
    }
    JacksonWrapper.deserialize[PyramidMetadata](txt)
  }

  /*
   * Constructs a metadata from tiff files 
   * 
   * path - path to a tiff file or directory containing TIFF files. The directory can be 
   * arbitrarily deep, and will be recursively searched for all TIFF files
   */
  def fromTifFiles(tiffPath: Path, conf: Configuration): Tuple2[List[Path], PyramidMetadata] = {

    val fs = tiffPath.getFileSystem(conf)

    val allFiles = HdfsUtils.listFiles(tiffPath, conf)

    def getMetadata(file: Path) = {
      val url = new URL(file.toUri().toString())
      val meta = GeoTiff.getMetadata(url, Ingest.DefaultProjection)
      (file, meta)
    }
    def filterNone(fileMeta: Tuple2[Path, Option[Metadata]]) = {
      val (file, meta) = fileMeta
      meta match {
        case Some(m) => true
        case None    => false
      }
    }
    val (files, optMetas) = allFiles.map(getMetadata(_)).filter(filterNone(_)).unzip
    val meta = optMetas.flatten.reduceLeft { (acc, meta) =>
      if (acc.bands != meta.bands)
        sys.error("Error: All input tifs must have the same number of bands")
      if (acc.pixelSize != meta.pixelSize)
        sys.error("Error: All input tifs must have the same resolution")
      if (acc.rasterType != meta.rasterType)
        sys.error("Error: All input tifs must have same raster type")
      if (acc.nodata != meta.nodata)
        sys.error("Error: All input tifs must have same nodata value")

      acc.bounds.add(meta.bounds)
      acc
    }

    val tileSize = TmsTiling.DefaultTileSize

    val zoom = math.max(TmsTiling.zoom(meta.pixelSize._1, tileSize),
      TmsTiling.zoom(meta.pixelSize._2, tileSize))

    val (w, s, e, n) =
      (meta.bounds.getLowerCorner.getOrdinate(0),
        meta.bounds.getLowerCorner.getOrdinate(1),
        meta.bounds.getUpperCorner.getOrdinate(0),
        meta.bounds.getUpperCorner.getOrdinate(1))

    val extent = Extent(w, s, e, n)
    val tileExtent = TmsTiling.extentToTile(extent, zoom, tileSize)
    val pixelExtent = TmsTiling.extentToPixel(extent, zoom, tileSize)

    (files,
      PyramidMetadata(extent, tileSize, meta.bands, meta.nodata, meta.rasterType, zoom,
        Map(zoom.toString -> RasterMetadata(pixelExtent, tileExtent))))
  }
}