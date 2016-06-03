cd /Users/mjg/Projects/test/data
rm -rf *
mkdir staging
cd ..
cp /Users/mjg/Projects/noao/vospace/python/test/burbidge.vot data/node12
cp /Users/mjg/Projects/repos/vospace/python/test/file.dat data/node10
cp /Users/mjg/Projects/repos/vospace/python/test/file.dat data/node10a
cp /Users/mjg/Projects/repos/vospace/python/test/file.dat data/node10b
mkdir data/sarah
mkdir data/siawork
mysql -u root -p < /Users/mjg/Projects/noao/vospace/python/cleardb.sql
