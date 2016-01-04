title: How I manage my music on Linux
date: 2016-01-04 19:14:57
tags:
---

I wanted to take the time to talk about how I manage my music on Linux. I use the following tools:

* [`morituri`](http://thomas.apestaart.org/morituri/trac/), to rip CDs,
* [`beets`](http://beets.radbox.org/), to manage my music library,
* [`mopidy`](http://beets.radbox.org/), as a music server,
* [`ncmpcpp`](http://rybczak.net/ncmpcpp/), as a music server client,
* [`SSHelper`](http://arachnoid.com/android/SSHelper/) and [`rsync`](https://rsync.samba.org/), to sync music to my phone.

![ncmpcpp with mopidy as a server, listing artists, albums and tracks](/2016/how-i-manage-my-music/ncmpcpp.png)

<!-- more -->


# Ripping a CD: morituri

I use [morituri](http://thomas.apestaart.org/morituri/trac/) to rip CDs.

I just got a CD with a book I bought, *Her name is Calla*'s album *Navigator*. I will use it as an example in this blog post.

## Finding the drive's offset

You only have to do this once (for each CD drive). Insert a CD in your drive, then run `rip offset find`. Use a wellknown album: *Her name is Calla*'s album was "not found in AccurateRip database", so I tried again with *Franz Ferdinand*'s album of the same name.

```bash
$ rip offset find
Checking device /dev/cdrom
Album not found in AccurateRip database. 
$ # Changing the CD to something that might have a better chance of being included in the database
$ rip offset find
Checking device /dev/cdrom
Trying read offset 6 ...                      
Trying read offset 48 ...                     
Trying read offset 102 ...                    
Offset of device is likely 102, confirming ...
                                              
Read offset of device is: 102.
```

## Ripping a CD to FLAC

We wil rip the CD to [FLAC](https://en.wikipedia.org/wiki/FLAC), a loss-less open-source codec. *102* is the offset we found above.

```bash
$ mkdir Her_Name_Is_Calla_-_Navigator
$ cd Her_Name_Is_Calla_-_Navigator
$ rip cd rip --offset 102
Checking device /dev/sr0
CDDB disc id: a60e080c                        
MusicBrainz disc id mxCOjvkQ7UxZIWQf6._MXdeNdk0-
MusicBrainz lookup URL http://mm.musicbrainz.org/bare/cdlookup.html?toc=1+12+269576+150+14387+33156+41485+63566+85189+123198+148717+154783+186693+199010+251815&tracks=12&id=mxCOjvkQ7UxZIWQf6._MXdeNdk0-
Disc duration: 00:59:52.346, 12 audio tracks
Error: NotFoundException(ResponseError(),)
Continuing without metadata
Submit this disc to MusicBrainz at the above URL.
```

Eh, what's going on here? Well, `morituri` tries to find the CD on [MusicBrainz](http://http://musicbrainz.org) to automatically tag it. MusicBrainz is like Wikipedia, but for bands, albums, and other music releases.. In this case, the CD was not recognized, and it's suggested we add it to MusicBrainz ourselves.

After logging in, we can continue the process of creating the CD on MusicBrainz.

![The screen we see after logging in to MusicBrainz](musicbrainz-cd-search-by-artist.png)

![Searching for an artist](musicbrainz-cd-search-by-artist-entered.png)

![Selecting the correct artist](musicbrainz-cd-search-select-artist.png)

![None of the release match the album we're looking for, we'll add a new one](musicbrainz-cd-search-add-new-release.png)

![Adding a new release is a matter of filling out some forms](musicbrainz-cd-search-creating-new-release.png)

Now that we created the release, our ripping should go fine. I had to click the link one more time to link the TOC of the CD to the release, but everything went smoothly after that.

![Just started ripping](rip-started.png)

![The rip is compelete, and we are now left with a bunch of flac files](rip-complete.png)

Now that the rip is complete, we can import these files into `beets`.

# Importing and tagging files: beets for organisation

I use [beets](http://beets.radbox.org/) to manage my music library.

## Importing files into beets

After setting up a library, you can start to import music. You use `beet import` for this. As an example, let's import the album we ripped earlier.

```bash
$ beet import Her\ Name\ Is\ Calla\ -\ Navigator 

/home/thomas/tmp/Her_Name_Is_Calla_-_Navigator/album/Her Name Is Calla - Navigator (12 items)
Tagging:
    Her Name Is Calla - Navigator
URL:
    http://musicbrainz.org/release/8d1604ad-8351-4467-bbbc-3da68973a404
(Similarity: 100.0%) (CD, 2014)
```

As you can see, `beets` will also contact MusicBrainz, where it finds the album. I like to have all of my music backed by MusicBrainz, that way, I know the metadata is always correct.

The power of `beets` is correcting music. For example, if the artist name on the FLAC files was "her name is calla", `beets` would offer to correct this to `Her Name Is Calla`.

We can check that `beets` imported the album:

```bash
$ beet ls her name is calla
Her Name Is Calla - Navigator - I Was on the Back of a Nightingale
Her Name Is Calla - Navigator - The Roots Run Deep
Her Name Is Calla - Navigator - It's Called, 'Daisy'
Her Name Is Calla - Navigator - Ragman Roll
Her Name Is Calla - Navigator - Meridian Arc
Her Name Is Calla - Navigator - Navigator
Her Name Is Calla - Navigator - Burial
Her Name Is Calla - Navigator - A Second Life
Her Name Is Calla - Navigator - It Was Flood
Her Name Is Calla - Navigator - Whale Fall: A Journal
Her Name Is Calla - Navigator - Dreamlands
Her Name Is Calla - Navigator - Perfect Prime
```

Looks great! Now we actually want to listen to this. But first, I'll talk a little about my `beets` setup.

## beets setup

This is my `~/.config/beets/config.yaml`:

```yaml
directory: ~/music
library: ~/.beets_library.blb
import:
  copy: yes

paths:
  default: $albumartist - $album ($year)%aunique{}/$track $title

plugins: discogs fetchart missing lastimport lastgenre lyrics web thumbnails scrub convert

lastfm:
  user: siyck

lastgenre:
  count: 1

convert:
  copy_album_art: yes
  dest: /home/thomas/converted_music
```

### Music directory and library

I use the idiomatic `~/music` as a place to store my music. This is where `beets` will store my music. `~/.beets_library.blb` stores `beets`' music library (as a SQL database).

### Import style

`copy: yes` makes `beets` copy the files, instead of moving them. That way, nothing I'm working on suddenly disappears. Moving is obviously faster though, you might want to consider it.

### Path

The `paths` defines how albums will be stored in the music directory. I like the `Artist name - Album name (year)/Track number - Track name` convention, which is expressed by `$albumartist - $album ($year)%aunique{}/$track $title`. `$albumartist` is the artist responsible for the album (there can be tracks of different artists on one album), this keeps the album together. `%aunique` adds a unique identifier in case there are duplicate names.

### Plugins

Here are some of the plugins I use I wanted to highlight:

* `fetchart` will grab album covers from the web, and add them to your library
* `lastgenre` will get the genre for songs from [last.fm](https://last.fm)
* `lyrics` will get track lyrics from a variety of sources
* `scrub` will remove extra tags not used or added by `beets` from music files
* `convert` is used to convert music to other formats

### Convert

These settings are for when I'm converting music. I convert to MP3 to save space on my phone, I don't really need FLACs there. `copy_album_art` will copy album art (obviously), `dest` selects a destination folder.

# Playing music: mopidy as the music server

I use [mopidy](https://www.mopidy.com/) as a music player. I used to use plain old `mpd`, but `mopidy` is more flexible. I [installed it using `apt`](https://docs.mopidy.com/en/latest/installation/debian/).

## My configuration

```ini
[mpd]
hostname = ::

[scrobbler]
username = siyck
password = XXXXXXXXXXXXXXXXXXXXX

[local]
media_dir = /home/thomas/music
excluded_file_extensions =
  .m4a
  .directory
  .html
  .jpeg
  .jpg
  .log
  .nfo
  .png
  .txt

[soundcloud]
auth_token = X-XXXXX-XXXXXXXX-XXXXXXXXXXXXXXX
```

`hostname = ::` binds to all local addresses. The `[scrobbler]` setup is used to scrobble to [last.fm](https://last.fm).

In `[local]`, the music directory is defined. I also define some excludes: the defaults and `.m4a`, since the latest `mopidy` struggles to play those.

And lastly, an auth token to play music from Soundcloud.

## Making mopidy scan for new music in the library

`mopidy` isn't constantly watching the library for changes, we have to let it know about them. `mopidy local scan` does exactly that.

After scanning, restart `mopidy`:

```bash
$ killall mopidy
$ mopidy &
```

## Playing music: ncmpcpp

I use `ncmpcpp` as an `mpd` frontend. `mopidy` presents itself as an `mpd`-compatible server, which means other `mpd` clients work for it too.

I'm not going to fully introduce `ncmpcpp` here, but you can switch to the list of albums by pressing `4`, then use the mouse or arrow keys to navigate. Searching is simple too: `/`, your query, and enter. You can play tracks by pressing enter, and queue them using `space`. For more on `ncmpcpp`, check out the [Arch Wiki](https://wiki.archlinux.org/index.php/Ncmpcpp).

![Playing music through ncmpcpp](ncmpcpp.png)

### ncmpcpp config

One thing that annoys me, is that `ncmpcpp` sorts by `artist` by default, and not `albumArtist`. You can change this in the Media Library view, but to change the default, edit `~/.ncmpcpp/config`:

```bash
media_library_primary_tag = "album_artist"
```

## Control through Android: MPDroid

`mopidy` is `mpd`-compatible. This means that you can use most `mpd` clients to control it. For example, I control my laptop's `mopidy` remotely using [MPDroid](https://play.google.com/store/apps/details?id=com.namelessdev.mpdroid).

![Controlling remote mopidy on Android with MPDroid](mpdroid.png)

# Syncing to my phone

I don't always have my laptop with me, but I don't want to be musicless during those times. I carry my phone with me pretty much all the time, so wouldn't it be an idea to put music on that?

## Converting

Music is converted using the `beet convert` command. I already talked about the configuration of that command earlier. Now we're actually going to use it. `beet convert` will convert all the files in your library, this may take a very long time. For now, we will convert a small subset.

Using `beet ls`, we check what files will be selected for a query. We try out the query `calla`, this will match tracks in which the word `calla` appears:

```bash
$ beet ls calla                                            
Her Name Is Calla - Navigator - I Was on the Back of a Nightingale
Her Name Is Calla - Navigator - The Roots Run Deep
Her Name Is Calla - Navigator - It's Called, 'Daisy'
Her Name Is Calla - Navigator - Ragman Roll
Her Name Is Calla - Navigator - Meridian Arc
Her Name Is Calla - Navigator - Navigator
Her Name Is Calla - Navigator - Burial
Her Name Is Calla - Navigator - A Second Life
Her Name Is Calla - Navigator - It Was Flood
Her Name Is Calla - Navigator - Whale Fall: A Journal
Her Name Is Calla - Navigator - Dreamlands
```

Looks reasonable. Let's convert them!

```bash
$ beet convert calla
Her Name Is Calla - Navigator - I Was on the Back of a Nightingale
Her Name Is Calla - Navigator - The Roots Run Deep
Her Name Is Calla - Navigator - It's Called, 'Daisy'
Her Name Is Calla - Navigator - Ragman Roll
Her Name Is Calla - Navigator - Meridian Arc
Her Name Is Calla - Navigator - Navigator
Her Name Is Calla - Navigator - Burial
Her Name Is Calla - Navigator - A Second Life
Her Name Is Calla - Navigator - It Was Flood
Her Name Is Calla - Navigator - Whale Fall: A Journal
Her Name Is Calla - Navigator - Dreamlands
Her Name Is Calla - Navigator - Perfect Prime
Convert? (Y/n) 
convert: Encoding /home/thomas/music/Her Name Is Calla - Navigator (2014)/04 Ragman Roll.flac
convert: Encoding /home/thomas/music/Her Name Is Calla - Navigator (2014)/01 I Was on the Back of a Nightingale.flac
convert: Encoding /home/thomas/music/Her Name Is Calla - Navigator (2014)/05 Meridian Arc.flac
convert: Encoding /home/thomas/music/Her Name Is Calla - Navigator (2014)/02 The Roots Run Deep.flac
convert: Encoding /home/thomas/music/Her Name Is Calla - Navigator (2014)/06 Navigator.flac
convert: Encoding /home/thomas/music/Her Name Is Calla - Navigator (2014)/07 Burial.flac
convert: Encoding /home/thomas/music/Her Name Is Calla - Navigator (2014)/08 A Second Life.flac
convert: Encoding /home/thomas/music/Her Name Is Calla - Navigator (2014)/03 It's Called, 'Daisy'.flac
convert: Finished encoding /home/thomas/music/Her Name Is Calla - Navigator (2014)/08 A Second Life.flac
convert: Encoding /home/thomas/music/Her Name Is Calla - Navigator (2014)/09 It Was Flood.flac
convert: Finished encoding /home/thomas/music/Her Name Is Calla - Navigator (2014)/03 It's Called, 'Daisy'.flac
convert: Encoding /home/thomas/music/Her Name Is Calla - Navigator (2014)/10 Whale Fall_ A Journal.flac
convert: Finished encoding /home/thomas/music/Her Name Is Calla - Navigator (2014)/01 I Was on the Back of a Nightingale.flac
convert: Encoding /home/thomas/music/Her Name Is Calla - Navigator (2014)/11 Dreamlands.flac
convert: Finished encoding /home/thomas/music/Her Name Is Calla - Navigator (2014)/02 The Roots Run Deep.flac
convert: Encoding /home/thomas/music/Her Name Is Calla - Navigator (2014)/12 Perfect Prime.flac
convert: Finished encoding /home/thomas/music/Her Name Is Calla - Navigator (2014)/05 Meridian Arc.flac
convert: Finished encoding /home/thomas/music/Her Name Is Calla - Navigator (2014)/10 Whale Fall_ A Journal.flac
convert: Finished encoding /home/thomas/music/Her Name Is Calla - Navigator (2014)/04 Ragman Roll.flac
convert: Finished encoding /home/thomas/music/Her Name Is Calla - Navigator (2014)/07 Burial.flac
convert: Finished encoding /home/thomas/music/Her Name Is Calla - Navigator (2014)/12 Perfect Prime.flac
convert: Finished encoding /home/thomas/music/Her Name Is Calla - Navigator (2014)/09 It Was Flood.flac
convert: Finished encoding /home/thomas/music/Her Name Is Calla - Navigator (2014)/06 Navigator.flac
convert: Finished encoding /home/thomas/music/Her Name Is Calla - Navigator (2014)/11 Dreamlands.flac
$ ls ~/converted_music/Her\ Name\ Is\ Calla\ -\ Navigator\ \(2014\)
01 I Was on the Back of a Nightingale.mp3  07 Burial.mp3
02 The Roots Run Deep.mp3                  08 A Second Life.mp3
03 It's Called, 'Daisy'.mp3                09 It Was Flood.mp3
04 Ragman Roll.mp3                         10 Whale Fall_ A Journal.mp3
05 Meridian Arc.mp3                        11 Dreamlands.mp3
06 Navigator.mp3                           12 Perfect Prime.mp3
```

Looks like it worked!

## Selecting

My phone is too small to carry my entire music library. That's why I make a selection of what music to put on it. I manage this in a directory `~/phone_music`, this is the directory that will be synced to my phone. All that this directory contains, is a bunch of symlinks to `~/converted_music/...` folders.

We will select the album from before by symlinking it:

```bash
~/phone_music$ ln -s ../converted_music/Her\ Name\ Is\ Calla\ -\ Navigator\ \(2014\)/
```

I wrote a little script to automate this the first time:

```bash
#!/bin/bash

for FILE in ../converted_music/*; do
  echo "$FILE y/n?";
  read CHOICE;
  if [ "$CHOICE" = "y" ]; then
    echo linking $FILE to $(basename "$FILE")
    BASENAME=$(basename "$FILE")
    ln -s "$FILE" "$BASENAME"
    echo "$FILE linked";
  fi;
done;
```

## rsync server on my phone: SSHelper

I tested out multiple solutions for syncing to my phone (Samba server on my laptop, USB OTG cable, mounting my phone...) but none were particulary reliable or handy. The solution I came up with was running a server on my phone, and rsyncing from my laptop.

[SSHelper](http://arachnoid.com/android/SSHelper/) goes on my Android. After running, I `rsync` into it from my laptop.

![SSHelper running](sshelper.png)

Now I run rsync to synchronize:

```bash
$ rsync -avzL --no-perms --no-times --size-only --info=progress2 -e 'ssh -p 2222' . 192.168.1.103:SDCard/Music

SSHelper Version 7.9 Copyright 2014, P. Lutus
Default password is "admin" (recommend: change it)
thomas@192.168.1.103's password: 
sending incremental file list
              0   0%    0.00kB/s    0:00:00 (xfr#0, ir-chk=1000/3131)
Her Name Is Calla - Navigator (2014)/
Her Name Is Calla - Navigator (2014)/01 I Was on the Back of a Nightingale.mp3
      4,208,841   0%    2.35MB/s    0:00:01 (xfr#1, ir-chk=1006/3162)
Her Name Is Calla - Navigator (2014)/02 The Roots Run Deep.mp3
     10,320,332   0%    1.44MB/s    0:00:06 (xfr#2, ir-chk=1016/3162)
Her Name Is Calla - Navigator (2014)/03 It's Called, 'Daisy'.mp3
     13,354,276   0%    1.39MB/s    0:00:09 (xfr#3, ir-chk=1015/3162)
Her Name Is Calla - Navigator (2014)/04 Ragman Roll.mp3
     19,973,344   0%    1.31MB/s    0:00:14 (xfr#4, ir-chk=1014/3162)
Her Name Is Calla - Navigator (2014)/05 Meridian Arc.mp3
     26,848,527   0%    1.26MB/s    0:00:20 (xfr#5, ir-chk=1013/3162)
Her Name Is Calla - Navigator (2014)/06 Navigator.mp3
     38,621,591   0%    1.25MB/s    0:00:29 (xfr#6, ir-chk=1012/3162)
Her Name Is Calla - Navigator (2014)/07 Burial.mp3
     46,411,725   0%    1.24MB/s    0:00:35 (xfr#7, ir-chk=1011/3162)
Her Name Is Calla - Navigator (2014)/08 A Second Life.mp3
     48,063,182   0%    1.25MB/s    0:00:36 (xfr#8, ir-chk=1010/3162)
Her Name Is Calla - Navigator (2014)/09 It Was Flood.mp3
     57,304,507   0%    1.24MB/s    0:00:44 (xfr#9, ir-chk=1009/3162)
Her Name Is Calla - Navigator (2014)/10 Whale Fall_ A Journal.mp3
     61,930,675   0%    1.16MB/s    0:00:50 (xfr#10, ir-chk=1008/3162)
Her Name Is Calla - Navigator (2014)/11 Dreamlands.mp3
     78,548,445   0%    1.11MB/s    0:01:07 (xfr#11, ir-chk=1007/3162)
Her Name Is Calla - Navigator (2014)/12 Perfect Prime.mp3
     83,451,398   0%    1.07MB/s    0:01:14 (xfr#12, ir-chk=1004/4276)

sent 83,384,321 bytes  received 2,601 bytes  1,048,892.10 bytes/sec
total size is 42,082,835,611  speedup is 504.67
```

The beautiful thing is that `rsync` is built to be robust: if it's interrupted, for any reason, you can just continue later.

# Listening on my phone

I used to use [Google Play Music](https://play.google.com/store/apps/details?id=com.google.android.music), but I've recently made the switch to [Poweramp](https://play.google.com/store/apps/details?id=com.maxmpz.audioplayer). I'm not sure if I'll continue using it. Play Music feels very intuitive and is a breeze to use.

# Keeping track: last.fm

[last.fm](http://www.last.fm/) may have a reputation as a dead site, I still use it to track my music.

## mopidy

`mopidy` has a scrobbling plugin aptly called [Mopidy-Scrobbler](https://github.com/mopidy/mopidy-scrobbler). Configuration is easy:

```bash
$ cat ~/.config/mopidy/mopidy.conf
...
[scrobbler]
username = siyck
password = XXXXXXXXXXXXXXXXXX 
...
```

## On my phone

On my Android phone, I use [Simple Last.fm Scrobbler](https://play.google.com/store/apps/details?id=com.adam.aslfms), a no-nonsense scrobbler. It features a cache, in which it stores scrobbles if you're offline, to scrobble them when you're back online.

# Legality

What we did here was ripping a CD for personal use. We also placed it on our phone. In Belgium, this is legal, as it falls under "private copies". For more information, see [here](http://economie.fgov.be/nl/ondernemingen/Intellectuele_Eigendom/auteursrecht/Bescherming_door_auteursrecht/privekopie/). Obviously, uploading this to the internet or a Bittorrent network is not legal, and not ethical either.

