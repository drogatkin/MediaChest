@echo off
set JAVA_HOME=C:\Program Files\Java\j2sdk1.5.0
set MEDIACHEST_HOME=C:\My Projects\PhotoOrganizer
rem 
"%JAVA_HOME%\bin\java" -cp "%MEDIACHEST_HOME%\lib" photoorganizer.formats.StrippedJpeg %1 %2 %3
