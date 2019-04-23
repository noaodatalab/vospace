# Creates the default account for MySQL/MariaDB
create user 'dba' identified by PASSWORD '*381AD08BBFA647B14C82AC1094A29AD4D7E4F51D';
grant all privileges on vospace.* TO 'dba'@'%' IDENTIFIED BY PASSWORD '*381AD08BBFA647B14C82AC1094A29AD4D7E4F51D';
create database vospace;
