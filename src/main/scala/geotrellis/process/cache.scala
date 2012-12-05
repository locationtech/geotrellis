package geotrellis.process
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

/** Base trait for a caching strategy
 *
 * K is the cache key
 * V is the cache value
 */
trait CacheStrategy[K,V] {

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
class NoCacheStrategy[K,V] extends CacheStrategy[K,V] {
  def lookup(k: K):Option[V] = None
  def insert(k: K, v: V):Boolean = false
  def remove(k: K):Option[V] = None
}

/** An unbounded hash-backed cache
 * Operations on this cache execute in O(1) time
 */
trait HashBackedCache[K,V] extends CacheStrategy[K,V] {
  val cache = new HashMap[K,V].empty

  def lookup(k: K):Option[V] = cache.get(k)
  def remove(k: K):Option[V] = cache.remove(k)
  def insert(k: K, v: V):Boolean = { cache.put(k,v); true }
}

trait LoggingCache[K,V] extends CacheStrategy[K,V] {
  abstract override def lookup(k: K):Option[V] = super.lookup(k) match {
    case None => { 
      println("Cache miss on %s".format(k)); 
      None 
    }
    case z => { 
      println("Cache hit on %s".format(k)); 
      z
    }
  }
}

/** A hash backed cache with a size boundary
 * Operations on this cache may required O(N) time to execute (N = size of cache)
 */
trait BoundedCache[K,V] extends CacheStrategy[K,V] {
  
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
  def cacheFree(l: Long):Unit

  var currentSize: Long = 0

  /**
   * returns true iff v can be inserted into the cache without violating the
   * size boundary
   */
  def canInsert(v: V) = {
    val ok = currentSize + sizeOf(v) <= maxSize
    println("[cache] canInsert says %s (%d + %d <= %d)" format (ok, currentSize, sizeOf(v), maxSize))
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
      println("[Cache] Inserted %s".format(k))
      super.insert(k,v)

      true
    } else {
      println("[Cache] Failed to insert %s (cache full)".format(k))
      false
    }
  }
}

trait OrderedBoundedCache[K,V] extends BoundedCache[K,V] {

  /** Called when attempt to make space in the cache. This function should
   * return the index of the item to remove next
   * 
   * @param k: Seq[K]  first element was accessed most recently
   */
  val removeIdx: Seq[K] => Int

  /** Attempt to free space in the cache starting with the last accessed element
   * @param ltgt: The additional space in the cache requested
   */
  def cacheFree(ltgt: Long):Unit = {
    println("[Cache] Attempting to free %d units of data (cache max: %d, cache cur: %d)".format(ltgt, maxSize, currentSize))

    if (ltgt > maxSize) {
       println("[Cache] File to big to fit in cache at all")
    }

    var l = ltgt
    while(l > 0 && cacheOrder.length > 0) {
      val item:K = cacheOrder.remove(removeIdx(cacheOrder))
      val removedSize: Long = remove(item) match {
        case Some(v) => {
          println("[Cache]\tEvicted %s (%d units) from cache".format(item, sizeOf(v)))
          sizeOf(v)
        }
        case None => 0L
      }
      l -= removedSize
    }
  }

  /* Contains order of cache requests
   * the item at index 0 was read most recently and item at (length - 1) was read the longest ago
   */
  private[this] var cacheOrder:ListBuffer[K] = new ListBuffer[K]()
  private[this] var lock = new Object();

  // Signal that the value for k was recently looked up
  private[this] def prepend(k: K) = {
    lock.synchronized {
      cacheOrder -= k
      cacheOrder.prepend(k)
    }
    k
  }

  abstract override def lookup(k: K) = super.lookup(k) match {
    case v@Some(_) => { prepend(k); v }
    case None => None
  }

  abstract override def insert(k: K, v: V) = if (super.insert(k,v)) {
    prepend(k); true 
  } else {
    false
  }
}        

class LRUCache[K,V](val maxSize: Long, val sizeOf: V => Long = (v:V) => 1) extends HashBackedCache[K,V] with OrderedBoundedCache[K,V] with AtomicCache[K,V] with LoggingCache[K,V] {
  val removeIdx: Seq[K] => Int = (s: Seq[K]) => s.length - 1
}

class MRUCache[K,V](val maxSize: Long, val sizeOf: V => Long = (v:V) => 1) extends HashBackedCache[K,V] with OrderedBoundedCache[K,V] with AtomicCache[K,V] with LoggingCache[K,V] {
  val removeIdx: Seq[K] => Int = (s: Seq[K]) => 0
}


/** Atomic cache provides an atomic getOrInsert(k,v) method
 * This cache assumes that (k,v) pair is immutable
 */
trait AtomicCache[K,V] extends CacheStrategy[K,V] {
  val bigLock:Lock = new ReentrantLock()

  val currentlyLoading:HashMap[K,Lock] = new HashMap[K,Lock].empty

  abstract override def lookup(k: K):Option[V] = {
    bigLock.lock()
    val smallLockOpt = currentlyLoading.get(k)
    bigLock.unlock()

    smallLockOpt.map(smallLock => {
      smallLock.lock()
      smallLock.unlock()
    })

    super.lookup(k)
  }

  abstract override def getOrInsert(k: K, v: => V):V = {
    val t0 = System.currentTimeMillis
    bigLock.lock()
    try {
      if (currentlyLoading.contains(k)) {
        // Another thread is currently loading up the cache entry for k so we
        // block until it is done.
        val smallLock = currentlyLoading.get(k).get
        bigLock.unlock()

        // the small lock blocks until the thread that is doing the loading is
        // complete.
        smallLock.lock()
        smallLock.unlock()
        
        // v will already have been evaluated by some other thread
        // if there was an error we'll evaluate v
        //super.lookup(k).getOrElse(v)
        val resultOpt = super.lookup(k)
        resultOpt match {
          case None => {
            val vv = v
            val t = System.currentTimeMillis - t0            
            println("waited on other thread, but failed: %d ms" format (t))
            vv
          }
          case Some(vv) => {
            val t = System.currentTimeMillis - t0            
            println("waited on other thread: %d ms" format (t))
            vv
          }
        }
      } else {
        super.lookup(k) match {
          case Some(vv) => {
            bigLock.unlock()
            val t = System.currentTimeMillis - t0            
            println("found in cache: %d ms" format t)
            vv
          }
          case None => {
            val smallLock = new ReentrantLock()
            currentlyLoading.put(k, smallLock)
            smallLock.lock()
            val vv = try {
              bigLock.unlock()
            
              val vv = v    // Evaluate v

              bigLock.lock()
              super.insert(k,v)

              currentlyLoading.remove(k)
              bigLock.unlock()
              vv

            } catch {
              case t:Throwable => { println("error"); t.printStackTrace(); println("rethrow..."); throw t }
            } finally {
              smallLock.unlock()
            }
            
            val t = System.currentTimeMillis - t0            
            println("added to cache: %d ms" format t)
            vv
          }
        }
      }
    } catch {
      case t:Throwable => {
        bigLock.unlock()
        throw t
      }
    }
  }
      

  abstract override def insert(k: K, v: V):Boolean = {
    bigLock.lock()
    try {
      super.insert(k,v)
    } finally {
      bigLock.unlock()
    }
  }          
}
