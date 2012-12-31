---
layout: overview
title: The Scala Actors Migration Guide
label-color: success
label-text: New in 2.10
---

**Vojin Jovanovic and Philipp Haller**

## Introduction

Starting with Scala 2.10.0, the Scala
[Actors](http://docs.scala-lang.org/overviews/core/actors.html)
library is deprecated. In Scala 2.10.0 the default actor library is
[Akka](http://akka.io).

To ease the migration from Scala Actors to Akka we are providing the
Actor Migration Kit (AMK). The AMK consists of an extension to Scala
Actors which is enabled by including the `scala-actors-migration.jar`
on a project's classpath. In addition, Akka 2.1 includes features,
such as the `ActorDSL` singleton, which enable a simpler conversion of
code using Scala Actors to Akka. The purpose of this document is to
guide users through the migration process and explain how to use the
AMK.

This guide has the following structure. In Section "Limitations of the
Migration Kit" we outline the main limitations of the migration
kit. In Section "Migration Overview" we describe the migration process
and talk about changes in the [Scala
distribution](http://www.scala-lang.org/downloads) that make the
migration possible. Finally, in Section "Step by Step Guide for
Migrating to Akka" we show individual steps, with working examples,
that are recommended when migrating from Scala Actors to Akka's
actors.

A disclaimer: concurrent code is notorious for bugs that are hard to
debug and fix.  Due to differences between the two actor
implementations it is possible that errors appear. It is recommended
to thoroughly test the code after each step of the migration process.

## Limitations of the Migration Kit

Due to differences in Akka and Scala actor models the complete functionality can not be migrated smoothly. The following list explains parts of the behavior that are hard to migrate:

1. Relying on termination reason and bidirectional behavior with `link` method - Scala and Akka actors have different fault-handling and actor monitoring models.
In Scala linked actors terminate if one of the linked parties terminates abnormally. If termination is tracked explicitly (by `self.trapExit`) the actor receives
the termination reason from the failed actor. This functionality can not be migrated to Akka with the AMK. The AMK allows migration only for the
[Akka monitoring](http://doc.akka.io/docs/akka/2.1.0-RC1/general/supervision.html#What_Lifecycle_Monitoring_Means)
mechanism. Monitoring is different than linking because it is unidirectional and the termination reason is now known. If monitoring support is not enough, the migration
of `link` must be postponed until the last possible moment (Step 5 of migration).
Then, when moving to Akka, users must create an [supervision hierarchy](http://doc.akka.io/docs/akka/2.1.0-RC1/general/supervision.html) that will handle faults.

2. Usage of the `restart` method - Akka does not provide explicit restart of actors so we can not provide the smooth migration for this use-case.
The user must change the system so there are no usages of the `restart` method.

3. Usage of method `getState` - Akka actors do not have explicit state so this functionality can not be migrated. The user code must not
have `getState` invocations.

4. Not starting actors right after instantiation - Akka actors are automatically started when instantiated. Users will have to
reshape their system so it starts all the actors right after their instantiation.

5. Method `mailboxSize` does not exist in Akka and therefore can not be migrated. This method is seldom used and can easily be removed.


## Migration Overview

### Migration Kit
In Scala 2.10.0 actors reside inside the [Scala distribution](http://www.scala-lang.org/downloads) as a separate jar ( *scala-actors.jar* ), and 
the their interface is deprecated. The distribution also includes Akka actors in the *akka-actor.jar*. 
The AMK resides both in the Scala actors and in the *akka-actor.jar*. Future major releases of Scala will not contain Scala actors and the AMK.

To start the migration user needs to add the *scala-actors.jar* and the *scala-actors-migration.jar* to the build of their projects.
Addition of *scala-actors.jar* and *scala-actors-migration.jar* enables the usage of the AMK described below.
 These artifacts reside in the [Scala Tools](https://oss.sonatype.org/content/groups/scala-tools/org/scala-lang/)
 repository and in the [Scala distribution](http://www.scala-lang.org/downloads).

### Step by Step Migration
Actor Migration Kit should be used in 5 steps. Each step is designed to introduce minimal changes
to the code base and allows users to run all system tests after it. In the first four steps of the migration
the code will use the Scala actors implementation. However, the methods and class signatures will be transformed to closely resemble Akka.
The migration kit on the Scala side introduces a new actor type (`ActWithStash`) and enforces access to actors through the `ActorRef` interface.

It also enforces creation of actors through special methods on the `ActorDSL` object. In these steps it will be possible to migrate one
actor at a time. This reduces the possibility of complex errors that are caused by several bugs introduced at the same time.

After the migration on the Scala side is complete the user should change import statements and change
the library used to Akka. On the Akka side, the `ActorDSL` and the `ActWithStash` allow
 modeling the `react` construct of Scala Actors and their life cycle. This step migrates all actors to the Akka back-end and could introduce bugs in the system. Once code is migrated to Akka, users will be able to use all the features of Akka.

## Step by Step Guide for Migrating to Akka

In this chapter we will go through 5 steps of the actor migration. After each step the code can be tested for possible errors. In the first 4
 steps one can migrate one actor at a time and test the functionality. However, the last step migrates all actors to Akka and it can be tested
only as a whole. After this step the system should have the same functionality as before, however it will use the Akka actor library.

### Step 1 - Everything as an Actor
The Scala actors library provides public access to multiple types of actors. They are organized in the class hierarchy and each subclass
provides slightly richer functionality. To make further steps of the migration easier we will first change each actor in the system to be of type `Actor`.
This migration step is straightforward since the `Actor` class is located at the bottom of the hierarchy and provides the broadest functionality.

The Actors from the Scala library should be migrated according to the following rules:

1. `class MyServ extends Reactor[T]` -> `class MyServ extends Actor`

    Note that `Reactor` provides an additional type parameter which represents the type of the messages received. If user code uses
that information then one needs to: _i)_ apply pattern matching with explicit type, or _ii)_ do the downcast of a message from
`Any` to the type `T`.

2. `class MyServ extends ReplyReactor` -> `class MyServ extends Actor`

3. `class MyServ extends DaemonActor` -> `class MyServ extends Actor`

    To pair the functionality of the `DaemonActor` add the following line to the class definition.

        override def scheduler: IScheduler = DaemonScheduler

### Step 2 - Instantiations

In Akka, actors can be accessed only through the narrow interface called `ActorRef`. Instances of `ActorRef` can be acquired either
by invoking an `actor` method on the `ActorDSL` object or through the `actorOf` method on an instance of an `ActorRefFactory`.
In the Scala side of AMK we provide a subset of the Akka `ActorRef` and the `ActorDSL` which is the actual singleton object in the Akka library.

This step of the migration makes all accesses to actors through `ActorRef`s. First, we show how to migrate common patterns for instantiating
Scala `Actor`s. Then we show how to overcome issues with the different interfaces of `ActorRef` and `Actor`, respectively.

#### Actor Instantiation

The translation rules for actor instantiation (the following rules require importing `scala.actors.migration._`):

1. Constructor Call Instantiation

        val myActor = new MyActor(arg1, arg2)
        myActor.start()

    should be replaced with

        ActorDSL.actor(new MyActor(arg1, arg2))

2. DSL for Creating Actors

        val myActor = actor {
          // actor definition
        }

    should be replaced with

        val myActor = ActorDSL.actor(new Actor {
           def act() {
             // actor definition
           }
        })

3. Object Extended from the `Actor` Trait

        object MyActor extends Actor {
          // MyActor definition
        }
        MyActor.start()

    should be replaced with

        class MyActor extends Actor {
          // MyActor definition
        }

        object MyActor {
          val ref = ActorDSL.actor(new MyActor)
        }

   All accesses to the object `MyActor` should be replaced with accesses to `MyActor.ref`.

Note that Akka actors are always started on instantiation. In case actors in the migrated
 system are created and started at different locations, and changing this can affect the behavior of the system,
users need to change the code so actors are started right after instantiation.

Remote actors also need to be fetched as `ActorRef`s. To get an `ActorRef` of an remote actor use the method `selectActorRef`.

#### Different Method Signatures

At this point we have changed all the actor instantiations to return `ActorRef`s, however, we are not done yet.
There are differences in the interface of `ActorRef`s and `Actor`s so we need to change the methods invoked on each migrated instance.
Unfortunately, some of the methods that Scala `Actor`s provide can not be migrated. For the following methods users need to find a workaround:

1. `getState()` - actors in Akka are managed by their supervising actors and are restarted by default.
In that scenario state of an actor is not relevant.

2. `restart()` - explicitly restarts a Scala actor. There is no corresponding functionality in Akka.

All other `Actor` methods need to be translated to two methods that exist on the ActorRef. The translation is achieved by the rules described below.
Note that all the rules require the following imports:

    import scala.concurrent.duration._
    import scala.actors.migration.pattern.ask
    import scala.actors.migration._
    import scala.concurrent._

Additionally rules 1-3 require an implicit `Timeout` with infinite duration defined in the scope. However, since Akka does not allow for infinite timeouts, we will use
100 years. For example:

    implicit val timeout = Timeout(36500 days)

Rules:

1. `!!(msg: Any): Future[Any]` gets replaced with `?`. This rule will change a return type to the `scala.concurrent.Future` which might not type check.
Since `scala.concurrent.Future` has broader functionality than the previously returned one, this type error can be easily fixed with local changes:

        actor !! message -> respActor ? message

2. `!![A] (msg: Any, handler: PartialFunction[Any, A]): Future[A]` gets replaced with `?`. The handler can be extracted as a separate
function and then applied to the generated future result. The result of a handle should yield another future like
in the following example:

        val handler: PartialFunction[Any, T] =  ... // handler
        actor !! (message, handler) -> (respActor ? message) map handler

3. `!? (msg: Any): Any` gets replaced with `?` and explicit blocking on the returned future:

        actor !? message ->
          Await.result(respActor ? message, Duration.Inf)

4. `!? (msec: Long, msg: Any): Option[Any]` gets replaced with `?` and explicit blocking on the future:

        actor !? (dur, message) ->
          val res = respActor.?(message)(Timeout(dur milliseconds))
          val optFut = res map (Some(_)) recover { case _ => None }
          Await.result(optFut, Duration.Inf)

Public methods that are not mentioned here are declared public for purposes of the actors DSL. They can be used only
inside the actor definition so their migration is not relevant in this step.

### Step 3 - `Actor`s become `ActWithStash`s

At this point all actors inherit the `Actor` trait, we instantiate actors through special factory methods,
and all actors are accessed through the `ActorRef` interface.
Now we need to change all actors to the `ActWithStash` class from the AMK. This class behaves exactly the same like Scala `Actor`
but, additionally, provides methods that correspond to methods in Akka's `Actor` trait. This allows easy, step by step, migration to the Akka behavior.

To achieve this all classes that extend `Actor` should extend the `ActWithStash`. Apply the
following rule:

    class MyActor extends Actor -> class MyActor extends ActWithStash

After this change code might not compile. The `receive` method exists in `ActWithStash` and can not be used in the body of the `act` as is. To redirect the compiler to the previous method
add the type parameter to all `receive` calls in your system. For example:

    receive { case x: Int => "Number" } ->
      receive[String] { case x: Int => "Number" }

Additionally, to make the code compile, users must add the `override` keyword before the `act` method, and to create
the empty `receive` method in the code. Method `act` needs to be overriden since its implementation in `ActWithStash`
mimics the message processing loop of Akka. The changes are shown in the following example:

    class MyActor extends ActWithStash {

       // dummy receive method (not used for now)
       def receive = {case _ => }

       override def act() {
         // old code with methods receive changed to react.
       }
    }


`ActWithStash` instances have variable `trapExit` set to `true` by default. If that is not desired set it to `false` in the initializer of the class.

The remote actors will not work with `ActWithStash` out of the box. The method `register('name, this)` needs to be replaced with:

    registerActorRef('name, self)

In later steps of the migration, calls to `registerActorRef` and `alive` should be treated like any other calls.

After this point user can run the test suite and the whole system should behave as before. The `ActWithStash` and `Actor` use the same infrastructure so the system
should behave exactly the same.

### Step 4 - Removing the `act` Method

In this section we describe how to remove the `act` method from `ActWithStash`s and how to
change the methods used in the `ActWithStash` to resemble Akka. Since this step can be complex, it is recommended
to do changes one actor at a time. In Scala, an actor's behavior is defined by implementing the `act` method. Logically, an actor is a concurrent process
which executes the body of its `act` method, and then terminates. In Akka, the behavior is defined by using a global message
handler which processes the messages in the actor's mailbox one by one. The message handler is a partial function, returned by the `receive` method,
which gets applied to each message.

Since the behavior of Akka methods in the `ActWithStash` depends on the removal of the `act` method we have to do that first. Then we will give the translation
rules for translating individual methods of the `scala.actors.Actor` trait.

#### Removal of `act`

In the following list we present the translation rules for common message processing patterns. This list is not
exhaustive and it covers only some common patterns. However, users can migrate more complex `act` methods to Akka by looking
 at existing translation rules and extending them for more complex situations.

A note about nested `react`/`reactWithin` calls: the message handling
partial function needs to be expanded with additional constructs that
bring it closer to the Akka model. Although these changes can be
complicated, migration is possible for an arbitrary level of
nesting. See below for examples.

A note about using `receive`/`receiveWithin` with complex control
flow: migration can be complicated since it requires refactoring the
`act` method. A `receive` call can be modeled using `react` and
`andThen` on the message processing partial function. Again, simple
examples are shown below.

1. If there is any code in the `act` method that is being executed before the first `loop` with `react` that code
should be moved to the `preStart` method.

        def act() {
          // initialization code here
          loop {
            react { ... }
          }
        }

   should be replaced with

        override def preStart() {
          // initialization code here
        }

        def act() {
          loop {
            react{ ... }
          }
        }

   This rule should be used in other patterns as well if there is code before the first react.

2. When `act` is in the form of a simple `loop` with a nested `react` use the following pattern.

        def act() = {
          loop {
            react {
              // body
            }
          }
        }

   should be replaced with

        def receive = {
          // body
        }

3. When `act` contains a `loopWhile` construct use the following translation.

        def act() = {
          loopWhile(c) {
            react {
              case x: Int =>
                // do task
                if (x == 42) {
                  c = false
                }
            }
          }
        }

   should be replaced with

        def receive = {
          case x: Int =>
            // do task
            if (x == 42) {
              context.stop(self)
            }
        }

4. When `act` contains nested `react`s use the following rule:

        def act() = {
          var c = true
          loopWhile(c) {
          react {
            case x: Int =>
              // do task
              if (x == 42) {
                c = false
              } else {
                react {
                  case y: String =>
                    // do nested task
                }
              }
            }
          }
        }

   should be replaced with

        def receive = {
          case x: Int =>
            // do task
            if (x == 42) {
              context.stop(self)
            } else {
              context.become(({
                case y: String =>
                // do nested task
              }: Receive).andThen(x => {
                unstashAll()
                context.unbecome()
             }).orElse { case x => stash(x) })
            }
        }

5. For `reactWithin` method use the following translation rule:

        loop {
          reactWithin(t) {
            case TIMEOUT => // timeout processing code
            case msg => // message processing code
          }
        }

   should be replaced with

        import scala.concurrent.duration._

        context.setReceiveTimeout(t millisecond)
        def receive = {
          case ReceiveTimeout => // timeout processing code
          case msg => // message processing code
        }

6. Exception handling is done in a different way in Akka. To mimic Scala actors behavior apply the following rule

        def act() = {
          loop {
            react {
              case msg =>
              // work that can fail
            }
          }
        }

        override def exceptionHandler = {
          case x: Exception => println("got exception")
        }

   should be replaced with

        def receive = PFCatch({
          case msg =>
            // work that can fail
        }, { case x: Exception => println("got exception") })

   where `PFCatch` is defined as

        class PFCatch(f: PartialFunction[Any, Unit],
          handler: PartialFunction[Exception, Unit])
          extends PartialFunction[Any, Unit] {

          def apply(x: Any) = {
            try {
              f(x)
            } catch {
              case e: Exception if handler.isDefinedAt(e) =>
                handler(e)
            }
          }

          def isDefinedAt(x: Any) = f.isDefinedAt(x)
        }

        object PFCatch {
          def apply(f: PartialFunction[Any, Unit],
            handler: PartialFunction[Exception, Unit]) =
              new PFCatch(f, handler)
        }

   `PFCatch` is not included in the AMK as it can stay as the permanent feature in the migrated code
   and the AMK will be removed with the next major release. Once the whole migration is complete fault-handling
    can also be converted to the Akka [supervision](http://doc.akka.io/docs/akka/2.1.0-RC1/general/supervision.html#What_Supervision_Means).



#### Changing `Actor` Methods

After we have removed the `act` method we should rename the methods that do not exist in Akka but have similar functionality. In the following list we present
the list of differences and their translation:

1. `exit()`/`exit(reason)` - should be replaced with `context.stop(self)`

2. `receiver` - should be replaced with `self`

3. `reply(msg)` - should be replaced with `sender ! msg`

4. `link(actor)` - In Akka, linking of actors is done partially by [supervision](http://doc.akka.io/docs/akka/2.1.0-RC1/general/supervision.html#What_Supervision_Means)
and partially by [actor monitoring](http://doc.akka.io/docs/akka/2.1.0-RC1/general/supervision.html#What_Lifecycle_Monitoring_Means). In the AMK we support
only the monitoring method so the complete Scala functionality can not be migrated.

   The difference between linking and watching is that watching actors always receive the termination notification.
However, instead of matching on the Scala `Exit` message that contains the reason of termination the Akka watching
returns the `Terminated(a: ActorRef)` message that contains only the `ActorRef`. The functionality of getting the reason
 for termination is not supported by the migration. It can be done in Akka, after the Step 4, by organizing the actors in a [supervision hierarchy](http://doc.akka.io/docs/akka/2.1.0-RC1/general/supervision.html).

   If the actor that is watching does not match the `Terminated` message, and this message arrives, it will be terminated with the `DeathPactException`.
Note that this will happen even when the watched actor terminated normally. In Scala linked actors terminate, with the same termination reason, only if
one of the actors terminates abnormally.

   If the system can not be migrated solely with `watch` the user should leave invocations to `link` and `exit(reason)` as is. However since `act()` overrides the `Exit` message the following transformation
needs to be applied:

        case Exit(actor, reason) =>
          println("sorry about your " + reason)
          ...

    should be replaced with

        case t @ Terminated(actorRef) =>
          println("sorry about your " + t.reason)
          ...

   NOTE: There is another subtle difference between Scala and Akka actors. In Scala, `link`/`watch` to the already dead actor will not have affect.
In Akka, watching the already dead actor will result in sending the `Terminated` message. This can give unexpected behavior in the Step 5 of the migration guide.

### Step 5 - Moving to the Akka Back-end

At this point user code is ready to operate on Akka actors. Now we can switch the actors library from Scala to
Akka actors. In order to do this configure the build to exclude the `scala-actors.jar` and the `scala-actors-migration.jar`
 and add the *akka-actor.jar*. The AMK is built to work only with Akka actors version 2.1 which are included in the [Scala distribution](http://www.scala-lang.org/downloads)
  and can be configured by these [instructions](http://doc.akka.io/docs/akka/2.1.0-RC1/intro/getting-started.html#Using_a_build_tool). During
  the RC phase the Akka RC number should match the Scala one (e.g. Scala 2.10.0-RC1 runs with Akka 2.1-RC1).

After this change the compilation will fail due to different package names and slight differences in the API. We will have to change each imported actor
from scala to Akka. Following is the non-exhaustive list of package names that need to be changed:

    scala.actors._ -> akka.actor._
    scala.actors.migration.pattern.ask -> akka.pattern.ask
    scala.actors.migration.Timeout -> akka.util.Timeout

Also, method declarations `def receive =` in `ActWithStash` should be prepended with `override`.

In Scala actors the `stash` method needs a message as a parameter. For example:

    def receive = {
      ...
      case x => stash(x)
    }

In Akka only the currently processed message can be stashed. Therefore replace the above example with:

    def receive = {
      ...
      case x => stash()
    }

#### Adding Actor Systems

The Akka actors are organized in [Actor systems](http://doc.akka.io/docs/akka/2.1.0-RC1/general/actor-systems.html). Each actor that is instantiated
must belong to one `ActorSystem`. To achieve this add an `ActorSystem` instance to each actor instatiation call as a first argument. The following example
shows the transformation.

To achieve this transformation you need to have an actor system instantiated. For example:

    val system = ActorSystem("migration-system")

Then apply the following transformation:

    ActorDSL.actor(...) -> ActorDSL.actor(system)(...)

If many calls to `actor` use the same `ActorSystem` it can be passed as an implicit parameter. For example:

    ActorDSL.actor(...) ->
      import project.implicitActorSystem
      ActorDSL.actor(...)

Finally, Scala programs are terminating when all the non-daemon threads and actors finish. With Akka the program ends when all the non-daemon threads finish and all actor systems are shut down.
 Actor systems need to be explicitly terminated before the program can exit. This is achieved by invoking the `shutdown` method on an Actor system.

#### Remote Actors

Once the code base is moved to Akka remoting will not work any more. The methods methods `registerActorFor` and `alive` need to be removed. In Akka, remoting is done solely by configuration and
for further details refer to the [Akka remoting documentation](http://doc.akka.io/docs/akka/2.1-RC1/scala/remoting.html).

#### Examples and Issues
All of the code snippets presented in this document can be found in the [Actors Migration test suite](http://github.com/scala/actors-migration/tree/master/src/test/) as test files with the prefix `actmig`.

This document and the Actor Migration Kit were designed and implemented by: [Vojin Jovanovic](http://people.epfl.ch/vojin.jovanovic) and [Philipp Haller](http://lampwww.epfl.ch/~phaller/)

If you find any issues or rough edges please report them at the [Scala Bugtracker](https://scala.github.com/actors-migration/issues).
During the RC release phase bugs will be fixed within several working days thus that would be the best time to try the AMK on an application.
