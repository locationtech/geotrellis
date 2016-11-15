import atexit
import os
import select
import signal
import socket
import struct
import sys
import time

from py4j.java_gateway import (
    java_import,
    JavaGateway,
    CallbackServerParameters,
    GatewayClient,
    get_method
)

from subprocess import Popen, PIPE


def read_int(stream):
    length = stream.read(4)
    return struct.unpack("!i", length)[0]

def launch_java_gateway(address=None, port=None):

    callback = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    callback.bind(('127.0.0.1', 0))
    callback.listen(1)
    callback_host, callback_port = callback.getsockname()

    env = dict(os.environ)
    env['_PYTHON_HOST'] = callback_host
    env['_PYTHON_PORT'] = str(callback_port)

    path = "../../"

    command = ["sbt", "project python", "run"]

    def preexec_func():
        signal.signal(signal.SIGINT, signal.SIG_IGN)
    proc = Popen(command, cwd=path, stdin=PIPE, preexec_fn=preexec_func, env=env)

    gateway_port = None
    while gateway_port is None and proc.poll() is None:
        timeout = 1
        readable, _, _ = select.select([callback], [], [], timeout)
        if callback in readable:
            connection = callback.accept()[0]
            gateway_port = read_int(connection.makefile(mode="rb"))
            connection.close()
            callback.close()

    if gateway_port is None:
        raise Exception("Gateway process exited before sending the driver its port")

    gateway = JavaGateway(GatewayClient(port=gateway_port), auto_convert=True)

    java_import(gateway.jvm, "geotrellis.raster.*")
    java_import(gateway.jvm, "geotrellis.proj4.*")
    java_import(gateway.jvm, "geotrellis.raster.io.geotiff.*")
    java_import(gateway.jvm, "geotrellis.raster.io.geotiff.reader.*")

    return gateway
