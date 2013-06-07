"""
Cheap and easy logging
probably should use python logging
"""

def error(m,code=1):
    print 'ERROR: %s' % m
    exit(code)

def notice(m):
    print 'NOTICE: %s' % m
