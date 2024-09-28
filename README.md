## Introduction
GeluidKrasser is a quadruple live sampler. Four buffers to record live audio to play back independently using MIDI. Controls include startposition, length and speed of the playback, volume and panning. Audio and MIDI settings can be configured per sampler by clicking on the tiny keyboard icon.
Added in version 2:
* Loading samples from specified folders on disk
* Dynamic buffer lengths determined by the recording length
* Simple skin functionality: Dark, Grey, Pink

## Installation
1. Place this folder where you want it to be.
2. Download and install SuperCollider 3.13 from the official website: https://supercollider.github.io/downloads.
3. Move all the files and folders in Support_files to ~/Library/Application Support/SuperCollider. (Your library folder might be hidden; you get there in the Finder by clicking on the Go menu and then press the Alt key; an extra menu item 'Library' should be added to the list.) If this SuperCollider folder does not exist, create it. If one of the files or folders already exist and you did not make any changes to them that you want to keep, you can override them.
4. Move the file GeluidKrasser.sc from Extensions into ~/Library/Application Support/SuperCollider/Extensions/.
5. You can delete the Extensions and Support_files folders.
6. Start SuperCollider.

## Configuration
1. Open ~/Library/Application Support/SuperCollider/startup.scd in the SuperCollider editor.
2. If you want to start GeluidKrasser immediately when starting SuperCollider, look for the line with "~/Desktop/GeluidKrasser/GeluidKrasserStart.scd", remove the // before it, and change the path to the location of your GeluidKrasserStart.scd file. Hit save. Next time you start SuperCollider, GeluidKrasser will be automatically loaded.
3. Alternatively, if you want to start GeluidKrasser from the SuperCollider editor, open the GeluidKrasserStart.scd file, put the cursor somewhere between the outer brackets () and hit Apple-enter. The program is executed and GeluidKrasser starts.
4. SuperCollider can only use an audio interface that is set as default for input and output in the OS X Audio Midi Setup. Make sure that is the case.
5. To configure your audio interface in GeluidKrasser, quit SuperCollider, connect the interface and start it again. Open startup.scd again (from ~/Library/Application Support/SuperCollider/). In the postwindow (on your right, where all kinds of system messages appear), look for the line 'All available audio devices'. This should also list your audio interface. Copy its name and use it as device1 (which by default is MOTU UltraLite mk3 Hybrid). You can add up to three audio devices. Now restart SuperCollider.
6. Some settings can be adjusted at the top of the GeluidKrasserStart.scd file:
* bufferlenghts: set length of the buffer for each sampler in seconds
* showMidiInput (true or false): whether to show MIDI input messages in the post window
* sampleFolderPaths: the path to the folders to load samples from, in alphabetical order; if the samples are too long for the buffer, only a part will be loaded; samples can be mono or stereo; MIDI notes that trigger sample loading can be specified in the settings under the tiny keyboard icon in the interface.
* resizeBufferAfterRecZone (true or false): to switch on or off resizing of the buffer triggered by the recording length
* resetBufferAfterSampleLoading (true or false): whether the sample buffer should be reset to its original length after loading a sampe from disk
* clearBufferBeforeRecording (true or false): whether to clear out the buffer before recording
* skin (\Grey, \Dark or \Pink): setting on of three skins




