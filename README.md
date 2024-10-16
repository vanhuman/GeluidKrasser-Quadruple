# Introduction
GeluidKrasser is a live sampler with 1 to 6 independent sample players. Each player has a buffer to record live audio or to load sample files from disk. Sound in the buffer can be played back controlled by onscreen controls or using MIDI. Controls include startposition, length and speed of the playback, volume and panning. Audio and MIDI settings can be configured per sampler. A bufferview shows the recorded or loaded sample.

### New in version 4
* GeluidKrasser is extended from 4 to 6 players. Also the user can now configure any number of players between 1 and 6.
* When more than 4 players are configured, the bufferview is automatically switched off to save space on the screen.
* The bufferview can also be switched off independently.

### New in version 3
* The sample players can be configured to play backwards with lower values for the speed controller.

### New in version 2
* Loading samples from specified folders on disk, triggered by MIDI notes as set in the settings.
* Dynamic buffer lengths determined by the recording length; after stopping the recording the buffer will be set to the length of the recording, all controls will be limited to this new buffer length. This feature can be switched off.
* Simple skin functionality: Dark, Grey, Pink.

# Installation
1. Download and install SuperCollider 3.13 from the official website: https://supercollider.github.io/downloads.
2. Download the application folder from https://github.com/vanhuman/GeluidKrasser-Quadruple/archive/refs/heads/development.zip, unzip it and place it where you want it to be.
3. In the application folder there are two folders: Support_files and Extensions. Move all the files and folders from the Support_files folder to ~/Library/Application Support/SuperCollider ( ~ indicates your user home folder). Your ~/Library folder might be hidden; you get there in the Finder by clicking on the Go menu and then press the Alt key; an extra menu item 'Library' should be added to the list. If this SuperCollider folder does not exist, create it. If one of the files or folders already exist and you did not make any changes to them that you want to keep, you can override them.
4. Move the file GeluidKrasser.sc from Extensions into ~/Library/Application Support/SuperCollider/Extensions/.
5. Now you can delete the Support_files and Extensions folders from the application folder.
6. Start SuperCollider.

## Configuration
1. Open ~/Library/Application Support/SuperCollider/startup.scd in the SuperCollider editor. You can do this easily from the File menu in Supercollider with 'Open startup file'.
2. If you want to start GeluidKrasser immediately when starting SuperCollider, look for the line with "~/Desktop/GeluidKrasser/GeluidKrasserStart.scd", remove the // before it, and change the path to the location of your GeluidKrasserStart.scd file. Hit save. Next time you start SuperCollider, GeluidKrasser will be automatically loaded.
3. Alternatively, if you want to start GeluidKrasser from the SuperCollider editor, open the GeluidKrasserStart.scd file, put the cursor somewhere between the outer brackets () and hit Apple-enter. The program is executed and GeluidKrasser starts.
4. SuperCollider can only use an audio interface that is set as default for input and output in the OS X Audio Midi Setup. Make sure that you set the correct audio interface there.
5. To configure your audio interface in GeluidKrasser, quit SuperCollider, connect the interface and start it again. Open startup.scd again (from ~/Library/Application Support/SuperCollider/). In the postwindow (on your right, where all kinds of system messages appear), look for the line 'All available audio devices'. This should also list your audio interface. Copy its name and use it as device1 (which by default is 'MOTU UltraLite mk3 Hybrid'). You can add up to three audio devices. Now restart SuperCollider.
6. MIDI and audio mapping can be done in the settings under the tiny keyboard icon after you've launched GeluidKrasser.
7. Some settings can be adjusted at the top of the GeluidKrasserStart.scd file. The settings are documented there.




