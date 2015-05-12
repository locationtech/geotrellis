package geotrellis.spark

/**
 * This type class marks K as point that can be bounded in space.
 * It is used to construct bounding hypercube for a set of Ks.
 *
 * The bounds must be calculated by taking min/max of each component dimension of K.
 * Consequently the result may be neither a nor b, but a new value.
 */
trait Boundable[K] extends Serializable {
  def minBound(p1: K, p2: K): K
  
  def maxBound(p1: K, p2: K): K
  
  def include(p: K, bounds: KeyBounds[K]): KeyBounds[K] = {    
    KeyBounds(
      minBound(bounds.minKey, p),
      maxBound(bounds.maxKey, p))
  }
  
  def combine(b1: KeyBounds[K], b2: KeyBounds[K]): KeyBounds[K] = {
    KeyBounds(
      minBound(b1.minKey, b2.minKey),
      maxBound(b1.maxKey, b2.maxKey))
  }

  def getKeyBounds(rdd: RasterRDD[K]): KeyBounds[K]
}