import java_gateway

def main():
    gateway = java_gateway.launch_java_gateway()
    jvm = gateway.jvm
    path = "../econic.tif"

    print "\nReading a Proj4String from a SinglebandGeoTiff"
    singleband_geotiff = jvm.GeoTiffReader.readSingleband(path)
    print "Here is the result of my read"
    crs = singleband_geotiff.crs

    # this won't work
    # print crs.toProj4String

    # this will work
    print jvm.crs.toProj4String

if __name__ == "__main__":
    main()
