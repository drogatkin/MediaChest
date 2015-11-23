set JAVA2=C:\jdk1.3
set JAIHOME=C:\jdk1.3\jre\lib\ext
set JMAIL=C:\jdk1.3\jre\lib\ext\mail.jar
set JAF=C:\jdk1.3\jre\lib\ext\activation.jar
set XBOXHOME=C:\tmp\builds\xBox101
set XMLPARSER=C:\jdk1.3\jre\lib\ext\parser.jar

%JAVA2%\jre\bin\javaw -cp "lib\PhotoOrganizer.zip;lib\openD.jar;lib\addressbook.jar;%JAIHOME%\jai_core.jar;%JAIHOME%\jai_codec.jar;%JAIHOME%\ext\mlibwrapper_jai.jar;%JMAIL%;%JAF%;%XBOXHOME%\lib\xbox.zip;%XMLPARSER%" -Djdbc.drivers=sun.jdbc.odbc.JdbcOdbcDriver:com.ms.jdbc.odbc.JdbcOdbcDriver -DUSE_JAI -DR-PhotoOrganizer.home=.\ photoorganizer.PhotoOrganizer
