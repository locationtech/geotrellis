package geotrellis.spark.io

import org.apache.avro.generic.GenericRecord

package object avro {
  implicit class GenericRecordMethods(rec: GenericRecord) {
    def apply[X](name: String) = rec.get(name).asInstanceOf[X]
  }
}
