(
// User configuration
var bufferLengths = [20, 20, 20, 20]; // length of the 4 buffers, in seconds
var showMidiInput = false;
var sampleFolderPaths = [
	// location of samples, for each player, samples in the folders will be read alphabetically
	"/Users/Robert/Desktop/gk_samples/*",
  	"/Users/Robert/---data---/Temp/Zaal100/*",
	"/Users/Robert/Desktop/gk_samples/*",
	"/Users/Robert/Desktop/gk_samples/*",
];
var resizeBufferAfterRecZone = true;
var resetBufferAfterSampleLoading = true;
var clearBufferBeforeRecording = false;
var color = \Dark;  // one of \Grey, \Dark, \Pink

// Any changes to the code below is at your own risk
var screenWidth = Window.screenBounds.width, screenHeight = Window.screenBounds.height;
var wDummy = Window().front.close; // to prevent main window to start hidden behind sclang
var version = "2.0";
var wGeluidKrasser = Window("GeluidKrasser v" ++ version, Rect(0,0,screenWidth,screenHeight)).background_(Color.grey(0.8,0.2));
var geluidKrasser = Array.newClear(4);
var instances = [0,1,2,3];
var colors = (
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

MIDIIn.connectAll;

instances.do { arg id;
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
		colors[color],
	);
};

wGeluidKrasser.view.keyDownAction = {
	arg view, char, modifiers, unicode, keycode, key;
	if (keycode == 37) { // L
		instances.do { arg id;
			geluidKrasser[id].clearBuffer();
		};
	};
};

wGeluidKrasser.onClose = {
	instances.do { arg id;
		geluidKrasser[id].cleanUp();
	};
};
wGeluidKrasser.front();
)

