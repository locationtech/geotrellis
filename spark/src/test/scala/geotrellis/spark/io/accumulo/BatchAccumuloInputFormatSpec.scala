package geotrellis.spark.io.accumulo

import java.io.IOException

import geotrellis.raster._
import geotrellis.spark

import geotrellis.spark._
import geotrellis.spark.ingest._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.tiling._
import geotrellis.raster.op.local._
import geotrellis.spark.utils.SparkUtils
import geotrellis.proj4.LatLng

import org.apache.spark._
import org.apache.spark.rdd._
import org.joda.time.DateTime
import org.scalatest._
import org.scalatest.Matchers._
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import org.apache.accumulo.core.data.{Key, Mutation, Value, Range => ARange}
import org.apache.accumulo.core.client.mapreduce.lib.impl.{ConfiguratorBase => CB}
import org.apache.accumulo.core.client.mapreduce.{AccumuloInputFormat, InputFormatBase }
import org.apache.accumulo.core.client.{BatchWriterConfig, IteratorSetting}
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.Text
import scala.collection.JavaConverters._

// we don't care about data or row/column selection
case class Row(id: String, colFam: String, colQual: String){
  def mutation: Mutation = {
    val mutation = new Mutation(new Text(id))
    mutation.put(
      new Text(colFam), new Text(colQual),
      System.currentTimeMillis(), new Value(Array.empty[Byte])
    )
    mutation
  }
}

/**
 * We need to test if Batch IF behaves the same as Accumulo IF.
 * The operations we care about is range selection and filter application.
 */
class BatchAccumuloInputFormatSpec extends FunSpec 
  with Matchers
  with TestEnvironment {
  val data = List[Row] (
    Row("1", "col", "a"), Row("1", "col", "b"), Row("1", "col", "c"),
    Row("2", "col", "a"), Row("2", "col", "b"), Row("2", "col", "c"),
    Row("3", "col", "a"), Row("3", "col", "b"), Row("3", "col", "c")
  )
  
  describe("BatchAccumuloInputFormat") {
    val table = "data"
    val accumulo = new MockAccumuloInstance()
    val job = sc.newJob
    val jconf = job.getConfiguration
    CB.setMockInstance(classOf[AccumuloInputFormat], jconf, "fake")
    CB.setConnectorInfo(classOf[AccumuloInputFormat], jconf, "root", new PasswordToken(""))
    InputFormatBase.setInputTableName(job, table)
    accumulo.connector.tableOperations().create(table)
    val writer = accumulo.connector.createBatchWriter(table, new BatchWriterConfig())
    data map { _.mutation } foreach { writer.addMutation }

    it("full table scan") {
      val ifSet = sc.newAPIHadoopRDD(job.getConfiguration, classOf[AccumuloInputFormat], classOf[Key], classOf[Value]).collect.toSet        
      val bifSet = sc.newAPIHadoopRDD(job.getConfiguration, classOf[BatchAccumuloInputFormat], classOf[Key], classOf[Value]).collect.toSet
      info(s"AccumloInputFormat records ${ifSet.size}")
      info(s"BatchAccumloInputFormat records ${bifSet.size}")        
      bifSet should be (ifSet)
    }

    it("range constraint") {       
      InputFormatBase.setRanges(job, List(
        new ARange(new Text("1"), new Text("1")), 
        new ARange(new Text("3"), new Text("3"))
      ).asJava)       
      val ifSet = sc.newAPIHadoopRDD(job.getConfiguration, classOf[AccumuloInputFormat], classOf[Key], classOf[Value]).collect.toSet        
      val bifSet = sc.newAPIHadoopRDD(job.getConfiguration, classOf[BatchAccumuloInputFormat], classOf[Key], classOf[Value]).collect.toSet
      info(s"AccumloInputFormat records ${ifSet.size}")
      info(s"BatchAccumloInputFormat records ${bifSet.size}")        
      bifSet should be (ifSet)
      ifSet.size should be (6)
    }    

    it("filter constraints") {
      val props =  Map(
        "startBound" -> "b",
        "endBound" -> "c",
        "startInclusive" -> "true",
        "endInclusive" -> "true"
      )

      InputFormatBase.addIterator(job, 
        new IteratorSetting(1, "TimeColumnFilter", "org.apache.accumulo.core.iterators.user.ColumnSliceFilter", props.asJava))

      val ifSet = sc.newAPIHadoopRDD(job.getConfiguration, classOf[AccumuloInputFormat], classOf[Key], classOf[Value]).collect.toSet        
      val bifSet = sc.newAPIHadoopRDD(job.getConfiguration, classOf[BatchAccumuloInputFormat], classOf[Key], classOf[Value]).collect.toSet
      info(s"AccumloInputFormat records ${ifSet.size}")
      info(s"BatchAccumloInputFormat records ${bifSet.size}")
      bifSet should be (ifSet)
      ifSet.size should be (4)
    }
  }
}
