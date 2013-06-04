#!/usr/bin/env python
import argparse

import log
import catalog
from convert import *
from datatypes import arg
from raster import GdalLayer

class InfoCommand:
    @staticmethod
    def execute(args):
        layer = GdalLayer(args.input)
        
        for i in layer.toMap().iteritems():
            print "%s: %s" % i

    @staticmethod
    def add_parser(subparsers):
        parser = subparsers.add_parser('info')

        parser.add_argument('input',
                            metavar='INPUT',
                            help='Path to input file.')
        parser.set_defaults(func=InfoCommand.execute)

class ConvertCommand:
    @staticmethod
    def execute(args):
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
                args.rows_per_tile)
    
    @staticmethod
    def add_parser(subparsers):
        convert_parser = subparsers.add_parser('convert')
        convert_parser.add_argument('--data-type',
                                    help='Arg data type. Defaults to converting '\
                                    'between whatever the input datatype is. '\
                                    'Since unsigned types are not supported '\
                                    'they will automatically be promoted to '\
                                    'the next highest signed type.',
                                    choices=arg.datatypes,
                                    default=None)
        convert_parser.add_argument('--no-verify',
                                    action='store_true',
                                    help="Don't verify input data falls in a given "\
                                    'range (just truncate)')

        convert_parser.add_argument('--name',
                                    default=None,
                                    help='Each layer requires a name for the '\
                                         'metadata. By default the name is '\
                                         'input file without the extension.')

        convert_parser.add_argument('--band',
                                    type=int,
                                    default=1,
                                    help='A specific band to extract')

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
    ConvertCommand.add_parser(subparsers)
    CatalogCommand.add_parser(subparsers)

    args = parser.parse_args()
    args.func(args)
