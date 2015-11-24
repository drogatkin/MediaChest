           MediaChest version 2.2 build 211 update
           ================================================

Thank you for downloading the Open Source release of the MediaChest.
New H2 support is added read doc/datamigration.txt

  1. The purpose of the program.
  2. How to install the program.
  3. The status of the program.
  4. The distribution status of the program.
  5. How to contact the author in a case of questions or problems.

     The purpose of the program
     ~~~~~~~~~~~~~~~~~~~~~~~~~~
  The MediaChest is a powerful organizer of multimedia data on your computer.
  In addition, it provides sharing capabilities as e-mailing, ftping, httping, 
  nntping for organized multimedia files. The program is capable to show and use
  in categorization a specific information saved in auxiliary headers of
  multimedia files, like Exif or ID3 tags. MediaChest supports also additional
  header formats provided by QuickTime or some other digital cameras or
  photo editing software. You can find many competitive products like
  Thumb Plus, PIE, MusicMatch, Windows 8, MediaCenter 9/10,
  iTunes, GTKPod, or PhotoAlbum, so MediaChest is just another one. 
  The basic feature set includes:
    - Displaying exposure, and other information about a picture added by
      a digital camera. 
    - Showing an original thumbnails prepared by a camera and a full resolution
      image in JPEG or TIFF formats, or raw for some vendors as well. 
    - Loss-less image transformation, like rotation, mirroring, crop and others
      (including Exif transformation) for JPEG format only.
    - Adding commentaries to JPEG format images. 
    - Manipulation with a digital camera specific information, as export,
      import, and editing. 
    - Image and media files renaming/copying based on a customizable rename
      mask + topic.
    - Creation image and music collections using as a backend any JDBC compatible
      database. 
    - Creation HTML pages for picture and music presentations and web
      publishing. 
    - Export collections to CD and other medias.
    - Complete web publishing including FTP, and HTTP upload.
    - Sending thumbnails and images, and/or music collections to E-Mail
      recipients (SMTP) or to news groups (NNTP).
    - Upload images to web printshops.
    - Printing pictures using different layouts and sizes. 
    - View properties of ID3/QuickTime tags,
    - Playback of MP3/AAC/QuickTime/OGG/FLAC/APE/Wavpack/ALAC/DSD/SACD+DST files including
      32/192 and 1/5644800 support.
    - Edit ID3 tags with possibility adding album artwork and uploading to iPod photo/video.
    - Building play lists using different criteria, magic(smart) play lists.
    - Complete drag & drop support.
    - Can be operated by a remote control as irMan or similar.
    - Copy music/audio/pictures files to/from iPod with folders.
    - Support iTunes database, playlists, smart playlists, on-the-go lists, and more.
  This program offers also some unique features, like extracting a camera
  specific information and then restore it in an image, if such information
  was lost as a result of an editing of an image by a photo editing software.
  MediaChest is 100% Java application, so you can likely run it on
  any computer, and on any OS.

     How to install the program
     ~~~~~~~~~~~~~~~~~~~~~~~~~~
  This version is distributed as a ZIP file. Unzip this file somewhere on your
  hard disk with saving directories structure and launch script file corresponding
  to yours OS. A web start based installation is available directly from your browser.

     Windows executable version:
     ~~~~~~~~~~~~~~~~~~~~~~~~~~~
  Windows executable starter is planned for version 2.3.

     Pure Java version (any platform where Java is available):
     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  If you don't have preinstalled Java VM 1.8 or better on you computer, then
  you have to do that. You can download the latest Oracle's JRE 1.8 from
  http://java.oracle.com/ for your OS. MediaChest is also running on all Java flavors
  on Linux systems. Since Oracle dropped supporting Java on Windows lower than XP,
  you can consider to migrate to Linux with many benefits including MediaChest support.

  If you want to use advanced features of the program, such as sending your 
  albums or selections by e-mail, then some additional downloads can be required.

  Now, you're ready to launch the MediaChest. Since, the current version
  doesn't include an installation software, you might need to modify
  batch/script file by specifying a correct path to runtime class libraries,
  and JVM. Note, you can use web started version, just paste the link:
  http://mediachest.sf.net/mediachest/MediaChest.jnlp
  in your browser and accept security warnings.
  If you are going using E-Mail feature, then you have to specify
  a correct class path for Java Mail API, and Activation Framework class
  libraries. For remote control operation you need to have a communication
  port Java library installed for your platform.
  NOTE: that mentioned libraries are supplied with this distribution.
  If extra libraries installed as extensions of JDK 1.8, then you can
  launch the program by typing "java -jar MediaChest.jar", otherwise use
  MediaChest.bat on Windows platforms, and ./MediaChest on Unix platforms.
  Note that you may need to use chmod to allow MediaChest to execute after
  inflating.

    The status of the program
    ~~~~~~~~~~~~~~~~~~~~~~~~~
  This program is freeware and open source. Since the author supports only
  features interesting for himself, it prevents moving the program to
  shareware.
  The Web site where you can find more information about this program, including
  the latest build, is: http://mediachest.sourceforge.net/ .
  This site is a member of the Open Source Java ring. The program was reviewed
  by JARS (top 5%) and added to Sourcebank.

    The distribution status of the program
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  MediaChest is freely distributable software. Use it, make it better,
  give it to your friends.
   
    Known problems
    ~~~~~~~~~~~~~~
  Playback with option play in new window checked off can work incorrectly.
  Use menubar player by unchecking the option in Tools/Options/Media (MP3)
      --------------
 
    Licensing
    ~~~~~~~~~
  BSD like license agreement has to be accepted before using and modifying this
  program as in source or binary forms. The program includes portions of software
  under GPL, BSD, MPL, and EPL licenses, so you may also compliane with terms of
  the licenses.

    How to contact the author in the cases of questions or problems
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  Feel free to contact Dmitriy Rogatkin the author of the MediaChest,
  when you have any questions, suggestions, or bug reports.
  Use e-mail: jaddressbook@gmail.com .
  Visit also web site, http://mediachest.sourceforge.net/, for obtaining
  the latest information.

# $Id: README.TXT,v 1.51 2014/06/27 06:45:20 cvs Exp $
