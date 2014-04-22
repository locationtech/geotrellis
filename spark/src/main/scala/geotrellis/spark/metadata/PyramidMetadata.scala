/*
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
 */

package geotrellis.spark.metadata

import geotrellis._
import geotrellis.RasterType
import geotrellis.spark.ingest.GeoTiff
import geotrellis.spark.ingest.MetadataInputFormat
import geotrellis.spark.tiling.PixelExtent
import geotrellis.spark.tiling.TileExtent
import geotrellis.spark.tiling.TmsTiling
import geotrellis.spark.utils.HdfsUtils

import org.apache.commons.codec.binary.Base64
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.PrintWriter

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

  def toBase64: String = {
    val baos = new ByteArrayOutputStream
    val oos = new ObjectOutputStream(baos)
    oos.writeObject(this)
    val rawBytes = baos.toByteArray()
    oos.close
    baos.close

    new String(Base64.encodeBase64(rawBytes))
  }

  def writeToJobConf(conf: Configuration) = conf.set(PyramidMetadata.JobConfKey, toBase64)
}

object PyramidMetadata {
  final val MetaFile = "metadata"
  final val JobConfKey = "geotrellis.spark.metadata"

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
        }
        finally {
          in.close
        }
      case None =>
        sys.error(s"oops - couldn't find metadata here - ${metaPath.toUri.toString}")
    }

    JacksonWrapper.deserialize[PyramidMetadata](txt)
  }

  /*
   * Constructs a metadata from tiff files. All processing is done in local mode, i.e., 
   * outside Spark and in RAM. 
   * 
   * path - path to a tiff file or directory containing TIFF files. The directory can be 
   * arbitrarily deep, and will be recursively searched for all TIFF files
   */
  def fromTifFiles(tiffPath: Path, conf: Configuration): (Seq[Path], PyramidMetadata) = {
    val allFiles = HdfsUtils.listFiles(tiffPath, conf)

    val (files, optMetas) = 
      allFiles
        .map { file =>
          val meta = GeoTiff.getMetadata(file, conf)
          (file, meta)
         }
        .filter { case (file, meta) => meta.isDefined }
        .unzip

    val meta = optMetas.flatten.reduceLeft(_.merge(_))

    (files, fromGeoTiffMeta(meta))
  }

  /*
   * Constructs a metadata from tiff files. All processing is done in Spark 
   * 
   * path - path to a tiff file or directory containing TIFF files. The directory can be 
   * arbitrarily deep, and will be recursively searched for all TIFF files
   */
  def fromTifFiles(tiffPath: Path, conf: Configuration, sc: SparkContext): (Seq[Path], PyramidMetadata) = {
    val allFiles = HdfsUtils.listFiles(tiffPath, conf)

    val newConf = HdfsUtils.putFilesInConf(allFiles.mkString(","), conf)

    val (acceptedFiles, optMetas) = sc.newAPIHadoopRDD(newConf,
      classOf[MetadataInputFormat],
      classOf[String],
      classOf[Option[GeoTiff.Metadata]]).collect.unzip

    val files = acceptedFiles.map(new Path(_))
    val meta = optMetas.flatten.reduceLeft(_.merge(_))
    (files, fromGeoTiffMeta(meta))
  }

  def fromBase64(encoded: String): PyramidMetadata = {
    val bytes = Base64.decodeBase64(encoded.getBytes())

    val bais = new ByteArrayInputStream(bytes)
    val ois = new ObjectInputStream(bais)
    val meta = ois.readObject().asInstanceOf[PyramidMetadata]

    ois.close()
    bais.close()
    meta
  }

  def fromJobConf(conf: Configuration) = fromBase64(conf.get(PyramidMetadata.JobConfKey))

  def fromGeoTiffMeta(meta: GeoTiff.Metadata): PyramidMetadata = {
    val tileSize = TmsTiling.DefaultTileSize

    val zoom = math.max(TmsTiling.zoom(meta.pixelSize._1, tileSize),
      TmsTiling.zoom(meta.pixelSize._2, tileSize))

    val tileExtent = TmsTiling.extentToTile(meta.extent, zoom, tileSize)
    val pixelExtent = TmsTiling.extentToPixel(meta.extent, zoom, tileSize)

    PyramidMetadata(
      meta.extent, 
      tileSize, 
      meta.bands, 
      meta.nodata, 
      meta.rasterType, 
      zoom,
      Map(zoom.toString -> RasterMetadata(pixelExtent, tileExtent))
    )
  }
}

