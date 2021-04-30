/*
 * Copyright 2020 Azavea
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

package geotrellis.geowave.adapter

import java.nio.ByteBuffer
import java.util
import java.util.ServiceLoader

import cats.data.Validated
import geotrellis.geowave.dsl.syntax._
import org.locationtech.geowave.core.index.persist.{Persistable, PersistenceUtils}
import org.locationtech.geowave.core.index.{ByteArrayUtils, StringUtils, VarintUtils}
import org.locationtech.geowave.core.store.adapter.{AdapterPersistenceEncoding, IndexedAdapterPersistenceEncoding}
import org.locationtech.geowave.core.store.api.{DataTypeAdapter, Index}
import org.locationtech.geowave.core.store.data.MultiFieldPersistentDataset
import org.locationtech.geowave.core.store.data.field.{FieldReader, FieldWriter}
import org.locationtech.geowave.core.store.index.{CommonIndexModel, CommonIndexValue}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

/** GeoTrellisDataAdapter abstracts over storing raster data indexed using GeoWave index.
  *
  * DataAdapter instances specifies:
  * - How to extract index values from the input entry
  * - How to read and write the entry for a GeoWave table
  * - How the data will be stored in native (Cassandra) fields
  * - What indexes are to be used when storing the data
  *
  * Each Adapter has a name, that should be unique per Cassandra namespace.
  * Adapter may have a one-to-many relationship with indexes.
  * - Relationships are persisted in `aim_geowave_metadata` table.
  *
  * DataApter and Index instances are associated together through GeoWave DataStore.
  *
  * {{{
  * val dataStore: DataStore = ???
  * // call to addType persists the adapter, index instances and their and relationships
  * dataStore.addType(adapter, index1, index2)
  * val writer = dataStore.createWriter[GeoTiff[MultibandTile]](adapter.getTypeName)
  * writer.write(entry)
  * }}}
  *
  * This will result with each entry being written to multiple tables each associated with a given index.
  *
  * In order to support this persistance mechanism each adapter must be registered with the adapter SPI in
  *   [[geotrellis.geowave.GeoTrellisPersistableRegistry]].
  *
  * @note All adapters should have an empty constructor to use Serialization mechanisms.
  *
  * @note Adapter needs to be registered with the store only once.
  *       Once the adapter is used to write a record once, it is presisted to the store.
  *       All further reads and writes will read that adapter from the store.
  *
  * @see https://locationtech.github.io/geowave/devguide.html#adapters
  */
