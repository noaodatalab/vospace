from dl import authClient as ac, queryClient as qc, storeClient as sc
import getpass
import os
import os.path
import sys
import datetime
import filecmp
import tempfile

ac.set_svc_url('http://dldev.datalab.noao.edu/auth')
sc.set_svc_url('http://dldev.datalab.noao.edu/storage')

def timed_ls(dir):
    start = datetime.datetime.now()
    rawls = sc.ls (dir, format="long")
    flist = rawls.rstrip().split('\n')
    if not flist or not flist[0]: flist = []
    print ("{} {} {}".format(datetime.datetime.now() - start, len(flist), dir))
    return rawls;

def recursive_ls(dir, max_depth=None):
    rawls = timed_ls(dir)
    flist = rawls.rstrip().split('\n')
    if not flist or not flist[0]: flist = []
    for f in flist:
        name = f.split(' ')[-1]
        if f[0] == "d" and (max_depth is None or (dir.count('/') - 1 <= max_depth)):
            recursive_ls(dir + name, max_depth=max_depth)
    return rawls;

def make_copies(file, dir, n_copies):
    for i in range(n_copies):
        f_name, f_ext = os.path.splitext(os.path.basename(file))
        n_name = "{}/{}_{:04}{}".format(dir, f_name, i, f_ext)
        print ("{} {}".format(n_name, sc.cp(file, n_name)))

def make_subdir_copies(file, n_dirs, n_copies):
    for i in range(n_dirs):
        subdir = "{}/data_{:04}".format(os.path.dirname(file), i)
        print ('Making ' + subdir + ' ' + sc.mkdir(subdir))
        make_copies(file, subdir, n_copies)
        timed_ls(subdir+'/')

def test_copies(fname, n_files, n_dirs):
    tmpf = tempfile.NamedTemporaryFile(delete=False)
    tmpf.close()
    try:
        print ('Making data dir ' + sc.mkdir('vos://data'))
        print ('Copying ' + fname + ' ' + sc.put(fname, 'vos://data/'))
        make_copies ('vos://data/' + os.path.basename(fname), 'vos://data', n_files)
        make_subdir_copies ('vos://data/' + os.path.basename(fname), n_dirs, n_files)
        timed_ls ('vos://data/')
        print (sc.get('vos://data/' + os.path.basename(fname), tmpf.name))
        print ('GET same as PUT: ' + str(filecmp.cmp(tmpf.name, fname, shallow=False)))
    except KeyboardInterrupt:
        pass
    finally:
        print ('Removing temp data: ' + sc.rmdir ('vos://data'))
        os.remove(tmpf.name)
        print (timed_ls ('vos://'))

def list_roots():
    for line in sys.stdin:
        line = line.rstrip()
        if ac.isValidUser (line): timed_ls (line + '://')
        else: print ("Invalid user " + line)

def main():
    username = sys.argv[1] if len(sys.argv) > 1 else 'geychaner'
    fname = sys.argv[2] if len(sys.argv) > 2 else os.path.expanduser('~') + '/sampledata/grzw1_sn10_15M.jpg'
    n_files = int(sys.argv[3]) if len(sys.argv) > 3 else 5
    n_dirs = int(sys.argv[4]) if len(sys.argv) > 4 else n_files
    while not ac.isUserLoggedIn(username):
        ac.login(username, getpass.getpass('Enter {} password (+ENTER): '.format(username)))
    else:
        try:
            print(timed_ls ('vos://'))
            print(recursive_ls ('fitz://'))
            flist = timed_ls ('fsvs://')
            if len(flist) == 1: print (flist);
            recursive_ls ('ls_dr7://')
        except KeyboardInterrupt:
            pass
        finally:
            test_copies(fname, n_files, n_dirs)

if __name__ == '__main__':
    main()