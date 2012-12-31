---
layout: overview
title: String Interpolation
disqus: true
label-color: success
label-text: New in 2.10
overview: string-interpolation
languages: [es, ja]
---

**Josh Suereth**

## Introduction

Starting in Scala 2.10.0, Scala offers a new mechanism to create strings from your data:  String Interpolation.
String Interpolation allows users to embed variable references directly in *processed* string literals.  Here's an example:

    val name = "James"
    println(s"Hello, $name")  // Hello, James

In the above, the literal `s"Hello, $name"` is a *processed* string literal.  This means that the compiler does some additional
work to this literal.  A processed string literal is denoted by a set of characters precedding the `"`. String interpolation
was introduced by [SIP-13](http://docs.scala-lang.org/sips/pending/string-interpolation.html), which contains all details of the implementation.

## Usage

Scala provides three string interpolation methods out of the box:  `s`, `f` and `raw`.

### The `s` String Interpolator

Prepending `s` to any string literal allows the usage of variables directly in the string. You've already seen an example here:

    val name = "James"
    println(s"Hello, $name")  // Hello, James

Here `$name` is nested inside an `s` processed string.  The `s` interpolator knows to insert the value of the `name` variable at this location
in the string, resulting in the string `Hello, James`.  With the `s` interpolator, any name that is in scope can be used within a string.

String interpolators can also take arbitrary expressions.  For example:

    println(s"1 + 1 = ${1 + 1}")

will print the string `1 + 1 = 2`.  Any arbitrary expression can be embedded in `${}`.


### The `f` Interpolator

Prepending `f` to any string literal allows the creation of simple formatted strings, similar to `printf` in other languages.  When using the `f`
interpolator, all variable references should be followed by a `printf`-style format string, like `%d`.   Let's look at an example:

    val height = 1.9d
    val name = "James"
    println(f"$name%s is $height%2.2f meters tall")  // James is 1.90 meters tall

The `f` interpolator is typesafe.  If you try to pass a format string that only works for integers but pass a double, the compiler will issue an
error.  For example:

    val height: Double = 1.9d
    
    scala> f"$height%4d"
    <console>:9: error: type mismatch;
     found   : Double
     required: Int
               f"$height%4d"
                  ^

The `f` interpolator makes use of the string format utilities available from Java.   The formats allowed after the `%` character are outlined in the 
[Formatter javadoc](http://docs.oracle.com/javase/1.6.0/docs/api/java/util/Formatter.html#detail).   If there is no `%` character after a variable
definition a formatter of `%s` (`String`) is assumed.


### The `raw` Interpolator

The raw interpolator is similar to the `s` interpolator except that it performs no escaping of literals within the string.  Here's an example processed string:

    scala> s"a\nb"
    res0: String = 
    a
    b

Here the `s` string interpolator replaced the characters `\n` with a return character.   The `raw` interpolator will not do that.

    scala> raw"a\nb"
    res1: String = a\nb

The raw interpolator is useful when you want to avoid having expressions like `\n` turn into a return character.


In addition to the three default string interpolators, users can define their own.

## Advanced Usage

In Scala, all processed string literals are simple code transformations.   Anytime the compiler encounters a string literal of the form:

    id"string content"

it transforms it into a method call (`id`) on an instance of [StringContext](http://www.scala-lang.org/archives/downloads/distrib/files/nightly/docs/library/index.html#scala.StringContext).
This method can also be available on implicit scope.   To define our own string interpolation, we simply need to create an implicit class that adds a new method
to `StringContext`.  Here's an example:

    // Note: We extends AnyVal to prevent runtime instantiation.  See 
    // value class guide for more info.
    implicit class JsonHelper(val sc: StringContext) extends AnyVal {
      def json(args: Any*): JSONObject = sys.error("TODO - IMPLEMENT")
    }
    
    def giveMeSomeJson(x: JSONObject): Unit = ...
    
    giveMeSomeJson(json"{ name: $name, id: $id }")

In this example, we're attempting to create a JSON literal syntax using string interpolation.   The JsonHelper implicit class must be in scope to use this syntax, and the json method would need a complete implementation.   However, the result of such a formatted string literal would not be a string, but a `JSONObject`.

When the compiler encounters the literal `json"{ name: $name, id: $id }"` it rewrites it to the following expression:

    new StringContext("{ name:", ",id: ", "}").json(name, id)

The implicit class is then used to rewrite it to the following:

    new JsonHelper(new StringContext("{ name:", ",id: ", "}")).json(name, id)

So, the `json` method has access to the raw pieces of strings and each expression as a value.   A simple (buggy) implementation of this method could be:

    implicit class JsonHelper(val sc: StringContext) extends AnyVal {
      def json(args: Any*): JSONObject = {
        val strings = sc.parts.iterator
        val expressions = args.iterator
        var buf = new StringBuffer(strings.next)
        while(strings.hasNext) {
          buf append expressions.next
          buf append strings.next
        }
        parseJson(buf)
      }
    }

Each of the string portions of the processed string are exposed in the `StringContext`'s `parts` member.  Each of the expression values is passed into the `json` method's `args` parameter.   The `json` method takes this and generates a big string which it then parses into JSON.   A more sophisticated implementation could avoid having to generate this string and simply construct the JSON directly from the raw strings and expression values.

## Limitations

String interpolation currently does not work within pattern matching statements.  This feature is targeted for Scala 2.11 release.

