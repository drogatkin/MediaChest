set JAVA_HOME=C:\Program Files\Java\jdk1.6.0_03
set JAIHOME=%JAVA_HOME%\jre\lib\ext

start %JAVA_HOME%\bin\javaw -cp lib\MediaChest.jar;lib\aldan3.jar;lib\mp3.jar;lib\mediautil.jar;lib\jl.jar;lib\addressbook.jar;lib\mail.jar;lib\activation.jar;lib\comm.jar;lib\kunststoff.jar;%JAIHOME%\jai_core.jar;%JAIHOME%\jai_codec.jar;%JAIHOME%\mlibwrapper_jai.jar -Djdbc.drivers=sun.jdbc.odbc.JdbcOdbcDriver:com.ms.jdbc.odbc.JdbcOdbcDriver -DMediaChest.home=.\data -DR-AddressBook.home=.\data -DUSE_JAI -DJAI_IMAGE_READER_USE_CODECS photoorganizer.PhotoOrganizer
