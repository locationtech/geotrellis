package geotrellis.spark.io.parquet

import java.io.PrintWriter

import geotrellis.spark._
import geotrellis.spark.io._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark._
import org.apache.spark.sql._
import org.apache.spark.sql.types._
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.util.matching.Regex

class ParqueteAttributeStore(val attributePath: Path)(implicit sc: SparkContext) extends BlobLayerAttributeStore {
  val ss = SparkSession.builder().getOrCreate()
  import ss.implicits._

  val schema = StructType(Seq(
    StructField("layerId", StringType, nullable = false),
    StructField("name", StringType, nullable = false),
    StructField("value", StringType, nullable = false)
  ))

  val SEP = "__.__"

  def layerIdString(layerId: LayerId): String =
    s"${layerId.name}${SEP}${layerId.zoom}"

  val fs = attributePath.getFileSystem(sc.hadoopConfiguration)

  // Create directory if it doesn't exist
  if(!fs.exists(attributePath)) {
    fs.mkdirs(attributePath)
  }

  lazy val frame = ss.read.parquet(attributePath.toString)
  //frame.map { t => t.getAs[Long]("key") }

  private def fetch(layerId: Option[LayerId], attributeName: String): DataFrame = {
    val query =
      layerId match {
        case Some(id) =>
          frame.filter($"layerId" === layerIdString(id) && $"name" === attributeName)
        case None =>
          frame.filter($"name" === attributeName)
      }

    query.select("value")
  }

  private def delete(layerId: LayerId, path: Path): Unit = {
    if(!layerExists(layerId)) throw new LayerNotFoundError(layerId)
    /*frame.drop*/
    /*HdfsUtils
      .listFiles(new Path(attributePath, path), hadoopConfiguration)
      .foreach(fs.delete(_, false))*/
    clearCache(layerId)
  }


  def read[T: JsonFormat](layerId: LayerId, attributeName: String): T = {
    val values = fetch(Some(layerId), attributeName)

    val size = values.count()
    if (size == 0) {
      throw new AttributeNotFoundError(attributeName, layerId)
    } else if (size > 1) {
      throw new LayerIOError(s"Multiple attributes found for $attributeName for layer $layerId")
    } else {
      val (_, result) = values.head().getAs[String]("value").parseJson.convertTo[(LayerId, T)]
      result
    }
  }

  def readAll[T: JsonFormat](attributeName: String): Map[LayerId, T] = ???
    /*fetch(None, attributeName).map {
      _.getAs[String]("value").parseJson.convertTo[(LayerId, T)]
    }.collect().toMap*/

  def write[T: JsonFormat](layerId: LayerId, attributeName: String, value: T): Unit = {

    val row = Row(layerIdString(layerId), attributeName, (layerId, value).toJson.compactPrint)

    val df = ss.createDataFrame(sc.parallelize(Seq(row)), schema)
    df.write.partitionBy("layerId", "name").mode("append").parquet(attributePath.toString)
  }

  def layerExists(layerId: LayerId): Boolean = ???
    /*HdfsUtils
      .listFiles(new Path(attributePath, s"*.json"), hadoopConfiguration)
      .exists { path: Path =>
        val List(name, zoomStr) = path.getName.split(SEP).take(2).toList
        layerId == LayerId(name, zoomStr.toInt)
      }*/

  def delete(layerId: LayerId): Unit = {
    delete(layerId, new Path(s"${layerId.name}${SEP}${layerId.zoom}${SEP}*.json"))
    clearCache(layerId)
  }

  def delete(layerId: LayerId, attributeName: String): Unit = {
    delete(layerId, new Path(s"${layerId.name}${SEP}${layerId.zoom}${SEP}${attributeName}.json"))
    clearCache(layerId, attributeName)
  }

  def layerIds: Seq[LayerId] = ???
    /*HdfsUtils
      .listFiles(new Path(attributePath, s"*.json"), hadoopConfiguration)
      .map { path: Path =>
        val List(name, zoomStr) = path.getName.split(SEP).take(2).toList
        LayerId(name, zoomStr.toInt)
      }
      .distinct*/

  def availableAttributes(layerId: LayerId): Seq[String] = {
    /*HdfsUtils
      .listFiles(layerWildcard(layerId), hadoopConfiguration)
      .map { path: Path =>
        val attributeRx(name, zoom, attribute) = path.getName
        attribute
      }
      .toVector*/

      Seq()
  }
}

object ParquetAttributeStore {
  final val SEP = "___"

  val attributeRx = {
    val slug = "[a-zA-Z0-9-]+"
    new Regex(s"""($slug)$SEP($slug)${SEP}($slug).json""", "layer", "zoom", "attribute")
  }

  /*def apply(rootPath: Path, config: Configuration): HadoopAttributeStore =
    new HadoopAttributeStore(rootPath, config)

  def apply(rootPath: Path)(implicit sc: SparkContext): HadoopAttributeStore =
    apply(rootPath, sc.hadoopConfiguration)*/
}
