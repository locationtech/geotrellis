package geotrellis.spark.io.avro

import org.apache.avro._
import org.apache.avro.generic._
import scala.collection.JavaConverters._
import scala.reflect.ClassTag

/**
 * Restricted to be a union of Records that share a super type.
 * @param formats list of formats that make up the union
 * @tparam T      superclass of listed formats
 */
class AvroUnionCodec[T: ClassTag](formats: AvroRecordCodec[X] forSome {type X <: T} *) extends AvroRecordCodec[T] {
  def schema: Schema =
    Schema.createUnion(formats.map(_.schema).asJava)

  override
  def encode(thing: T): GenericRecord = {
    val format = findFormat(_.supported(thing), thing.getClass.toString)
    val rec = new GenericData.Record(format.schema)
    format.encode(thing, rec)
    rec
  }

  def encode(thing: T, rec: GenericRecord) = {
    findFormat(_.supported(thing), thing.getClass.toString).encode(thing, rec)
  }

  def decode(rec: GenericRecord): T = {
    val fullName = rec.getSchema.getFullName
    findFormat(_.schema.getFullName == fullName, fullName).decode(rec)
  }

  private def findFormat(f: AvroRecordCodec[_] => Boolean, target: String): AvroRecordCodec[T] =
    formats.filter(f) match {
      case Seq(format) => format.asInstanceOf[AvroRecordCodec[T]]
      case Seq() => sys.error(s"No formats found to support $target")
      case list => sys.error(s"Multiple formats support $target: ${list.toList}")
    }

}
