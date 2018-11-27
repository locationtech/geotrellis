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

package geotrellis.spark.io.kryo

import org.apache.spark.serializer.{KryoRegistrator => SparkKryoRegistrator}

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.serializers._
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

import java.util.{Comparator, TreeMap}

/** Account for a bug in Kryo < 2.22 for serializing TreeMaps */
class XTreeMapSerializer extends MapSerializer {
  override def write (kryo: Kryo, output: Output, map: java.util.Map[_, _]) {
    val treeMap = map.asInstanceOf[TreeMap[_, _]]
    kryo.writeClassAndObject(output, treeMap.comparator())
    super.write(kryo, output, map)
  }

  protected override def create (kryo: Kryo, input: Input, t: Class[java.util.Map[_, _]]): java.util.Map[_, _] = {
    new TreeMap(kryo.readClassAndObject(input).asInstanceOf[Comparator[_]])
  }

  protected override def createCopy (kryo: Kryo, original: java.util.Map[_, _]): java.util.Map[_, _] = {
    new TreeMap(original.asInstanceOf[TreeMap[_, _]].comparator())
  }
}

class KryoRegistrator extends SparkKryoRegistrator {

  override def registerClasses(kryo: Kryo) {
    // TreeMap serializaiton has a bug; we fix it here as we're stuck on low
    // Kryo versions due to Spark. Hack-tastic.
    kryo.register(classOf[TreeMap[_, _]], (new XTreeMapSerializer).asInstanceOf[com.esotericsoftware.kryo.Serializer[TreeMap[_, _]]])

    kryo.register(classOf[(_,_)])
    kryo.register(classOf[::[_]])
    kryo.register(classOf[geotrellis.raster.ByteArrayFiller])

    // CellTypes
    kryo.register(geotrellis.raster.BitCellType.getClass)                     // Bit
    kryo.register(geotrellis.raster.ByteCellType.getClass)                    // Byte
    kryo.register(geotrellis.raster.ByteConstantNoDataCellType.getClass)
    kryo.register(classOf[geotrellis.raster.ByteUserDefinedNoDataCellType])
    kryo.register(geotrellis.raster.UByteCellType.getClass)                   // UByte
    kryo.register(geotrellis.raster.UByteConstantNoDataCellType.getClass)
    kryo.register(classOf[geotrellis.raster.UByteUserDefinedNoDataCellType])
    kryo.register(geotrellis.raster.ShortCellType.getClass)                   // Short
    kryo.register(geotrellis.raster.ShortConstantNoDataCellType.getClass)
    kryo.register(classOf[geotrellis.raster.ShortUserDefinedNoDataCellType])
    kryo.register(geotrellis.raster.UShortCellType.getClass)                  // UShort
    kryo.register(geotrellis.raster.UShortConstantNoDataCellType.getClass)
    kryo.register(classOf[geotrellis.raster.UShortUserDefinedNoDataCellType])
    kryo.register(geotrellis.raster.IntCellType.getClass)                     // Int
    kryo.register(geotrellis.raster.IntConstantNoDataCellType.getClass)
    kryo.register(classOf[geotrellis.raster.IntUserDefinedNoDataCellType])
    kryo.register(geotrellis.raster.FloatCellType.getClass)                   // Float
    kryo.register(geotrellis.raster.FloatConstantNoDataCellType.getClass)
    kryo.register(classOf[geotrellis.raster.FloatUserDefinedNoDataCellType])
    kryo.register(geotrellis.raster.DoubleCellType.getClass)                  // Double
    kryo.register(geotrellis.raster.DoubleConstantNoDataCellType.getClass)
    kryo.register(classOf[geotrellis.raster.DoubleUserDefinedNoDataCellType])

    // ArrayTiles
    kryo.register(classOf[geotrellis.raster.BitArrayTile])                    // Bit
    kryo.register(classOf[geotrellis.raster.ByteArrayTile])                   // Byte
    kryo.register(classOf[geotrellis.raster.ByteRawArrayTile])
    kryo.register(classOf[geotrellis.raster.ByteConstantNoDataArrayTile])
    kryo.register(classOf[geotrellis.raster.ByteUserDefinedNoDataArrayTile])
    kryo.register(classOf[geotrellis.raster.UByteArrayTile])                  // UByte
    kryo.register(classOf[geotrellis.raster.UByteRawArrayTile])
    kryo.register(classOf[geotrellis.raster.UByteConstantNoDataArrayTile])
    kryo.register(classOf[geotrellis.raster.UByteUserDefinedNoDataArrayTile])
    kryo.register(classOf[geotrellis.raster.ShortArrayTile])                  // Short
    kryo.register(classOf[geotrellis.raster.ShortRawArrayTile])
    kryo.register(classOf[geotrellis.raster.ShortConstantNoDataArrayTile])
    kryo.register(classOf[geotrellis.raster.ShortUserDefinedNoDataArrayTile])
    kryo.register(classOf[geotrellis.raster.UShortArrayTile])                 // UShort
    kryo.register(classOf[geotrellis.raster.UShortRawArrayTile])
    kryo.register(classOf[geotrellis.raster.UShortConstantNoDataArrayTile])
    kryo.register(classOf[geotrellis.raster.UShortUserDefinedNoDataArrayTile])
    kryo.register(classOf[geotrellis.raster.IntArrayTile])                    // Int
    kryo.register(classOf[geotrellis.raster.IntRawArrayTile])
    kryo.register(classOf[geotrellis.raster.IntConstantNoDataArrayTile])
    kryo.register(classOf[geotrellis.raster.IntUserDefinedNoDataArrayTile])
    kryo.register(classOf[geotrellis.raster.FloatArrayTile])                  // Float
    kryo.register(classOf[geotrellis.raster.FloatRawArrayTile])
    kryo.register(classOf[geotrellis.raster.FloatConstantNoDataArrayTile])
    kryo.register(classOf[geotrellis.raster.FloatUserDefinedNoDataArrayTile])
    kryo.register(classOf[geotrellis.raster.DoubleArrayTile])                 // Double
    kryo.register(classOf[geotrellis.raster.DoubleRawArrayTile])
    kryo.register(classOf[geotrellis.raster.DoubleConstantNoDataArrayTile])
    kryo.register(classOf[geotrellis.raster.DoubleUserDefinedNoDataArrayTile])

    kryo.register(classOf[Array[geotrellis.raster.Tile]])
    kryo.register(classOf[Array[geotrellis.raster.TileFeature[_,_]]])
    kryo.register(classOf[geotrellis.raster.Tile])
    kryo.register(classOf[geotrellis.raster.TileFeature[_,_]])

    kryo.register(classOf[geotrellis.raster.ArrayMultibandTile])
    kryo.register(classOf[geotrellis.raster.CompositeTile])
    kryo.register(classOf[geotrellis.raster.ConstantTile])
    kryo.register(classOf[geotrellis.raster.CroppedTile])
    kryo.register(classOf[geotrellis.raster.Raster[_]])
    kryo.register(classOf[geotrellis.raster.RasterExtent])
    kryo.register(classOf[geotrellis.raster.CellGrid])
    kryo.register(classOf[geotrellis.raster.CellSize])
    kryo.register(classOf[geotrellis.raster.GridBounds])
    kryo.register(classOf[geotrellis.raster.GridExtent])
    kryo.register(classOf[geotrellis.raster.mapalgebra.focal.TargetCell])
    kryo.register(geotrellis.raster.mapalgebra.focal.TargetCell.All.getClass)
    kryo.register(geotrellis.raster.mapalgebra.focal.TargetCell.Data.getClass)
    kryo.register(geotrellis.raster.mapalgebra.focal.TargetCell.NoData.getClass)

    kryo.register(classOf[geotrellis.spark.SpatialKey])
    kryo.register(classOf[geotrellis.spark.SpaceTimeKey])
    kryo.register(classOf[geotrellis.spark.io.index.rowmajor.RowMajorSpatialKeyIndex])
    kryo.register(classOf[geotrellis.spark.io.index.zcurve.ZSpatialKeyIndex])
    kryo.register(classOf[geotrellis.spark.io.index.zcurve.ZSpaceTimeKeyIndex])
    kryo.register(classOf[geotrellis.spark.io.index.hilbert.HilbertSpatialKeyIndex])
    kryo.register(classOf[geotrellis.spark.io.index.hilbert.HilbertSpaceTimeKeyIndex])
    kryo.register(classOf[geotrellis.vector.ProjectedExtent])
    kryo.register(classOf[geotrellis.vector.Extent])
    kryo.register(classOf[geotrellis.proj4.CRS])

    // UnmodifiableCollectionsSerializer.registerSerializers(kryo)
    kryo.register(geotrellis.spark.buffer.Direction.Center.getClass)
    kryo.register(geotrellis.spark.buffer.Direction.Top.getClass)
    kryo.register(geotrellis.spark.buffer.Direction.Bottom.getClass)
    kryo.register(geotrellis.spark.buffer.Direction.Left.getClass)
    kryo.register(geotrellis.spark.buffer.Direction.Right.getClass)
    kryo.register(geotrellis.spark.buffer.Direction.TopLeft.getClass)
    kryo.register(geotrellis.spark.buffer.Direction.TopRight.getClass)
    kryo.register(geotrellis.spark.buffer.Direction.BottomLeft.getClass)
    kryo.register(geotrellis.spark.buffer.Direction.BottomRight.getClass)

    /* Exhaustive Registration */
    kryo.register(classOf[Array[Double]])
    kryo.register(classOf[Array[Float]])
    kryo.register(classOf[Array[Int]])
    kryo.register(classOf[Array[String]])
    kryo.register(classOf[Array[org.locationtech.jts.geom.Coordinate]])
    kryo.register(classOf[Array[org.locationtech.jts.geom.LinearRing]])
    kryo.register(classOf[Array[org.locationtech.jts.geom.Polygon]])
    kryo.register(classOf[Array[geotrellis.spark.io.avro.AvroRecordCodec[Any]]])
    kryo.register(classOf[Array[geotrellis.spark.SpaceTimeKey]])
    kryo.register(classOf[Array[geotrellis.spark.SpatialKey]])
    kryo.register(classOf[Array[geotrellis.vector.Feature[Any,Any]]])
    kryo.register(classOf[Array[geotrellis.vector.MultiPolygon]])
    kryo.register(classOf[Array[geotrellis.vector.Point]])
    kryo.register(classOf[Array[geotrellis.vector.Polygon]])
    kryo.register(classOf[Array[scala.collection.Seq[Any]]])
    kryo.register(classOf[Array[scala.Tuple2[Any, Any]]])
    kryo.register(classOf[Array[scala.Tuple3[Any, Any, Any]]])
    kryo.register(classOf[org.locationtech.jts.geom.Coordinate])
    kryo.register(classOf[org.locationtech.jts.geom.Envelope])
    kryo.register(classOf[org.locationtech.jts.geom.GeometryFactory])
    kryo.register(classOf[org.locationtech.jts.geom.impl.CoordinateArraySequence])
    kryo.register(classOf[org.locationtech.jts.geom.impl.CoordinateArraySequenceFactory])
    kryo.register(classOf[org.locationtech.jts.geom.LinearRing])
    kryo.register(classOf[org.locationtech.jts.geom.MultiPolygon])
    kryo.register(classOf[org.locationtech.jts.geom.Point])
    kryo.register(classOf[org.locationtech.jts.geom.Polygon])
    kryo.register(classOf[org.locationtech.jts.geom.PrecisionModel])
    kryo.register(classOf[org.locationtech.jts.geom.PrecisionModel.Type])
    kryo.register(classOf[geotrellis.raster.histogram.FastMapHistogram])
    kryo.register(classOf[geotrellis.raster.histogram.Histogram[Any]])
    kryo.register(classOf[geotrellis.raster.histogram.MutableHistogram[Any]])
    kryo.register(classOf[geotrellis.raster.histogram.StreamingHistogram])
    kryo.register(classOf[geotrellis.raster.histogram.StreamingHistogram.DeltaCompare])
    kryo.register(classOf[geotrellis.raster.histogram.StreamingHistogram.Delta])
    kryo.register(classOf[geotrellis.raster.histogram.StreamingHistogram.Bucket])
    kryo.register(classOf[geotrellis.raster.density.KernelStamper])
    kryo.register(classOf[geotrellis.raster.summary.polygonal.MeanResult])
    kryo.register(classOf[geotrellis.raster.ProjectedRaster[Any]])
    kryo.register(classOf[geotrellis.raster.TileLayout])
    kryo.register(classOf[geotrellis.spark.TemporalProjectedExtent])
    kryo.register(classOf[geotrellis.spark.buffer.BufferSizes])
    kryo.register(classOf[geotrellis.spark.io.avro.AvroRecordCodec[Any]])
    kryo.register(classOf[geotrellis.spark.io.avro.AvroUnionCodec[Any]])
    kryo.register(classOf[geotrellis.spark.io.avro.codecs.KeyValueRecordCodec[Any,Any]])
    kryo.register(classOf[geotrellis.spark.io.avro.codecs.TupleCodec[Any,Any]])
    kryo.register(classOf[geotrellis.spark.KeyBounds[Any]])
    kryo.register(classOf[geotrellis.spark.knn.KNearestRDD.Ord[Any]])
    kryo.register(classOf[geotrellis.vector.Feature[Any,Any]])
    kryo.register(classOf[geotrellis.vector.Geometry], new GeometrySerializer[geotrellis.vector.Geometry])
    kryo.register(classOf[geotrellis.vector.GeometryCollection])
    kryo.register(classOf[geotrellis.vector.Line], new GeometrySerializer[geotrellis.vector.Line])
    kryo.register(classOf[geotrellis.vector.MultiGeometry])
    kryo.register(classOf[geotrellis.vector.MultiLine], new GeometrySerializer[geotrellis.vector.MultiLine])
    kryo.register(classOf[geotrellis.vector.MultiPoint], new GeometrySerializer[geotrellis.vector.MultiPoint])
    kryo.register(classOf[geotrellis.vector.MultiPolygon], new GeometrySerializer[geotrellis.vector.MultiPolygon])
    kryo.register(classOf[geotrellis.vector.Point])
    kryo.register(classOf[geotrellis.vector.Polygon], new GeometrySerializer[geotrellis.vector.Polygon])
    kryo.register(classOf[geotrellis.vector.SpatialIndex[Any]])
    kryo.register(classOf[java.lang.Class[Any]])
    kryo.register(classOf[java.util.TreeMap[Any, Any]])
    kryo.register(classOf[java.util.HashMap[Any,Any]])
    kryo.register(classOf[java.util.HashSet[Any]])
    kryo.register(classOf[java.util.LinkedHashMap[Any,Any]])
    kryo.register(classOf[java.util.LinkedHashSet[Any]])
    kryo.register(classOf[org.apache.hadoop.io.BytesWritable])
    kryo.register(classOf[org.apache.hadoop.io.BigIntWritable])
    kryo.register(classOf[Array[org.apache.hadoop.io.BigIntWritable]])
    kryo.register(classOf[Array[org.apache.hadoop.io.BytesWritable]])
    kryo.register(classOf[org.codehaus.jackson.node.BooleanNode])
    kryo.register(classOf[org.codehaus.jackson.node.IntNode])
    kryo.register(classOf[org.osgeo.proj4j.CoordinateReferenceSystem])
    kryo.register(classOf[org.osgeo.proj4j.datum.AxisOrder])
    kryo.register(classOf[org.osgeo.proj4j.datum.AxisOrder.Axis])
    kryo.register(classOf[org.osgeo.proj4j.datum.Datum])
    kryo.register(classOf[org.osgeo.proj4j.datum.Ellipsoid])
    kryo.register(classOf[org.osgeo.proj4j.datum.Grid])
    kryo.register(classOf[org.osgeo.proj4j.datum.Grid.ConversionTable])
    kryo.register(classOf[org.osgeo.proj4j.util.PolarCoordinate])
    kryo.register(classOf[org.osgeo.proj4j.util.FloatPolarCoordinate])
    kryo.register(classOf[org.osgeo.proj4j.util.IntPolarCoordinate])
    kryo.register(classOf[Array[org.osgeo.proj4j.util.FloatPolarCoordinate]])
    kryo.register(classOf[org.osgeo.proj4j.datum.PrimeMeridian])
    kryo.register(classOf[org.osgeo.proj4j.proj.LambertConformalConicProjection])
    kryo.register(classOf[org.osgeo.proj4j.proj.LongLatProjection])
    kryo.register(classOf[org.osgeo.proj4j.proj.TransverseMercatorProjection])
    kryo.register(classOf[org.osgeo.proj4j.proj.MercatorProjection])
    kryo.register(classOf[org.osgeo.proj4j.units.DegreeUnit])
    kryo.register(classOf[org.osgeo.proj4j.units.Unit])
    kryo.register(classOf[scala.collection.mutable.WrappedArray$ofInt])
    kryo.register(classOf[scala.collection.mutable.WrappedArray$ofRef])
    kryo.register(classOf[scala.collection.Seq[Any]])
    kryo.register(classOf[scala.Tuple3[Any, Any, Any]])
    kryo.register(geotrellis.proj4.LatLng.getClass)
    kryo.register(geotrellis.spark.EmptyBounds.getClass)
    kryo.register(scala.collection.immutable.Nil.getClass)
    kryo.register(scala.math.Ordering.Double.getClass)
    kryo.register(scala.math.Ordering.Float.getClass)
    kryo.register(scala.math.Ordering.Int.getClass)
    kryo.register(scala.math.Ordering.Long.getClass)
    kryo.register(scala.None.getClass)
  }
}
