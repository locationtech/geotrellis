package geotrellis.spark.io.cassandra;

import org.apache.spark.SparkContext;
import org.apache.spark.rdd.RDD;
import org.apache.cassandra.hadoop.cql3.CqlInputFormat;
import org.apache.hadoop.conf.Configuration;
import com.datastax.driver.core.Row;

import scala.Tuple2;

/**
 * A workaround to issue related to invariant java types
 * scala newAPIHadoopRDD requires a strong subtyping (newAPIHadoopRDD[K, V, F <: NewInputFormat[K, V]])
 * and function call newAPIHadoopRDD[Long, Row, CqlInputFormat] not corresponds that requirement
 */

public class CassandraJavaUtils {
    public static RDD<Tuple2<Long, Row>> cassandraAPIHadoopRDD(SparkContext sc, Configuration conf) {
        return sc.newAPIHadoopRDD(conf, CqlInputFormat.class, Long.class, Row.class);
    }
}
