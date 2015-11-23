set JDK11=C:\jdk1.1.8
set SWING11=C:\jdk1.1.8\swing-1.1.1fcs
set JMAIL=C:\JavaAPIExt\mail.jar
set JAF=C:\JavaAPIExt\activation.jar
set XMLPARSER=C:\JavaAPIExt\parser.jar

start %JDK11%\bin\jrew -ms96000 -classpath %JDK11%\lib\classes.zip;%SWING11%\swingall.jar;lib\PhotoOrganizer.zip;lib\openD.jar;lib\addressbook.jar;%JMAIL%;%JAF%;%XMLPARSER% -Djdbc.drivers=sun.jdbc.odbc.JdbcOdbcDriver:com.ms.jdbc.odbc.JdbcOdbcDriver -DR-PhotoOrganizer.home=.\ photoorganizer.PhotoOrganizer
