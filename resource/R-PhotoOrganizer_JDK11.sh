#!/bin/sh

$JAVA_HOME/bin/java -classpath $JAVA_HOME/lib/classes.zip:swing.jar:lib/PhotoOrganizer.zip:lib -Djdbc.drivers=sun.jdbc.odbc.JdbcOdbcDriver:com.ms.jdbc.odbc.JdbcOdbcDriver -DR-PhotoOrganizer.home=./ photoorganizer.PhotoOrganizer


