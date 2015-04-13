cd data
rm -rf *
cd ..
cp test/file.dat data/node12
ls data
mysql -u root -p < cleardb.sql
python controller.py
