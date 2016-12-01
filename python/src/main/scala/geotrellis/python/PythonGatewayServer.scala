package geotrellis.python

import java.io.DataOutputStream
import java.net.Socket

import py4j.GatewayServer

object PytonGatewayServer {
	def main(args: Array[String]): Unit = {
		val gateway: GatewayServer = new GatewayServer(null, 0)
		gateway.start()
		val boundPort: Int = gateway.getListeningPort
		if (boundPort == -1)
			throw new Error("Failed to bind")
		else
			println(s"Started the PythonGatewayServer on port $boundPort")

		val pythonHost = sys.env("_PYTHON_HOST")
		val pythonPort = sys.env("_PYTHON_PORT").toInt

		println(s"The python host and port is $pythonHost:$pythonPort")

		val pythonSocket = new Socket(pythonHost, pythonPort)
		val dos = new DataOutputStream(pythonSocket.getOutputStream)

		dos.writeInt(boundPort)
		dos.close()
		pythonSocket.close()

		while(System.in.read() != -1) {
		}

		println("Exiting now")
	}
}
