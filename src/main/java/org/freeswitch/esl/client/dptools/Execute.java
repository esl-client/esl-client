package org.freeswitch.esl.client.dptools;

import org.freeswitch.esl.client.internal.IModEslApi;
import org.freeswitch.esl.client.transport.CommandResponse;
import org.freeswitch.esl.client.transport.SendMsg;
import org.freeswitch.esl.client.transport.message.EslMessage;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class Execute {

  IModEslApi api;
  String _uuid;

  public Execute(IModEslApi api, String uuid) {
    this.api = api;
    this._uuid = uuid;
  }

  /**
   * Sends an info packet with a sipfrag. If the phone supports it will show
   * message on the display.
   *
   * @param message to display
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_Send_Display">FreeSWITCH wiki</a>
   */
  public void sendDisplay(String message) throws ExecuteException {
    sendExeMesg("send_display", message);
  }

  /**
   * Answers an incoming call or session.
   *
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_answer">FreeSWITCH wiki</a>
   */
  public void answer() throws ExecuteException {
    sendExeMesg("answer");
  }

  /**
   * Make an attended transfer.
   *
   * @param channelUrl ex: sofia/default/${attxfer_callthis}
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_att_xfer">FreeSWITCH wiki</a>
   */
  public void attAnswer(String channelUrl) throws ExecuteException {
    sendExeMesg("att_xfer", channelUrl);
  }

  /**
   * @param key         the button you want to respond to after the * button is
   *                    pressed. If you wanted to respond to *1, you would put 1 in
   *                    place of KEY. You are limited to a single digit.
   * @param leg         which call leg(s) to listen on. Acceptable parameters are a, b
   *                    or ab.
   * @param flags       modifies the behavior. The following flags are available: a -
   *                    Respond on A leg, b - Respond on B leg, o - Respond on
   *                    opposite leg, s - Respond on same leg, i - Execute inline, 1 -
   *                    Unbind this meta_app after it is used one time
   * @param application is which application you want to execute.
   * @param params      are the arguments you want or need to provide to the
   *                    APPLICATION.
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_bind_meta_app">FreeSWITCH wiki</a>
   */
  public void bindMetaApp(char key, String leg, char flags,
                          String application, String params) throws ExecuteException {
    sendExeMesg("bind_meta_app", key + "" + leg + " " + flags + " "
      + application + "::" + params);
  }

  /**
   * Cancels currently running application on the given UUID. Dialplan
   * execution proceeds to the next application. Optionally clears all
   * unprocessed events (queued applications) on the channel.
   *
   * @param all clear all unprocessed events (queued applications) on the
   *            channel, otherwise just the current application.
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_break">FreeSWITCH wiki</a>
   */
  public void breakChannel(boolean all) throws ExecuteException {
    sendExeMesg("break", all ? "all" : "");
  }

  /**
   * Provides the ability to bridge two endpoints. Generally used to route an
   * incoming call to one or more endpoints. Multiple endpoints can be dialed
   * simultaneously or sequentially using the comma and pipe delimiters,
   * respectively.
   *
   * @param endpoint endpoint
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_bridge">FreeSWITCH wiki</a>
   */
  public void bridge(String endpoint) throws ExecuteException {
    sendExeMesg("bridge", endpoint);
  }

  /**
   * Export a channel variable across a bridge. This application differs from
   * export in that it works with *any* kind of bridge, not just a bridge
   * called from the dialplan. For example, bridge_export will export its
   * variables if the leg is uuid_transfer'd whereas export will not
   *
   * @param key   channel variable name
   * @param value channel variable value
   * @param local to only export to the B leg false, otherwise true
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_bridge_export">FreeSWITCH wiki</a>
   */
  public void bridgeExport(String key, String value, boolean local)
    throws ExecuteException {
    sendExeMesg("bridge_export", (local ? "" : "nolocal:") + key + "="
      + value);
  }

  /**
   * Send a text message to a IM client.
   *
   * @param proto   ex: sip
   * @param from    ex: 1000@127.0.0.1
   * @param to      ex: 1001@127.0.0.1
   * @param message ex: Hello chat from freeswitch
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_chat">FreeSWITCH wiki</a>
   */
  public void chat(String proto, String from, String to, String message)
    throws ExecuteException {
    sendExeMesg("chat", proto + '|' + from + '|' + to + '|' + '|' + message);
  }

  /**
   * cng plc is just an app that says to perform plc on any lost packets and
   * execute on originate. It is like execute on answer, etc. but only for
   * outbound calls during originate.
   *
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_cng_plc">FreeSWITCH wiki</a>
   */
  public void cngPlc() throws ExecuteException {
    sendExeMesg("cng_plc");
  }

  /**
   * Deflect sends a Refer to the client. The deflect application allows
   * FreeSWITCH to be removed from the list of connection hops and tell the
   * originator to reroute the call. When using the deflect application,
   * FreeSWITCH first hangs up the channel and then send a REFER message and a
   * new INVITE message to the originator. The originator, which could be a
   * gateway or sip proxy, should read the INVITE and reroute the call
   * accordingly.
   *
   * @param endpoint SIP URI to contact (with or without "sip:")
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_deflect">FreeSWITCH wiki</a>
   */
  public void deflect(String endpoint) throws ExecuteException {
    sendExeMesg("deflect", endpoint);
  }

  /**
   * Places the calling channel in delayed loopback mode. It simply returns
   * everything sent, including voice, DTMF, etc but with the specified delay
   * [ms]. It is generally useful for checking if RTP audio path works both
   * ways. Normal echo app can fail when tested on speaker phone.
   *
   * @param unit     timeunit to wait
   * @param duration duration in timeunit units to wait
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_delay_echo">FreeSWITCH wiki</a>
   */
  public void delayEcho(TimeUnit unit, int duration) throws ExecuteException {
    sendExeMesg("delay_echo", MILLISECONDS.convert(duration, unit) + "");
  }

  /**
   * Implements speech recognition.
   *
   * @param args <mod_name> <gram_name> <gram_path> [<addr>] grammar
   *             <gram_name> [<path>] grammaron <gram_name> grammaroff
   *             <gram_name> grammarsalloff nogrammar <gram_name> param <name>
   *             <value> pause resume start_input_timers stop
   * @throws ExecuteException
   * @see <a hre"http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_detect_speech">FreeSWITCH wiki</a>
   */
  public void detectSpeech(String args) throws ExecuteException {
    sendExeMesg("detect_speech", args);
  }

  /**
   * Displace file. Plays a file or stream to a channel.
   *
   * @param path                    any sound format FreeSWITCH supports, wav, local_steam, shout
   *                                etc.
   * @param optionalFlags           flags to stream, null if none
   * @param optionalTimeLimitMillis optional time limit, 0 for none
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_displace_session">FreeSWITCH wiki</a>
   */
  public void displaceSession(String path, String optionalFlags,
                              long optionalTimeLimitMillis) throws ExecuteException {
    sendExeMesg("displace_session", path
      + (optionalFlags != null ? " " + optionalFlags : "")
      + (optionalTimeLimitMillis > 0 ? " +" + optionalTimeLimitMillis
      : ""));
  }

  /**
   * Provides the ability to spy on a channel. It often referred to as call
   * barge. For persistent spying on a user see Mod_spy.
   * <p/>
   * DTMF signals during eavesdrop, 2 to speak with the uuid, 1 to speak with
   * the other half, 3 to engage a three way, 0 to restore eavesdrop, * to
   * next channel.
   *
   * @param uuid                  uuid of the call or 'all' for all
   * @param optionalGroupId       if specified, eavesdrop only works with channels that have an
   *                              "eavesdrop_group" channel variable set to the same name.
   * @param optionalFailedWav     ex: /sounds/failed.wav
   * @param optionalNewChannelWav ex: /sounds/new_chan_announce.wav
   * @param optionalIdleWav       ex: /sounds/idle.wav
   * @param enableDTMF
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_eavesdrop">FreeSWITCH wiki</a>
   */
  public void eavesdrop(String uuid, String optionalGroupId,
                        String optionalFailedWav, String optionalNewChannelWav,
                        String optionalIdleWav, boolean enableDTMF) throws ExecuteException {

    if (optionalGroupId != null)
      set("eavesdrop_require_group", optionalGroupId);
    if (optionalFailedWav != null)
      set("eavesdrop_indicate_failed", optionalFailedWav);
    if (optionalNewChannelWav != null)
      set("eavesdrop_indicate_new", optionalNewChannelWav);
    if (optionalIdleWav != null)
      set("eavesdrop_indicate_idle", optionalIdleWav);
    if (enableDTMF)
      set("eavesdrop_enable_dtmf", "true");

    sendExeMesg("eavesdrop", uuid);
  }

  /**
   * Places the calling channel in loopback mode. It simply returns everything
   * sent, including voice, DTMF, etc. Consider it an "echo test". It is
   * generally useful for checking delay in a call path.
   *
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_echo">FreeSWITCH wiki</a>
   */
  public void echo() throws ExecuteException {
    sendExeMesg("echo");
  }

  /**
   * This application is used to play a file endlessly and the playing cannot
   * be stopped externally.
   *
   * @param file to play
   * @throws ExecuteException
   */
  public void endlessPlayback(String file) throws ExecuteException {
    sendExeMesg("endless_playback", file);
  }

  /**
   * Eval can be used to execute an internal API or simply log some text to
   * the console.
   *
   * @param string to eval
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_eval">FreeSWITCH wiki</a>
   */
  public void eval(String string) throws ExecuteException {
    sendExeMesg("eval", string);
  }

  /**
   * Event application can be used to fire aribtrary events.
   *
   * @param event to send ex:
   *              Event-Subclass=myevent::notify,Event-Name=CUSTOM,key1
   *              =value1,key2=value2
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_event">FreeSWITCH wiki</a>
   */
  public void event(String event) throws ExecuteException {
    sendExeMesg("event", event);
  }

  /**
   * execute an extension from within another extension with this dialplan
   * application.
   *
   * @param extension
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_execute_extension">FreeSWITCH wiki</a>
   */
  public void executeExtension(String extension) throws ExecuteException {
    executeExtension(extension, null, null);
  }

  /**
   * execute an extension from within another extension with this dialplan
   * application.
   *
   * @param extension
   * @param dialplan
   * @param context   (will only be set if optionalDialplan is not null)
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_execute_extension">FreeSWITCH wiki</a>
   */
  public void executeExtension(String extension, String dialplan,
                               String context) throws ExecuteException {
    sendExeMesg("execute_extension", extension
      + (dialplan != null ? " " + dialplan
      + (context != null ? " " + context : "") : ""));
  }

  /**
   * Exports a channel variable from the A leg to the B leg. Variables and
   * their values will be replicated in any new channels created from the one
   * export was called.
   *
   * @param key   channel variable name
   * @param value channel variable value
   * @param local to only export to the B leg false, otherwise true
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_export">FreeSWITCH wiki</a>
   */
  public void export(String key, String value, boolean local)
    throws ExecuteException {
    sendExeMesg("export", (local ? "" : "nolocal:") + key + "=" + value);
  }

  /**
   * When a fax is detected, the call will be routed to the ext in the context
   *
   * @param context
   * @param ext
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_fax_detect">FreeSWITCH wiki</a>
   */
  public void faxDetect(String context, String ext) throws ExecuteException {
    sendExeMesg("tone_detect", "fax 1100 r +5000 transfer " + ext + " XML "
      + context);
  }

  /**
   * Flushes DTMFs received on a channel. Useful in cases where callers may
   * have entered extra digits in one dialog and you want to flush them out
   * before sending them into another dialog.
   *
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_flush_dtmf">FreeSWITCH wiki</a>
   */
  public void flushDTMF() throws ExecuteException {
    sendExeMesg("flush_dtmf");
  }

  /**
   * Generate TGML tones.
   *
   * @param tone          ex: Generate a 500ms beep at 800Hz, tone = "%(500,0,800)"
   * @param optionalLoops set to a non zero number, -1 for infinate loop
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_gentones">FreeSWITCH wiki</a>
   * @see <a href="http://wiki.freeswitch.org/wiki/TGML">FreeSWITCH wiki</a>
   */
  public void gentones(String tone, int optionalLoops)
    throws ExecuteException {
    sendExeMesg("gentones", tone
      + (optionalLoops != 0 ? "|" + optionalLoops : ""));
  }

  /**
   * adds/deletes groups to/from the db(internal db or ODBC) and allows calls
   * to these groups in conjunction with the bridge-application. Depends on
   * mod_db and mod_dptools.
   *
   * @param action    (insert|delete|call )
   * @param groupName ex: :01@example.com
   * @param url       ex: sofia/gateway/provider/0123456789
   * @throws ExecuteException
   * @see <a href ="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_group">FreeSWITCH wiki</a>
   */
  public void group(String action, String groupName, String url)
    throws ExecuteException {
    sendExeMesg("group", action + ":" + groupName + ":" + url);
  }

  /**
   * Hangs up a channel, with an optional reason supplied.
   *
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_hangup">FreeSWITCH wiki</a>
   */
  public void hangup() throws ExecuteException {
    hangup("");
  }

  /**
   * Hangs up a channel, with an optional reason supplied.
   *
   * @param reason if not null the hangup reason, ex: USER_BUSY
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_hangup">FreeSWITCH wiki</a>
   */
  public void hangup(String reason) throws ExecuteException {
    sendExeMesg("hangup", reason);
  }

  /**
   * Dumps channel information to console.
   *
s   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_info">FreeSWITCH wiki</a>
   */
  public void info() throws ExecuteException {
    info("");
  }

  /**
   * Dumps channel information to console.
   *
   * @param level if not null the level to log. Ex: notice Ex:
   *              bridge_pre_execute_bleg_app=info
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_info">FreeSWITCH wiki</a>
   */
  public void info(String level) throws ExecuteException {
    sendExeMesg("info", level);
  }

  /**
   * Allows one channel to bridge itself to the a or b leg of another call.
   * The remaining leg of the original call gets hungup
   *
   * @param uuid
   * @param bleg intercept the b leg of the call
   * @throws ExecuteException
   */
  public void intercept(String uuid, boolean bleg) throws ExecuteException {
    sendExeMesg("intercept", (bleg ? "-bleg " : "") + uuid);
  }

  /**
   * Logs a string of text to the console
   *
   * @param message
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_log">FreeSWITCH wiki</a>
   * @see <a href="http://wiki.freeswitch.org/wiki/Mod_logfile">FreeSWITCH wiki</a>
   */
  public void log(String message) throws ExecuteException {
    log("", message);
  }

  /**
   * Logs a string of text to the console
   *
   * @param level   ex: DEBUG, INFO
   * @param message
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_log">FreeSWITCH wiki</a>
   * @see <a href="http://wiki.freeswitch.org/wiki/Mod_logfile">FreeSWITCH wiki</a>
   */
  public void log(String level, String message)
    throws ExecuteException {
    sendExeMesg("log", (level != null ? level + " " : "")
      + message);
  }

  /**
   * Creates a directory. Also creates parent directories by default(When they
   * don't exist).
   *
   * @param path
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_mkdir">FreeSWITCH wiki</a>
   */
  public void mkdir(String path) throws ExecuteException {
    sendExeMesg("mkdir", path);
  }

  /**
   * Places a channel "on hold" in the switch, instead of in the phone. Allows
   * for a number of different options, including: Set caller in a place where
   * the channel won't be hungup on, while waiting for someone to talk to.
   * Generic "hold" mechanism, where you transfer a caller to it. Please note
   * that to retrieve a call that has been "parked", you'll have to bridge to
   * them or transfer the call to a valid location. Also, remember that
   * parking a call does *NOT* supply music on hold or any other media. Park
   * is quite literally a way to put a call in limbo until you you
   * bridge/uuid_bridge or transfer/uuid_transfer it. For a different means of
   * using 'park', see mod_fifo.
   *
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_park">FreeSWITCH wiki</a>
   */
  public void park() throws ExecuteException {
    sendExeMesg("park");
  }

  /**
   * Speak a phrase of text using a predefined phrase macro. (For more
   * information on TTS see mod_cepstral and OpenMRCP.) See also the speech
   * phrase management page for more information and examples; This command
   * relies on the configuration in the phrases section of the freeswitch.xml
   * file and including xml files in lang/en/*.xml. Following is a sample of
   * phrases management:
   *
   * @param macroName ex: spell, timespec, saydate
   * @param data
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_phrase">FreeSWITCH wiki</a>
   */
  public void phrase(String macroName, String data) throws ExecuteException {
    sendExeMesg("phrase", macroName + "," + data);
  }

  /**
   * Permits proper answering of multiple simultaneous calls to the same
   * pickup group.
   *
   * @param group
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_pickup">FreeSWITCH wiki</a>
   */
  public void pickup(String group) throws ExecuteException {
    sendExeMesg("pickup", group);
  }

  /**
   * Play while doing speech recognition. Result is stored in the
   * detect_speech_result channel variable.
   *
   * @param file
   * @param engine
   * @param grammer
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_play_and_detect_speech">FreeSWITCH wiki</a>
   */
  public String playAndDetectSpeech(String file, String engine, String grammer) throws ExecuteException {
    return playAndDetectSpeech(file, engine, grammer, null);
  }

  /**
   * Play while doing speech recognition. Result is stored in the
   * detect_speech_result channel variable.
   *
   * @param file
   * @param engine
   * @param params
   * @param grammer
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_play_and_detect_speech">FreeSWITCH wiki</a>
   */
  public String playAndDetectSpeech(String file, String engine, String grammer, String params) throws ExecuteException {
    CommandResponse resp = sendExeMesg("play_and_detect_speech", file
      + " detect:" + engine + " {"
      + (params != null ? params : "") + "}"
      + grammer);
    if (resp.isOk()) {
      EslMessage eslMessage = api.sendApiCommand("uuid_getvar", _uuid
        + " detect_speech_result");
      if (eslMessage.getBodyLines().size() > 0)
        return eslMessage.getBodyLines().get(0);
    } else {
      throw new ExecuteException(resp.getReplyText());
    }
    return null;
  }

  /**
   * Play a prompt and get digits.
   *
   * @param min
   * @param max
   * @param tries
   * @param terminator
   * @param file
   * @param invalidFile
   * @param regexp
   * @param timeout
   * @return collected digits or null if none
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_play_and_get_digits">FreeSWITCH wiki</a>
   */
  public String playAndGetDigits(int min, int max, int tries, int timeout,
                                 char terminator, String file, String invalidFile, String regexp,
                                 int digitTimeout) throws ExecuteException {

    String id = UUID.randomUUID().toString();

    StringBuilder sb = new StringBuilder();
    sb.append(min).append(" ");
    sb.append(max).append(" ");
    sb.append(tries).append(" ");
    sb.append(timeout).append(" ");
    sb.append(terminator).append(" ");
    sb.append(file).append(" ");
    sb.append(invalidFile).append(" ");
    sb.append(id).append(" ");
    sb.append(regexp).append(" ");
    sb.append(digitTimeout);

    CommandResponse resp = sendExeMesg("playAndGetDigits", sb.toString());

    if (resp.isOk()) {
      EslMessage eslMessage = api.sendApiCommand("uuid_getvar", _uuid
        + " " + id);
      if (eslMessage.getBodyLines().size() > 0)
        return eslMessage.getBodyLines().get(0);
    } else {
      throw new ExecuteException(resp.getReplyText());
    }
    return null;
  }

  /**
   * Plays a sound file on the current channel.
   *
   * @param file
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_playback">FreeSWITCH wiki</a>
   */
  public void playback(String file)
    throws ExecuteException {
    playback(file, "");
  }

  /**
   * Plays a sound file on the current channel.
   *
   * @param file
   * @param data ex: var1=val1,var2=val2 adds specific vars that will be sent
   *                     in PLAYBACK_START and PLAYBACK_STOP events
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_playback">FreeSWITCH wiki</a>
   */
  public void playback(String file, String data)
    throws ExecuteException {
    sendExeMesg("playback", file
      + (data != null ? " {" + data + "}" : ""));
  }

  /**
   * equivalent to a SIP status code 183 with SDP. (This is the same as cmd
   * Progress in Asterisk.) It establishes media (early media) but does not
   * answer. You can use this for example to send an in-band error message to
   * the caller before disconnecting them (pre_answer, playback, reject with a
   * cause code of xxxx).
   *
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_pre_answer">FreeSWITCH wiki</a>
   */
  public void preAnswer() throws ExecuteException {
    sendExeMesg("pre_answer");
  }

  /**
   * Sends an event of either type PRESENCE_IN or PRESENCE_OUT. Currently,
   * this function is not very useful in conjunction with sofia. This does not
   * affect the presence of hook state for use with BLF either, but sending an
   * event that expresses the user's hook state does.
   *
   * @param user
   * @param in      true if in, false if out
   * @param rpid    ex: dnd, unavailable
   * @param message
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_presence">FreeSWITCH wiki</a>
   */
  public void presence(String user, boolean in, String rpid, String message)
    throws ExecuteException {
    sendExeMesg("presence", (in ? "in" : "out") + "|" + user + "|" + rpid
      + "|" + message);
  }

  /**
   * Set caller privacy on calls.
   *
   * @param type ex: no, yes, name, full, member
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_privacy">FreeSWITCH wiki</a>
   */
  public void privacy(String type) throws ExecuteException {
    sendExeMesg("privacy", type);
  }

  /**
   * Send DTMF digits after a bridge is successful from the session using the
   * method(s) configured on the endpoint in use. use the character w for a .5
   * second delay and the character W for a 1 second delay.
   *
   * @param digits
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_queue_dtmf">FreeSWITCH wiki</a>
   */
  public void queueDTMF(String digits) throws ExecuteException {
    sendExeMesg("dtmf_digits", digits);
  }


  /**
   * Send DTMF digits after a bridge is successful from the session using the
   * method(s) configured on the endpoint in use. use the character w for a .5
   * second delay and the character W for a 1 second delay.
   *
   * @param digits
   * @param unit     how long to wait units
   * @param duration wait duration in units
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_queue_dtmf">FreeSWITCH wiki</a>
   */
  public void queueDTMF(String digits, TimeUnit unit, int duration) throws ExecuteException {
    sendExeMesg("dtmf_digits", digits + "@" + TimeUnit.MILLISECONDS.convert(duration, unit));
  }

  /**
   * Read DTMF (touch-tone) digits.
   *
   * @param min         Minimum number of digits to fetch.
   * @param max         Maximum number of digits to fetch.
   * @param soundFile   Sound file to play before digits are fetched.
   * @param timeout     Number of milliseconds to wait on each digit
   * @param terminators Digits used to end input if less than <min> digits have been
   *                    pressed. (Typically '#')
   * @return read string or null
   * @throws ExecuteException
   * @see <a href="http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_read">FreeSWITCH wiki</a>
   */
  public String read(int min, int max, String soundFile, long timeout,
                     String terminators) throws ExecuteException {

    String id = UUID.randomUUID().toString();

    StringBuilder sb = new StringBuilder();
    sb.append(min).append(" ");
    sb.append(max).append(" ");
    sb.append(soundFile).append(" ");
    sb.append(id).append(" ");
    sb.append(timeout).append(" ");
    sb.append(terminators);

    CommandResponse resp = sendExeMesg("read", sb.toString());

    if (resp.isOk()) {
      EslMessage eslMessage = api.sendApiCommand("uuid_getvar", _uuid + " " + id);
      if (eslMessage.getBodyLines().size() > 0) {
        return eslMessage.getBodyLines().get(0);
      }
    } else {
      throw new ExecuteException(resp.getReplyText());
    }
    return null;
  }

  /**
   * Set a channel variable for the channel calling the application.
   *
   * @param key   channel_variable name
   * @param value channel_variable value
   * @throws ExecuteException
   */
  public void set(String key, String value) throws ExecuteException {
    sendExeMesg("set", key + "=" + value);
  }

  private CommandResponse sendExeMesg(String app) throws ExecuteException {
    return sendExeMesg(app, null);
  }

  private CommandResponse sendExeMesg(String app, String args) throws ExecuteException {
    SendMsg msg = new SendMsg();
    msg.addCallCommand("execute");
    msg.addExecuteAppName(app);
    if (args != null)
      msg.addExecuteAppArg(args);
    CommandResponse resp = api.sendMessage(msg);
    if (!resp.isOk())
      throw new ExecuteException(resp.getReplyText());
    else
      return resp;
  }

}
