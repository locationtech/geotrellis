package trellis.process

import java.io.File
import scala.util.matching.Regex

import trellis.data._
import trellis.data.FileExtensionRegexes._
import trellis.RasterExtent
import trellis.operation._
import trellis.IntRaster

// akka imports
import akka.actor._
import akka.routing._
import akka.dispatch.Await
import akka.util.duration._
import akka.util.Timeout

class Context (server:Server) {
  val timer = new Timer()

  def run[T](op:Operation[T])(implicit m:Manifest[T]):T = {
    server._run(op)(m, timer).value
  }

  def getResult[T](op:Operation[T])(implicit m:Manifest[T]):Complete[T] = {
    server._run(op)(m, timer)
  }

  def getRaster(path:String, layer:RasterLayer, re:RasterExtent):IntRaster = {
    server.getRaster(path, layer, re)
  }

  def loadRaster(path:String, g:RasterExtent):IntRaster = {
    server.getRaster(path, null, g)
  }
}

import com.typesafe.config.ConfigFactory

class Server (id:String, val catalog:Catalog) extends FileCaching {
  val debug = false

    val customConf2 = ConfigFactory.parseString("""
      akka {
        version = "2.0-M2"
        logConfigOnStart = off
        loglevel = "INFO"
        stdout-loglevel = "INFO"
        event-handlers = ["akka.event.Logging$DefaultLogger"]
        remote {
          client {
            "message-frame-size": "100 MiB"
          },
          server {
            "message-frame-size": "100 MiB"
          }
        }
        default-dispatcher {
          core-pool-size-factor = 80.0
        }
        actor {
          debug {
            receive = off
            autoreceive = off
            lifecycle = off
          }
          deployment {
            /routey {
              nr-of-instances = 300
            }
          }
        }
      }
  
      """)
  val customConf = ConfigFactory.parseString("""
{
    # system properties
    "os" : {
        # system properties
        "arch" : "amd64",
        # system properties
        "name" : "Linux",
        # system properties
        "version" : "3.0.0-12-generic"
    },
    # system properties
    "file" : {
        # system properties
        "encoding" : {
            # system properties
            "pkg" : "sun.io"
        },
        # system properties
        "separator" : "/"
    },
    # system properties
    "path" : {
        # system properties
        "separator" : ":"
    },
    # system properties
    "line" : {
        # system properties
        "separator" : "\n"
    },
    # system properties
    "java" : {
        # system properties
        "vm" : {
            # system properties
            "vendor" : "Sun Microsystems Inc.",
            # system properties
            "name" : "Java HotSpot(TM) 64-Bit Server VM",
            # system properties
            "specification" : {
                # system properties
                "vendor" : "Sun Microsystems Inc.",
                # system properties
                "name" : "Java Virtual Machine Specification",
                # system properties
                "version" : "1.0"
            },
            # system properties
            "version" : "20.1-b02",
            # system properties
            "info" : "mixed mode"
        },
        # system properties
        "home" : "/usr/lib/jvm/java-6-sun-1.6.0.26/jre",
        # system properties
        "awt" : {
            # system properties
            "graphicsenv" : "sun.awt.X11GraphicsEnvironment",
            # system properties
            "printerjob" : "sun.print.PSPrinterJob"
        },
        # system properties
        "vendor" : {
            # system properties
            "url" : {
                # system properties
                "bug" : "http://java.sun.com/cgi-bin/bugreport.cgi"
            }
        },
        # system properties
        "endorsed" : {
            # system properties
            "dirs" : "/usr/lib/jvm/java-6-sun-1.6.0.26/jre/lib/endorsed"
        },
        # system properties
        "library" : {
            # system properties
            "path" : "/usr/lib/jvm/java-6-sun-1.6.0.26/jre/lib/amd64/server:/usr/lib/jvm/java-6-sun-1.6.0.26/jre/lib/amd64:/usr/lib/jvm/java-6-sun-1.6.0.26/jre/../lib/amd64:/usr/java/packages/lib/amd64:/usr/lib64:/lib64:/lib:/usr/lib"
        },
        # system properties
        "specification" : {
            # system properties
            "vendor" : "Sun Microsystems Inc.",
            # system properties
            "name" : "Java Platform API Specification",
            # system properties
            "version" : "1.6"
        },
        # system properties
        "class" : {
            # system properties
            "path" : "/home/jmarcus/bin/sbt-launch.jar",
            # system properties
            "version" : "50.0"
        },
        # system properties
        "runtime" : {
            # system properties
            "name" : "Java(TM) SE Runtime Environment",
            # system properties
            "version" : "1.6.0_26-b03"
        },
        # system properties
        "ext" : {
            # system properties
            "dirs" : "/usr/lib/jvm/java-6-sun-1.6.0.26/jre/lib/ext:/usr/java/packages/lib/ext"
        },
        # system properties
        "io" : {
            # system properties
            "tmpdir" : "/tmp"
        },
        # system properties
        "version" : "1.6.0_26"
    },
    # system properties
    "user" : {
        # system properties
        "home" : "/home/jmarcus",
        # system properties
        "timezone" : "America/New_York",
        # system properties
        "dir" : "/home/jmarcus/projects/github/trellis",
        # system properties
        "name" : "jmarcus",
        # system properties
        "language" : "en",
        # system properties
        "country" : "US"
    },
    # system properties
    "sun" : {
        # system properties
        "os" : {
            # system properties
            "patch" : {
                # system properties
                "level" : "unknown"
            }
        },
        # system properties
        "boot" : {
            # system properties
            "library" : {
                # system properties
                "path" : "/usr/lib/jvm/java-6-sun-1.6.0.26/jre/lib/amd64"
            },
            # system properties
            "class" : {
                # system properties
                "path" : "/usr/lib/jvm/java-6-sun-1.6.0.26/jre/lib/resources.jar:/usr/lib/jvm/java-6-sun-1.6.0.26/jre/lib/rt.jar:/usr/lib/jvm/java-6-sun-1.6.0.26/jre/lib/sunrsasign.jar:/usr/lib/jvm/java-6-sun-1.6.0.26/jre/lib/jsse.jar:/usr/lib/jvm/java-6-sun-1.6.0.26/jre/lib/jce.jar:/usr/lib/jvm/java-6-sun-1.6.0.26/jre/lib/charsets.jar:/usr/lib/jvm/java-6-sun-1.6.0.26/jre/lib/modules/jdk.boot.jar:/usr/lib/jvm/java-6-sun-1.6.0.26/jre/classes"
            }
        },
        # system properties
        "arch" : {
            # system properties
            "data" : {
                # system properties
                "model" : "64"
            }
        },
        # system properties
        "jnu" : {
            # system properties
            "encoding" : "UTF-8"
        },
        # system properties
        "cpu" : {
            # system properties
            "endian" : "little",
            # system properties
            "isalist" : ""
        },
        # system properties
        "desktop" : "gnome",
        # system properties
        "java" : {
            # system properties
            "launcher" : "SUN_STANDARD",
            # system properties
            "command" : "/home/jmarcus/bin/sbt-launch.jar"
        },
        # system properties
        "management" : {
            # system properties
            "compiler" : "HotSpot 64-Bit Tiered Compilers"
        },
        # system properties
        "io" : {
            # system properties
            "unicode" : {
                # system properties
                "encoding" : "UnicodeLittle"
            }
        }
    },
    # reference.conf: 8
    "akka" : {
        # reference.conf: 13
        #  Home directory of Akka, modules in the deploy directory will be loaded
        "home" : "",
        # reference.conf: 21
        #  Log level used by the configured loggers (see "event-handlers") as soon
        #  as they have been started; before that, see "stdout-loglevel"
        #  Options: ERROR, WARNING, INFO, DEBUG
        "loglevel" : "INFO",
        # reference.conf: 218
        #  Used to set the behavior of the scheduler.
        #  Changing the default values may change the system behavior drastically so make sure
        #  you know what you're doing!
        "scheduler" : {
            # reference.conf: 227
            #  The HashedWheelTimer (HWT) implementation from Netty is used as the default scheduler
            #  in the system.
            #  HWT does not execute the scheduled tasks on exact time.
            #  It will, on every tick, check if there are any tasks behind the schedule and execute them.
            #  You can increase or decrease the accuracy of the execution timing by specifying smaller
            #  or larger tick duration.
            #  If you are scheduling a lot of tasks you should consider increasing the ticks per wheel.
            #  For more information see: http://www.jboss.org/netty/
            "tickDuration" : "100ms",
            # reference.conf: 228
            "ticksPerWheel" : 512
        },
        # reference.conf: 25
        #  Log level for the very basic logger activated during AkkaApplication startup
        #  Options: ERROR, WARNING, INFO, DEBUG
        "stdout-loglevel" : "WARNING",
        # reference.conf: 29
        #  Log the complete configuration at INFO level when the actor system is started.
        #  This is useful when you are uncertain of what configuration is used.
        "logConfigOnStart" : "off",
        # reference.conf: 149
        "cluster" : {
            # reference.conf: 150
            "name" : "default-cluster",
            # reference.conf: 152
            "seed-nodes" : [],
            # reference.conf: 151
            "nodename" : "default"
        },
        # reference.conf: 16
        #  Event handlers to register at boot time (Logging$DefaultLogger logs to STDOUT)
        "event-handlers" : [
            # reference.conf: 16
            "akka.event.Logging$DefaultLogger"
        ],
        # reference.conf: 10-36
        "actor" : {
            # reference.conf: 198
            #  Entries for pluggable serializers and their bindings. If a binding for a specific
            #  class is not found, then the default serializer (Java serialization) is used.
            "serializers" : {
                # reference.conf: 202
                "default" : "akka.serialization.JavaSerializer"
            },
            # reference.conf: 41
            #  Timeout for ActorSystem.actorOf
            "creation-timeout" : "20s",
            # reference.conf: 59
            #  Serializes and deserializes creators (in Props) to ensure that they can be sent over the network,
            #  this is only intended for testing.
            "serialize-creators" : "off",
            # reference.conf: 103
            "default-dispatcher" : {
                # reference.conf: 116
                #  Toggles whether the threads created by this dispatcher should be daemons or not
                "daemonic" : "off",
                # reference.conf: 119
                #  Keep alive time for threads
                "keep-alive-time" : "60s",
                # reference.conf: 170
                #  Specifies the timeout to add a new message to a mailbox that is full -
                #  negative number means infinite timeout
                "mailbox-push-timeout-time" : "10s",
                # reference.conf: 132
                #  Hint: max-pool-size is only used for bounded task queues
                #  minimum number of threads to cap factor-based max number to
                "max-pool-size-min" : 8,
                # reference.conf: 175
                #  FQCN of the MailboxType, if not specified the default bounded or unbounded
                #  mailbox is used. The Class of the FQCN must have a constructor with a
                #  com.typesafe.config.Config parameter.
                "mailboxType" : "",
                # reference.conf: 151
                #  How long time the dispatcher will wait for new actors until it shuts down
                "shutdown-timeout" : "1s",
                # reference.conf: 110
                #  Must be one of the following
                #  Dispatcher, (BalancingDispatcher, only valid when all actors using it are of
                #  the same type), PinnedDispatcher, or a FQCN to a class inheriting
                #  MessageDispatcherConfigurator with a constructor with
                #  com.typesafe.config.Config parameter and akka.dispatch.DispatcherPrerequisites
                #  parameters
                "type" : "Dispatcher",
                # reference.conf: 155
                #  Throughput defines the number of messages that are processed in a batch
                #  before the thread is returned to the pool. Set to 1 for as fair as possible.
                "throughput" : 5,
                # reference.conf: 141
                #  Specifies the bounded capacity of the task queue (< 1 == unbounded)
                "task-queue-size" : -1,
                # reference.conf: 158
                #  Throughput deadline for Dispatcher, set to 0 or negative for no deadline
                "throughput-deadline-time" : "0ms",
                # reference.conf: 166
                #  If negative (or zero) then an unbounded mailbox is used (default)
                #  If positive then a bounded mailbox is used and the capacity is set using the
                #  property
                #  NOTE: setting a mailbox to 'blocking' can be a bit dangerous, could lead to
                #  deadlock, use with care
                #  The following are only used for Dispatcher and only if mailbox-capacity > 0
                "mailbox-capacity" : -1,
                # reference.conf: 113
                #  Name used in log messages and thread names.
                "name" : "default-dispatcher",
                # reference.conf: 125
                #  No of core threads ... ceil(available processors * factor)
                "core-pool-size-factor" : 8,
                # reference.conf: 138
                #  maximum number of threads to cap factor-based max number to
                "max-pool-size-max" : 4096,
                # reference.conf: 128
                #  maximum number of threads to cap factor-based number to
                "core-pool-size-max" : 4096,
                # reference.conf: 122
                #  minimum number of threads to cap factor-based core number to
                "core-pool-size-min" : 8,
                # reference.conf: 135
                #  Max no of threads ... ceil(available processors * factor)
                "max-pool-size-factor" : 8,
                # reference.conf: 145
                #  Specifies which type of task queue will be used, can be "array" or
                #  "linked" (default)
                "task-queue-type" : "linked",
                # reference.conf: 148
                #  Allow core threads to time out
                "allow-core-timeout" : "on"
            },
            # reference.conf: 12-61
            "deployment" : {
                # reference.conf: 14-64
                #  deployment id pattern - on the format: /parent/child etc.
                "default" : {
                    # reference.conf: 83
                    #  number of children to create in case of a non-direct router; this setting
                    #  is ignored if routees.paths is given
                    "nr-of-instances" : 1,
                    # reference.conf: 79
                    #  routing (load-balance) scheme to use
                    #      available: "from-code", "round-robin", "random", "scatter-gather", "broadcast"
                    #      or:        fully qualified class name of the router class
                    #      default is "from-code";
                    #  Whether or not an actor is transformed to a Router is decided in code only (Props.withRouter).
                    #  The type of router can be overridden in the configuration; specifying "from-code" means
                    #  that the values specified in the code shall be used.
                    #  In case of routing, the actors to be routed to can be specified
                    #  in several ways:
                    #  - nr-of-instances: will create that many children given the actor factory
                    #    supplied in the source code (overridable using create-as below)
                    #  - routees.paths: will look the paths up using actorFor and route to
                    #    them, i.e. will not create children
                    "router" : "from-code",
                    # reference.conf: 89
                    #  FIXME document 'create-as', ticket 1511
                    "create-as" : {
                        # reference.conf: 91
                        #  fully qualified class name of recipe implementation
                        "class" : ""
                    },
                    # reference.conf: 86
                    #  within is the timeout used for routers containing future calls
                    "within" : "5 seconds",
                    # reference.conf: 18
                    #  if this is set to a valid remote address, the named actor will be deployed
                    #  at that node e.g. "akka://sys@host:port"
                    "remote" : "",
                    # reference.conf: 20-94
                    "routees" : {
                        # reference.conf: 98
                        #  Alternatively to giving nr-of-instances you can specify the full
                        #  paths of those actors which should be routed to. This setting takes
                        #  precedence over nr-of-instances
                        "paths" : [],
                        # reference.conf: 32
                        #  A list of hostnames and ports for instantiating the children of a
                        #  non-direct router
                        #    The format should be on "akka://sys@host:port", where:
                        #     - sys is the remote actor system name
                        #     - hostname can be either hostname or IP address the remote actor
                        #       should connect to
                        #     - port should be the port for the remote server on the other node
                        #  The number of actor instances to be spawned is still taken from the
                        #  nr-of-instances setting as for local routers; the instances will be
                        #  distributed round-robin among the given nodes.
                        "nodes" : []
                    }
                }
            },
            # reference.conf: 38
            "provider" : "akka.actor.LocalActorRefProvider",
            # reference.conf: 178
            "debug" : {
                # reference.conf: 193
                #  enable DEBUG logging of subscription changes on the eventStream
                "event-stream" : "off",
                # reference.conf: 184
                #  enable DEBUG logging of all AutoReceiveMessages (Kill, PoisonPill and the like)
                "autoreceive" : "off",
                # reference.conf: 187
                #  enable DEBUG logging of actor lifecycle changes
                "lifecycle" : "off",
                # reference.conf: 190
                #  enable DEBUG logging of all LoggingFSMs for events, transitions and timers
                "fsm" : "off",
                # reference.conf: 181
                #  enable function of Actor.loggable(), which is to log any received message at
                #  DEBUG level
                "receive" : "off"
            },
            # reference.conf: 51
            #  Default timeout for Future based invocations
            #     - Actor:        ask && ?
            #     - UntypedActor: ask
            #     - TypedActor:   methods with non-void return type
            "timeout" : "5s",
            # reference.conf: 45
            #  frequency with which stopping actors are prodded in case they had to be
            #  removed from their parents
            "reaper-interval" : "5s",
            # reference.conf: 55
            #  Serializes and deserializes (non-primitive) messages to ensure immutability,
            #  this is only intended for testing.
            "serialize-messages" : "off"
        },
        # reference.conf: 39
        "remote" : {
            # reference.conf: 63
            #  accrual failure detection config
            "failure-detector" : {
                # reference.conf: 72
                "max-sample-size" : 1000,
                # reference.conf: 70
                #  defines the failure detector threshold
                #      A low threshold is prone to generate many wrong suspicions but ensures
                #      a quick detection in the event of a real crash. Conversely, a high
                #      threshold generates fewer mistakes but needs more time to detect
                #      actual crashes
                "threshold" : 8
            },
            # reference.conf: 48
            #  In case of increased latency / overflow how long
            #  should we wait (blocking the sender) until we deem the send to be cancelled?
            #  0 means "never backoff", any positive number will indicate time to block at most.
            "backoff-timeout" : "0ms",
            # reference.conf: 75
            "gossip" : {
                # reference.conf: 76
                "initialDelay" : "5s",
                # reference.conf: 77
                "frequency" : "1s"
            },
            # reference.conf: 130
            "client" : {
                # reference.conf: 143
                "message-frame-size" : "1 MiB",
                # reference.conf: 131
                "buffering" : {
                    # reference.conf: 139
                    #  If negative (or zero) then an unbounded mailbox is used (default)
                    #  If positive then a bounded mailbox is used and the capacity is set using
                    #  the property
                    "capacity" : -1,
                    # reference.conf: 134
                    #  Should message buffering on remote client error be used (buffer flushed
                    #  on successful reconnect)
                    "retry-message-send-on-failure" : "off"
                },
                # reference.conf: 145
                #  Maximum time window that a client should try to reconnect for
                "reconnection-time-window" : "600s",
                # reference.conf: 141
                "reconnect-delay" : "5s",
                # reference.conf: 142
                "read-timeout" : "3600s"
            },
            # reference.conf: 43
            #  Which implementation of akka.remote.RemoteSupport to use
            #  default is a TCP-based remote transport based on Netty
            "transport" : "akka.remote.netty.NettyRemoteSupport",
            # reference.conf: 87
            #  The dispatcher used for the system actor "network-event-sender"
            "network-event-sender-dispatcher" : {
                # reference.conf: 88
                "type" : "PinnedDispatcher"
            },
            # reference.conf: 81
            #  The dispatcher used for remote system messages
            "compute-grid-dispatcher" : {
                # reference.conf: 83
                #  defaults to same settings as default-dispatcher
                "name" : "ComputeGridDispatcher"
            },
            # reference.conf: 91
            "server" : {
                # reference.conf: 121
                #  Size of the core pool of the remote execution unit
                "execution-pool-size" : 4,
                # reference.conf: 101
                #  Increase this if you want to be able to send messages with large payloads
                "message-frame-size" : "1 MiB",
                # reference.conf: 98
                #  The default remote server port clients should connect to.
                #  Default is 2552 (AKKA)
                "port" : 2552,
                # reference.conf: 108
                #  Should the remote server require that it peers share the same secure-cookie
                #  (defined in the 'remote' section)?
                "require-cookie" : "off",
                # reference.conf: 115
                #  Sets the size of the connection backlog
                "backlog" : 4096,
                # reference.conf: 112
                #  Enable untrusted mode for full security of server managed actors, allows
                #  untrusted clients to connect.
                "untrusted-mode" : "off",
                # reference.conf: 124
                #  Maximum channel size, 0 for off
                "max-channel-memory-size" : "0b",
                # reference.conf: 94
                #  The hostname or ip to bind the remoting to,
                #  InetAddress.getLocalHost.getHostAddress is used if empty
                "hostname" : "",
                # reference.conf: 127
                #  Maximum total size of all channels, 0 for off
                "max-total-memory-size" : "0b",
                # reference.conf: 104
                #  Timeout duration
                "connection-timeout" : "120s",
                # reference.conf: 118
                #  Length in akka.time-unit how long core threads will be kept alive if idling
                "execution-pool-keepalive" : "60s"
            },
            # reference.conf: 57
            #  Timeout for ACK of cluster operations, lik checking actor out etc.
            "remote-daemon-ack-timeout" : "30s",
            # reference.conf: 54
            #  Generate your own with '$AKKA_HOME/scripts/generate_config_with_secure_cookie.sh'
            #      or using 'akka.util.Crypt.generateSecureCookie'
            "secure-cookie" : "",
            # reference.conf: 50
            "use-compression" : "off",
            # reference.conf: 60
            #  Reuse inbound connections for outbound messages
            "use-passive-connections" : "on"
        },
        # reference.conf: 34
        #  List FQCN of extensions which shall be loaded at actor system startup.
        #  Should be on the format: 'extensions = ["foo", "bar"]' etc.
        #  FIXME: clarify "extensions" here, "Akka Extensions (<link to docs>)"
        "extensions" : [],
        # reference.conf: 10
        #  Akka version, checked against the runtime version of Akka.
        "version" : "2.0-M2"
    }
}
  """)
  val system = akka.actor.ActorSystem(id, ConfigFactory.load(customConf))
  val actor = system.actorOf(Props(new ServerActor(id, this)), "server")

