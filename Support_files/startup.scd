
{ //------------------- Server settings -------------------

	var device1 = "MOTU UltraLite mk3 Hybrid";
	var device2 = "UltraLite-mk5";
	var device3 = "ES-9";

	"\nKill all servers".postln;
	Server.killAll;

	"\nAll available audio devices:".postln;
	ServerOptions.devices.postln;

	if (ServerOptions.devices.find([device1]).notNil) {
		Server.local.options
		.device_(device1)
		.hardwareBufferSize_(64)
		.numInputBusChannels_(12)
		.numOutputBusChannels_(10);
	};

	if (ServerOptions.devices.find([device2]).notNil) {
		Server.local.options
		.device_(device2)
		.hardwareBufferSize_(64)
		.numInputBusChannels_(12)
		.numOutputBusChannels_(10);
	};

	if (ServerOptions.devices.find([device3]).notNil) {
		Server.local.options
		.device_(device3)
		.hardwareBufferSize_(64)
		.numInputBusChannels_(12)
		.numOutputBusChannels_(10);
	};

	Server.local.options
	.memSize_(65536 * 4)
	.numWireBufs_(256)
	.recSampleFormat_("int24");

	("\nServer Memory Size:" + Server.local.options.memSize).postln;
	("Server Audio Device: Default").postln;
	("Server Hardware Buffer Size:" + Server.local.options.hardwareBufferSize).postln;
	("Server Num Wire Bufs:" + Server.local.options.numWireBufs).postln;
	("Server Num Input Channels:" + Server.local.options.numInputBusChannels).postln;
	("Server Num Output Channels:" + Server.local.options.numOutputBusChannels + "\n").postln;

}.value;

{ //------------------- Environment settings -------------------

	s.boot;
	// ("~/Desktop/GeluidKrasser/GeluidKrasserStart.scd").standardizePath.load();

}.value;

