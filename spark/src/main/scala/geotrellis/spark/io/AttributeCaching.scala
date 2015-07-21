package geotrellis.spark.io

import geotrellis.spark.{LayerId, KeyBounds}
import geotrellis.spark.io.index.KeyIndex
import scala.reflect._
import scala.util.Try

trait AttributeCaching[MetadataType] {

  val attributeStore: AttributeStore

  private val metadataCache = new collection.mutable.HashMap[LayerId, MetadataType]
  private val keyBoundsCache     = new collection.mutable.HashMap[LayerId, KeyBounds[_]]
  private val keyIndexCache      = new collection.mutable.HashMap[LayerId, KeyIndex[_]]

  def getLayerMetadata(id: LayerId)(implicit ev: attributeStore.ReadableWritable[MetadataType]) = 
    metadataCache
      .getOrElseUpdate(
        id,
        attributeStore.read[MetadataType](id, "metadata"))    
  
  def getLayerKeyBounds[K: ClassTag](id: LayerId)(implicit ev: attributeStore.ReadableWritable[KeyBounds[K]]): KeyBounds[K] =    
    keyBoundsCache
      .getOrElseUpdate(
        id, 
        attributeStore.read[KeyBounds[K]](id, "keyBounds"))
      .asInstanceOf[KeyBounds[K]]
  
  def getLayerKeyIndex[K: ClassTag](id: LayerId)(implicit ev: attributeStore.ReadableWritable[KeyIndex[K]]): KeyIndex[K] =
    keyIndexCache
      .getOrElseUpdate(
        id, 
        attributeStore.read[KeyIndex[K]](id, "keyIndex"))
      .asInstanceOf[KeyIndex[K]]

  def setLayerMetadata(id: LayerId, metadata: MetadataType)(implicit ev: attributeStore.ReadableWritable[MetadataType]) = {
    attributeStore.write[MetadataType](id, "metadata", metadata)
    metadataCache(id) = metadata
  }
  
  def setLayerKeyBounds[K: ClassTag](id: LayerId, keyBounds: KeyBounds[K])(implicit ev: attributeStore.ReadableWritable[KeyBounds[K]]) = {
    attributeStore.write[KeyBounds[K]](id, "keyBounds", keyBounds)
    keyBoundsCache(id) = keyBounds
  }
  
  def setLayerKeyIndex[K: ClassTag](id: LayerId, keyIndex: KeyIndex[K])(implicit ev: attributeStore.ReadableWritable[KeyIndex[K]])= {
    attributeStore.write[KeyIndex[K]](id, "keyIndex", keyIndex)
    keyIndexCache(id) = keyIndex
  }

  def layerExists(id: LayerId)(implicit ev: attributeStore.ReadableWritable[MetadataType]): Boolean = 
    Try(getLayerMetadata(id)).isSuccess

  def clearCache = {
    metadataCache.clear()
    keyBoundsCache.clear()
    keyIndexCache.clear()
  }
}