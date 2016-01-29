package geotrellis.spark.io.index

object Index {
  /** Encode an index value as a string */
  def encode(index: Long, max: Int): String =
    index.toString.reverse.padTo(max, '0').reverse

  /** The number of digits in this index */
  def digits(x: Long): Int =
    if (x < 10) 1 else 1 + digits(x/10)
}
