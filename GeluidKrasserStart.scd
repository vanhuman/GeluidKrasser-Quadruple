(
s.waitForBoot {

	////////////////////////////////////////////////////////////////////////////////////////////
	// User configuration
	////////////////////////////////////////////////////////////////////////////////////////////

	// numberOfInstances: a number between 1 and 6 to indicate the number of sample players
	// If this is more than 4, showBufferView is set to false.
	var numberOfInstances = 4;

	// bufferLengths: length of the buffers, in seconds
	// Make sure you enter as many numbers as there are instances.
	var bufferLengths = [20, 20, 20, 20, 20, 20];

	// sampleFolderPaths: a full path to a folder with samples to load; one for each player
	// Samples in the folders will be read alphabetically.
	// Make sure you enter as many paths as there are instances.
	var sampleFolderPaths = [
		"/Users/Robert/Desktop/gk_samples/*",
		"/Users/Robert/---data---/Temp/Zaal100/*",
		"/Users/Robert/Desktop/gk_samples/*",
		"/Users/Robert/Desktop/gk_samples/*",
		"/Users/Robert/Desktop/gk_samples/*",
		"/Users/Robert/Desktop/gk_samples/*",
	];

	// showMidiInput: when true, all MIDI input will be shown in the Post window
	var showMidiInput = false;

	// resizeBufferAfterRecZone: when true, the buffers will be resized to the time in seconds the recording has been running
	//     after recording has been stopped
	var resizeBufferAfterRecZone = true;

	// resetBufferAfterSampleLoading: when true, the buffer will be reset after loading a sample t
	// It will be reset to its value as indicated in the variable bufferLengths.
	var resetBufferAfterSampleLoading = true;

	// clearBufferBeforeRecording: when true, the buffer will be emptied when starting recording
	var clearBufferBeforeRecording = false;

	// skin: this will determine the look of the sample players
	var skin = \Dark;  // one of \Grey, \Dark, \Pink

	// pitchShiftIncludesBackwards: when true, the lower values for the pitchshift controller will play the sample backwards
	// This comes with the cost of being a bit more clicky during looping.
	var pitchShiftIncludesBackwards = true;

	// showBufferView: when true, the recorded or loaded sample is shown in a bufferview, when false, it is not
	// If numberOfInstances is more than 4, showBufferView is set to false.
	var showBufferView = true;



	////////////////////////////////////////////////////////////////////////////////////////////
	// Any changes to the code below is at your own risk
	////////////////////////////////////////////////////////////////////////////////////////////

	var screenWidth = Window.screenBounds.width, screenHeight = Window.screenBounds.height;
	var wDummy = Window().front.close; // to prevent main window to start hidden behind sclang
	var version = "4";
	var wGeluidKrasser = Window("GeluidKrasser v" ++ version, Rect(0,0,screenWidth,screenHeight)).background_(Color.grey(0.8,0.2));
	var geluidKrasser = Array.newClear(6);
	var skins = (
		Grey: (
			backgroundWindow: Color.new255(192, 192, 192),
			labels: Color.black,
		),
		Dark: (
			backgroundWindow: Color.new255(19, 19, 19),
			labels: Color.white,
		),
		Pink: (
			backgroundWindow: Color.new255(255, 192, 203),
			labels: Color.black,
		)
	);
	var error = false;

	if (numberOfInstances > 6, {
		"ERROR: number of instances cannot be more than 6".postln;
		error = true;
	});
	if (bufferLengths.size < numberOfInstances, {
		"ERROR: number of entries in variable bufferLengths should be at least numberOfInstances".postln;
		error = true;
	});
	if (sampleFolderPaths.size < numberOfInstances, {
		"ERROR: number of entries in variable sampleFolderPaths should be at least numberOfInstances".postln;
		error = true;
	});

	if(error.not) {
		MIDIIn.connectAll;

		if (numberOfInstances > 4, {showBufferView = false});

		numberOfInstances.do { arg id;
			geluidKrasser[id] = GeluidKrasser.new(
				id,
				s,
				wGeluidKrasser,
				bufferLengths[id],
				showMidiInput,
				sampleFolderPaths[id],
				resizeBufferAfterRecZone,
				resetBufferAfterSampleLoading,
				clearBufferBeforeRecording,
				skins[skin],
				pitchShiftIncludesBackwards,
				showBufferView,
				numberOfInstances,
			);
		};

		wGeluidKrasser.view.keyDownAction = {
			arg view, char, modifiers, unicode, keycode, key;
			if (keycode == 37) { // L
				numberOfInstances.do { arg id;
					geluidKrasser[id].clearBuffer();
				};
			};
		};

		wGeluidKrasser.onClose = {
			numberOfInstances.do { arg id;
				geluidKrasser[id].cleanUp();
			};
		};
		wGeluidKrasser.front();
	}
}
)

/*
Wensen:
	stop/start playback
	samples opslaan naar disk, met folder aan te geven (evt zelfde als load folder)
*/