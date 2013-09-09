import osr

import log

def get_epsg(raster):
    """ Get the EPSG code from a raster or quit if there is an error """
    sr = osr.SpatialReference(raster.GetProjection())
    sr.AutoIdentifyEPSG()

    auth = sr.GetAttrValue('AUTHORITY',0)
    epsg = sr.GetAttrValue('AUTHORITY',1)

    if auth is None or epsg is None or auth.lower() != 'epsg':
        log.warn('Could not get EPSG projection')
        return 0
    else:
        return epsg
