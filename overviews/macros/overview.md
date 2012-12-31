---
layout: overview-large
title: Def Macros

disqus: true

partof: macros
num: 1
outof: 4
---

**Eugene Burmako**

## Motivation

Compile-time metaprogramming has been recognized as a valuable tool for enabling such programming techniques as:
* Language virtualization (overloading/overriding semantics of the original programming language to enable deep embedding of DSLs),
* Program reification (providing programs with means to inspect their own code),
* Self-optimization (self-application of domain-specific optimizations based on program reification),
* Algorithmic program construction (generation of code that is tedious to write with the abstractions supported by a programming language).

In this overview we introduce a macro system for Scala. This facility allows programmers to write macro defs: functions that are transparently loaded by the compiler and executed during compilation. This realizes the notion of compile-time metaprogramming for Scala.

## Intuition

Here is a prototypical macro definition:

    def m(x: T): R = macro implRef

At first glance macro definitions are equivalent to normal function definitions, except for their body, which starts with the conditional keyword `macro` and is followed by a possibly qualified identifier that refers to a static macro implementation method.

If, during type-checking, the compiler encounters an application of the macro `m(args)`, it will expand that application by invoking the corresponding macro implementation method, with the abstract-syntax trees of the argument expressions args as arguments. The result of the macro implementation is another abstract syntax tree, which will be inlined at the call site and will be type-checked in turn.

The following code snippet declares a macro definition assert that references a macro implementation Asserts.assertImpl (definition of assertImpl is provided below):

    def assert(cond: Boolean, msg: Any) = macro Asserts.assertImpl

A call `assert(x < 10, "limit exceeded")` would then lead at compile time to an invocation

    assertImpl(c)(<[ x < 10 ]>, <[ “limit exceeded” ]>)

where `c` is a context argument that contains information collected by the compiler at the call site, and the other two arguments are abstract syntax trees representing the two expressions `x < 10` and `limit exceeded`.

In this document, `<[ expr ]>` denotes the abstract syntax tree that represents the expression expr. This notation has no counterpart in our proposed extension of the Scala language. In reality, the syntax trees would be constructed from the types in trait `scala.reflect.api.Trees` and the two expressions above would look like this:

    Literal(Constant("limit exceeded"))

    Apply(
      Select(Ident(newTermName("x")), newTermName("$less"),
      List(Literal(Constant(10)))))

Here is a possible implementation of the `assert` macro:

    import scala.reflect.macros.Context
    import scala.language.experimental.macros

    object Asserts {
      def raise(msg: Any) = throw new AssertionError(msg)
      def assertImpl(c: Context)
        (cond: c.Expr[Boolean], msg: c.Expr[Any]) : c.Expr[Unit] =
       if (assertionsEnabled)
          <[ if (!cond) raise(msg) ]>
          else
          <[ () ]>
    }

As the example shows, a macro implementation takes several parameter lists. First comes a single parameter, of type `scala.reflect.macros.Context`. This is followed by a list of parameters that have the same names as the macro definition parameters. But where the original macro parameter has type `T`, a macro implementation parameter has type `c.Expr[T]`. `Expr[T]` is a type defined in `Context` that wraps an abstract syntax tree of type `T`. The result type of the `assertImpl` macro implementation is again a wrapped tree, of type `c.Expr[Unit]`.

Also note that macros are considered an experimental and advanced feature, so they need to be enabled explicitly.
Do that either with `import scala.language.experimental.macros` on per-file basis
or with `-language:experimental.macros` (providing a compiler switch) on per-compilation basis.

### Generic macros

Macro definitions and macro implementations may both be generic. If a macro implementation has type parameters, actual type arguments must be given explicitly in the macro definition’s body. Type parameters in an implementation may come with `TypeTag` context bounds. In that case the corresponding type tags describing the actual type arguments instantiated at the application site will be passed along when the macro is expanded.

The following code snippet declares a macro definition `Queryable.map` that references a macro implementation `QImpl.map`:

    class Queryable[T] {
     def map[U](p: T => U): Queryable[U] = macro QImpl.map[T, U]
    }

    object QImpl {
     def map[T: c.TypeTag, U: c.TypeTag]
            (c: Context)
            (p: c.Expr[T => U]): c.Expr[Queryable[U]] = ...
    }

Now consider a value `q` of type `Queryable[String]` and a macro call

    q.map[Int](s => s.length)

The call is expanded to the following reflective macro invocation

    QImpl.map(c)(<[ s => s.length ]>)
       (implicitly[TypeTag[String]], implicitly[TypeTag[Int]])

## A complete example

This section provides an end-to-end implementation of a `printf` macro, which validates and applies the format string at compile-time.
For the sake of simplicity the discussion uses console Scala compiler, but as explained below macros are also supported by Maven and SBT.

