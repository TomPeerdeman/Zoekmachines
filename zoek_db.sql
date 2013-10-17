-- phpMyAdmin SQL Dump
-- version 3.5.8.1deb1
-- http://www.phpmyadmin.net
--
-- Machine: localhost
-- Genereertijd: 17 okt 2013 om 19:11
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
  `doc_id` varchar(32) NOT NULL,
  `title` mediumtext,
  `contents` text,
  `category` text NOT NULL,
  `questions` mediumtext NOT NULL,
  `answers` mediumtext NOT NULL,
  `answerers` text NOT NULL,
  `keywords` text NOT NULL,
  `questioners` text NOT NULL,
  `questioners_party` text NOT NULL,
  `answerers_ministry` text NOT NULL,
  `entering_date` date NOT NULL,
  `answering_date` date NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `doc_id` (`doc_id`),
  FULLTEXT KEY `title` (`title`,`contents`,`category`,`questions`,`answers`,`answerers`,`answerers_ministry`,`keywords`,`questioners`,`questioners_party`,`doc_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
