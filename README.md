Zoekmachines
============

Repository for Zoekmachines 2013 of the University Of Amsterdam of the persons:<br />
Ruben Janssen<br />
Tom Peerdeman


Installation
=====
Have installed:<br />
Maven<br />
MySQL<br />
tomcat<br />

Import the zoek_db.sql file into your MySQL database

In the zoekmachine folder:<br />
mvn dependency:resolve<br />
mvn eclipse:eclipse

Deploying
=====
First time:<br />
Change pom.xml to match your tomcat configuration (default localhost)<br />
Change src/main/webapp/META-INF/context.xml to match your MySQL setup

Actual deploy:<br />
mvn tomcat7:deploy
