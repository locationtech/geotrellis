package geotrellis.spark.io.hadoop.geotiff

import java.io.PrintWriter

import geotrellis.raster.io.geotiff.reader.TiffTagsReader
import geotrellis.util.Filesystem
import geotrellis.vector._
import geotrellis.vector.io._
import geotrellis.spark._
import geotrellis.spark.io._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.rdd.RDD
import spray.json._
import spray.json.DefaultJsonProtocol._
import java.net.URI
import java.nio.file.{Files, Paths}

import scala.collection.JavaConverters._

/**
  *
  * In case of a layer per tiff band, each band is a 'layer' in a common GeoTrellis layer sense
  * In case of a layer per tiff folder, each folder is a 'layer' in a common GeoTrellis layer sense
  *
  * @param data - input metadata
  * @param layerName - the whole input name
  * @param subLayerName - function to generate layer name from files in case of
  */

case class RDDGeoTiffInputMetadata(
  data: RDD[(ProjectedExtent, URI)],
  layerName: Option[String] = None,
  subLayerName: Option[String => String] = None
) extends GeoTiffInputMetadata[RDD]

case class CollectionGeoTiffInputMetadata(
  data: Seq[(ProjectedExtent, URI)],
  layerName: Option[String] = None,
  subLayerName: Option[String => String] = None
) extends GeoTiffInputMetadata[Seq]

/**
  * Represents fetching metadata from a file
  *
  * @param uri - path to a metadata JSON File
  */
case class FileVRT(uri: URI, conf: Configuration = new Configuration()) extends CollectionVRT[(ProjectedExtent, URI)] {
  def get: Seq[(ProjectedExtent, URI)] = {
    val path = new Path(uri)
    val fs = path.getFileSystem(conf)
    val stream = fs.open(path)
    val lines = scala.io.Source.fromInputStream(stream).getLines
    val json = lines.mkString(" ")
    stream.close()
    json.parseJson.convertTo[Seq[(ProjectedExtent, URI)]]
  }
}

case class PostgresVRT(table: String, conf: Configuration = new Configuration()) extends IteratorVRT[(ProjectedExtent, URI)] {
  def get: Iterator[(ProjectedExtent, URI)] = ???
}

trait CollectionVRT[T] extends VRT[Seq, T]
trait IteratorVRT[T] extends VRT[Iterator, T]

trait VRT[M[_], T] {
  def get: M[T]
}

case class FileCollectionGeoTiffInputMetadata(
  data: FileVRT,
  layerName: Option[String] = None,
  subLayerName: Option[String => String] = None
) extends GeoTiffInputMetadata[CollectionVRT]

trait GeoTiffInputMetadata[M[_]] {
  val data: M[(ProjectedExtent, URI)]
  // in case the folder is an entire layer
  val layerName: Option[String]
  // in case folder contains separate files and each file is a band
  val subLayerName: Option[String => String]
}


trait GeoTiffInput[M[_]] {
  val uri: URI
  def layerName: Option[String]
  def subLayerName: Option[String => String]
  def collect: M[(ProjectedExtent, URI)]
  def fetch: GeoTiffInputMetadata[M]
}

case class HadoopGeoTiffInput(
  uri: URI,
  layerName: Option[String] = None,
  subLayerName: Option[String => String] = Some(s => s.split("/").last.split("\\.").head)
) extends GeoTiffInput[Seq] {
  /** Selects a folder name or file name as a layerName */
  def collect: Seq[(ProjectedExtent, URI)] = {
    Files
      .walk(Paths.get(uri))
      .iterator()
      .asScala
      .toList
      .collect { case p if Files.isRegularFile(p) =>
        val tiffTags = TiffTagsReader.read(Filesystem.toMappedByteBuffer(p.toAbsolutePath.toString))
        ProjectedExtent(tiffTags.extent, tiffTags.crs) -> uri
      }

    null
  }

  def fetch: CollectionGeoTiffInputMetadata =
    CollectionGeoTiffInputMetadata(
      data = collect,
      layerName = layerName,
      subLayerName = subLayerName
    )

  def persist(path: URI): Unit = {
    //val text = fetch.toJson.prettyPrint
    //Filesystem.writeText(path.toString, text)
  }
}

case class HadoopVRTGeoTiffInput(
  uri: URI,
  table: URI,
  layerName: Option[String] = None, // persistent
  subLayerName: Option[String => String] = Some(s => s.split("/").last.split("\\.").head) // not persistent -> think about how to store it
) extends GeoTiffInput[CollectionVRT] {
  /** Selects a folder name or file name as a layerName */
  def collect: CollectionVRT[(ProjectedExtent, URI)] = {
    Files
      .walk(Paths.get(uri))
      .iterator()
      .asScala
      .toList
      .collect { case p if Files.isRegularFile(p) =>
        val tiffTags = TiffTagsReader.read(Filesystem.toMappedByteBuffer(p.toAbsolutePath.toString))
        ProjectedExtent(tiffTags.extent, tiffTags.crs) -> uri
      }

    null
  }



  def persist(path: Path, configuration: Configuration = new Configuration): Unit = {
    val fs = path.getFileSystem(configuration)

    if(fs.exists(path)) {
      fs.delete(path, false)
    }

    val fdos = fs.create(path)
    val out = new PrintWriter(fdos)
    try {
      //val s = fetch.toJson.prettyPrint
      //out.println(s)
    } finally {
      out.close()
      fdos.close()
    }
  }

  def fetch = ???
}
