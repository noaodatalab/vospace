SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL';

CREATE SCHEMA IF NOT EXISTS `mydb` DEFAULT CHARACTER SET latin1 COLLATE latin1_swedish_ci ;
CREATE SCHEMA IF NOT EXISTS `VOSPACE` ;

-- -----------------------------------------------------
-- Table `VOSPACE`.`nodes`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `VOSPACE`.`nodes` (
  `identifier` VARCHAR(128) NOT NULL ,
  `type` TINYINT NOT NULL ,
  `view` VARCHAR(128) NULL DEFAULT NULL ,
  `status` SMALLINT NULL DEFAULT 0 ,
  `owner` VARCHAR(128) NULL DEFAULT NULL ,
  `location` VARCHAR(128) NULL DEFAULT NULL ,
  `creationDate` DATETIME NULL DEFAULT NULL ,
  `lastModificationDate` TIMESTAMP NOT NULL ,
  `node` TEXT NULL DEFAULT NULL ,
  PRIMARY KEY (`identifier`) );


-- -----------------------------------------------------
-- Table `VOSPACE`.`jobs`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `VOSPACE`.`jobs` (
  `identifier` VARCHAR(128) NOT NULL ,
  `type` VARCHAR(45) NOT NULL ,
  `userid` VARCHAR(128) NULL DEFAULT NULL ,
  `phase` VARCHAR(45) NOT NULL ,
  `method` VARCHAR(45) NULL ,
  `created` DATETIME NULL DEFAULT NULL ,
  `completed` DATETIME NULL DEFAULT NULL ,
  `resultid` VARCHAR(45) NULL ,
  `job` TEXT NULL ,
  PRIMARY KEY (`identifier`) );


-- -----------------------------------------------------
-- Table `VOSPACE`.`metaproperties`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `VOSPACE`.`metaproperties` (
  `identifier` VARCHAR(128) NOT NULL ,
  `type` SMALLINT NULL DEFAULT 0 ,
  `readonly` SMALLINT NULL DEFAULT 0 ,
  PRIMARY KEY (`identifier`) );


-- -----------------------------------------------------
-- Table `VOSPACE`.`properties`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `VOSPACE`.`properties` (
  `identifier` VARCHAR(128) NOT NULL ,
  `property` VARCHAR(128) NOT NULL ,
  `value` VARCHAR(256) NULL DEFAULT NULL );


-- -----------------------------------------------------
-- Table `VOSPACE`.`listings`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `VOSPACE`.`listings` (
  `token` VARCHAR(128) NOT NULL ,
  `offset` INT NULL DEFAULT 0 ,
  `count` INT NULL DEFAULT 0 ,
  `updateDate` TIMESTAMP NOT NULL ,
  `whereQuery` TEXT NULL DEFAULT NULL ,
  PRIMARY KEY (`token`) );


-- -----------------------------------------------------
-- Table `VOSPACE`.`transfers`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `VOSPACE`.`transfers` (
  `identifier` INT NOT NULL AUTO_INCREMENT ,
  `jobid` VARCHAR(128) NULL ,
  `endpoint` VARCHAR(128) NOT NULL ,
  `created` DATETIME NULL ,
  `completed` DATETIME NULL ,
  PRIMARY KEY (`identifier`) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `VOSPACE`.`results`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `VOSPACE`.`results` (
  `identifier` VARCHAR(128) NOT NULL ,
  `details` TEXT NULL ,
  PRIMARY KEY (`identifier`) )
ENGINE = InnoDB;



SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
