(
var screenWidth = Window.screenBounds.width, screenHeight = Window.screenBounds.height;
var wDummy = Window().front.close; // to prevent main window to start hidden behind sclang
var version = "0.3.0";
var wGeluidKrasser = Window("GeluidKrasser v" ++ version, Rect(0,0,screenWidth,screenHeight)).background_(Color.grey(0.8,0.2));
var geluidKrasser = Array.newClear(4);
var instances = [0,1,2,3];
var bufferLengths = [20, 20, 20, 20];

MIDIIn.connectAll;

instances.do { arg id;
	geluidKrasser[id] = GeluidKrasser.new(
		id,
		s,
		wGeluidKrasser,
		bufferLengths[id],
		false, // showMidi
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