Writing a macro starts with a macro definition, which represents the facade of the macro.
Macro definition is a normal function with anything one might fancy in its signature.
Its body, though, is nothing more that a reference to an implementation.
As mentioned above, to define a macro one needs to import `scala.language.experimental.macros`
or to enable a special compiler switch, `-language:experimental.macros`.

    import scala.language.experimental.macros
    def printf(format: String, params: Any*): Unit = macro printf_impl

Macro implementation must correspond to macro definitions that use it (typically there's only one, but there might also be many). In a nutshell, every parameter of type `T` in the signature of a macro definition must correspond to a parameter of type `c.Expr[T]` in the signature of a macro implementation. The full list of rules is quite involved, but it's never a problem, because if the compiler is unhappy, it will print the signature it expects in the error message.

    import scala.reflect.macros.Context
    def printf_impl(c: Context)(format: c.Expr[String], params: c.Expr[Any]*): c.Expr[Unit] = ...

Compiler API is exposed in `scala.reflect.macros.Context`. Its most important part, reflection API, is accessible via `c.universe`.
It's customary to import `c.universe._`, because it includes a lot of routinely used functions and types

    import c.universe._

First of all, the macro needs to parse the provided format string.
Macros run during the compile-time, so they operate on trees, not on values.
This means that the format parameter of the `printf` macro will be a compile-time literal, not an object of type `java.lang.String`.
This also means that the code below won't work for `printf(get_format(), ...)`, because in that case `format` won't be a string literal, but rather an AST that represents addition of two string literals. Adjusting the macro to work for arbitrary expressions is left as an exercise for the reader.

    val Literal(Constant(s_format: String)) = format.tree

Typical macros (and this macro is not an exception) need to create ASTs (abstract syntax trees) which represent Scala code.
To learn more about generation of Scala code, take a look at [the overview of reflection](http://docs.scala-lang.org/overviews/reflection/overview.html). Along with creating ASTs the code provided below also manipulates types.
Note how we get a hold of Scala types that correspond to `Int and String`.
Reflection overview linked above covers type manipulations in detail.
The final step of code generation combines all the generated code into a `Block`.
Note the call to `reify`, which provides a shortcut for creating ASTs.

    val evals = ListBuffer[ValDef]()
    def precompute(value: Tree, tpe: Type): Ident = {
      val freshName = newTermName(c.freshName("eval$"))
      evals += ValDef(Modifiers(), freshName, TypeTree(tpe), value)
      Ident(freshName)
    }

    val paramsStack = Stack[Tree]((params map (_.tree)): _*)
    val refs = s_format.split("(?<=%[\\w%])|(?=%[\\w%])") map {
      case "%d" => precompute(paramsStack.pop, typeOf[Int])
      case "%s" => precompute(paramsStack.pop, typeOf[String])
      case "%%" => Literal(Constant("%"))
      case part => Literal(Constant(part))
    }

    val stats = evals ++ refs.map(ref => reify(print(c.Expr[Any](ref).splice)).tree)
    c.Expr[Unit](Block(stats.toList, Literal(Constant(()))))

The snippet below represents a complete definition of the `printf` macro.
To follow the example, create an empty directory and copy the code to a new file named `Macros.scala`.

    import scala.reflect.macros.Context
    import scala.collection.mutable.{ListBuffer, Stack}

    object Macros {
      def printf(format: String, params: Any*): Unit = macro printf_impl

      def printf_impl(c: Context)(format: c.Expr[String], params: c.Expr[Any]*): c.Expr[Unit] = {
        import c.universe._
        val Literal(Constant(s_format: String)) = format.tree

        val evals = ListBuffer[ValDef]()
        def precompute(value: Tree, tpe: Type): Ident = {
          val freshName = newTermName(c.freshName("eval$"))
          evals += ValDef(Modifiers(), freshName, TypeTree(tpe), value)
          Ident(freshName)
        }

        val paramsStack = Stack[Tree]((params map (_.tree)): _*)
        val refs = s_format.split("(?<=%[\\w%])|(?=%[\\w%])") map {
          case "%d" => precompute(paramsStack.pop, typeOf[Int])
          case "%s" => precompute(paramsStack.pop, typeOf[String])
          case "%%" => Literal(Constant("%"))
          case part => Literal(Constant(part))
        }

        val stats = evals ++ refs.map(ref => reify(print(c.Expr[Any](ref).splice)).tree)
        c.Expr[Unit](Block(stats.toList, Literal(Constant(()))))
      }
    }

To use the `printf` macro, create another file `Test.scala` in the same directory and put the following code into it.
Note that using a macro is as simple as calling a function. It also doesn't require importing `scala.language.experimental.macros`.

    object Test extends App {
      import Macros._
      printf("hello %s!", "world")
    }

An important aspect of macrology is separate compilation. To perform macro expansion, compiler needs a macro implementation in executable form. Thus macro implementations need to be compiled before the main compilation, otherwise you might see the following error:

    ~/Projects/Kepler/sandbox$ scalac -language:experimental.macros Macros.scala Test.scala
    Test.scala:3: error: macro implementation not found: printf (the most common reason for that is that
    you cannot use macro implementations in the same compilation run that defines them)
    pointing to the output of the first phase
      printf("hello %s!", "world")
            ^
    one error found

    ~/Projects/Kepler/sandbox$ scalac Macros.scala && scalac Test.scala && scala Test
    hello world!

## Tips and tricks

### Using macros with the command-line Scala compiler

This scenario is covered in the previous section. In short, compile macros and their usages using separate invocations of `scalac`, and everything should work fine. If you use REPL, then it's even better, because REPL processes every line in a separate compilation run, so you'll be able to define a macro and use it right away.

### Using macros with Maven or SBT

The walkthrough in this guide uses the simplest possible command-line compilation, but macros also work with build tools such as Maven and SBT. The separate compilation restriction requires that macros must be placed in a separate SBT project or Maven submodule, but other than that everything should work smoothly. Check out [https://github.com/scalamacros/sbt-example](https://github.com/scalamacros/sbt-example) or [https://github.com/scalamacros/maven-example](https://github.com/scalamacros/maven-example) for additional information.

### Using macros with Scala IDE or Intellij IDEA

Both in Scala IDE and in Intellij IDEA macros are known to work fine, given they are moved to a separate project.

### Debugging macros

Debugging macros (i.e. the logic that drives macro expansion) is fairly straightforward. Since macros are expanded within the compiler, all that you need is to run the compiler under a debugger. To do that, you need to: 1) add all (!) the libraries from the lib directory in your Scala home (which include such jar files as `scala-library.jar`, `scala-reflect.jar`, `scala-compiler.jar` and `forkjoin.jar`) to the classpath of your debug configuration, 2) set `scala.tools.nsc.Main` as an entry point, 3) set command-line arguments for the compiler as `-cp <path to the classes of your macro> Test.scala`, where `Test.scala` stands for a test file containing macro invocations to be expanded. After all that is done, you should be able to put a breakpoint inside your macro implementation and launch the debugger.

