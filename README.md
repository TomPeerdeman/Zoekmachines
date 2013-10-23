Zoekmachines
============

Repository for Zoekmachines 2013 of the University Of Amsterdam of the persons:
Ruben Janssen
Tom Peerdeman


Installation
=====
Have installed:
MySQL
tomcat

Import the zoek_db.sql file into your MySQL database

In the zoekmachine folder:
mvn dependency:resolve
mvn eclipse:eclipse

Deploying
=====
First time:
Change pom.xml to match your tomcat configuration (default localhost)
Change src/main/webapp/META-INF/context.xml to match your MySQL setup

Actual deploy:
mvn tomcat7:deploy
