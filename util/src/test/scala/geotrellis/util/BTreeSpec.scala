package geotrellis.util

import org.scalatest._

// --- //

class BTreeSpec extends FunSpec with Matchers {
  private val v: Vector[Int] = (1 to 7).toVector

  describe("Instantiation") {
    it("should succeed on empty input") {
      BTree.fromSortedSeq(Vector.empty[Int]) shouldBe None
    }

    it("should succeed for sorted input") {
      BTree.fromSortedSeq(v) shouldBe defined
    }

    it("should succeed for Vector(1)") {
      BTree.fromSortedSeq(Vector(1)) shouldBe Some(BTree(1, None, None))
    }

    it("should succeed for Vector(1, 2)") {
      BTree.fromSortedSeq(Vector(1, 2)) shouldBe Some(BTree(
        2,
        Some(BTree(1, None, None)),
        None
      ))
    }

    it("should succeed for Vector(1, 2, 3)") {
      BTree.fromSortedSeq(Vector(1, 2, 3)) shouldBe Some(BTree(
        2,
        Some(BTree(1, None, None)),
        Some(BTree(3, None, None))
      ))
    }

    it("should produce a balanced tree") {
      BTree.fromSortedSeq(v).get.value shouldBe 4
    }
  }

  describe("Binary Search") {
    val b: BTree[Int] = BTree.fromSortedSeq(v).get

    it("searchWith should find every value in a normal BTree") {
      b.foreach({ v =>
        val res = b.searchWith({ tree =>
          if (v == tree.value) {
            Right(tree.value)
          } else if (v < tree.value) {
            Left(tree.left)
          } else {
            Left(tree.right)
          }
        })

        res shouldBe Some(v)
      })
    }

    it("searchWith should not find something not in a BTree") {
      val v = 97

      val res = b.searchWith({ tree =>
        if (v == tree.value) {
          Right(tree.value)
        } else if (v < tree.value) {
          Left(tree.left)
        } else {
          Left(tree.right)
        }
      })

      res shouldBe None
    }
  }
}