What really requires special support in tools is debugging the results of macro expansion (i.e. the code that is generated by a macro). Since this code is never written out manually, you cannot set breakpoints there, and you won't be able to step through it. Scala IDE and Intellij IDEA teams will probably add support for this in their debuggers at some point, but for now the only way to debug macro expansions are diagnostic prints: `-Ymacro-debug-lite` (as described below), which prints out the code emitted by macros, and println to trace the execution of the generated code.

### Inspecting generated code

With `-Ymacro-debug-lite` it is possible to see both pseudo-Scala representation of the code generated by macro expansion and raw AST representation of the expansion. Both have their merits: the former is useful for surface analysis, while the latter is invaluable for fine-grained debugging.

    ~/Projects/Kepler/sandbox$ scalac -Ymacro-debug-lite Test.scala
    typechecking macro expansion Macros.printf("hello %s!", "world") at
    source-C:/Projects/Kepler/sandbox\Test.scala,line-3,offset=52
    {
      val eval$1: String = "world";
      scala.this.Predef.print("hello ");
      scala.this.Predef.print(eval$1);
      scala.this.Predef.print("!");
      ()
    }
    Block(List(
    ValDef(Modifiers(), newTermName("eval$1"), TypeTree().setType(String), Literal(Constant("world"))),
    Apply(
      Select(Select(This(newTypeName("scala")), newTermName("Predef")), newTermName("print")),
      List(Literal(Constant("hello")))),
    Apply(
      Select(Select(This(newTypeName("scala")), newTermName("Predef")), newTermName("print")),
      List(Ident(newTermName("eval$1")))),
    Apply(
      Select(Select(This(newTypeName("scala")), newTermName("Predef")), newTermName("print")),
      List(Literal(Constant("!"))))),
    Literal(Constant(())))

### Macros throwing unhandled exceptions

