set JAVA2=D:\jdk1.3
set JMAIL=D:\jdk1.3\jre\lib\ext\mail.jar
set JAF=C:\jdk1.3\jre\lib\ext\activation.jar

start %JAVA2%\jre\bin\javaw -cp lib\PhotoOrganizer.jar;lib\openD.jar;lib\addressbook.jar;lib\parser.jar;%JMAIL%;%JAF% -Djdbc.drivers=sun.jdbc.odbc.JdbcOdbcDriver:com.ms.jdbc.odbc.JdbcOdbcDriver -DR-PhotoOrganizer.home=.\data -DR-AddressBook.home=.\data photoorganizer.PhotoOrganizer
