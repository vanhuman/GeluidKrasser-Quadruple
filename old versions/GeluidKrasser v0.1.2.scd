/* inputs voor midiNoteToggle, rec, play, vol, start, len
Vragen voor Ivo:
1. midiNoteToggle of niet?
2. welk midi kanaal en welke midi notes/cc nummers?
3. buffer lengte
*/

(
if(~standalone.isNil, {~standalone = false});
{
	s.waitForBoot {

		var bufferLength = 30;
		var midiNoteToggle = true;
		var midiChannel = 0;
		var verbose = false;
		var version = "0.1.2";

		var buffer;
		var sRate;
		var id = 0;
		var fInit, fAddSynths, fInitMidi, fBuildGui, fCleanUp, fPlaySynth;
		var fRestartPlaySynth, fInitOsc, fRefreshBufferView, fRecSynth;
		var volBus, lenBus, startPos = 0, startPosPrev = 0;
		var playButton, recButton, win, bufferView, volumeLabel, volumeSlider, midiChannelPopupLabel, midiChannelPopup;
		var expanationText;
		var playSynth, recSynth;
		var spec = ();
		var fileBufferView, bufferViewFolder;
		var recZone = 1, playZone = 2;

		fInit = {
			(if(~standalone, {"## Standalone version"}, {"## SuperCollider version"})).postln;
			"Initialize".postln;
			sRate = s.sampleRate;
			buffer = Buffer.alloc(s, sRate * bufferLength, 2);
			buffer.write(bufferViewFolder++"sampleRec.wav","WAV","int16", bufferLength * sRate, 0);
			spec[\start] = Env.new([0.01, 1 * bufferLength], [1], \lin);
			spec[\len] = Env.new([0.01, 0.5 * bufferLength], [1], \exp);
			lenBus = Bus.control(s,1).set(0.1 * bufferLength);
			volBus = Bus.control(s,1).set(0.5);
			fileBufferView = SoundFile.new();
			bufferViewFolder = if(~standalone,
				{ "/tmp/" },
				// { Platform.resourceDir ++ "/GeluidKrasser_files/bufferViewTemp/" },
				{ Archive.archiveDir ++ "/bufferViewTemp/" });
		};

		fAddSynths = {
			"Add synths".postln;
			SynthDef(\rec, {
				arg gate, buf;
				var playhead = (Phasor.ar(1,1, 0, bufferLength * sRate, 0)) % BufFrames.kr(buffer);
				SendReply.kr(Impulse.kr(20), "/playhead", playhead, recZone);
				SendReply.kr(Impulse.kr(0.5),"/bufferViewRefresh", playhead);
				RecordBuf.ar(SoundIn.ar([id,id]), buf, 0, loop: 0) * EnvGen.kr(Env.linen(0,bufferLength,0), gate, doneAction: 2);
			}).add;

			SynthDef(\play, {
				arg gate = 1, buf, lenBus, start = 0, volBus;
				var sig, env, trig, lenVal, volVal, playhead;
				lenVal = In.kr(lenBus,1);
				volVal = In.kr(volBus,1);
				playhead = (Phasor.ar(1, 1, start * sRate, (start + lenVal) * sRate, start * sRate)) % BufFrames.kr(buffer);
				SendReply.kr(Impulse.kr(20), "/playhead", playhead, playZone);
				trig = Impulse.kr(1 / lenVal);
				env = EnvGen.kr(Env.adsr(0.01,0,1,0.01), gate, doneAction: 2);
				sig = PlayBufCF.ar(2, buf, 1, trig, start * sRate, 1);
				sig = sig * env * volVal;
				Out.ar(0, sig);
			}).add;
		};

		fInitOsc = {
			"Initialize OSC".postln;
			OSCdef(\bufferViewRefresh, { |msg|
				fRefreshBufferView.value();
			},
			'bufferViewRefresh'
			).fix;

			OSCdef(\playhead, { |msg|
				var playhead = msg[3];
				var zone = msg[2];
				if (zone == recZone) {
					if (playhead > (bufferLength * sRate - 4000), {recButton.valueAction_(0)});
				};
				if (zone == playZone) {
					{ bufferView.timeCursorPosition = playhead; }.defer(0);
				};
			},
			'playhead'
			).fix;
		};

		fInitMidi = {
			"Initialize MIDI".postln;
			MIDIIn.connectAll;

			MIDIdef.cc(\CC ++ id, {
				arg val, num, chan;
				if (verbose) {("CC val=" ++ val + "num=" ++ num + "chan=" ++ chan).postln};
				if (chan == midiChannel) {
					case
					{ num == 1 }
					{
						volumeSlider.valueAction_(val/127);
					}
					{ num == 2 }
					{
						startPos = spec.start.at(val/127);
						if(startPos != startPosPrev, {
							{ bufferView.setSelectionStart(0, startPos * sRate) }.defer;
							fRestartPlaySynth.value();
							startPosPrev = startPos;
						});
					}
					{ num == 3 }
					{
						lenBus.set(spec.len.at(val/127));
						lenBus.get({ arg busVal; { bufferView.setSelectionSize(0, busVal * sRate) }.defer });
					}
					;
				};
			}).fix;
			MIDIdef.noteOn(\NoteOn ++ id, {
				arg val, num, chan, src;
				if (verbose) {("NON val=" ++ val + "num=" ++ num + "chan=" ++ chan).postln};
				if (chan == midiChannel) {
					case
					{ num == 60 }
					{
						if (midiNoteToggle.not, {
							playButton.valueAction_(1);
						}, {
							playButton.valueAction_(playSynth.isNil.asInt);
						});
					}
					{ num == 48 }
					{
						if (midiNoteToggle.not, {
							recButton.valueAction_(1);
						}, {
							recButton.valueAction_(recSynth.isNil.asInt);
						});
					}
					;
				};
			}).fix;
			MIDIdef.noteOff(\NoteOff ++ id, {
				arg val, num, chan, src;
				if (verbose) {("NOF val=" ++ val + "num=" ++ num + "chan=" ++ chan).postln};
				if (chan == midiChannel) {
					case
					{ num == 60 }
					{
						if (midiNoteToggle.not) {
							playButton.valueAction_(0);
						};
					}
					{ num == 48 }
					{
						if (midiNoteToggle.not) {
							recButton.valueAction_(0);
						};
					}
					;
				};
			}).fix;
		};

		fBuildGui = {
			var screenWidth = Window.screenBounds.width, screenHeight = Window.screenBounds.height;
			var border = 4, view, title, font = "Avenir", textGui = (), number;
			var width = screenWidth / 2 - (1.5*border), height = screenHeight / 2 - (1.5*border);
			var left = (id%2) * width + ((id%2+1)*border);
			var top = if(id > 1, {height + (2 * border)}, {border});

			"Build GUI".postln;

			w = Window().front.close; // to prevent main window to start hidden behind sclang
			win = Window("GeluidKrasser v" ++ version, Rect(left, top, width, height)).onClose_({ fCleanUp.value() }).front;
			left = 0; top = 0;
			view = View(win, Rect(left, top, width, height)).background_(Color.new255(192, 192, 192));
			win.view.keyDownAction = {
				arg view, char, modifiers, unicode, keycode, key;
				// [modifiers, keycode].postln;
				if (keycode == 37) { // L
					buffer.zero;
					fRefreshBufferView.value();
					"Buffer cleared".postln;
				};
				if (keycode == 35) { // P
					if (playSynth.isNil, {
						playButton.valueAction_(1);
					}, {
						playButton.valueAction_(0);
					});
				};
				if (keycode == 15) { // R
					if (recSynth.isNil, {
						recButton.valueAction_(1);
					}, {
						recButton.valueAction_(0);
					});
				};
				if ((keycode == 13 || (keycode == 12 && ~standalone)) && modifiers.isCmd) { // Cmd-W or Cmd-Q (only in standalone)
					win.close;
				}
			};

			midiChannelPopupLabel = StaticText(view, Rect(20, height - 190, 100, 20));
			midiChannelPopupLabel.font = Font(font, 12);
			midiChannelPopupLabel.string = "MIDI Channel";

			midiChannelPopup = (PopUpMenu(view, Rect(110, height - 190, 50, 20))
				.canFocus_(true).items_((1..16)).background_(Color.grey(0.9)).font_(font)
				.action_({ |p|
					midiChannel = p.value;
				})
				.keyDownAction_(false)
			);

			expanationText = StaticText(view, Rect(20, height - 140, 300, 120));
			expanationText.font = Font(font, 12);
			expanationText.string =
			"Recording: NON 48 or key r" +
			"\nPlayback: NON 60 or key p" +
			"\nVolume: CC 1" +
			"\nStart position: CC 2" +
			"\nLength: CC 3" +
			"\n\nMIDI note toggle is" + if(midiNoteToggle, "ON", "OFF");


			playButton = (SmoothButton(view, Rect(width - 240,height - 140,100,100))
				.border_(1).radius_(50).canFocus_(false).font_(Font(font,30))
				.states_([ [ "Play", Color.black, Color.grey(0.9) ], [ "Play", Color.black, Color.green(1,1) ] ])
				.action_({ |b|
					fPlaySynth.value(b.value == 1);
				})
			);
			recButton = (SmoothButton(view, Rect(width - 380,height - 140,100,100))
				.border_(1).radius_(50).canFocus_(false).font_(Font(font,30))
				.states_([ [ "Rec", Color.black, Color.grey(0.9) ], [ "Rec", Color.black, Color.red(1,1) ] ])
				.action_({ |b|
					fRecSynth.value(b.value == 1);
				})
			);

			volumeLabel = StaticText(view, Rect(width - 97, height - 50, 200, 60));
			volumeLabel.font = Font(font, 16);
			volumeLabel.string = "Volume";

			volumeSlider = SmoothSlider(view, Rect(width - 100, height - 190, 60, 150))
			.hilightColor_(Color.grey(1,0.4))
			.background_(Color.green.alpha_(0))
			.knobColor_(Color.grey(0.9))
			.border_(1)
			.borderColor_(Color.grey(0.4))
			.knobSize_(0.05)
			.value_(0.5)
			.canFocus_(false)
			.action_({
				volBus.set(volumeSlider.value);
			});

			bufferView = (SoundFileView.new(view, Rect(10, 10, width - 20, height / 2 - 20))
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
				fileBufferView.openRead(bufferViewFolder++"sampleRec.wav");
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
					fRestartPlaySynth.value();
					lenBus.set(len/sRate);
				});
				lenBus.get {arg val; { bufferView.setSelectionSize(0, val * sRate) }.defer };
			}.defer(1);
		};

		fPlaySynth = {
			arg play;
			if (verbose) {
				("Playback" + (if(play && playSynth.isNil, "started", "stopped"))).postln;
			};
			if (play && playSynth.isNil, {
				playSynth = Synth(\play, [
					\buf, buffer, \volBus, volBus.index, \lenBus, lenBus.index, \start, startPos
				]);
			}, {
				playSynth.release(0.01);
				playSynth = nil;
			});
		};

		fRecSynth = {
			arg rec;
			if (verbose) {
				("Recording" + (if(rec && recSynth.isNil, "started", "stopped"))).postln;
			};
			if (rec && recSynth.isNil, {
				recSynth = Synth(\rec, [\gate, 1, \buf, buffer]);
			}, {
				recSynth.release(0.01);
				recSynth = nil;
				fRefreshBufferView.value();
			});
		};

		fRestartPlaySynth = {
			if(playSynth.notNil, {
				fPlaySynth.value(false);
				fPlaySynth.value(true);
			});
		};

		fCleanUp = {
			"Cleaning up".postln;§
			if (playSynth.notNil, {playSynth.release(0.01)});
			if (recSynth.notNil, {recSynth.release(0.01)});
			buffer.zero;
			buffer.write(bufferViewFolder++"sampleRec.wav","WAV","int16", bufferLength * sRate, 0);
			buffer.free; buffer = nil;
			MIDIdef.freeAll;

			if(~standalone, {
				// clean up all processes
				thisProcess.shutdown;
				// stop sclang
				0.exit;
				// stop the IDE
				"osascript -e 'tell application \"GeluidKrasser\" to quit'".unixCmd;
			});
		};

		fRefreshBufferView = {
			buffer.write(bufferViewFolder++"sampleRec.wav","WAV","int16", bufferLength * sRate, 0);
			{
				fileBufferView.openRead(bufferViewFolder++"sampleRec.wav");
				bufferView.soundfile = fileBufferView;
				bufferView.read(0, bufferLength * sRate, 512).refresh;
			}.defer(0.1);
		};

		fInit.value();
		fAddSynths.value();
		fInitOsc.value();
		fInitMidi.value();
		fBuildGui.value();

	}
}.defer(1);
)
