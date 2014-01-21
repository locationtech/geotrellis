package geotrellis.spark.metadata
import geotrellis._
import geotrellis.RasterType
import geotrellis.data.GeoTiff
import geotrellis.data.GeoTiff.Metadata
import geotrellis.spark.ingest.Ingest
import geotrellis.spark.tiling.Bounds
import geotrellis.spark.tiling.PixelBounds
import geotrellis.spark.tiling.TileBounds
import geotrellis.spark.tiling.TmsTiling
import geotrellis.spark.utils.HdfsUtils
import geotrellis.spark.utils.SparkUtils

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

import java.io.PrintWriter
import java.net.URL

/* 
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

case class RasterMetadata(pixelBounds: PixelBounds, tileBounds: TileBounds)

case class PyramidMetadata(
  bounds: Bounds,
  tileSize: Int,
  bands: Int,
  nodata: Double,
  awtRasterType: Int,
  maxZoomLevel: Int,
  rasterMetadata: Map[String, RasterMetadata]) {

  def rasterType: RasterType = RasterType.fromAwtType(awtRasterType)

  def save(path: Path, conf: Configuration) = {
    val metaPath = new Path(path, PyramidMetadata.MetaFile)
    val fs = metaPath.getFileSystem(conf)
    val fdos = fs.create(metaPath)
    val out = new PrintWriter(fdos)
    out.println(JacksonWrapper.prettyPrint(this))
    out.close()
    fdos.close()
  }

  override def equals(that: Any): Boolean =
    that match {
      case other: PyramidMetadata => {
        (this.bounds == other.bounds &&
          this.tileSize == other.tileSize &&
          (	(isNoData(this.nodata) && isNoData(other.nodata)) || 
            (this.nodata == other.nodata) ) &&
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
              41 + bounds.hashCode)
              + tileSize.hashCode)
              + nodata.hashCode)
              + rasterType.hashCode)
              + maxZoomLevel.hashCode)
  +rasterMetadata.hashCode

}

object PyramidMetadata {
  val MetaFile = "metadata"

  def apply(path: Path, conf: Configuration) = {
    val metaPath = new Path(path, MetaFile)

    val txt = HdfsUtils.getLineScanner(metaPath, conf) match {
      case Some(in) =>
        try {
          in.mkString
        } finally {
          in.close
        }
      case None =>
        sys.error(s"oops - couldn't find metaPath")
    }
    JacksonWrapper.deserialize[PyramidMetadata](txt)
  }

  def fromTifFiles(path: Path, conf: Configuration): Tuple2[List[Path], PyramidMetadata] = {

    val fs = path.getFileSystem(conf)

    val allFiles = HdfsUtils.listFiles(path, conf)

    def getMetadata(file: Path) = {
      val url = new URL(file.toUri().toString())
      val meta = GeoTiff.getMetadata(url, Ingest.Default_Projection)
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
      if (acc.pixelDims != meta.pixelDims)
        sys.error("Error: All input tifs must have the same resolution")
      if (acc.rasterType != meta.rasterType)
        sys.error("Error: All input tifs must have same raster type")
      if (acc.nodata != meta.nodata)
        sys.error("Error: All input tifs must have same nodata value")

      acc.bounds.add(meta.bounds)
      acc
    }

    val tileSize = TmsTiling.DefaultTileSize

    val zoom = math.max(TmsTiling.zoom(meta.pixelDims._1, tileSize),
      TmsTiling.zoom(meta.pixelDims._2, tileSize))

    val (w, s, e, n) =
      (meta.bounds.getLowerCorner.getOrdinate(0),
        meta.bounds.getLowerCorner.getOrdinate(1),
        meta.bounds.getUpperCorner.getOrdinate(0),
        meta.bounds.getUpperCorner.getOrdinate(1))

    val bounds = Bounds(w, s, e, n)
    val tileBounds = TmsTiling.boundsToTile(bounds, zoom, tileSize)
    val (pixelLower, pixelUpper) =
      (TmsTiling.latLonToPixels(s, w, zoom, tileSize),
        TmsTiling.latLonToPixels(n, e, zoom, tileSize))
    val pixelBounds = PixelBounds(0, 0,
      pixelUpper.px - pixelLower.px, pixelUpper.py - pixelLower.py)

    (files,
      PyramidMetadata(bounds, tileSize, meta.bands, meta.nodata, meta.rasterType, zoom,
        Map(zoom.toString -> RasterMetadata(pixelBounds, tileBounds))))
  }

  def main(args: Array[String]) {
    val inPath = new Path("file:///home/akini/test/big_files")
    val conf = SparkUtils.createHadoopConfiguration

    val (files, meta) = PyramidMetadata.fromTifFiles(inPath, conf)
    println("------- FILES ------")
    println(files.mkString("\n"))
    println("\n\n\n")
    println("------- META ------")
    println(JacksonWrapper.prettyPrint(meta))

  }

}