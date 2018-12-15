from dl import authClient as ac, queryClient as qc, storeClient as sc
import getpass
import os
import os.path
import sys
import datetime
import signal

ctrlc_pressed = False

# def sigint_handler(signum, frame):
#     global ctrlc_pressed
#     print ('Interrupt received.')
#     ctrlc_pressed = True
#
# signal.signal(signal.SIGINT, sigint_handler)

ac.set_svc_url('http://dldev.datalab.noao.edu/auth')
sc.set_svc_url('http://dldev.datalab.noao.edu/storage')

def timed_ls(dir):
    start = datetime.datetime.now()
    flist = sc.ls (dir, format="long").rstrip().split('\n')
    if not flist or not flist[0]: flist = []
    print ("{} {} {}".format(datetime.datetime.now() - start, len(flist), dir))
    return flist;

def recursive_ls(dir, n_total=0):
    global ctrlc_pressed
    flist = timed_ls(dir)
    n_total += len(flist)
    for f in flist:
        if ctrlc_pressed: sys.exit()
        name = f.split(' ')[-1]
        if f[0] == "d": n_total = recursive_ls(dir + name, n_total=n_total)
    return n_total;

def make_copies(file, dir, n_copies):
    for i in range(n_copies):
        f_name, f_ext = os.path.splitext(os.path.basename(file))
        n_name = "{}/{}_{:04}{}".format(dir, f_name, i, f_ext)
        print ("{} {}".format(n_name, sc.cp(file, n_name)))

def make_subdir_copies(file, n_dirs, n_copies):
    for i in range(n_dirs):
        subdir = "{}/data_{:04}".format(os.path.dirname(file), i)
        print (sc.mkdir(subdir))
        make_copies(file, subdir, n_copies)
        timed_ls(subdir+'/')

def test_copies(fname, n_files, n_dirs):
    print (sc.mkdir('vos://data'))
    print (sc.put(fname, 'vos://data/'))
    make_copies ('vos://data/' + os.path.basename(fname), 'vos://data', n_files)
    make_subdir_copies ('vos://data/' + os.path.basename(fname), n_dirs, n_files)
    timed_ls ('vos://data/')
    print (sc.get('vos://data/' + os.path.basename(fname), 'test_data.jpg'))
    input ('Hit ENTER to continue: ')
    print (sc.rmdir ('vos://data'))
    print (sc.ls ('vos://'))
    os.remove('test_data.jpg')

def list_roots():
    for line in sys.stdin:
        line = line.rstrip()
        if ac.isValidUser (line): timed_ls (line + '://')

username = sys.argv[1] if len(sys.argv) > 1 else 'geychaner'
fname = sys.argv[2] if len(sys.argv) > 2 else os.path.expanduser('~') + '/sampledata/grzw1_sn10_15M.jpg'
n_files = int(sys.argv[3]) if len(sys.argv) > 3 else 10
n_dirs = int(sys.argv[4]) if len(sys.argv) > 4 else n_files
while not ac.isUserLoggedIn(username):
    ac.login(username, getpass.getpass('Enter {} password (+ENTER): '.format(username)))
else:
    timed_ls ('fsvs://')
    recursive_ls('fitz://')
    test_copies(fname, n_files, n_dirs)
    sys.exit()
