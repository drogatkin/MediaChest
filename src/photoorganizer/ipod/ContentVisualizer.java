/* MediaChest - ContentVisualizer.java
 * Copyright (C) 1999-2005 Dmitriy Rogatkin.  All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  $Id: ContentVisualizer.java,v 1.9 2008/01/22 06:31:00 dmitriy Exp $
 * Created on Jan 19, 2005
 */

package photoorganizer.ipod;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import javax.swing.JOptionPane;
import javax.servlet.http.HttpSession;

import mediautil.gen.BasicIo;

import org.aldan3.util.TemplateEngine;

import photoorganizer.Controller;
import photoorganizer.formats.MP3;

/**
 * @author dmitriy
 *
 * 
 */
public class ContentVisualizer {
    public static final String TL_TITLE = "title";
    public static final String TL_IMAGEPATH = "imagepath";
    public static final String TL_SETS = "sets";
    public static final String TL_DISKS = "disks";
    public static final String TL_TRACKS = "tracks";
    public static final String TL_PLAYTIME = "playtime";
    public static final String TL_SIZE = "size";
    public static final String TL_ARTISTS = "artists";
    public static final String TL_ALBUMS = "albums";
    public static final String TL_SONGS = "songs";
    public static final String TL_COMPOSER = "composer";
    public static final String TL_COMMENT = "comment";
    public static final String TL_GENRE = "genre";
    public static final String TL_CHARSET = "charset";
    public static final String TL_DATE = "date";
    public static final String TL_USER = "user";
    
    public ContentVisualizer(Controller controller, ITunesDB itunes,
            String template, Writer outwriter) throws IOException {
        if (outwriter == null) {
            JOptionPane.showMessageDialog(null, "No writer");
            return;
        }
        Map tunesMap = new HashMap();
        tunesMap.put(TL_TITLE, "iPod Contents");
        tunesMap.put(TL_USER, controller.getPrefs().getProperty(Controller.REGISTER, Controller.NAME));
        Map<String, Map<String, List<PlayItem>>> artists = new HashMap<String, Map<String,List<PlayItem>>>();
        long totalTime = 0;
        long totalSize = 0;
        int disks = 0;
        int tracks = 0;
        int sets = 0;
        // tunesMap.put(TL_UNCLASSIFIED,
        // tunesMap.put(TL_VARIOUS_ARTISTS,
        ITunesDB.PlayDirectory pd = itunes.directories[2];
        assert pd.descriptor.selector == PlayItem.ALBUM;
        
        for (int ia=0; ia<pd.size(); ia++) {
            PlayList pl = pd.get(ia);
            assert pl.isVirtual() && pl.isFileDirectory() == false;
            for (PlayItem pi:pl) {
                String artist = (String)pi.get(PlayItem.ARTIST);
                if (artist == null) {
                    // add to unclassified list of artists
                } else {
                    if ((Boolean)pi.get(PlayItem.COMPILATION)) {
                        // add to various artists
                    } else {
                        Map<String, List<PlayItem>> albums = artists.get(artist);
                        if (albums == null)
                            artists.put(artist, albums = new HashMap<String,List<PlayItem>>());
                        // add entire album and skip?, no we need stats
                        List<PlayItem> album = albums.get(pl.toString());
                        if (album == null)
                            albums.put(pl.toString(), album = new ArrayList<PlayItem>());
                        album.add(pi);
                    }
                }
            }
        }
        // re-map
        List<Map> artistList = new ArrayList<Map>();
        for (String artistName:artists.keySet()) {
            Map artistMap = new HashMap<String,Object>();
            artistMap.put(TL_TITLE, artistName);
            artistMap.put(TL_SETS, sets++);
            List<Map> albumList = new ArrayList<Map>();
            Map<String, List<PlayItem>> albums = artists.get(artistName);
            int artistTime = 0;
            int artistTracks = 0;
            int artistDisks = 0;
            for(String albumName:albums.keySet()) {
                Map<String,Object> albumMap = new HashMap<String,Object>();
                albumMap.put(TL_TITLE, albumName);
                albumMap.put(TL_DISKS, disks++);
                List<Map> songList = new ArrayList<Map>();
                List<PlayItem> songs = albums.get(albumName);
                int albumTracks = 0;
                int albumTime = 0;
                for (PlayItem song:songs) {
                    Map<String,Object> songMap = new HashMap<String,Object>();
                    songMap.put(TL_TITLE, song.get(PlayItem.TITLE));
                    songMap.put(TL_COMPOSER, song.get(PlayItem.COMPOSER));
                    songMap.put(TL_COMMENT, song.get(PlayItem.COMMENT));
                    songMap.put(TL_GENRE, song.get(PlayItem.GENRE));
                    songMap.put(TL_TRACKS, song.get(PlayItem.ORDER));
                    songMap.put(TL_PLAYTIME, MP3.convertTime(((Integer)song.get(PlayItem.LENGTH))/1000));
                    songList.add(songMap);
                    tracks++;
                    totalSize += (Integer)song.get(PlayItem.SIZE);
                    totalTime += (Integer)song.get(PlayItem.LENGTH);
                    albumTime += (Integer)song.get(PlayItem.LENGTH);
                    albumTracks++;
                }
                albumMap.put(TL_SONGS, songList);
                albumMap.put(TL_PLAYTIME, MP3.convertTime(albumTime/1000));
                albumMap.put(TL_TRACKS, albumTracks);
                artistDisks++;
                artistTracks += albumTracks;
                artistTime += albumTime;
                albumList.add(albumMap);
            }
            artistMap.put(TL_ALBUMS, albumList);
            artistMap.put(TL_PLAYTIME, MP3.convertTime(artistTime/1000));
            artistMap.put(TL_TRACKS, artistTracks);
            artistMap.put(TL_DISKS, artistDisks);
            artistList.add(artistMap);
        }
        tunesMap.put(TL_ARTISTS, artistList); // level artists
        tunesMap.put(TL_DISKS, disks);
        tunesMap.put(TL_SETS, sets);
        tunesMap.put(TL_PLAYTIME, MP3.convertTime(totalTime/1000));
        tunesMap.put(TL_TRACKS, tracks);
        tunesMap.put(TL_SIZE, BasicIo.convertLength(totalSize));
        tunesMap.put(TL_CHARSET, "utf-8");
        tunesMap.put(TL_DATE, new Date());
        
        new TemplateEngine().process(outwriter, template.toCharArray(), 0,
                template.length(), tunesMap, (HttpSession)null, new Properties(), null, null);        
    }

}
