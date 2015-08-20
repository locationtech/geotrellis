package geotrellis.spark.io

package object s3 {
  private[s3]
  def maxIndexWidth(maxIndex: Long): Int = {
    def digits(x: Long): Int = if (x < 10) 1 else 1 + digits(x/10)
    digits(maxIndex)
  }

  private[s3]
  val encodeIndex = (index: Long, max: Int) => {
    index.toString.reverse.padTo(max, '0').reverse
  }

  def makePath(chunks: String*) =
    chunks.filter(_.nonEmpty).mkString("/")
}
