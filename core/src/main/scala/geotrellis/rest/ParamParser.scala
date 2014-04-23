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

package geotrellis.rest

object ParamParser {
  /**
   * Turn exceptions into Option[A]. It's magic!
   */
  def tryIt[A](a: => A):Option[A] = try {
    Some(a)
  } catch {
    case _:Exception => None
  }

  /**
   * Non-exceptions versions of your favorite Java methods!
   */
  def getInt(s:String) = tryIt(s.toInt)
  def getLong(s:String) = tryIt(s.toLong)
  def getFloat(s:String) = tryIt(s.toFloat)
  def getDouble(s:String) = tryIt(s.toDouble)

  /**
   * Turns out this thing is called traverse()
   *
   * The basic idea is to do bs = as.map(f). If bs contains no None values
   * then we return Some(bs). Otherwise we return None, and propagate the
   * "error" up the call.
   *
   * It's in scalaz apparently; eventually we might want to consider using that.
   */
  def traverse[A,B:Manifest](as:Array[A], f:A => Option[B]):Option[Array[B]] = {
    val bs = Array.ofDim[B](as.length)
    var i = 0
    while (i < as.length) {
      f(as(i)) match {
        case Some(a) => bs(i) = a
        case None => return None
      }
      i += 1
    }
    Some(bs)
  }

  /**
   * Parse a String into Some[Array[Double]]; return None on error.
   */
  def parseDoubles(s:String) = traverse(s.split(','), (t:String) => getDouble(t))

  /**
   * Parse a String into Some[(Double, Double)]; return None on error.
   */
  def parsePoint(s:String) = parseDoubles(s) match {
    case Some(Array(a, b)) => Some((a, b))
    case _ => None
  }

  /**
   * Parse a String into Some[(Double, Double, Double, Double)]; return None on error.
   */
  def parseBox(s:String) = parseDoubles(s) match {
    case Some(Array(a, b, c, d)) => Some((a, b, c, d))
    case _ => None
  }

  /**
   * Parse a String into Some[Array[(Double, Double)]]; return None on error.
   */
  def parsePoints(s:String) = traverse(s.split('|'), (t:String) => parsePoint(t))

  /**
   * Like parsePoints but ensures that we have at least 3 points.
   */
  def parsePolygon(s:String) = parsePoints(s) match {
    case Some(ps) if ps.length > 2 => Some(ps)
    case _ => None
  }
}
