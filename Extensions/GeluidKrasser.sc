GeluidKrasser {
	var id, server, win, bufferLength, showMidi;

	var sRate, buffer, audioIn, audioOut, midiChannel, midiNotePlay, midiNoteRec, midiCcVol, midiCcStart, midiCcLen, midiNoteToggle;

	var bufferViewBaseDelay;
	var volBus, lenBus, startPos, startPosPrev;
	var playButton, recButton, bufferView, volumeSlider;
	var numAudioInChannels, numAudioOutChannels;
	var playSynth, recSynth, spec, recZone, playZone;
	var midiNotePlayPopupLabel, midiNoteRecPopupLabel, midiCcVolPopupLabel, midiCcStartPopupLabel, midiCcLenPopupLabel;
	var fileBufferView, bufferViewSoundFile;
	var midiSources, midiSourcesUids, midiPortUid, midiPortIndex;
	var midiDefNoteOn, midiDefNoteOff, midiDefCc;
	var configFile;
	var fontSize, fontSizeNormal;

	*new {
		arg id = 0, server, win, bufferLength, showMidi;
		^super.newCopyArgs(id, server, win, bufferLength, showMidi).initGeluidKrasser;
	}

	initGeluidKrasser {
		this.initVars();
		this.addSynths();
		this.initOsc();
		this.initMidi();
		this.buildGui();
	}

	initVars {
		var bufferViewFolder, config;

		this.log("initialize variables", true);
		if (bufferLength.isNil) { bufferLength = 20 };
		if (showMidi.isNil) { showMidi = false };

		sRate = server.sampleRate;
		buffer = Buffer.alloc(server, sRate * bufferLength, 2);

		configFile = Archive.archiveDir ++ "/GeluidKrasserConfig.scd";
		config = configFile.load[id];
		midiPortUid = config[0];
		midiChannel = config[1];
		audioIn = config[2];
		audioOut = config[3];
		midiNotePlay = config[4];
		midiNoteRec = config[5];
		midiCcVol = config[6];
		midiCcStart = config[7];
		midiCcLen = config[8];
		midiNoteToggle = config[9];

		bufferViewBaseDelay = 0.07; // in seconds
		startPos = 0;
		startPosPrev = 0;
		spec = ();
		recZone = 1;
		playZone = 2;
		midiSources = List.newClear();
		midiSourcesUids = List.newClear();
		numAudioInChannels = Server.local.options.numInputBusChannels;
		numAudioOutChannels = Server.local.options.numOutputBusChannels;
		spec[\start] = Env.new([0.01, 1 * bufferLength], [1], \lin);
		spec[\len] = Env.new([0.01, 0.5 * bufferLength], [1], \exp);
		lenBus = Bus.control(server,1).set(0.1 * bufferLength);
		volBus = Bus.control(server,1).set(0.5);
		fileBufferView = SoundFile.new();
		fontSize = 10;
		fontSizeNormal = 12;

		bufferViewFolder = Archive.archiveDir ++ "/bufferViewTemp/";
		bufferViewSoundFile = bufferViewFolder++"sampleRec"++id++".wav";
		buffer.write(bufferViewSoundFile,"WAV","int16", bufferLength * sRate, 0);
	}

	addSynths {
		this.log("add synths");

		SynthDef(\rec ++ id, {
			arg gate, buf;
			var playhead = (Phasor.ar(1,1, 0, bufferLength * sRate, 0)) % BufFrames.kr(buffer);
			SendReply.kr(Impulse.kr(20), "/playhead" ++ id, playhead, recZone);
			SendReply.kr(Impulse.kr(0.5),"/bufferViewRefresh" ++ id, playhead);
			RecordBuf.ar(SoundIn.ar([audioIn,audioIn]), buf, 0, loop: 0)
			* EnvGen.kr(Env.linen(0,bufferLength,0), gate, doneAction: 2);
		}).add;

		SynthDef(\play ++ id, {
			arg gate = 1, buf, lenBus, start = 0, volBus;
			var sig, env, trig, lenVal, volVal, playhead;
			lenVal = In.kr(lenBus,1);
			volVal = In.kr(volBus,1);
			playhead = (Phasor.ar(1, 1, start * sRate, (start + lenVal) * sRate, start * sRate)) % BufFrames.kr(buffer);
			SendReply.kr(Impulse.kr(20), "/playhead" ++ id, playhead, playZone);
			trig = Impulse.kr(1 / lenVal);
			env = EnvGen.kr(Env.adsr(0.01,0,1,0.01), gate, doneAction: 2);
			sig = PlayBufCF.ar(2, buf, 1, trig, start * sRate, 1);
			sig = sig * env * volVal;
			Out.ar(audioOut, sig);
		}).add;
	}

	initOsc {
		this.log("initialize OSC");

		OSCdef(\bufferViewRefresh ++ id, { |msg|
			this.refreshBufferView();
		},
		'bufferViewRefresh' ++ id
		).fix;

		OSCdef(\playhead ++ id, { |msg|
			var playhead = msg[3];
			var zone = msg[2];
			if (zone == recZone) {
				if (playhead > (bufferLength * sRate - 4000), {recButton.valueAction_(0)});
			};
			if (zone == playZone) {
				{ bufferView.timeCursorPosition = playhead; }.defer(0);
			};
		},
		'playhead' ++ id
		).fix;
	}

	initMidi {
		this.log("initialize MIDI");

		midiSources = List.newClear();
		midiSourcesUids = List.newClear();
		MIDIClient.sources.size.do {|i|
			midiSources.add(MIDIClient.sources[i].device + MIDIClient.sources[i].name);
			midiSourcesUids.add(MIDIClient.sources[i].uid);
		};
		if (midiPortUid.isNil && (midiSourcesUids.size != 0)) {
			midiPortUid = 999;
		};
		if (midiSources.size == 0) {
			midiSources.add("No MIDI sources available");
			midiSourcesUids.add(nil);
		} {
			midiSources.add("All MIDI sources");
			midiSourcesUids.add(999);
		};
		midiPortIndex = midiSourcesUids.indexOfEqual(midiPortUid);
		if (midiPortIndex.isNil) {
			midiPortIndex = 0;
		};
		if (midiPortUid == 999) {
			midiPortUid = nil;
		};

		midiDefCc = MIDIdef.cc(\CC ++ id, {
			arg val, num, chan;
			if (showMidi) {this.log("CC val=" ++ val + "num=" ++ num + "chan=" ++ chan)};
			case
			{ num == midiCcVol }
			{
				volumeSlider.valueAction_(val/127);
			}
			{ num == midiCcStart }
			{
				startPos = spec.start.at(val/127);
				if(startPos != startPosPrev, {
					{ bufferView.setSelectionStart(0, startPos * sRate) }.defer;
					this.restartPlaySynth();
					startPosPrev = startPos;
				});
			}
			{ num == midiCcLen }
			{
				lenBus.set(spec.len.at(val/127));
				lenBus.get({ arg busVal; { bufferView.setSelectionSize(0, busVal * sRate) }.defer });
			}
			;
		}, srcID: midiPortUid, chan: midiChannel).fix;
		midiDefNoteOn = MIDIdef.noteOn(\NoteOn ++ id, {
			arg val, num, chan, src;
			if (showMidi) {this.log("NON val=" ++ val + "num=" ++ num + "chan=" ++ chan)};
			case
			{ num == midiNotePlay }
			{
				if (midiNoteToggle.not, {
					playButton.valueAction_(1);
				}, {
					playButton.valueAction_(playSynth.isNil.asInteger);
				});
			}
			{ num == midiNoteRec }
			{
				if (midiNoteToggle.not, {
					recButton.valueAction_(1);
				}, {
					recButton.valueAction_(recSynth.isNil.asInteger);
				});
			}
			;
		}, srcID: midiPortUid, chan: midiChannel).fix;
		midiDefNoteOff = MIDIdef.noteOff(\NoteOff ++ id, {
			arg val, num, chan, src;
			if (showMidi) {this.log("NOF val=" ++ val + "num=" ++ num + "chan=" ++ chan)};
			case
			{ num == midiNotePlay }
			{
				if (midiNoteToggle.not) {
					playButton.valueAction_(0);
				};
			}
			{ num == midiNoteRec }
			{
				if (midiNoteToggle.not) {
					recButton.valueAction_(0);
				};
			}
			;
		}, srcID: midiPortUid, chan: midiChannel).fix;
	}

	buildGui {
		var screenWidth = Window.screenBounds.width, screenHeight = Window.screenBounds.height;
		var border = 4, view, title, font = "Avenir", textGui = (), number;
		var width = screenWidth / 2 - (2*border), height = screenHeight / 2 - (2*border) - 20;
		var left = (id%2) * width + ((id%2+1)*border);
		var top = if(id > 1, {height + (2 * border)}, {border});
		var volumeLabel, instanceNumber;

		this.log("build GUI");

		view = View(win, Rect(left, top, width, height)).background_(Color.new255(192, 192, 192));

		bufferView = (SoundFileView.new(view, Rect(10, 10, width - 20, height - 220 - 20))
			.gridOn_(false)
			.gridResolution_(10)
			.gridColor_(Color.grey)
			.timeCursorOn_(true)
			.timeCursorColor_(Color.black)
			.waveColors_([Color.black, Color.black])
			.background_(Color.white)
			.canFocus_(false)
			.setSelectionColor(0, Color.grey(0.6))
		);
		{
			fileBufferView.openRead(bufferViewSoundFile);
			bufferView.soundfile = fileBufferView;
			bufferView.read(0, bufferLength * sRate, 512).refresh;
			bufferView.setSelectionStart(0, startPos * sRate);
			bufferView.action_({ arg value;
				var start = value.selections[0][0];
				var len = value.selections[0][1];
				if (len == 0) {
					len = bufferLength * sRate / 10;
					bufferView.setSelectionSize(0, len);
				};
				startPos = spec.start.at(start/(bufferLength * sRate));
				this.restartPlaySynth();
				lenBus.set(len/sRate);
			});
			lenBus.get {arg val; { bufferView.setSelectionSize(0, val * sRate) }.defer };
		}.defer(1);

		instanceNumber = StaticText(view, Rect(15, height - 35, 150, 30))
			.font_(Font(font, fontSizeNormal)).string_("GeluidKrasser" + id);

		recButton = (SmoothButton(view, Rect(width - 450,height - 160,100,100))
			.border_(1).radius_(50).canFocus_(false).font_(Font(font,30))
			.states_([ [ "Rec", Color.black, Color.grey(0.9) ], [ "Rec", Color.black, Color.red(1,1) ] ])
			.action_({ |b|
				this.toggleRecSynth(b.value == 1);
			})
		);

		playButton = (SmoothButton(view, Rect(width - 310,height - 160,100,100))
			.border_(1).radius_(50).canFocus_(false).font_(Font(font,30))
			.states_([ [ "Play", Color.black, Color.grey(0.9) ], [ "Play", Color.black, Color.green(1,1) ] ])
			.action_({ |b|
				this.togglePlaySynth(b.value == 1);
			})
		);

		volumeLabel = StaticText(view, Rect(width - 91, height - 50, 200, 60))
			.font_(Font(font, fontSizeNormal))
			.string_("Volume");

		volumeSlider = SmoothSlider(view, Rect(width - 100, height - 190, 60, 150))
			.hilightColor_(Color.grey(1,0.4))
			.background_(Color.green.alpha_(0))
			.border_(1)
			.borderColor_(Color.grey(0.4))
			.knobSize_(0.05)
			.value_(0.5)
			.canFocus_(false)
			.action_({
				volBus.set(volumeSlider.value);
			});

		this.buildGuiSettings(view, width, height);
	}

	buildGuiSettings {
		arg view, width, height;
		var font = "Avenir";
		var settingsView, settingsButton;
		var midiChannelPopupLabel, midiChannelPopup;
		var audioOutChannelPopupLabel, audioOutChannelPopup, audioInChannelPopupLabel, audioInChannelPopup;
		var midiNotePlayPopup, midiNoteRecPopup, midiCcVolPopup, midiCcStartPopup, midiCcLenPopup;
		var midiPortPopupLabel, midiPortPopup, midiNoteTogglePopupLabel, midiNoteTogglePopup;
		var audioOutChannels;
		var audioLabel, midiLabel;
		var midiLeft = 30;
		var audioLeft = 350;

		settingsView = View(view, Rect(10, height - 220, width - 20, 210)).background_(Color.new255(220, 231, 242));
		settingsView.visible_(false);

		midiLabel = StaticText(settingsView, Rect(midiLeft, 20, 110, 15))
			.font_(Font(font ++ "Bold", fontSizeNormal)).string_("MIDI settings");

		midiPortPopupLabel = StaticText(settingsView, Rect(midiLeft, 45, 110, 15))
			.font_(Font(font, fontSize)).string_("MIDI In Port");
		midiPortPopup = (PopUpMenu(settingsView, Rect(midiLeft + 110, 45, 180, 15))
			.canFocus_(true).items_(midiSources.asArray).background_(Color.grey(0.9)).font_(font)
			.action_({ |p|
				midiPortIndex = p.value;
				midiPortUid = midiSourcesUids[midiPortIndex];
				playButton.valueAction_(0);
				recButton.valueAction_(0);
				this.freeMidi();
				this.initMidi();
				this.writeConfig();
			})
			.keyDownAction_(false)
			.value_(midiPortIndex)
			.font_(Font(font, fontSize))
		);

		midiChannelPopupLabel = StaticText(settingsView, Rect(midiLeft, 60, 110, 15))
			.font_(Font(font, fontSize)).string_("MIDI In Channel");
		midiChannelPopup = (PopUpMenu(settingsView, Rect(midiLeft + 110, 60, 70, 15))
			.canFocus_(true).items_((1..16)).background_(Color.grey(0.9)).font_(font)
			.action_({ |p|
				midiChannel = p.value;
				playButton.valueAction_(0);
				recButton.valueAction_(0);
				this.log("set MIDI In Channel to" + (midiChannel + 1), true);
				this.freeMidi();
				this.initMidi();
				this.writeConfig();
			})
			.keyDownAction_(false)
			.value_(midiChannel)
			.font_(Font(font, fontSize))
		);

		midiNotePlayPopupLabel = StaticText(settingsView, Rect(midiLeft, 75, 110, 15))
			.font_(Font(font, fontSize)).string_("Play MIDI note");
		midiNotePlayPopup = (PopUpMenu(settingsView, Rect(midiLeft + 110, 75, 70, 15))
			.canFocus_(true).items_((1..127)).background_(Color.grey(0.9)).font_(font)
			.action_({ |p|
				midiNotePlay = p.value + 1;
				playButton.valueAction_(0);
				this.log("set Play MIDI note to" + midiNotePlay, true);
				this.freeMidi();
				this.initMidi();
				this.checkNoteConflict();
				this.writeConfig();
			})
			.keyDownAction_(false)
			.value_(midiNotePlay - 1)
			.font_(Font(font, fontSize))
		);

		midiNoteRecPopupLabel = StaticText(settingsView, Rect(midiLeft, 90, 110, 15))
			.font_(Font(font, fontSize)).string_("Rec MIDI note");
		midiNoteRecPopup = (PopUpMenu(settingsView, Rect(midiLeft + 110, 90, 70, 15))
			.canFocus_(true).items_((1..127)).background_(Color.grey(0.9)).font_(font)
			.action_({ |p|
				midiNoteRec = p.value + 1;
				recButton.valueAction_(0);
				this.log("set Rec MIDI note to" + midiNoteRec, true);
				this.freeMidi();
				this.initMidi();
				this.checkNoteConflict();
				this.writeConfig();
			})
			.keyDownAction_(false)
			.value_(midiNoteRec - 1)
			.font_(Font(font, fontSize))
		);

		midiCcVolPopupLabel = StaticText(settingsView, Rect(midiLeft, 105, 110, 15))
			.font_(Font(font, fontSize)).string_("Volume MIDI CC");
		midiCcVolPopup = (PopUpMenu(settingsView, Rect(midiLeft + 110, 105, 70, 15))
			.canFocus_(true).items_((1..127)).background_(Color.grey(0.9)).font_(font)
			.action_({ |p|
				midiCcVol = p.value + 1;
				this.log("set Volume MIDI CC to" + midiCcVol, true);
				this.freeMidi();
				this.initMidi();
				this.checkCcConflict();
				this.writeConfig();
			})
			.keyDownAction_(false)
			.value_(midiCcVol - 1)
			.font_(Font(font, fontSize))
		);

		midiCcStartPopupLabel = StaticText(settingsView, Rect(midiLeft, 120, 110, 15))
			.font_(Font(font, fontSize)).string_("StartPos MIDI CC");
		midiCcStartPopup = (PopUpMenu(settingsView, Rect(midiLeft + 110, 120, 70, 15))
			.canFocus_(true).items_((1..127)).background_(Color.grey(0.9)).font_(font)
			.action_({ |p|
				midiCcStart = p.value + 1;
				this.log("set StartPos MIDI CC to" + midiCcStart, true);
				this.freeMidi();
				this.initMidi();
				this.checkCcConflict();
				this.writeConfig();
			})
			.keyDownAction_(false)
			.value_(midiCcStart - 1)
			.font_(Font(font, fontSize))
		);

		midiCcLenPopupLabel = StaticText(settingsView, Rect(midiLeft, 135, 110, 15))
			.font_(Font(font, fontSize)).string_("Length MIDI CC");
		midiCcLenPopup = (PopUpMenu(settingsView, Rect(midiLeft + 110, 135, 70, 15))
			.canFocus_(true).items_((1..127)).background_(Color.grey(0.9)).font_(font)
			.action_({ |p|
				midiCcLen = p.value + 1;
				this.log("set Length MIDI CC to" + midiCcLen, true);
				this.freeMidi();
				this.initMidi();
				this.checkCcConflict();
				this.writeConfig();
			})
			.keyDownAction_(false)
			.value_(midiCcLen - 1)
			.font_(Font(font, fontSize))
		);

		midiNoteTogglePopupLabel = StaticText(settingsView, Rect(midiLeft, 150, 110, 15))
			.font_(Font(font, fontSize)).string_("MIDI note toggle");
		midiNoteTogglePopup = (PopUpMenu(settingsView, Rect(midiLeft + 110, 150, 70, 15))
			.canFocus_(true).items_(["ON","OFF"]).background_(Color.grey(0.9)).font_(font)
			.action_({ |p|
				midiNoteToggle = if (p.value == 0) { true } { false };
				this.log("set MIDI note toggle" + (if (midiNoteToggle) { "ON" } { "OFF" }), true);
				this.freeMidi();
				this.initMidi();
				this.writeConfig();
			})
			.keyDownAction_(false)
			.value_(if (midiNoteToggle) { 0 } { 1 })
			.font_(Font(font, fontSize))
		);
		midiNoteTogglePopup.value = if (midiNoteToggle) { 0 } { 1 };

		audioLabel = StaticText(settingsView, Rect(audioLeft, 20, 110, 15))
			.font_(Font(font ++ "Bold", fontSizeNormal)).string_("Audio settings");

		audioInChannelPopupLabel = StaticText(settingsView, Rect(audioLeft, 45, 110, 15))
			.font_(Font(font, fontSize)).string_("Audio In Channel");
		audioInChannelPopup = (PopUpMenu(settingsView, Rect(audioLeft + 110, 45, 70, 15))
			.canFocus_(true).items_((1..numAudioInChannels)).background_(Color.grey(0.9)).font_(font)
			.action_({ |p|
				audioIn = p.value;
				playButton.valueAction_(0);
				recButton.valueAction_(0);
				this.log("set Audio In Channel to" + (audioIn + 1), true);
				this.addSynths();
				this.writeConfig();
			})
			.keyDownAction_(false)
			.value_(audioIn)
			.font_(Font(font, fontSize))
		);

		audioOutChannels = Array.fill(numAudioOutChannels / 2, {
			arg i;
			var chanL = 2 * i + 1, chanR = chanL + 1;
			chanL.asInteger + "+" + chanR.asInteger
		});
		audioOutChannelPopupLabel = StaticText(settingsView, Rect(audioLeft, 60, 110, 15))
			.font_(Font(font, fontSize)).string_("Audio Out Channels");
		audioOutChannelPopup = (PopUpMenu(settingsView, Rect(audioLeft + 110, 60, 70, 15))
			.canFocus_(true).items_(audioOutChannels).background_(Color.grey(0.9)).font_(font)
			.action_({ |p|
				audioOut = p.value * 2;
				playButton.valueAction_(0);
				recButton.valueAction_(0);
				this.log("set Audio Out Channels to" + (audioOut + 1) + "+" + (audioOut + 2), true);
				this.addSynths();
				this.writeConfig();
			})
			.keyDownAction_(false)
			.value_(audioOut)
			.font_(Font(font, fontSize))
		);

		settingsButton = (SmoothButton(view, Rect(5,height - 222,30,30))
			.border_(0).radius_(15).canFocus_(false).font_(Font(font,13))
			.states_([ [ "🎹", Color.black, Color.grey(0.9,0) ], [ "🎹", Color.new255(176,233,252), Color.new255(176,233,252,0) ] ])
			.action_({ |b|
				settingsView.visible_(b.value == 1);
			})
		);
	}

	togglePlaySynth  {
		arg play;
		if (play && playSynth.isNil, {
			playSynth = Synth(\play ++ id, [
				\buf, buffer, \volBus, volBus.index, \lenBus, lenBus.index, \start, startPos
			]);
		}, {
			playSynth.release(0.01);
			playSynth = nil;
		})
	}

	toggleRecSynth  {
		arg rec;
		if (rec && recSynth.isNil, {
			recSynth = Synth(\rec ++ id, [\gate, 1, \buf, buffer]);
		}, {
			recSynth.release(0.01);
			recSynth = nil;
			this.refreshBufferView();
		});
	}

	restartPlaySynth {
		if(playSynth.notNil, {
			this.togglePlaySynth(false);
			this.togglePlaySynth(true);
		});
	}

	cleanUp {
		this.log("Cleaning up", true);
		if (playSynth.notNil, {playSynth.release(0.01)});
		if (recSynth.notNil, {recSynth.release(0.01)});
		buffer.zero;
		buffer.write(bufferViewSoundFile,"WAV","int16", bufferLength * sRate, 0);
		buffer.free; buffer = nil;
		this.freeMidi();
		OSCdef.freeAll;
	}

	refreshBufferView {
		var delay = 0.1 * (bufferLength / 100) + bufferViewBaseDelay;
		buffer.write(bufferViewSoundFile,"WAV","int16", bufferLength * sRate, 0);
		{
			fileBufferView.openRead(bufferViewSoundFile);
			bufferView.soundfile = fileBufferView;
			bufferView.read(0, bufferLength * sRate, 512).refresh;
		}.defer(delay);
	}

	clearBuffer {
		buffer.zero;
		this.refreshBufferView();
		this.log("Buffer cleared", true);
	}

	freeMidi {
		midiDefCc.free;
		midiDefNoteOn.free;
		midiDefNoteOff.free;
	}

	log {
		arg msg, newline = false;
		((if (newline) {"\n"} {""}) ++ "GeluidKrasser" ++ id ++ ": " ++ msg).postln;
	}

	checkNoteConflict {
		if (midiNotePlay == midiNoteRec) {
			midiNotePlayPopupLabel.stringColor_(Color.red);
			midiNoteRecPopupLabel.stringColor_(Color.red);
		} {
			midiNotePlayPopupLabel.stringColor_(Color.black);
			midiNoteRecPopupLabel.stringColor_(Color.black);
		};
	}

	checkCcConflict {
		midiCcVolPopupLabel.stringColor_(Color.black);
		midiCcStartPopupLabel.stringColor_(Color.black);
		midiCcLenPopupLabel.stringColor_(Color.black);

		case
		{ midiCcVol == midiCcStart }
		{
			midiCcVolPopupLabel.stringColor_(Color.red);
			midiCcStartPopupLabel.stringColor_(Color.red);
		}
		{ midiCcVol == midiCcLen }
		{
			midiCcVolPopupLabel.stringColor_(Color.red);
			midiCcLenPopupLabel.stringColor_(Color.red);
		}
		{ midiCcStart == midiCcLen }
		{
			midiCcLenPopupLabel.stringColor_(Color.red);
			midiCcStartPopupLabel.stringColor_(Color.red);
		};
		if (midiCcStart == midiCcLen && midiCcLen == midiCcVol) {
			midiCcLenPopupLabel.stringColor_(Color.red);
			midiCcStartPopupLabel.stringColor_(Color.red);
			midiCcVolPopupLabel.stringColor_(Color.red);
		};
	}

	writeConfig {
		var file, allConfig;

		allConfig = configFile.load;
		allConfig[id][0] = midiPortUid;
		allConfig[id][1] = midiChannel;
		allConfig[id][2] = audioIn;
		allConfig[id][3] = audioOut;
		allConfig[id][4] = midiNotePlay;
		allConfig[id][5] = midiNoteRec;
		allConfig[id][6] = midiCcVol;
		allConfig[id][7] = midiCcStart;
		allConfig[id][8] = midiCcLen;
		allConfig[id][9] = midiNoteToggle;

		file = File(configFile,"w");
		file.write(allConfig.asCompileString);
		file.close;
	}

}