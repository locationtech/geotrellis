package geotrellis.spark
import org.apache.spark.SparkContext
import org.scalatest._
import org.scalatest.BeforeAndAfterAll

/* 
 * Creates a local SparkContext for use in all tests in a suite 
 */
trait SharedSparkContext extends BeforeAndAfterAll { self: Suite =>

  @transient private var _sc: SparkContext = _

  def sc: SparkContext = _sc

  override def beforeAll() {
    System.setProperty("spark.master.port", "0")    
    // we don't call the version in SparkUtils as that moves the jar file dependency around
    // and that is not needed for local spark context
    _sc = new SparkContext("local", self.suiteName)
    super.beforeAll()
  }

  override def afterAll() {
    _sc.stop
    _sc = null
    System.clearProperty("spark.driver.port")
    super.afterAll()
  }
}
