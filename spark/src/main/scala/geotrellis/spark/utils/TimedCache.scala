package geotrellis.spark.utils

class TimedCache[K, V](timeout: Long = 1000*10) {  
  var cache: Map[K, (V, Long)] = Map.empty

  def get(key: K): Option[V] = {
    cache.get(key) flatMap { case (value, time) => 
      if (time > System.currentTimeMillis){
        Some(value)
      } else {
        cache = cache - key
        None
      }
    }
  }

  def put(key: K, value: V) = {
    cache = cache updated (key, value -> (System.currentTimeMillis + timeout))
  }

}