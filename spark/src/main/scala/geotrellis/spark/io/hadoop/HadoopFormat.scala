package geotrellis.spark.io.hadoop

import geotrellis.spark.io.hadoop.formats._

import org.apache.hadoop.mapreduce.{OutputFormat, InputFormat}
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat
import org.apache.hadoop.mapreduce.lib.output.MapFileOutputFormat

import scala.reflect._

/**
 * Hadoop API requires arguments concrete classes for Key, Value and OutputFormat as arguments.
 * This type class provides those concrete classes based on K, V type parameters acting as a type adapter.
 *
 * @see [[spatialHadoopFormat]] for example
 */
trait HadoopFormat[K, V] extends Serializable {
  type KW <: AvroKeyWritable[K, KW]
  type VW <: AvroWritable[V]
  type FilteredInputFormat <: InputFormat[KW, VW]
  type FullInputFormat <: InputFormat[KW, VW]
  type FullOutputFormat <: OutputFormat[KW, VW]

  def kClass: Class[KW]
  def vClass: Class[VW]
  def fullInputFormatClass: Class[FullInputFormat]
  def fullOutputFormatClass: Class[FullOutputFormat]
  def filteredInputFormatClass: Class[FilteredInputFormat]
}

object HadoopFormat {
  /**
   * Creates an instance of HadoopFormat from type parameters using scala provider ClassTags.
   *
   * @tparam K    AvroKeyWritable subclass
   * @tparam V    AvroWritable subclass
   * @tparam FIF  FilterMapFileInputFormat subclass that is able to filter by K
   */
  def Aux[K, V, KWP <: AvroKeyWritable[K, KWP] : ClassTag, VWP <: AvroWritable[V] : ClassTag, FIF <: InputFormat[KWP, VWP]: ClassTag]: HadoopFormat[K, V] =
    new HadoopFormat[K, V] {
      type KW = KWP
      type VW  = VWP
      type FilteredInputFormat = FIF

      // These are always suitable and not required to be explicitly specified
      type FullInputFormat = SequenceFileInputFormat[KWP, VWP]
      type FullOutputFormat = MapFileOutputFormat[KWP, VWP]

      val kClass = classTag[KWP].runtimeClass.asInstanceOf[Class[KW]]
      val vClass = classTag[VWP].runtimeClass.asInstanceOf[Class[VW]]
      val filteredInputFormatClass = classTag[FIF].runtimeClass.asInstanceOf[Class[FIF]]
      val fullInputFormatClass = classOf[FullInputFormat]
      val fullOutputFormatClass = classOf[FullOutputFormat]
    }
}

