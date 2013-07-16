#!/usr/bin/env python
import argparse, os

import log
import catalog
from convert import *
from datatypes import arg
from raster import Layer

class InfoCommand:
    @staticmethod
    def execute(args):
        layer = Layer.fromPath(args.input)
        
        for i in layer.arg_metadata().iteritems():
            print "%s: %s" % i

    @staticmethod
    def add_parser(subparsers):
        parser = subparsers.add_parser('info')

        parser.add_argument('input',
                            metavar='INPUT',
                            help='Path to input file.')
        parser.set_defaults(func=InfoCommand.execute)

class ConvertAllCommand:
    @staticmethod
    def execute(args):
        if os.path.isfile(args.input):
            log.error("Path %s exists, but is a file." % args.input)

        if not os.path.isdir(args.input):
            log.error("Path %s does not exist." % args.input)

        if (args.rows_per_tile and not args.cols_per_tile) or \
           (args.cols_per_tile and not args.rows_per_tile):
            log.error('You must specifiy both --rows-per-tile and '\
                      '--cols-per-tile or neither')

        if args.data_type == 'bit':
            #TODO: Support bit types
            log.error('bit datatype not yet supported')

        flist = os.listdir(args.input)
        if args.extension:
            flist = filter(lambda x: x.endswith(args.extension), flist)

        if not args.rows_per_tile:
            get_output = lambda f: os.path.join(args.output,os.path.splitext(f)[0] + '.arg')
        else:
            get_output = lambda f: args.output

        for f in flist:
            path = os.path.join(args.input,f)
            if not os.path.isfile(path):
                continue

            print "Converting %s" % (path)
            convert(path, 
                    get_output(f), 
                    args.data_type,
                    args.band, 
                    args.name,
                    not args.no_verify,
                    args.cols_per_tile,
                    args.rows_per_tile,
                    args.legacy,
                    args.clobber)
    
    @staticmethod
    def add_parser(subparsers):
        parser = subparsers.add_parser('convert-all')
        parser.add_argument('-t', '--data-type',
                                    help='Arg data type. Defaults to converting '\
                                    'between whatever the input datatype is. '\
                                    'Since unsigned types are not supported '\
                                    'they will automatically be promoted to '\
                                    'the next highest signed type.',
                                    choices=arg.datatypes,
                                    default=None)

        parser.add_argument('-n', '--name',
                                    default=None,
                                    help='Each layer requires a name for the '\
                                         'metadata. By default the name is '\
                                         'input file without the extension.')

        parser.add_argument('-b', '--band',
                                    type=int,
                                    default=1,
                                    help='A specific band to extract')

        parser.add_argument('-e', '--extension',
                                    help="Extension of files in the input directory to convert.")

        parser.add_argument('--clobber',
                                    action='store_true',
                                    help='Clobber existing files or directories.')

        parser.add_argument('--no-verify',
                                    action='store_true',
                                    help="Don't verify input data falls in a given "\
                                    'range (just truncate)')

        # File names
        parser.add_argument('input',
                                    help='Name of the input directory.')
        parser.add_argument('output',
                                help='Output directory to write converted ARGs to.')
        # Tiles
        tiles_group = parser.add_argument_group('tiles')
        tiles_group.add_argument('--rows-per-tile',
                             help='Number of rows per tile',
                             default=None,
                             type=int)
        tiles_group.add_argument('--cols-per-tile',
                             help='Number of cols per tile',
                             default=None,
                             type=int)
        tiles_group.add_argument('--legacy',
                             help='Write out the tiles in old tile format ' \
                                  '(to work with GeoTrellis 0.8.x)',
                             action='store_true')

        parser.set_defaults(func=ConvertAllCommand.execute)

