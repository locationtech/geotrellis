package geotrellis.spark.metadata
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
 * Using List[Float] instead of Array[Float] for defaultValues is a workaround for an issue
 * with using jackson-module-scala with nested collections, in particular nested Arrays
 * If I use Array[Float], I get back a json string that looks like an object reference instead
 * of the array values themselves. I tried adding the @JsonDeserialize as suggested here 
 * https://github.com/FasterXML/jackson-module-scala/wiki/FAQ
 * like so, @JsonDeserialize(contentAs = classOf[java.lang.Float]) defaultValues: Array[Float]
 * but still see the exception. The closest issue I saw to this is 
 * https://github.com/FasterXML/jackson-module-scala/issues/48
 * and the fix seems to be in 2.1.2 (I'm using 2.3.0) but doesn't seem to fix this problem
 */

case class PyramidMetadata(
  bounds: Bounds,
  tileSize: Int,
  bands: Int,
  defaultValues: List[Float],
  tileType: RasterType,
  maxZoomLevel: Int,
  rasterMetadata: Map[Int, PyramidMetadata.RasterMetadata]) {

  def save(path: Path, conf: Configuration) = {
    val metaPath = new Path(path, PyramidMetadata.MetaFile)
    println("writing metadata to " + metaPath)
    val fs = metaPath.getFileSystem(conf)
    val fdos = fs.create(metaPath)
    val out = new PrintWriter(fdos)
    out.println(JacksonWrapper.prettyPrint(this))
    out.close()
    fdos.close()
  }
}

object PyramidMetadata {
  val MetaFile = "metadata"

  type RasterMetadata = Tuple2[PixelBounds, TileBounds]

  def apply(path: Path, conf: Configuration) = {
    val metaPath = new Path(path, MetaFile)

    val txt = HdfsUtils.getLineScanner(metaPath, conf) match {
      case Some(in) =>
        try {
          in.mkString
        }
        finally {
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

    val bounds = new Bounds(w, s, e, n)
    val tileBounds = TmsTiling.boundsToTile(bounds, zoom, tileSize)
    val (pixelLower, pixelUpper) =
      (TmsTiling.latLonToPixels(s, w, zoom, tileSize),
        TmsTiling.latLonToPixels(n, e, zoom, tileSize))
    val pixelBounds = new PixelBounds(0, 0,
      pixelUpper.px - pixelLower.px, pixelUpper.py - pixelLower.py)

    (files, new PyramidMetadata(bounds, tileSize, meta.bands, List(-9999.0f),
      meta.rasterType, zoom,
      Map(1 -> new RasterMetadata(pixelBounds, tileBounds))))
  }

  def main(args: Array[String]) {
    //    val inPath = new Path("file:////    val inPath = new Path("file:///tmp/inmetadata")
    //    val conf = SparkUtils.createHadoopConfiguration/tmp/inmetadata")
    //    val conf = SparkUtils.createHadoopConfiguration
    //    val outPath = new Path("file:///tmp/outmetadata")
    //    val meta = PyramidMetadata(inPath, conf)
    //    meta.save(outPath, conf)
    //val meta = PyramidMetadata("/tmp/imagemetadatad")
    //println(JacksonWrapper.prettyPrint(meta))
    //    val meta = PyramidMetadata(
    //      Bounds(1, 1, 1, 1),
    //      512,
    //      1,
    //      List(-9999.0f),
    //      "float32",
    //      10,
    //      Map(1 -> new Itmp/inmetadatamageMetadata(PixelBounds(0, 0, 0, 0), TileBounds(0, 0, 0, 0))))
    //    val ser = JacksonWrapper.serialize(meta)
    //    println("ser = " + ser)
    //        val deser = JacksonWrapper.deserialize[PyramidMetadata](ser)
    //        println("ser = " + ser + " and deser = " + deser)

    val inPath = new Path("file:///home/akini/test/big_files")
    val conf = SparkUtils.createHadoopConfiguration

    val (files, meta) = PyramidMetadata.fromTifFiles(inPath, conf)
    println("------- FILES ------")
    println(files.mkString("\n"))
    println("\n\n\n")
    println("------- META ------")
    println(meta)

  }

}