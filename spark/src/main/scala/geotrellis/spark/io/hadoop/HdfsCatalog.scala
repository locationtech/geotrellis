package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io.Catalog
import geotrellis.spark.io.accumulo.{Layer, MetaDataCatalog}
import org.apache.accumulo.core.util.{Pair => JPair}
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import scala.reflect._

class HdfsCatalog(sc: SparkContext, metaDataCatalog: MetaDataCatalog) extends Catalog {
  //type Source = HdfsRddSource
  type DriverType[K] = HdfsDriver[K]

  var loaders: Map[ClassTag[_],  HdfsDriver[_]] = Map.empty
  def getLoader[K:ClassTag] = loaders.get(classTag[K]).map(_.asInstanceOf[HdfsDriver[K]])

  def register[K: ClassTag](loader: HdfsDriver[K]): Unit = loaders += classTag[K] -> loader

  def load[K:ClassTag](layerName: String, zoom: Int, filters: FilterSet[K]): Option[RasterRDD[K]] = {
    for {
      (table, metaData) <- metaDataCatalog.get(Layer(layerName, zoom))
      loader <- getLoader[K]
    } yield {
      val path: Path = ??? //came from metadata
      loader.load[K](sc)(path, metaData, filters)
    }
  }.flatten

  def save[K: ClassTag](rdd: RasterRDD[K], layer: String, path: Path): Unit = {
    //metaDataCatalog.save(rdd.metaData)
  }
}