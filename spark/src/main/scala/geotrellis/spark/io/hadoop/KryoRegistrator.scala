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
    kryo.register(classOf[(_,_)])
    kryo.register(classOf[::[_]])
    kryo.register(classOf[SpatialKeyWritable])
    kryo.register(classOf[SpaceTimeKeyWritable])
    kryo.register(classOf[TileWritable])
    kryo.register(classOf[geotrellis.raster.BitArrayTile])
    kryo.register(classOf[geotrellis.raster.ByteArrayFiller])
    kryo.register(classOf[geotrellis.raster.FloatArrayTile])
    kryo.register(classOf[geotrellis.raster.DoubleArrayTile])
    kryo.register(classOf[geotrellis.raster.ShortArrayTile])
    kryo.register(classOf[geotrellis.raster.IntArrayTile])
    kryo.register(classOf[org.apache.accumulo.core.client.impl.ConnectorImpl])
    kryo.register(classOf[org.apache.accumulo.core.client.mock.MockConnector])
    kryo.register(classOf[geotrellis.spark.SpatialKey])
    kryo.register(classOf[geotrellis.spark.SpaceTimeKey])
    kryo.register(classOf[org.joda.time.DateTime], new de.javakaffee.kryoserializers.jodatime.JodaDateTimeSerializer)
    kryo.register(classOf[org.joda.time.Interval], new de.javakaffee.kryoserializers.jodatime.JodaIntervalSerializer)
    kryo.register(classOf[geotrellis.spark.io.index.rowmajor.RowMajorSpatialKeyIndex])
    kryo.register(classOf[geotrellis.spark.io.index.zcurve.ZSpatialKeyIndex])
    kryo.register(classOf[geotrellis.spark.io.index.zcurve.ZSpaceTimeKeyIndex])
    kryo.register(classOf[geotrellis.spark.io.index.hilbert.HilbertSpatialKeyIndex])
    kryo.register(classOf[geotrellis.spark.io.index.hilbert.HilbertSpaceTimeKeyIndex])
    kryo.register(classOf[geotrellis.vector.ProjectedExtent])
    kryo.register(classOf[geotrellis.vector.Extent])
    kryo.register(classOf[geotrellis.proj4.CRS])

    // de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer.registerSerializers(kryo)
    import java.util._
    import de.javakaffee.kryoserializers._

    kryo.register( Arrays.asList( "" ).getClass, new ArraysAsListSerializer )
    kryo.register( Collections.EMPTY_LIST.getClass, new CollectionsEmptyListSerializer() )
    kryo.register( Collections.EMPTY_MAP.getClass(), new CollectionsEmptyMapSerializer() )
    kryo.register( Collections.EMPTY_SET.getClass(), new CollectionsEmptySetSerializer() )
    kryo.register( Collections.singletonList( "" ).getClass(), new CollectionsSingletonListSerializer )
    kryo.register( Collections.singleton( "" ).getClass(), new CollectionsSingletonSetSerializer )
    kryo.register( Collections.singletonMap( "", "" ).getClass(), new CollectionsSingletonMapSerializer )

    UnmodifiableCollectionsSerializer.registerSerializers( kryo )
    SynchronizedCollectionsSerializer.registerSerializers( kryo )
  }
}
