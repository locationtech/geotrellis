/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.sql

import org.apache.spark.sql.{Encoder, Encoders}

import scala.reflect.{ClassTag, classTag}

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

  implicit def tuple4[A1: Encoder, A2: Encoder, A3: Encoder, A4: Encoder]: Encoder[(A1, A2, A3, A4)] =
    Encoders.tuple[A1, A2, A3, A4](implicitly[Encoder[A1]], implicitly[Encoder[A2]], implicitly[Encoder[A3]], implicitly[Encoder[A4]])

  implicit def tuple5[A1: Encoder, A2: Encoder, A3: Encoder, A4: Encoder, A5: Encoder]: Encoder[(A1, A2, A3, A4, A5)] =
    Encoders.tuple[A1, A2, A3, A4, A5](implicitly[Encoder[A1]], implicitly[Encoder[A2]], implicitly[Encoder[A3]], implicitly[Encoder[A4]], implicitly[Encoder[A5]])
}
