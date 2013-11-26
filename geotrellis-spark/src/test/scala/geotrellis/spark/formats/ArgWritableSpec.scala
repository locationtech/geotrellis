package geotrellis.spark.formats

import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSpec

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class ArgWritableSpec extends FunSpec with MustMatchers with ShouldMatchers {
	describe("conversion from/to ArgWritable") {
	  it("should convert from Array to ArgWritable and back") {
		  val expected = Array.fill(10)(1)
		  val actual = ArgWritable.toArray(ArgWritable.toWritable(expected))
		  expected should be(actual)		  
	  }
	}
}