package geotrellis.spark.ingest

import geotrellis.raster.CellType
import geotrellis.spark._
import geotrellis.spark.cmd.args.{AccumuloArgs, SparkArgs}
import geotrellis.spark.tiling._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats.NetCdfBand
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import org.apache.hadoop.fs.Path
import org.apache.spark._
import com.quantifind.sumac.ArgMain
import org.apache.spark.rdd.PairRDDFunctions

import scala.reflect.ClassTag

/**
 * Ingests raw multi-band NetCDF tiles into a re-projected and tiled RasterRDD
 */
object NetCDFIngestCommand extends ArgMain[AccumuloIngestArgs] with Logging {
  def main(args: AccumuloIngestArgs): Unit = {
    System.setProperty("com.sun.media.jai.disableMediaLib", "true")

    val conf = args.hadoopConf
    conf.set("io.map.index.interval", "1")

    implicit val sparkContext = args.sparkContext("Ingest")

    implicit val tiler: Tiler[NetCdfBand, SpaceTimeKey] = {
      val getExtent = (inKey: NetCdfBand) => inKey.extent
      val createKey = (inKey: NetCdfBand, spatialComponent: SpatialKey) =>
        SpaceTimeKey(spatialComponent, inKey.time)

      Tiler(getExtent, createKey)
    }

    val accumulo = AccumuloInstance(args.instance, args.zookeeper, args.user, new PasswordToken(args.password))
    val source = sparkContext.netCdfRDD(args.inPath)
    val layoutScheme = ZoomedLayoutScheme()
    val (level, rdd) =  Ingest[NetCdfBand, SpaceTimeKey](source, args.destCrs, layoutScheme)

    val save = { (rdd: RasterRDD[SpaceTimeKey], level: LayoutLevel) =>
      accumulo.catalog.save(LayerId(args.layerName, level.zoom), args.table, rdd, args.clobber)
    }

    if (args.pyramid) {
      Pyramid.saveLevels(rdd, level, layoutScheme)(save).get // expose exceptions
    } else{
      save(rdd, level).get
    }
  }
}

class CalcArgs extends SparkArgs with AccumuloArgs


import geotrellis.raster.op.local._
import geotrellis.raster._
import org.apache.spark.rdd.PairRDDFunctions
import geotrellis.raster._
import geotrellis.raster.op.local._
import geotrellis.spark._
import org.apache.spark.rdd.PairRDDFunctions
import scala.reflect.ClassTag
import com.quantifind.sumac.ArgMain
import geotrellis.raster.op.local._
import geotrellis.raster._

import geotrellis.spark._
import geotrellis.spark.cmd.args._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats.NetCdfBand
import geotrellis.spark.tiling._
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import org.apache.hadoop.fs.Path
import org.apache.spark._
import org.apache.spark.rdd.PairRDDFunctions
import org.joda.time.DateTime
import geotrellis.spark.io.hadoop._

object PredicateCount {
  def apply[K: ClassTag](cellType: CellType, predicate: Double=>Double, keyBin: K=>K)(rdd: RasterRDD[K]): RasterRDD[K] =
    asRasterRDD(rdd.metaData) {
      val bins = rdd.mapTiles{ case (key, tile) => keyBin(key) -> tile.convert(TypeByte).mapDouble(predicate) }
      new PairRDDFunctions(bins).reduceByKey{ (t1, t2) => t1.localAdd(t2) }
    }
}


/**
* Ingests raw multi-band NetCDF tiles into a re-projected and tiled RasterRDD
*/
object Calculate extends ArgMain[CalcArgs] with Logging {
  def timedTask[R](msg: String)(block: => R): R = {
    val t0 = System.currentTimeMillis
    val result = block
    val t1 = System.currentTimeMillis
    println(msg + " in " + ((t1 - t0) / 1000.0) + " s")
    result
  }

  def main(args: CalcArgs): Unit = {
    System.setProperty("com.sun.media.jai.disableMediaLib", "true")

    implicit val sparkContext = args.sparkContext("Ingest")
    //val accumulo = AccumuloInstance(args.instance, args.zookeeper, args.user, new PasswordToken(args.password))
    //val catalog = accumulo.catalog

    val catalog = HadoopCatalog(sparkContext, new Path("hdfs://localhost/catalog"))

    val rdd = catalog.load[SpaceTimeKey](LayerId("rcp45",1)).get.cache()


    val pred = { temp: Double => if (temp == Double.NaN) Double.NaN else if (temp > 0) 1 else 0 }
    val bin =  { key: SpaceTimeKey => key.updateTemporalComponent(key.temporalKey.time.withDayOfMonth(1).withMonthOfYear(1).withHourOfDay(0))}
    val ret:RasterRDD[SpaceTimeKey] = PredicateCount(TypeByte, pred, bin)(rdd);


    timedTask("doing it") {
      catalog.save[SpaceTimeKey](LayerId("over-0-daily", 1), ret, true).get
      //ret.first
    }
  }
}