class ConvertCommand:
    @staticmethod
    def execute(args):
        if not os.path.isfile(args.input):
            log.error("Path %s does not exist." % args.input)

        if (args.rows_per_tile and not args.cols_per_tile) or \
           (args.cols_per_tile and not args.rows_per_tile):
            log.error('You must specifiy both --rows-per-tile and '\
                      '--cols-per-tile or neither')

        if args.data_type == 'bit':
            #TODO: Support bit types
            log.error('bit datatype not yet supported')

        convert(args.input, 
                args.output, 
                args.data_type,
                args.band, 
                args.name,
                not args.no_verify,
                args.cols_per_tile,
                args.rows_per_tile,
                args.legacy,
                args.clobber)
    
    @staticmethod
    def add_parser(subparsers):
        convert_parser = subparsers.add_parser('convert')
        convert_parser.add_argument('-t', '--data-type',
                                    help='Arg data type. Defaults to converting '\
                                    'between whatever the input datatype is. '\
                                    'Since unsigned types are not supported '\
                                    'they will automatically be promoted to '\
                                    'the next highest signed type.',
                                    choices=arg.datatypes,
                                    default=None)

        convert_parser.add_argument('-n', '--name',
                                    default=None,
                                    help='Each layer requires a name for the '\
                                         'metadata. By default the name is '\
                                         'input file without the extension.')

        convert_parser.add_argument('-b', '--band',
                                    type=int,
                                    default=1,
                                    help='A specific band to extract')

        convert_parser.add_argument('--clobber',
                                    action='store_true',
                                    help='Clobber existing files or directories.')

        convert_parser.add_argument('--no-verify',
                                    action='store_true',
                                    help="Don't verify input data falls in a given "\
                                    'range (just truncate)')

        # File names
        convert_parser.add_argument('input',
                                    help='Name of the input file')
        convert_parser.add_argument('output',
                                help='Output file to write')
        # Tiles
        tiles_group = convert_parser.add_argument_group('tiles')
        tiles_group.add_argument('--rows-per-tile',
                             help='Number of rows per tile',
                             default=None,
                             type=int)
        tiles_group.add_argument('--cols-per-tile',
                             help='Number of cols per tile',
                             default=None,
                             type=int)
        tiles_group.add_argument('--legacy',
                             help='Write out the tiles in old tile format ' \
                                  '(to work with GeoTrellis 0.8.x)',
                             action='store_true')

        convert_parser.set_defaults(func=ConvertCommand.execute)

class CatalogCommand:
    class ListCommand:
        @staticmethod
        def execute(args):
            catalog.catalog_list(args)

        @classmethod
        def add_parser(cls,subparsers):
            catalog_list_parser = subparsers.add_parser('list')
            catalog_list_parser.add_argument('catalog',
                                             metavar='CATALOG',
                                             help='Path to catalog file',
                                             type=argparse.FileType('r'))
    
            catalog_list_parser.set_defaults(func=cls.execute)

    class UpdateCommand:
        @staticmethod
        def execute(args):
            catalog.catalog_update(args)

        @classmethod
        def add_parser(cls,subparsers):
                catalog_upd_parser = subparsers.add_parser('update')
                catalog_upd_parser.add_argument('catalog',
                                                help='Path to catalog file',
                                                type=argparse.FileType('rw'))
    
                catalog_upd_parser.add_argument('store',
                                                help='Data store to update')
                catalog_upd_parser.add_argument('field',
                                                help='Field to update')
                catalog_upd_parser.add_argument('value',
                                                help='New value')
    
                catalog_upd_parser.set_defaults(func=cls.execute)

    class AddDirectoryCommand:
        @staticmethod
        def execute(args):
            catalog.catalog_add_dir(args)

        @classmethod
        def add_parser(cls,subparsers):
            parser = subparsers.add_parser('add-dir')
            parser.add_argument('catalog',
                                help='Path to catalog file',
                                type=argparse.FileType('rw'))
    
            parser.add_argument('directory',
                                help='Directory to add')
    
            parser.add_argument('--name',
                                help='Name of the datasource, defaults '\
                                'to the name of the directory with :fs')
    
            parser.add_argument('--cache-all',
                                help='Set the cache all field to true',
                                action='store_true')            
            parser.set_defaults(func=cls.execute)

    class CreateCommand:
        @staticmethod
        def execute(args):
            catalog.catalog_create(args)

        @classmethod
        def add_parser(cls,subparsers):
            parser = subparsers.add_parser('create')
            parser.add_argument('catalog',
                                help='Path to catalog file')
            parser.add_argument('name',
                                nargs='?',
                                help='Name of the catalog',
                                default='catalog')
    
            parser.set_defaults(func=cls.execute)

    @classmethod
    def add_parser(cls,subparsers):
        catalog_parser = subparsers.add_parser('catalog')
        catalog_subparsers = catalog_parser.add_subparsers()

        cls.ListCommand.add_parser(catalog_subparsers)
        cls.UpdateCommand.add_parser(catalog_subparsers)
        cls.AddDirectoryCommand.add_parser(catalog_subparsers)
        cls.CreateCommand.add_parser(catalog_subparsers)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers()

    InfoCommand.add_parser(subparsers)
    ConvertAllCommand.add_parser(subparsers)
    ConvertCommand.add_parser(subparsers)
    CatalogCommand.add_parser(subparsers)

    args = parser.parse_args()
    args.func(args)
