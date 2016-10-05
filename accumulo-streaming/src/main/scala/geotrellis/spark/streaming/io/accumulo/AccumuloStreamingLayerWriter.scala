package geotrellis.spark.streaming.io.accumulo

import geotrellis.spark.io._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.streaming.io._

import org.apache.spark.SparkContext

object AccumuloStreamingLayerWriter {
  def apply(
    instance: AccumuloInstance,
    table: String,
    options: AccumuloLayerWriter.Options
  )(implicit sc: SparkContext): GenericStreamingLayerWriter = {
    val attributeStore = AccumuloAttributeStore(instance.connector)

    new GenericStreamingLayerWriter(
      attributeStore = attributeStore,
      layerWriter    = new AccumuloLayerWriter(attributeStore, instance, table, options),
      layerUpdater   = new AccumuloLayerUpdater(instance, attributeStore, new AccumuloLayerReader(attributeStore)(sc, instance), options)
    )
  }

  def apply(
    instance: AccumuloInstance,
    table: String
  )(implicit sc: SparkContext): GenericStreamingLayerWriter = {
    val attributeStore = AccumuloAttributeStore(instance.connector)

    new GenericStreamingLayerWriter(
      attributeStore = attributeStore,
      layerWriter    = new AccumuloLayerWriter(attributeStore, instance, table, AccumuloLayerWriter.Options.DEFAULT),
      layerUpdater   = new AccumuloLayerUpdater(instance, attributeStore, new AccumuloLayerReader(attributeStore)(sc, instance), AccumuloLayerWriter.Options.DEFAULT)
    )
  }

  def apply(
    instance: AccumuloInstance,
    attributeStore: AttributeStore,
    table: String,
    options: AccumuloLayerWriter.Options
  )(implicit sc: SparkContext): GenericStreamingLayerWriter =
    new GenericStreamingLayerWriter(
      attributeStore = attributeStore,
      layerWriter    = new AccumuloLayerWriter(attributeStore, instance, table, options),
      layerUpdater   = new AccumuloLayerUpdater(instance, attributeStore, new AccumuloLayerReader(attributeStore)(sc, instance), options)
    )

  def apply(
    instance: AccumuloInstance,
    attributeStore: AttributeStore,
    table: String
  )(implicit sc: SparkContext): GenericStreamingLayerWriter =
    new GenericStreamingLayerWriter(
      attributeStore = attributeStore,
      layerWriter    = new AccumuloLayerWriter(attributeStore, instance, table, AccumuloLayerWriter.Options.DEFAULT),
      layerUpdater   = new AccumuloLayerUpdater(instance, attributeStore, new AccumuloLayerReader(attributeStore)(sc, instance), AccumuloLayerWriter.Options.DEFAULT)
    )
}
