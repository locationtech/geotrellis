/*
 * Copyright (c) 2014 DigitalGlobe.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.io.hadoop

import geotrellis.spark.io.hadoop.formats._
import org.apache.spark.serializer.{ KryoRegistrator => SparkKryoRegistrator }

import com.esotericsoftware.kryo.Kryo

class KryoRegistrator extends SparkKryoRegistrator {
  override def registerClasses(kryo: Kryo) {
    kryo.register(classOf[SpatialKeyWritable])
    kryo.register(classOf[TileWritable])
    kryo.register(classOf[org.apache.accumulo.core.client.impl.ConnectorImpl])
    kryo.register(classOf[org.apache.accumulo.core.client.mock.MockConnector])
    kryo.register(classOf[geotrellis.spark.SpatialKey])
    kryo.register(classOf[geotrellis.spark.SpaceTimeKey])
    kryo.register(classOf[org.joda.time.DateTime])
  }
}
