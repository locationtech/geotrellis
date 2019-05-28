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

package geotrellis.spark.io.accumulo

import geotrellis.layers.LayerId
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo.conf.AccumuloConfig
import spray.json._
import spray.json.DefaultJsonProtocol._
import org.apache.accumulo.core.client.{BatchWriterConfig, Connector}
import org.apache.accumulo.core.security.Authorizations
import org.apache.accumulo.core.data._
import org.apache.accumulo.core.client.IteratorSetting
import org.apache.accumulo.core.iterators.user.RegExFilter
import org.apache.hadoop.io.Text

import scala.collection.JavaConverters._

object AccumuloAttributeStore {
  def apply(connector: Connector, attributeTable: String): AccumuloAttributeStore =
    new AccumuloAttributeStore(connector, attributeTable)

  def apply(connector: Connector): AccumuloAttributeStore =
    apply(connector, AccumuloConfig.catalog)

  def apply(instance: AccumuloInstance, attributeTable: String): AccumuloAttributeStore =
    apply(instance.connector, attributeTable)

  def apply(instance: AccumuloInstance): AccumuloAttributeStore =
    apply(instance.connector)
}

class AccumuloAttributeStore(val connector: Connector, val attributeTable: String) extends DiscreteLayerAttributeStore {
  //create the attribute table if it does not exist
  {
    val ops = connector.tableOperations()
    if (!ops.exists(attributeTable)) ops.create(attributeTable)
  }

  val SEP = "__.__"

  def layerIdText(layerId: LayerId): Text =
    s"${layerId.name}${SEP}${layerId.zoom}"

  private def fetch(layerId: Option[LayerId], attributeName: String): Iterator[Value] = {
    val scanner = connector.createScanner(attributeTable, new Authorizations())
    try {
      layerId.foreach { id => scanner.setRange(new Range(layerIdText(id))) }
      scanner.fetchColumnFamily(new Text(attributeName))
      scanner.iterator.asScala.map(_.getValue)
    } finally scanner.close()
  }

  private def delete(layerId: LayerId, attributeName: Option[String]): Unit = {
    val numThreads = 1
    val config = new BatchWriterConfig()
    config.setMaxWriteThreads(numThreads)
    val deleter = connector.createBatchDeleter(attributeTable, new Authorizations(), numThreads, config)

    try {
      deleter.setRanges(List(new Range(layerIdText(layerId))).asJava)
      attributeName.foreach { name =>
        deleter.fetchColumnFamily(new Text(name))
      }
      deleter.delete()
      attributeName match {
        case Some(attribute) => clearCache(layerId, attribute)
        case None => clearCache(layerId)
      }
    } finally deleter.close()
  }

  def read[T: JsonFormat](layerId: LayerId, attributeName: String): T = {
    val values = fetch(Some(layerId), attributeName).toVector

    if(values.isEmpty) {
      throw new AttributeNotFoundError(attributeName, layerId)
    } else if(values.size > 1) {
      throw new LayerIOError(s"Multiple attributes found for $attributeName for layer $layerId")
    } else {
      values.head.toString.parseJson.convertTo[(LayerId, T)]._2
    }
  }

  def readAll[T: JsonFormat](attributeName: String): Map[LayerId,T] = {
    fetch(None, attributeName)
      .map { _.toString.parseJson.convertTo[(LayerId, T)] }
      .toMap
  }

  def write[T: JsonFormat](layerId: LayerId, attributeName: String, value: T): Unit = {
    val mutation = new Mutation(layerIdText(layerId))
    mutation.put(
      new Text(attributeName), new Text(), System.currentTimeMillis(),
      new Value((layerId, value).toJson.compactPrint.getBytes)
    )

    connector.write(attributeTable, mutation)
  }

  def layerExists(layerId: LayerId): Boolean =
    fetch(Some(layerId), AttributeStore.Fields.metadata).nonEmpty

  def delete(layerId: LayerId): Unit = delete(layerId, None)

  def delete(layerId: LayerId, attributeName: String): Unit = delete(layerId, Some(attributeName))

  def layerIds: Seq[LayerId] = {
    val scanner = connector.createScanner(attributeTable, new Authorizations())
    try {
      scanner.iterator.asScala.map { kv =>
        val Array(name, zoomStr) = kv.getKey.getRow.toString.split(SEP)
        LayerId(name, zoomStr.toInt)
      }
      .toList
      .distinct
    } finally scanner.close()
  }

  def availableAttributes(id: LayerId): Seq[String] = {
    val scanner = connector.createScanner(attributeTable, new Authorizations())
    try {
      scanner.setRange(new Range(layerIdText(id)))
      scanner.iterator.asScala.map(_.getKey.getColumnFamily.toString).toVector
    } finally scanner.close
  }

  override def availableZoomLevels(layerName: String): Seq[Int] = {
    val scanner = connector.createScanner(attributeTable, new Authorizations())
    try {
      // 15 is a default priority from docs https://accumulo.apache.org/1.8/accumulo_user_manual.html#_setting_iterators_via_the_shell
      val iter = new IteratorSetting(15, "AttributeStoreLayerNameFilter", classOf[RegExFilter])
      RegExFilter.setRegexs(iter, s"${layerName}${SEP}.*", null, null, null, false)
      scanner.addScanIterator(iter)
      scanner.iterator.asScala.map { kv =>
        val Array(_, zoomStr) = kv.getKey.getRow.toString.split(SEP)
        zoomStr.toInt
      }
      .toList
      .distinct
    } finally scanner.close()
  }
}
