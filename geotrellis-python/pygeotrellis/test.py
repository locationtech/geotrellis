import java_gateway

def main():
    gateway = java_gateway.launch_java_gateway()
    jvm = gateway.jvm
    path = "../econic.tif"

    print "\nReading a GeoTiff on the Python side"
    result = jvm.GeoTiffReader.readSingleband(path)
    print "Here is the result of my read"
    print result

if __name__ == "__main__":
    main()