  def shutdown() {
    system.shutdown()
  }

  def log(msg:String) = if(debug) println(msg)

  def run[T](op:Operation[T])(implicit m:Manifest[T]):T = {
    val context = new Context(this)
    context.run(op)(m)
  }

  def getResult[T](op:Operation[T])(implicit m:Manifest[T]):Complete[T] = {
    val context = new Context(this)
    context.getResult(op)(m)
  }

  private[process] def _run[T](op:Operation[T])(implicit m:Manifest[T], t:TimerLike):Complete[T] = {
    log("server.run called with %s" format op)

    implicit val timeout = Timeout(60 seconds)
    val future = (actor ? Run(op)).mapTo[OperationResult[T]]
    val result = Await.result(future, 60 seconds)

    val result2:Complete[T] = result match {
      case OperationResult(c:Complete[_], _) => {
        val r = c.value
        val h = c.history
        log(" run is complete: received: %s" format r)
        log(op.toString)
        log("%s" format h.toPretty())
        t.add(h)
        //r.asInstanceOf[T]
        c.asInstanceOf[Complete[T]]
      }
      case OperationResult(Inlined(_), _) => {
        sys.error("server.run(%s) unexpected response: %s".format(op, result))
      }
      case OperationResult(Error(msg, trace), _) => {
        sys.error("server.run(%s) error: %s, trace: %s".format(op, msg, trace))
      }
      case _ => sys.error("unexpected status: %s" format result)
    }
    result2
  }
}

