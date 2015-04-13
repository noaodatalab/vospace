cd data
rm -rf *
cd ..
cp test/file.dat data/node12
mysql -u root -p < cleardb.sql
