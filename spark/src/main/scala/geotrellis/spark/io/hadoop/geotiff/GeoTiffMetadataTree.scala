package geotrellis.spark.io.hadoop.geotiff

import geotrellis.vector.ProjectedExtent
import geotrellis.spark.tiling._
import geotrellis.proj4.{CRS, WebMercator}

import com.vividsolutions.jts.index.strtree.STRtree
import java.io._

import collection.JavaConverters._

case class GeoTiffMetadataTree[T](index: STRtree, crs: CRS) {
  def insert(projectedExtent: ProjectedExtent, md: T): Unit =
    index.insert(projectedExtent.reproject(crs).jtsEnvelope, md)

  def query(projectedExtent: ProjectedExtent = ProjectedExtent(crs.worldExtent, crs)): List[T] = {
    val res = index
      .query(projectedExtent.reproject(crs).jtsEnvelope)
      .asScala
      .toList
      .asInstanceOf[List[T]]


    println(s"projectedExtent: ${projectedExtent}")
    println(s"res: ${res}")
    println(s"index.depth(): ${index.depth()}")

    res
  }

  def serialize: OutputStream = {
    val byteArrayStream = new ByteArrayOutputStream
    val out = new ObjectOutputStream(byteArrayStream)
    out.writeObject(index)
    out
  }

  def deserialize(is: InputStream): STRtree = {
    val in = new ObjectInputStream(is)
    val res = in.readObject.asInstanceOf[STRtree]
    in.close()
    res
  }
}

object GeoTiffMetadataTree {
  def fromGeoTiffMetadataList(list: List[GeoTiffMetadata], crs: CRS = WebMercator): GeoTiffMetadataTree[GeoTiffMetadata] = {
    val index = new STRtree()

    list.foreach { md =>
      index.insert(md.projectedExtent.reproject(crs).jtsEnvelope, md)
    }

    GeoTiffMetadataTree(index, crs)
  }

}
