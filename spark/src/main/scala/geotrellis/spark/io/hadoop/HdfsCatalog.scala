package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop.formats._

import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import scala.reflect._
import scala.util.{Failure, Success, Try}

// class HadoopCatalog(sc: SparkContext, metaDataCatalog: MetaDataCatalog) extends Catalog {
//   //type Source = HdfsRddSource
//   type DriverType[K] = HdfsDriver[K]

//   var drivers: Map[ClassTag[_],  HdfsDriver[_]] = Map.empty
//   def getDriver[K:ClassTag]: Try[HdfsDriver[K]] = drivers.get(classTag[K]) match {
//     case Some(driver) => Success(driver.asInstanceOf[HdfsDriver[K]])
//     case None         => Failure(new CatalogError(s"HdfsDriver not found for key type '${classTag[K]}'"))
//   }

//   def load[K: ClassTag: Ordering: HadoopWritable](layerName: String, zoom: Int, filters: FilterSet[K]): Try[RasterRDD[K]] = {
//     for {
//       md <- metaDataCatalog.load(LayerId(layerName, zoom))
//       loader <- getDriver[K]
//     } yield {
//       val path: Path = ??? //came from metadata
//       val metaData: LayerMetaData = ??? //came from md
//       loader.load[K](sc)(path, metaData, filters)
//     }
//   }.flatten

//   def save[K: ClassTag](rdd: RasterRDD[K], layer: String, path: Path): Try[Unit] = {
//     ???
//     //metaDataCatalog.save(rdd.metaData)
//   }
// }