trait FileCaching {
  val mb = math.pow(2,20).toLong
  val maxBytesInCache = 2000 * mb
  val cache:CacheStrategy[String,Array[Byte]] = new MRUCache(maxBytesInCache, _.length)
  val rasterCache:HashBackedCache[String,IntRaster] = new MRUCache(maxBytesInCache, _.length * 4)

  val catalog:Catalog

  var cacheSize:Int = 0
  val maxCacheSize:Int = 1000 * 1000 * 1000 // 1G

  var caching = false

  var id = "default"

  // TODO: remove this and add cache configuration to server's JSON config
  //       and/or constructor arguments
  def enableCaching() { caching = true }
  def disableCaching() { caching = false }

  def loadRaster(path:String, g:RasterExtent):IntRaster = getRaster(path, null, g)

  /**
   * THIS is the new thing that we are wanting to use.
   */
  def getRaster(path:String, layer:RasterLayer, re:RasterExtent):IntRaster = {

    def xyz(path:String, layer:RasterLayer) = {
      getReader(path).read(path, Option(layer), None)
    }

    if (this.caching) {
      val raster = rasterCache.getOrInsert(path, xyz(path, layer))
      IntRasterReader.read(raster, Option(re))
    } else {
      getReader(path).read(path, Option(layer), Option(re))
    }
  }

  def recursiveListFiles(f: File, r: Regex): Array[File] = {
    val these = f.listFiles
    val good = these.filter(f => r.findFirstIn(f.getName).isDefined)
    good ++ these.filter(_.isDirectory).flatMap(recursiveListFiles(_,r))
  }

  def cacheRasters() = catalog.stores.foreach {
    case (name, store) => store.layers.values.foreach {
      case (path, layer) => getRaster(path, layer, null)
    }
  }

  def getReader(path:String): FileReader = path match {
    case Arg32Pattern() => Arg32Reader
    case ArgPattern() => ArgReader
    case GeoTiffPattern() => GeoTiffReader
    case AsciiPattern() => AsciiReader
    case _ => sys.error("unknown path type %s".format(path))
  }
}

object Server {
  val catalogPath = "/etc/trellis/catalog.json"

  def apply(id:String): Server = Server(id, Catalog.fromPath(catalogPath))

  def apply(id:String, catalog:Catalog): Server = {
    val server = new Server(id, catalog)
    server
  }
}

object TestServer {
  def apply() = Server("test", Catalog.empty("test"))
}
