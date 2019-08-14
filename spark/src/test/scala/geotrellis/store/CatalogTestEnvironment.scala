package geotrellis.store

import geotrellis.spark.testkit.TestEnvironment

import org.scalatest.Suite

import java.io.File

trait CatalogTestEnvironment extends TestEnvironment { self: Suite =>
  override def beforeAll() = {
    val multibandDir = new File(TestCatalog.multibandOutputPath)
    // length >= 2 means that in the directory there are at least two folders - attributes and at least one layer folder
    if (!(multibandDir.exists && multibandDir.list().length >= 2)) TestCatalog.createMultiband
    else println(s"Test multi-band catalog exists at: $multibandDir")
    val singlebandDir = new File(TestCatalog.singlebandOutputPath)
    if (!(singlebandDir.exists && singlebandDir.list().length >= 2)) TestCatalog.createSingleband
    else println(s"Test single-band catalog exists at: $singlebandDir")
  }
}
