package geotrellis.spark.cmd

import com.quantifind.sumac.ArgMain
import com.quantifind.sumac.validation.Required
import geotrellis.spark.cmd.args.{HadoopArgs, SparkArgs}
import geotrellis.spark.rdd._
import org.apache.accumulo.core.client.ZooKeeperInstance
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import org.apache.spark._
import org.apache.spark.rdd._

import org.apache.hadoop.fs.Path
import org.apache.spark.Logging
import geotrellis.spark.accumulo._


class AccumuloArgs extends SparkArgs with HadoopArgs



object Accumulo extends ArgMain[AccumuloArgs] with Serializable {
  def main(args: AccumuloArgs) {


    val sc: SparkContext = args.sparkContext("Accumulo Tom Foolery")

    val rasterPath = new Path("hdfs://localhost/nlcd-2011/12")

//    val rdd = RasterRDD(rasterPath, sc).map(tt => tt.id -> tt.tile)
//    println(rdd.count)

    implicit val format = new TmsTilingAccumuloFormat("nlcd-2012", 12)

    val instance = new ZooKeeperInstance("gis", "localhost")
    val connector = instance.getConnector("root", new PasswordToken("secret"))

//    rdd.saveAccumulo("tiles2",  connector)

    sc.accumuloRDD("tiles2")

    sc.stop()
  }
}