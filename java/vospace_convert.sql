--
-- Table structure for table `capabilities`
--

DROP TABLE IF EXISTS `capabilities`;
CREATE TABLE `capabilities` (
  `identifier` varchar(4096) DEFAULT NULL,
  `capability` varchar(128) DEFAULT NULL,
  `active` int(4) DEFAULT NULL,
  INDEX cap_id_idx (`identifier`(767)),
  INDEX cap_cap_idx (`capability`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

INSERT INTO capabilities SELECT * from vospace.capabilities;

--
-- Table structure for table `jobs`
--

DROP TABLE IF EXISTS `jobs`;
CREATE TABLE `jobs` (
  `identifier` varchar(128) NOT NULL,
  `type` varchar(45) NOT NULL,
  `userid` varchar(128) DEFAULT NULL,
  `phase` varchar(45) NOT NULL,
  `method` varchar(45) DEFAULT NULL,
  `created` datetime DEFAULT NULL,
  `completed` datetime DEFAULT NULL,
  `resultid` varchar(45) DEFAULT NULL,
  `job` text,
  PRIMARY KEY (`identifier`),
  INDEX job_typ_idx (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

INSERT INTO jobs SELECT * from vospace.jobs;

--
-- Table structure for table `listings`
--

DROP TABLE IF EXISTS `listings`;
CREATE TABLE `listings` (
  `token` varchar(128) NOT NULL,
  `offset` int(11) DEFAULT '0',
  `count` int(11) DEFAULT '0',
  `updateDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `whereQuery` text,
  PRIMARY KEY (`token`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

INSERT INTO listings SELECT * from vospace.listings;

--
-- Table structure for table `metaproperties`
--

DROP TABLE IF EXISTS `metaproperties`;
CREATE TABLE `metaproperties` (
  `identifier` varchar(128) NOT NULL,
  `type` smallint(6) DEFAULT '0',
  `readonly` smallint(6) DEFAULT '0',
  PRIMARY KEY (`identifier`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- INSERT INTO metaproperties SELECT * from vospace.metaproperties;

--
-- Table structure for table `nodes`
--

DROP TABLE IF EXISTS `nodes`;
CREATE TABLE `nodes` (
  `identifier` varchar(4096) NOT NULL,
  `type` tinyint(4) NOT NULL,
  `view` varchar(128) DEFAULT NULL,
  `status` smallint(6) DEFAULT '0',
  `owner` varchar(128) DEFAULT NULL,
  `location` varchar(4096) DEFAULT NULL,
  `creationDate` datetime DEFAULT NULL,
  `lastModificationDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `node` text,
  PRIMARY KEY (`identifier`(767)),
  INDEX nod_typ_idx (`type`),
  INDEX nod_own_idx (`owner`),
  INDEX nod_loc_idx (`location`(767))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

INSERT INTO nodes SELECT * from vospace.nodes;
ALTER TABLE nodes DROP node;
ALTER TABLE nodes ADD COLUMN `depth` smallint AFTER identifier;
UPDATE nodes SET depth = (LENGTH(identifier)-LENGTH(REPLACE(identifier,'/',''))-3);

--
-- Table structure for table `results`
--

DROP TABLE IF EXISTS `results`;
CREATE TABLE `results` (
  `identifier` varchar(128) NOT NULL,
  `details` text,
  PRIMARY KEY (`identifier`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

INSERT INTO results SELECT * from vospace.results;

--
-- Table structure for table `transfers`
--

DROP TABLE IF EXISTS `transfers`;
CREATE TABLE `transfers` (
  `identifier` int(11) NOT NULL AUTO_INCREMENT,
  `jobid` varchar(128) DEFAULT NULL,
  `endpoint` varchar(4096) NOT NULL,
  `created` datetime DEFAULT NULL,
  `completed` datetime DEFAULT NULL,
  PRIMARY KEY (`identifier`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

INSERT INTO transfers SELECT * from vospace.transfers;

--
-- Table structure for table `properties`
--
DROP TABLE IF EXISTS `properties`;
/*
CREATE TABLE `properties` (
  `identifier` varchar(128) NOT NULL,
  `property` varchar(128) NOT NULL,
  `value` varchar(256) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
*/
