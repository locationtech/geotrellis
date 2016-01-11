package geotrellis.spark.io.index

object IndexRanges {
  /*
   * Will attempt to bin ranges into buckets, each containing at least the average number of elements.
   * Trailing bins may be empty if the count is too high for number of ranges.
   */
  def bin(ranges: Seq[(Long, Long)], count: Int): Seq[Seq[(Long, Long)]] = {
    var stack = ranges.toList

    def len(r: (Long, Long)) = r._2 - r._1 + 1l
    val total = ranges.foldLeft(0l) { (s, r) => s + len(r) }
    val binWidth = total / count + 1

    def splitRange(range: (Long, Long), take: Long): ((Long, Long), (Long, Long)) = {
      assert(len(range) > take)
      (range._1, range._1 + take - 1) -> (range._1 + take, range._2)
    }

    val arr = Array.fill(count)(Nil: List[(Long, Long)])
    var sum = 0l
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
