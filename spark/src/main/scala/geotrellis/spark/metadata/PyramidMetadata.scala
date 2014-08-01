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

import geotrellis.raster._
import geotrellis.vector.Extent

import geotrellis.spark.ingest.GeoTiff
import geotrellis.spark.ingest.MetadataInputFormat
import geotrellis.spark.tiling.Bounds
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
 * Using Int instead of CellType for cellType (issue with nested case classes that take 
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
  awtCellType: Int,
  maxZoomLevel: Int,
  rasterMetadata: Map[String, RasterMetadata]) {

  def cellType: CellType = CellType.fromAwtType(awtCellType)

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
            this.awtCellType == other.awtCellType &&
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
              + cellType.hashCode)
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

    // if the lon/lat is past the world bounds (which it can be, say, -180.001), 
    // then our TmsTiling calculations go awry. So we cap it here to world bounds,
    // which is slightly smaller than the right and northern edge
    println(meta.extent)
    val cappedExtent = Bounds.World.intersection(meta.extent).get
    val tileExtent = TmsTiling.extentToTile(cappedExtent, zoom, tileSize)
    val pixelExtent = TmsTiling.extentToPixel(cappedExtent, zoom, tileSize)

    PyramidMetadata(
      cappedExtent, 
      tileSize, 
      meta.bands, 
      meta.nodata, 
      meta.cellType, 
      zoom,
      Map(zoom.toString -> RasterMetadata(pixelExtent, tileExtent))
    )
  }
}

