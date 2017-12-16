"""
 This file is executed when the Python interpreter is started.
 Use this file to configure all your necessary python code.

"""

from __future__ import unicode_literals
import youtube_dl
import json
import sys

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
        'nocheckcertificate': True, # ssl not working yet
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
