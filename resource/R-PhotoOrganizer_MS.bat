set SWING11=C:\jdk1.1.8\swing-1.1.1fcs
set JMAIL=C:\JavaAPIExt\mail.jar
set JAF=C:\JavaAPIExt\activation.jar
set XMLPARSER=C:\JavaAPIExt\parser.jar

start WJview -cp:a lib\PhotoOrganizer.jar;lib\openD.jar;lib\addressbook.jar;lib\parser.jar;%SWING11%\swingall.jar;%JMAIL%;%JAF%;%XMLPARSER% -d:R-PhotoOrganizer.home=.\data -d:R-AddressBook.home=.\data -d:jdbc.drivers=sun.jdbc.odbc.JdbcOdbcDriver:com.ms.jdbc.odbc.JdbcOdbcDriver photoorganizer.PhotoOrganizer
