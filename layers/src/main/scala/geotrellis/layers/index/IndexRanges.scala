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

package geotrellis.layers.index

object IndexRanges {
  /*
   * Will attempt to bin ranges into buckets, each containing at least the average number of elements.
   * Trailing bins may be empty if the count is too high for number of ranges.
   */
  def bin(ranges: Seq[(BigInt, BigInt)], count: Int): Seq[Seq[(BigInt, BigInt)]] = {
    var stack = ranges.toList

    def len(r: (BigInt, BigInt)): BigInt = (r._2 - r._1) + BigInt(1)
    val total: BigInt = ranges.foldLeft(BigInt(0)) { (s, r) => s + len(r) }
    val binWidth: BigInt = (total / count) + 1

    def splitRange(range: (BigInt, BigInt), take: BigInt): ((BigInt, BigInt), (BigInt, BigInt)) = {
      assert(len(range) > take)
      (range._1, range._1 + take - 1) -> (range._1 + take, range._2)
    }

    val arr = Array.fill(count)(Nil: List[(BigInt, BigInt)])
    var sum = BigInt(0)
    var i = 0
    while (stack.nonEmpty) {
      val head = stack.head
      if (len(stack.head) + sum <= binWidth) {
        arr(i) = head :: arr(i)
        sum += len(head)
        stack = stack.tail
      } else {
        val (take, left) = splitRange(head, binWidth - sum)
        stack = left :: stack.tail
        arr(i) = take :: arr(i)
        sum += len(take)
      }

      if (sum >= binWidth) {
        sum = 0l
        i += 1
      }
    }
    arr
  }
}
