
/*
 * Copyright (C) 2012 The Regents of The University California.
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.utils

import java.nio.ByteBuffer

import org.apache.spark.{SparkConf, SparkEnv}
import org.apache.spark.serializer.{KryoSerializer => SparkKryoSerializer}

import scala.reflect.ClassTag

/**
 * Java object serialization using Kryo. This is much more efficient, but Kryo
 * sometimes is buggy to use. We use this mainly to serialize the object
 * inspectors.
 */
object KryoSerializer {

  @transient lazy val ser: SparkKryoSerializer = {
    val sparkConf = Option(SparkEnv.get).map(_.conf).getOrElse(new SparkConf())
    new SparkKryoSerializer(sparkConf)
  }

  def serialize[T: ClassTag](o: T): Array[Byte] = {
    ser.newInstance().serialize(o).array()
  }

  def deserialize[T: ClassTag](bytes: Array[Byte]): T  = {
    ser.newInstance().deserialize[T](ByteBuffer.wrap(bytes))
  }
}