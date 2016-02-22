package geotrellis.spark.io.accumulo.encoder

import geotrellis.spark._
import geotrellis.spark.io.index._

import org.apache.accumulo.core.client.IteratorSetting
import org.apache.accumulo.core.client.mapreduce.InputFormatBase
import org.apache.accumulo.core.data.Key
import org.apache.accumulo.core.data.{Range => AccumuloRange}
import org.apache.accumulo.core.util.{Pair => AccumuloPair}
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job
import org.joda.time.DateTimeZone

import scala.collection.JavaConverters._
import scala.reflect._

trait AccumuloKeyEncoderStrategy {
  def encoderFor[K: ClassTag](): AccumuloKeyEncoder[K]
}

object AccumuloKeyEncoderStrategy {
  def DEFAULT = DefaultKeyEncoderStrategy
}

object DefaultKeyEncoderStrategy extends AccumuloKeyEncoderStrategy {
  def encoderFor[K: ClassTag]() = new DefaultAccumuloKeyEncoder()
}

class CustomAccumuloKeyEncoderStrategy private (encoderMap: Map[Class[_], AccumuloKeyEncoder[_]]) {
  def withEncoder[K: ClassTag](keyEncoder: AccumuloKeyEncoder[K]): CustomAccumuloKeyEncoderStrategy =
    new CustomAccumuloKeyEncoderStrategy(encoderMap ++ Map(classTag[K].runtimeClass -> keyEncoder))

  def encoderFor[K: ClassTag](): AccumuloKeyEncoder[K] =
    encoderMap.get(classTag[K].runtimeClass) match {
      case Some(ke) => ke.asInstanceOf[AccumuloKeyEncoder[K]]
      case None => new DefaultAccumuloKeyEncoder()
    }
}
