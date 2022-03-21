set JAVA2=D:\jdk1.3
set JAIHOME=D:\jdk1.3\jre\lib\ext
set JMAIL=D:\jdk1.3\jre\lib\ext\mail.jar
set JAF=C:\jdk1.3\jre\lib\ext\activation.jar

start %JAVA2%\jre\bin\javaw -cp lib\PhotoOrganizer.jar;lib\openD.jar;lib\addressbook.jar;lib\parser.jar;%JAIHOME%\jai_core.jar;%JAIHOME%\jai_codec.jar;%JAIHOME%\mlibwrapper_jai.jar;%JMAIL%;%JAF% -Djdbc.drivers=sun.jdbc.odbc.JdbcOdbcDriver:com.ms.jdbc.odbc.JdbcOdbcDriver -DUSE_JAI -DR-PhotoOrganizer.home=.\data -DR-AddressBook.home=.\data photoorganizer.PhotoOrganizer
