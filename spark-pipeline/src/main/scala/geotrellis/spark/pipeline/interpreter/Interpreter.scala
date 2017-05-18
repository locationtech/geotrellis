package geotrellis.spark.pipeline.interpreter

import geotrellis.spark.pipeline.json._
import org.apache.spark.rdd.RDD

import scala.reflect.runtime.universe

object TypedObject {
  def read(clazz: String, `type`: String) = {
    val isSpatial = `type`.indexOfSlice("spatial") > 0
    val isSingleband = `type`.indexOfSlice("singleband") > 0
  }

}

object Inst {

  def apply(className: String, arg: Any) = {
    val runtimeMirror: universe.Mirror = universe.runtimeMirror(getClass.getClassLoader)

    val classSymbol: universe.ClassSymbol = runtimeMirror.classSymbol(Class.forName(className))

    val classMirror: universe.ClassMirror = runtimeMirror.reflectClass(classSymbol)

    if (classSymbol.companion.toString() == "<none>") {
      println(s"Info: $className has no companion object")
      val constructors = classSymbol.typeSignature.members.filter(_.isConstructor).toList
      if (constructors.length > 1) {
        println(s"Info: $className has several constructors")
      }
      else {
        val constructorMirror = classMirror.reflectConstructor(constructors.head.asMethod) // we can reuse it
        constructorMirror()
      }

    }
    else {
      val companionSymbol = classSymbol.companion
      println(s"Info: $className has companion object $companionSymbol")
      // TBD
    }

  }
}

object Interpreter {
  sealed trait Tree
  final case class Leaf(clazz: String) extends Tree
  final case class Node(clazz: String, ts: List[Tree]) extends Tree

  def interpretUntyped = {
    // everything should keep ordering
    val reads: List[Read] = List()
    val transformations: List[Transform] = List()
    val writes: List[Write] = List()

    val inputs: List[(String, RDD[Any])] = reads.map { r =>
      // make instance of a class and typed; after that it's possible to erase types again.
      // Class.forName(r.`type`).newInstance
      // read =>
      null: (String, RDD[Any])
    }

    // along with common transform operations there can be arguable aggregate functions,
    // to rename multiple inputs
    // or to merge them into multiband input

    val reorogonizedInputs = transformations.flatMap {
      case t: TransformGroup =>
        // make instance of a class and typed; after that it's possible to erase types again.
        // Class.forName(r.`type`).newInstance
        // List[(String, RDD[Any])] => List[(String, RDD[Any])] function applied
        // casting of RDD can be incapsulated into this functions
        null: List[(String, RDD[Any])]

      case t: TransformMerge =>
        // make instance of a class and typed; after that it's possible to erase types again.
        // Class.forName(r.`type`).newInstance
        // List[(String, RDD[Any])] => List[(String, RDD[Any])] function applied
        // casting of RDD can be incapsulated into this functions
        null: List[(String, RDD[Any])]

        // no transofmration steps applied
      case _ => null: List[(String, RDD[Any])]
    }

    val generalTransformations: List[(String, RDD[Any])] = reorogonizedInputs.map {
      case p @ (tag, rdd) =>
        transformations.foldLeft(p) { case (acc, tr: Transform) =>
          // make instance of a class and typed; after that it's possible to erase types again.
          // Class.forName(r.`type`).newInstance
          // (String, RDD[Any]) => (String, RDD[Any]) functions applied
          // casting of RDD can be incapsulated into this functions
          // String as a first tuple argument can be used to be sure that transformation can be applied
          // runtime exceptions can happen: class not found, or type can't be casted
          // shapeless.cast function can be used(?)

          // tr.instance.apply(acc)

          null: (String, RDD[Any])
        }
    }

    writes.collect { case w: Write =>
      // make instance of a class and typed; after that it's possible to erase types again.
      // Class.forName(r.`type`).newInstance
      // (String, RDD[Any]) => Boolean // Unit

      ()
    }

  }

  object o {
    // problem demonstration
    val example = List[String => Int](s => s.toInt, s => s.toInt + 1, s => s.toInt + 2, s => s.toInt + 3)
    example.foldLeft("0") { case (acc, f) => f(acc).toString } //> 5

  }
}
