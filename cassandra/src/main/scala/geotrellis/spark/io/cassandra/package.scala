package geotrellis.spark.io

import com.datastax.driver.core.Row
import org.apache.hadoop.conf.Configuration
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import java.lang.Long

import org.apache.hadoop.io.Text

package object cassandra {
  implicit def stringToText(s: String): Text = new Text(s)

  implicit class withCassandraHadoopApiRdd(self: SparkContext) {
    def cassandraAPIHadoopRDD(configuration: Configuration): RDD[(Long, Row)] =
      CassandraJavaUtils.cassandraAPIHadoopRDD(self, configuration)
  }
}
