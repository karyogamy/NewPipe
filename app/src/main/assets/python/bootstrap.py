"""
 This file is executed when the Python interpreter is started.
 Use this file to configure all your necessary python code.

"""

from __future__ import unicode_literals
import youtube_dl
import json
import sys
import os

class PassThroughLogger(object):
    def isatty(self):
        return False
    def debug(self, msg):
        print(msg)
    def warning(self, msg):
        print(msg)
    def error(self, msg):
        print(msg)

sys.stderr = PassThroughLogger()

# ssl certificate hack for android, since android cacerts format does not conform with python openssl
os.environ["SSL_CERT_FILE"] = '/data/user/0/org.schabi.newpipe.debug/assets/python/cacert.pem'
print(os.environ["SSL_CERT_FILE"])


def router(args):
    """
    Defines the router function that routes by function name.

    :param args: JSON arguments
    :return: JSON response
    """
    values = json.loads(args)

    try:
        function = routes[values.get('function')]

        status = 'ok'
        res = function(values)
    except:
        status = 'fail'
        res = None

    return json.dumps({
        'status': status,
        'result': res,
    })

def wait(args):
    import time
    t = args['wait']
    time.sleep(t)
    return t

def dl(args):
    ydl_opts = {
        'restrictfilenames': True,
        'youtube_include_dash_manifest': False, # DASH are not support atm, remove this later
        'geo_bypass': True, # bypass geographic blocks
        'nocheckcertificate': False, # toggle if not using ssl cert hack
        'socket_timeout': 20, # timeout of 20s
        'default_search': 'auto', # let youtube-dl guess url if given is invalid
        'encoding': 'UTF-8',
        'logger': PassThroughLogger(),
        'cachedir': args['path'],
        'outtmpl': args['path'] + '%(title)s.%(ext)s',
        'skip_download': args['skip'],
    }
    with youtube_dl.YoutubeDL(ydl_opts) as ydl:
        return ydl.extract_info(args['url'])

routes = {
    'wait': wait,
    'dl': dl,
}
