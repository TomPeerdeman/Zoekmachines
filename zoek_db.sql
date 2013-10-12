-- phpMyAdmin SQL Dump
-- version 3.5.8.1deb1
-- http://www.phpmyadmin.net
--
-- Machine: localhost
-- Genereertijd: 12 okt 2013 om 15:53
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
-- Tabelstructuur voor tabel `answerers`
--

CREATE TABLE IF NOT EXISTS `answerers` (
  `doc_id` int(10) unsigned NOT NULL,
  `answerer_function` varchar(32) NOT NULL,
  `answerer_ministry` varchar(64) NOT NULL,
  `answerer_name` varchar(64) NOT NULL,
  PRIMARY KEY (`doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Tabelstructuur voor tabel `answers`
--

CREATE TABLE IF NOT EXISTS `answers` (
  `doc_id` int(11) unsigned NOT NULL,
  `question_id` int(11) unsigned NOT NULL,
  `answer_text` text NOT NULL,
  UNIQUE KEY `doc_id` (`doc_id`,`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Tabelstructuur voor tabel `documents`
--

CREATE TABLE IF NOT EXISTS `documents` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `doc_id` varchar(32) NOT NULL,
  `contents` tinytext NOT NULL,
  `category` tinytext NOT NULL,
  `origin` tinytext NOT NULL,
  `entering_year` smallint(4) unsigned NOT NULL,
  `entering_month` tinyint(2) unsigned NOT NULL,
  `entering_day` tinyint(2) unsigned NOT NULL,
  `answering_year` smallint(4) unsigned NOT NULL,
  `answering_month` tinyint(2) unsigned NOT NULL,
  `answering_day` tinyint(2) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `doc_id` (`doc_id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Tabelstructuur voor tabel `questioners`
--

CREATE TABLE IF NOT EXISTS `questioners` (
  `doc_id` int(10) unsigned NOT NULL,
  `questioner_party` varchar(32) NOT NULL,
  `questioner_name` varchar(64) NOT NULL,
  PRIMARY KEY (`doc_id`),
  KEY `questioner_party` (`questioner_party`,`questioner_name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Tabelstructuur voor tabel `questions`
--

CREATE TABLE IF NOT EXISTS `questions` (
  `doc_id` int(10) unsigned NOT NULL,
  `question_id` int(10) unsigned NOT NULL,
  `question_text` text NOT NULL,
  UNIQUE KEY `doc_id` (`doc_id`,`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
