// package geotrellis.spark.io.json

// import geotrellis.spark._
// import geotrellis.spark.io.accumulo._
// import geotrellis.spark.io.accumulo.encoder._

// import spray.json._
// import spray.json.DefaultJsonProtocol._

// import scala.reflect._

// case class AccumuloKeyEncoderFormatEntry[K: JsonFormat, T <: AccumuloKeyEncoder[K]: JsonFormat: ClassTag](typeName: String) {
//   val jsonFormat = implicitly[JsonFormat[T]]
//   val classTag = implicitly[ClassTag[T]]
// }

// trait AccumuloKeyEncoderFormatFactory {
//   def get[K]: RootJsonFormat[AccumuloKeyEncoder[K]]
// }

// trait AccumuloKeyEncoderFormats {
//   implicit object DefaultAccumuloKeyEncoderFormat[K] extends RootJsonFormat[DefaultAccumuloKeyEncoder[K]] {
//     final def TYPE_NAME = "default"

//     def write(obj: DefaultAccumuloKeyEncoder[K]): JsValue =
//       JsObject("type" -> JsString(TYPE_NAME))

//     def read(value: JsValue): DefaultAccumuloKeyEncoder[K] =
//       value.asJsObject.getFields("type") match {
//         case Seq(JsString(typeName)) => {
//           if (typeName != TYPE_NAME)
//             throw new DeserializationException(s"Wrong KeyIndex type: ${TYPE_NAME} expected.")
//           new DefaultAccumuloKeyEncoder[K]
//         }
//         case _ =>
//           throw new DeserializationException("Wrong AccumuloKeyEncoder type: ${getClass.getName} expected.")
//       }
//   }

//   implicit object SpaceTimeAlternateAccumuloKeyEncoderFormat extends RootJsonFormat[SpaceTimeAlternateAccumuloKeyEncoder] {
//     final def TYPE_NAME = "spacetime-alt"

//     def write(obj: SpaceTimeAlternateAccumuloKeyEncoder): JsValue =
//       JsObject("type" -> JsString(TYPE_NAME))

//     def read(value: JsValue): SpaceTimeAlternateAccumuloKeyEncoder =
//       value.asJsObject.getFields("type") match {
//         case Seq(JsString(typeName)) => {
//           if (typeName != TYPE_NAME)
//             throw new DeserializationException(s"Wrong KeyIndex type: ${TYPE_NAME} expected.")
//           new DefaultAccumuloKeyEncoder
//         }
//         case _ =>
//           throw new DeserializationException("Wrong AccumuloKeyEncoder type: ${getClass.getName} expected.")
//       }
//   }

//   trait AccumuloKeyEncoderJsonFormat[K] extends RootJsonFormat[AccumuloKeyEncoder[K]] {
//     protected val entries: Vector[AccumuloKeyEncoderFormatEntry[_, _]]

//     def write(obj: AccumuloKeyEncoder[K]): JsValue =
//       entries.find(_.classTag.runtimeClass.isAssignableFrom(obj.getClass)) match {
//         case Some(entry) =>
//           obj.toJson(entry.jsonFormat.asInstanceOf[JsonFormat[AccumuloKeyEncoder[K]]])
//         case None =>
//           throw new SerializationException(s"Cannot serialize this key index with this AccumuloKeyEncoderJsonFormat: $obj")
//       }

//     def read(value: JsValue): AccumuloKeyEncoder[K] =
//       value.asJsObject.getFields("type", "properties") match {
//         case Seq(JsString(typeName), properties) =>
//           entries.find(_.typeName == typeName) match {
//             case Some(entry) =>
//               // Deserialize the key index based on the entry information.
//               entry.jsonFormat.read(value).asInstanceOf[AccumuloKeyEncoder[K]]
//             case None =>
//               throw new DeserializationException(s"Cannot deserialize key index with type $typeName in json $value")
//           }
//         case _ =>
//           throw new DeserializationException(s"Expected AccumuloKeyEncoder, got $value")
//       }
//   }

//   object SpatialAccumuloKeyEncoderJsonFormat extends AccumuloKeyEncoderJsonFormat[SpatialKey] {
//     val entries =
//       Vector(
//         AccumuloKeyEncoderFormatEntry[SpatialKey, HilbertSpatialAccumuloKeyEncoder](HilbertSpatialAccumuloKeyEncoderFormat.TYPE_NAME),
//         AccumuloKeyEncoderFormatEntry[SpatialKey, ZSpatialAccumuloKeyEncoder](ZSpatialAccumuloKeyEncoderFormat.TYPE_NAME),
//         AccumuloKeyEncoderFormatEntry[SpatialKey, RowMajorSpatialAccumuloKeyEncoder](RowMajorSpatialAccumuloKeyEncoderFormat.TYPE_NAME)
//       )
//   }

//   object SpaceTimeAccumuloKeyEncoderJsonFormat extends AccumuloKeyEncoderJsonFormat[SpaceTimeKey] {
//     val entries =
//       Vector(
//         AccumuloKeyEncoderFormatEntry[SpaceTimeKey, HilbertSpaceTimeAccumuloKeyEncoder](HilbertSpaceTimeAccumuloKeyEncoderFormat.TYPE_NAME),
//         AccumuloKeyEncoderFormatEntry[SpaceTimeKey, ZSpaceTimeAccumuloKeyEncoder](ZSpaceTimeAccumuloKeyEncoderFormat.TYPE_NAME)
//       )
//   }

//   implicit def spatialAccumuloKeyEncoderJsonFormat: AccumuloKeyEncoderJsonFormat[SpatialKey] = SpatialAccumuloKeyEncoderJsonFormat
//   implicit def spaceTimeAccumuloKeyEncoderJsonFormat: AccumuloKeyEncoderJsonFormat[SpaceTimeKey] = SpaceTimeAccumuloKeyEncoderJsonFormat

// }
