package geotrellis.spark

import org.apache.spark.rdd.RDD

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

  def getKeyBounds(rdd: RDD[(K, V)] forSome {type V}): KeyBounds[K]
}