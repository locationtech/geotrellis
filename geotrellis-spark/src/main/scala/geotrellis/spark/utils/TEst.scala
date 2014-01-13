package geotrellis.spark.utils

object Test {

  class ParentType {
   
    def bar: ParentType = {
      println("ParentType:bar")
      this
    }
  }
  class ChildType extends ParentType

  abstract class C {
    def foo: ParentType
  }

  class D extends C {
    override def foo: ChildType = {
      println("in D:foo")
      new ChildType
    }
  }

  class E extends D {
    override def foo = {
      super.foo.bar.asInstanceOf[ChildType]
    }
  }
}