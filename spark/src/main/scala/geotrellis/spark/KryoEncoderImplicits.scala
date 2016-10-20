package geotrellis.spark

import org.apache.spark.sql.{Encoder, Encoders}

import scala.reflect.{ClassTag, classTag}
import scala.reflect.runtime.universe.TypeTag

object KryoEncoderImplicits extends KryoEncoderImplicits

/**
  * Kryo required for typed Datasets support, probably we would find other workarounds
  */
trait KryoEncoderImplicits {
  implicit def single[A: ClassTag] = Encoders.kryo[A](classTag[A])

  implicit def tuple2[A1: Encoder, A2: Encoder]: Encoder[(A1, A2)] =
    Encoders.tuple[A1, A2](implicitly[Encoder[A1]], implicitly[Encoder[A2]])

  implicit def tuple3[A1: Encoder, A2: Encoder, A3: Encoder]: Encoder[(A1, A2, A3)] =
    Encoders.tuple[A1, A2, A3](implicitly[Encoder[A1]], implicitly[Encoder[A2]], implicitly[Encoder[A3]])

  implicit def ptuple3[A1 <: Product: TypeTag, A2: Encoder, A3: Encoder]: Encoder[(A1, A2, A3)] =
    Encoders.tuple[A1, A2, A3](Encoders.product[A1], implicitly[Encoder[A2]], implicitly[Encoder[A3]])

  implicit def tuple4[A1: Encoder, A2: Encoder, A3: Encoder, A4: Encoder]: Encoder[(A1, A2, A3, A4)] =
    Encoders.tuple[A1, A2, A3, A4](implicitly[Encoder[A1]], implicitly[Encoder[A2]], implicitly[Encoder[A3]], implicitly[Encoder[A4]])

  implicit def tuple5[A1: Encoder, A2: Encoder, A3: Encoder, A4: Encoder, A5: Encoder]: Encoder[(A1, A2, A3, A4, A5)] =
    Encoders.tuple[A1, A2, A3, A4, A5](implicitly[Encoder[A1]], implicitly[Encoder[A2]], implicitly[Encoder[A3]], implicitly[Encoder[A4]], implicitly[Encoder[A5]])
}
