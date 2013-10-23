-- phpMyAdmin SQL Dump
-- version 3.5.8.1deb1
-- http://www.phpmyadmin.net
--
-- Machine: localhost
-- Genereertijd: 23 okt 2013 om 13:39
-- Serverversie: 5.5.32-0ubuntu0.13.04.1
-- PHP-versie: 5.4.9-4ubuntu2.3

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Databank: `zoek_db`
--

-- --------------------------------------------------------

--
-- Tabelstructuur voor tabel `documents`
--

DROP TABLE IF EXISTS `documents`;
CREATE TABLE IF NOT EXISTS `documents` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `doc_id` tinytext NOT NULL,
  `title` mediumtext,
  `contents` text,
  `category` text,
  `questions` mediumtext,
  `answers` mediumtext,
  `answerers` text,
  `keywords` text,
  `questioners` text,
  `questioners_party` text,
  `answerers_ministry` text,
  `entering_date` date DEFAULT NULL,
  `answering_date` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  FULLTEXT KEY `title` (`title`,`contents`,`category`,`questions`,`answers`,`answerers`,`answerers_ministry`,`keywords`,`questioners`,`questioners_party`,`doc_id`),
  FULLTEXT KEY `questions` (`questions`),
  FULLTEXT KEY `answers` (`answers`),
  FULLTEXT KEY `keywords` (`keywords`),
  FULLTEXT KEY `questioners` (`questioners`),
  FULLTEXT KEY `answerers` (`answerers`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=40667 ;

-- --------------------------------------------------------

--
-- Tabelstructuur voor tabel `wordclouds`
--

DROP TABLE IF EXISTS `wordclouds`;
CREATE TABLE IF NOT EXISTS `wordclouds` (
  `doc_id` int(10) unsigned NOT NULL,
  `type` enum('DOC','ANSWER','QUESTION') NOT NULL,
  `data` text NOT NULL,
  PRIMARY KEY (`doc_id`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
