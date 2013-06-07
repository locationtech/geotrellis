"""
Handles creating and modifying a GeoTrellis catalog.
"""

import os
import json

import log

def catalog_add_dir(args):
    (head, tail) = os.path.split(args.directory)
    aname = args.name

    if aname is None or len(aname) == 0:
        if len(tail) == 0:
            tail = os.path.split(head)[1]

        aname = "%s:fs" % tail

    add_dir_to_catalog(args.catalog, args.directory, aname, args.cache_all)

def catalog_has_store(catalog, storename):
    stores = [s for s in catalog['stores'] if s['store'] == storename]
    return len(stores) == 1

def catalog_get_store(catalog, storename):
    stores = [s for s in catalog['stores'] if s['store'] == storename]

    if len(stores) == 0:
        log.error("Could not find store '%s'" % storename)
    elif len(stores) > 1:
        log.error("Invalid catalog, multiple stores with the name '%s'" % storename)
    else:
        return stores[0]

def add_dir_to_catalog(catalogf, directory, name, cacheAll):
    catalog = json.loads(catalogf.read())
    stores = catalog['stores']

    if catalog_has_store(catalog, name):
        log.error('A datastore named "%s" already exists' % name)

    store = { 'store': name,
              'params': {
                  'cacheAll': str(cacheAll),
                  'path': os.path.abspath(directory),
                  'type': 'fs'
              }
          }

    stores.append(store)

    with open(catalogf.name,'w') as cat:
        cat.write(json.dumps(catalog, sort_keys=True,
                             indent=4, separators=(',', ': ')))

def catalog_update(args):
    catalog = json.loads(args.catalog.read())
    datasource = args.store
    field = args.field
    value = args.value

    store = catalog_get_store(catalog, datasource)

    if field == 'name':
        store['store'] = value
    else:
        if field in store['params']:
            store['params'][field] = value
        else:
            valid = "valid params: name, %s" % ', '.join(store['params'].keys())
            log.error('Param "%s" does not exist in this data store (%s)' % (
                field, valid))

    with open(args.catalog.name,'w') as cat:
        cat.write(json.dumps(catalog, sort_keys=True,
                             indent=4, separators=(',', ': ')))

def catalog_list(args):
    catalog = json.loads(args.catalog.read())

    print "Catalog: %s" % catalog['catalog']

    if not catalog['stores']:
        print "[Catalog is empty]"
    else:
        for store in catalog['stores']:
            print store['store']
            for param in store['params'].iteritems():
                print "  %s: %s" % param

def catalog_create(args):
    catalog_file = args.catalog
    name = args.name

    if os.path.exists(catalog_file):
        log.error('A file already exists at "%s"' % catalog_file)

    base = { 'catalog':  name,
             'stores': [] }

    with open(catalog_file,'w') as cat:
        cat.write(json.dumps(base, sort_keys=True,
                             indent=4, separators=(',', ': ')))
