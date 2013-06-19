CREATE TABLE `http_resource_directory` (
  `nid` bigint(20) NOT NULL AUTO_INCREMENT,
  `ncrc32` int(10) unsigned DEFAULT '0',
  `surl` text COLLATE ascii_bin NOT NULL,
  `setag` text COLLATE ascii_bin,
  `ee` varchar(32) COLLATE ascii_bin DEFAULT NULL,
  `dd` varchar(32) COLLATE ascii_bin DEFAULT NULL,
  `scharacterset` varchar(16) COLLATE ascii_bin DEFAULT NULL,
  `ff` int(11) DEFAULT '0',
  `gg` varchar(80) COLLATE ascii_bin DEFAULT NULL,
  `hh` int(11) DEFAULT '0',
  `ii` datetime DEFAULT NULL,
  `jj` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `kk` datetime DEFAULT NULL,
  PRIMARY KEY (`nid`),
  KEY `IDX_CRC` (`ncrc32`),
  KEY `IDX_PURGE` (`dtexpires`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=ascii COLLATE=ascii_bin;

CREATE TABLE `config` (
  `nconfigid` bigint(20) NOT NULL AUTO_INCREMENT,
  `skey` varchar(255) NOT NULL,
  `col4` varchar(500) NOT NULL,
  `col2` smallint(6) DEFAULT NULL,
  `col1` char(5) DEFAULT NULL,
  `col3` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`nconfigid`),
  UNIQUE KEY `skey` (`skey`,`col2`,`col1`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `commented_table` (
  `col1` smallint(6) NOT NULL,
  `col2` text NOT NULL,
  `commented_column` tinyint(4) NOT NULL COMMENT 'value can be 1-3',
  PRIMARY KEY (`col1`,`col2`)
);


DROP TABLE IF EXISTS `OTHER_TABLE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `OTHER_TABLE`  (
	JOB_INSTANCE_ID BIGINT  NOT NULL PRIMARY KEY ,
	`VERSION` BIGINT ,
	`NORDER` int(11)  NOT NULL DEFAULT '1',
	`NORDER2` int(11) DEFAULT '1'  NOT NULL ,
	`JOB_NAME` VARCHAR(100) NOT NULL,
	`JOB_KEY` VARCHAR(32) NOT NULL,
	constraint OTHER_TABLE_FK foreign key (JOB_NAME) references REFERENCED_TABLE(JOB_NAME_ID)
    constraint OTHER_TABLE_UN unique (`JOB_NAME`, `JOB_KEY`)
);
-- ENGINE=InnoDB AUTO_INCREMENT=86 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

DROP TABLE IF EXISTS TEST_TABLE;
CREATE TABLE other_db.TEST_TABLE (
  client_id smallint(6) NOT NULL,
  recently datetime,
  updated DATETIME DEFAULT NULL,
  VERSION BIGINT  ,
  author varchar(32),
  DOUBLE_VAL DOUBLE PRECISION ,
  SERIALIZED_CONTEXT TEXT ,
  PRIMARY KEY (client_id, `version`)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE other_table_seq (
  ID BIGINT NOT NULL)
   ENGINE=MYISAM;
INSERT INTO OTHER_TABLE_SEQ values(0);


