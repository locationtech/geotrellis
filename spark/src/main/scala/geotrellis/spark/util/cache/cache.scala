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

package geotrellis.spark.util.cache

import java.util.concurrent.locks._
import java.util.concurrent._
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import scala.collection.mutable

/** Base trait for a caching strategy
 *
 * K is the cache key
 * V is the cache value
 */
@deprecated("This will be removed in favor of a pluggable cache in 2.0", "1.2")
trait Cache[K,V] extends Serializable {

  /** Lookup the value for key k
   * @return Some(v) if the value was cached, None otherwise
   */
  def lookup(k: K):Option[V]

  /** Insert the value v (keyed by k) into the cache
   * @return true if the value was inserted into the cache
   */
  def insert(k: K, v: V):Boolean

  /** Remove k from the cache */
  def remove(k: K):Option[V]

  /** Lookup the value for key k
   * If the value is in the cache, return it.
   * Otherwise, insert the value v and return it
   *
   * Note: This method does not guarantee that v will be in the cache when it returns
   *
   * @return the cached value keyed to k or the computed value of v
   */
  def getOrInsert(k: K, vv: => V):V = lookup(k) match {
    case Some(v) => v
    case None => {insert(k, vv); vv}
  }

}

/** A Cache Strategy that completely ignores caching and always returns the input object
 * Operations on this cache execute in O(1) time
 */
@deprecated("This will be removed in favor of a pluggable cache in 2.0", "1.2")
class NoCache[K,V] extends Cache[K,V] {
  def lookup(k: K):Option[V] = None
  def insert(k: K, v: V):Boolean = false
  def remove(k: K):Option[V] = None
}

/** An unbounded hash-backed cache
 * Operations on this cache execute in O(1) time
 */
@deprecated("This will be removed in favor of a pluggable cache in 2.0", "1.2")
trait HashBackedCache[K,V] extends Cache[K,V] {
  val cache = new ConcurrentHashMap[K, V]()

  def lookup(k: K):Option[V] = Option(cache.get(k))
  def remove(k: K):Option[V] = Option(cache.remove(k))
  def insert(k: K, v: V):Boolean = { cache.put(k,v); true }
}

/** A hash backed cache with a size boundary
 * Operations on this cache may required O(N) time to execute (N = size of cache)
 */
@deprecated("This will be removed in favor of a pluggable cache in 2.0", "1.2")
trait BoundedCache[K,V] extends Cache[K,V] {

  /** Return the size of a a given cache item
   * If items are objects (for example) this might just be: (x) => 1
   * If items are arrays of ints and the maxSize is in bytes this function might be:
   *   (x) => x.length * 4
   */
  val sizeOf: V => Long

  /** Maximum size of the cache
   */
  val maxSize: Long

  /** Attempt to free l amount of cache space
   * this method is called once after an insert would have failed due to
   * space constraints. After this method returns the insert will be retried
   */
  def cacheFree(l: Long): Unit

  var currentSize: Long = 0

  /**
   * returns true iff v can be inserted into the cache without violating the
   * size boundary
   */
  def canInsert(v: V) = {
    val ok = currentSize + sizeOf(v) <= maxSize
    //println(s"[cache] canInsert says $ok ($currentSize + ${sizeOf(v)} <= $maxSize)")
    ok
  }

  abstract override def remove(k: K):Option[V] = lookup(k) match {
    case None => None
    case Some(v) => {
      currentSize -= sizeOf(v)
      super.remove(k)
    }
  }

  abstract override def insert(k: K, v: V):Boolean = {
    // If something is already at this location, remove it from the cache size
    lookup(k) map ( r => currentSize -= sizeOf(r) )

    if (!canInsert(v)) {
      cacheFree(currentSize + sizeOf(v) - maxSize)
    }

    if (canInsert(v)) {
      currentSize += sizeOf(v)
      //println(s"[Cache] Inserted $k")
      super.insert(k,v)

      true
    } else {
      //println(s"[Cache] Failed to insert $k (cache full)")
      false
    }
  }

  def evicted(v: V): Unit = {}
}

@deprecated("This will be removed in favor of a pluggable cache in 2.0", "1.2")
class LRUCache[K,V](val maxSize: Long, val sizeOf: V => Long = (v:V) => 1) extends HashBackedCache[K,V]  with BoundedCache[K,V] {

  /** Contains order of cache requests, with the key at the tail read most recently */
  private[this] val cacheOrder = new ConcurrentLinkedQueue[K]()

  /** Signal that the value for k was recently looked up */
  private[this] def touch(k: K): Unit = {
    cacheOrder.remove(k)
    cacheOrder.add(k)
  }

  /** Attempt to free space in the cache starting with the last accessed element
   * @param ltgt: The additional space in the cache requested
   */
  def cacheFree(ltgt: Long):Unit = {
    // logger.trace(s"Attempting to free $ltgt units of data (cache max: $maxSize, cache cur: $currentSize)")

    // if (ltgt > maxSize) {
    //    logger.warn(s"Item to big to fit in cache at all")
    // }

    var l = ltgt
    while(l > 0 && cacheOrder.size > 0) {
      val item: K = cacheOrder.poll() // remove the oldest element
      val removedSize: Long = remove(item) match {
        case Some(v) => {
          // logger.trace(s"Evicted $item (${sizeOf(v)} units) from cache")
          evicted(v)
          sizeOf(v)
        }
        case None => 0L
      }
      l -= removedSize
    }
  }

  override def lookup(k: K) = super.lookup(k) match {
    case v: Some[_] =>
      touch(k)
      v
    case None =>
      None
  }

  override def insert(k: K, v: V) = if (super.insert(k,v)) {
    touch(k)
    true
  } else {
    false
  }
}