abstract class GeoTrellisDataAdapter[T <: AnyRef](
  /** The type name which serves as a unique identifier for this adapter. This also must be unique within a datastore. */
  private var typeName: TypeName,
  private var fieldHandlers: List[IndexFieldHandler[T]]
) extends DataTypeAdapter[T] {
  protected val logger = LoggerFactory.getLogger(this.getClass())
  protected val DATA_FIELD_ID: String

  /**
   * Copied from the RasterDataAdapter, we can make it configurable though
   * For SimpleFeatures it is a FeatureId, for odel this can be a ModelId
   */
  override def getDataId(entry: T): Array[Byte]

  /** Gets a reader to read the value from the row */
  def getReader(fieldName: String): FieldReader[AnyRef]

  /** Gets a writer to write the value into the row */
  def getWriter(fieldName: String): FieldWriter[T, AnyRef]


  // these are lazy because of initilization order
  lazy val fieldNames                     = fieldHandlers.map(_.getFieldName) :+ DATA_FIELD_ID
  lazy val fieldNameMatchingFieldHandlers = fieldHandlers.map { handler =>
    handler.getFieldName -> handler
  }.toMap[String, IndexFieldHandler[T]]
  lazy val fieldNameToIndex               = fieldNames.zipWithIndex.toMap
  lazy val indexToFieldName               = fieldNameToIndex.map { case (k, v) => (v, k) }

  override def getTypeName: String = {
    logger.trace(s"getTypeName: ${typeName}")
    typeName
  }

  /**
   * Field handlers helpers.
   * Helper is a function that knows how to extract index value from the entry.
   */
  def getFieldHandlers = {
    logger.trace(s"getFieldHandlers: $fieldHandlers")
    fieldHandlers
  }

  /**
   * Validates that this adapter can provide the index with all fields required by its dimensions
   */
  def validateIndexCompatability(index: Index): Validated[String, Index] = {
    val availableFields = fieldHandlers.map(_.getFieldName.value).toSet
    val requiredFields = index.getIndexModel.getDimensions.map(_.getFieldName).toSet
    val missingFields = requiredFields.diff(availableFields)

    if (missingFields.isEmpty) Validated.valid(index)
    else Validated.invalid(s"${index.getName} index requires ${missingFields} fields, ${availableFields} are available")
  }

  def getFieldHandler(name: String): Option[IndexFieldHandler[T]] = {
    logger.trace(s"getFieldHandler: ${name}")
    fieldNameMatchingFieldHandlers.get(name)
  }

  /** The position in a row is indexed, by the field name we can get the offset of the recorded field in a row */
  def getPositionOfOrderedField(commonIndexModel: CommonIndexModel, fieldName: String): Int = {
    logger.trace(s"getPositionofOrderedField($fieldName)")
    fieldNameToIndex(fieldName)
  }

  /** The position in a row is indexed, by the field offset we can get the name of the recorded field in a row */
  def getFieldNameForPosition(commonIndexModel: CommonIndexModel, index: Int): String = {
    logger.trace(s"getFieldNameForPosition($index)")
    indexToFieldName(index)
  }

  /** There is no need in using a handlers mechanism, since this is the only "DATA" field. */
  override def decode(data: IndexedAdapterPersistenceEncoding, index: Index): T = {
    logger.trace(s"decode($index)")
    data.getAdapterExtendedData.getValue(DATA_FIELD_ID).asInstanceOf[T]
  }

  override def encode(entry: T, indexModel: CommonIndexModel): AdapterPersistenceEncoding = {
    logger.trace(s"encode($entry)")
    /** Each handler is a function that knows how to extract value from the entry that would be used to build index */
    val indexData = new MultiFieldPersistentDataset[CommonIndexValue]()
    indexModel.getDimensions.foreach { dimension =>
      getFieldHandler(dimension.getFieldName).map { handler =>
        val value = handler.toIndexValue(entry)
        indexData.addValue(dimension.getFieldName, value)
      }
    }

    /** Since we have a single value field, there are no needs in using handlers mechanisms to extract data from the entry */
    val extendedData = new MultiFieldPersistentDataset[AnyRef]()
    extendedData.addValue(DATA_FIELD_ID, entry)

    new AdapterPersistenceEncoding(getDataId(entry), indexData, extendedData)
  }

  /**
   * Serializes adapter since it stores it in the table and uses this data on read
   * to construct the adapter from the persisted parameters.
   */
  def toBinary: Array[Byte] = {
    val persistables = new util.LinkedHashSet[Persistable]()
    fieldHandlers.map { indexHandler => persistables.add(indexHandler.asInstanceOf[Persistable]) }

    val typeNameBytes     = StringUtils.stringToBinary(typeName)
    val persistablesBytes = PersistenceUtils.toBinary(persistables)

    val buf =
      ByteBuffer.allocate(
        typeNameBytes.length
          + persistablesBytes.length
          + VarintUtils.unsignedIntByteLength(typeNameBytes.length)
          + VarintUtils.unsignedIntByteLength(persistablesBytes.length))

    VarintUtils.writeUnsignedInt(typeNameBytes.length, buf)
    VarintUtils.writeUnsignedInt(persistablesBytes.length, buf)

    buf.put(typeNameBytes)
    buf.put(persistablesBytes)
    logger.trace(s"toBinary $this: ${buf.position} bytes")
    buf.array
  }

  def fromBinary(bytes: Array[Byte]): Unit = {
    logger.trace(s"fromBinary: ${bytes.length} bytes")
    val buf = ByteBuffer.wrap(bytes)

    val typeNameBytesLength     = VarintUtils.readUnsignedInt(buf)
    val persistablesBytesLength = VarintUtils.readUnsignedInt(buf)

    val typeName     = StringUtils.stringFromBinary(ByteArrayUtils.safeRead(buf, typeNameBytesLength))
    val persistables = PersistenceUtils.fromBinaryAsList(ByteArrayUtils.safeRead(buf, persistablesBytesLength)).asScala.toList

    this.typeName = typeName.typeName
    fieldHandlers = persistables.map(_.asInstanceOf[IndexFieldHandler[T]])
  }
}

object GeoTrellisDataAdapter {
  def load(dataType: DataType, typeName: TypeName): GeoTrellisDataAdapter[_] = {
    import scala.collection.JavaConverters._

    ServiceLoader
      .load(classOf[GeoTrellisDataAdapterProvider])
      .iterator()
      .asScala
      .find(_.canProcess(dataType))
      .getOrElse(throw new RuntimeException(s"Unable to find DataAdapter for type $dataType"))
      .adapter(typeName)
  }
}