What happens if macro throws an unhandled exception? For example, let's crash the `printf` macro by providing invalid input.
As the printout shows, nothing dramatic happens. Compiler guards itself against misbehaving macros, prints relevant part of a stack trace, and reports an error.

    ~/Projects/Kepler/sandbox$ scala
    Welcome to Scala version 2.10.0-20120428-232041-e6d5d22d28 (Java HotSpot(TM) 64-Bit Server VM, Java 1.6.0_25).
    Type in expressions to have them evaluated.
    Type :help for more information.

    scala> import Macros._
    import Macros._

    scala> printf("hello %s!")
    <console>:11: error: exception during macro expansion:
    java.util.NoSuchElementException: head of empty list
            at scala.collection.immutable.Nil$.head(List.scala:318)
            at scala.collection.immutable.Nil$.head(List.scala:315)
            at scala.collection.mutable.Stack.pop(Stack.scala:140)
            at Macros$$anonfun$1.apply(Macros.scala:49)
            at Macros$$anonfun$1.apply(Macros.scala:47)
            at scala.collection.TraversableLike$$anonfun$map$1.apply(TraversableLike.scala:237)
            at scala.collection.TraversableLike$$anonfun$map$1.apply(TraversableLike.scala:237)
            at scala.collection.IndexedSeqOptimized$class.foreach(IndexedSeqOptimized.scala:34)
            at scala.collection.mutable.ArrayOps.foreach(ArrayOps.scala:39)
            at scala.collection.TraversableLike$class.map(TraversableLike.scala:237)
            at scala.collection.mutable.ArrayOps.map(ArrayOps.scala:39)
            at Macros$.printf_impl(Macros.scala:47)

                  printf("hello %s!")
                        ^

### Reporting warnings and errors

The canonical way to interact with the use is through the methods of `scala.reflect.macros.FrontEnds`.
`c.error` reports a compilation error, `c.info` issues a warning, `c.abort` reports an error and terminates
execution of a macro.

    scala> def impl(c: Context) =
      c.abort(c.enclosingPosition, "macro has reported an error")
    impl: (c: scala.reflect.macros.Context)Nothing

    scala> def test = macro impl
    defined term macro test: Any

    scala> test
    <console>:32: error: macro has reported an error
                  test
                  ^

### Writing bigger macros

When the code of a macro implementation grows big enough to warrant modularization beyond the body of the implementation method, it becomes apparent that one needs to carry around the context parameter, because most things of interest are path-dependent on the context.

One of the approaches is to write a class that takes a parameter of type `Context` and then split the macro implementation into a series of methods of that class. This is natural and simple, except that it's hard to get it right. Here's a typical compilation error.

    scala> class Helper(val c: Context) {
         | def generate: c.Tree = ???
         | }
    defined class Helper

    scala> def impl(c: Context): c.Expr[Unit] = {
         | val helper = new Helper(c)
         | c.Expr(helper.generate)
         | }
    <console>:32: error: type mismatch;
     found   : helper.c.Tree
        (which expands to)  helper.c.universe.Tree
     required: c.Tree
        (which expands to)  c.universe.Tree
           c.Expr(helper.generate)
                         ^

The problem in this snippet is in a path-dependent type mismatch. The Scala compiler does not understand that `c` in `impl` is the same object as `c` in `Helper`, even though the helper is constructed using the original `c`.

Luckily just a small nudge is all that is needed for the compiler to figure out what's going on. One of the possible ways of doing that is using refinement types (the example below is the simplest application of the idea; for example, one could also write an implicit conversion from `Context` to `Helper` to avoid explicit instantiations and simplify the calls).

    scala> abstract class Helper {
         | val c: Context
         | def generate: c.Tree = ???
         | }
    defined class Helper

    scala> def impl(c1: Context): c1.Expr[Unit] = {
         | val helper = new { val c: c1.type = c1 } with Helper
         | c1.Expr(helper.generate)
         | }
    impl: (c1: scala.reflect.macros.Context)c1.Expr[Unit]

An alternative approach is to pass the identity of the context in an explicit type parameter. Note how the constructor of `Helper` uses `c.type` to express the fact that `Helper.c` is the same as the original `c`. Scala's type inference can't figure this out on its own, so we need to help it.

    scala> class Helper[C <: Context](val c: C) {
         | def generate: c.Tree = ???
         | }
    defined class Helper

    scala> def impl(c: Context): c.Expr[Unit] = {
         | val helper = new Helper[c.type](c)
         | c.Expr(helper.generate)
         | }
    impl: (c: scala.reflect.macros.Context)c.Expr[Unit]

## More examples

Scala macros have quite a few early adopters. Thanks to active community involvement, we have been able to rapidly prototype,
and now there's some code that can be used as a reference. Read more about the state of the art at [http://scalamacros.org/news/2012/11/05/status-update.html](http://scalamacros.org/news/2012/11/05/status-update.html).

We also recommend a short tutorial on Scala macros written by Adam Warski: [http://www.warski.org/blog/2012/12/starting-with-scala-macros-a-short-tutorial](http://www.warski.org/blog/2012/12/starting-with-scala-macros-a-short-tutorial). It provides a step-by-step guide through writing a debug printing macro with explanations covering setup of an SBT project, usage of `reify` and `splice` and manually assembly of ASTs.