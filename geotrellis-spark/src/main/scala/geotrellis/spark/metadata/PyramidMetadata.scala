package geotrellis.spark.metadata

import org.codehaus.jackson.map.ObjectMapper
import org.apache.spark.Logging
import java.io.File
import geotrellis.spark.tiling.Bounds
import geotrellis.spark.tiling.TileBounds
import geotrellis.spark.tiling.PixelBounds
import scala.io.Source
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import geotrellis.spark.utils.HdfsUtils
import scala.collection.mutable.ListBuffer
import geotrellis.spark.formats.TileIdWritable
import org.apache.hadoop.fs.Path
import org.apache.hadoop.conf.Configuration
import java.nio.ByteBuffer
import geotrellis.spark.utils.SparkUtils
import java.io.PrintWriter

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
  tileType: String,
  maxZoomLevel: Int,
  imageMetadata: Map[Int, PyramidMetadata.ImageMetadata]) {
  
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

  type ImageMetadata = Tuple2[PixelBounds, TileBounds]

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

  def main(args: Array[String]) {
    val inPath = new Path("file:///tmp/inmetadata")
    val conf = SparkUtils.createHadoopConfiguration
    val outPath = new Path("file:///tmp/outmetadata")
    val meta = PyramidMetadata(inPath, conf)
    meta.save(outPath, conf)
    //val meta = PyramidMetadata("/tmp/imagemetadatad")
    //println(JacksonWrapper.prettyPrint(meta))
    //    val meta = PyramidMetadata(
    //      Bounds(1, 1, 1, 1),
    //      512,
    //      1,
    //      List(-9999.0f),
    //      "float32",
    //      10,
    //      Map(1 -> new ImageMetadata(PixelBounds(0, 0, 0, 0), TileBounds(0, 0, 0, 0))))
    //    val ser = JacksonWrapper.serialize(meta)
    //    println("ser = " + ser)
    //        val deser = JacksonWrapper.deserialize[PyramidMetadata](ser)
    //        println("ser = " + ser + " and deser = " + deser)
  }
}