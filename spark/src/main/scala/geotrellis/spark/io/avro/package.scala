package geotrellis.spark.io

import geotrellis.util._

import org.apache.avro._
import org.apache.avro.generic._

import scala.reflect.ClassTag

package object avro {
  implicit class withGenericRecordMethods(val self: GenericRecord) extends MethodExtensions[GenericRecord] {
    def apply[X](name: String) = self.get(name).asInstanceOf[X]
  }

  implicit class withSchemaMethods(val self: Schema) extends MethodExtensions[Schema] {
    def fingerprintMatches(other: Schema): Boolean =
      SchemaNormalization.parsingFingerprint64(self) == SchemaNormalization.parsingFingerprint64(other)
  }
}
