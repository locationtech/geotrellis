/*
 * Copyright (c) 2014 Azavea.
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

package geotrellis.engine

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import scala.collection.mutable

/**
 * Trait for a T-keyed, any valued cache.
 */
trait Cache[T] extends Serializable {
  /** Lookup the value for key k
   * @return Some(v) if the value was cached, None otherwise
   */
  def lookup[V](k: T):Option[V]

  /** Insert the value v (keyed by k) into the cache
   * @return true if the value was inserted into the cache
   */
  def insert[V](k: T, v: V):Boolean

  /** Remove k from the cache */
  def remove[V](k: T):Option[V]

  /** Lookup the value for key k
   * If the value is in the cache, return it.
   * Otherwise, insert the value v and return it
   *
   * Note: This method does not guarantee that v will be in the cache when it returns
   *
   * @return the cached value keyed to k or the computed value of v
   */
  def getOrInsert[V](k: T, vv: => V):V = lookup(k) match {
    case Some(v) => v
    case None => {insert(k, vv); vv}
  }
}

/**
 * Simple HashMap backed cache keyed by String and can hold any type.
 */
class HashCache[T] extends Cache[T] {
  val cache = new mutable.HashMap[T,Any].empty

  def lookup[V](k: T):Option[V] =
    cache.get(k) match {
      case Some(v) => Some(v.asInstanceOf[V])
      case None => None
    }

  def remove[V](k: T):Option[V] =
    cache.remove(k) match {
      case Some(v) => Some(v.asInstanceOf[V])
      case None => None
    }

  def insert[V](k: T, v: V):Boolean = { cache.put(k,v.asInstanceOf[Any]); true }
}