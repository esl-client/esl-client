package org.freeswitch.esl.client.dptools;

import java.util.UUID;

import org.freeswitch.esl.client.internal.IModEslApi;
import org.freeswitch.esl.client.transport.CommandResponse;
import org.freeswitch.esl.client.transport.SendMsg;
import org.freeswitch.esl.client.transport.message.EslMessage;

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
     * @param message
     *            to display
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_Send_Display
     */
    public void sendDiplay(String message) throws ExecuteException {
        sendExeMesg("send_display", message);
    }

    /**
     * Answers an incoming call or session.
     * 
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_answer
     */
    public void answer() throws ExecuteException {
        sendExeMesg("answer");

    }

    /**
     * Make an attended transfer.
     * 
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_att_xfer
     * @param transfer
     *            ex: sofia/default/${attxfer_callthis}
     */
    public void attAnswer(String channelUrl) throws ExecuteException {
        sendExeMesg("att_xfer", channelUrl);
    }

    /**
     * 
     * @param key
     *            the button you want to respond to after the * button is
     *            pressed. If you wanted to respond to *1, you would put 1 in
     *            place of KEY. You are limited to a single digit.
     * @param leg
     *            which call leg(s) to listen on. Acceptable parameters are a, b
     *            or ab.
     * @param flags
     *            modifies the behavior. The following flags are available: a -
     *            Respond on A leg, b - Respond on B leg, o - Respond on
     *            opposite leg, s - Respond on same leg, i - Execute inline, 1 -
     *            Unbind this meta_app after it is used one time
     * @param application
     *            is which application you want to execute.
     * @param params
     *            are the arguments you want or need to provide to the
     *            APPLICATION.
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_bind_meta_app
     * @throws ExecuteException
     */
    public void bindMetaApp(char key, String leg, char flags,
            String application, String params) throws ExecuteException {
        StringBuilder sb = new StringBuilder(key);
        sb.append(" ").append(leg);
        sb.append("").append(flags);
        sb.append(" ").append(application);
        sb.append("::").append(params);
        sendExeMesg("bind_meta_app", sb.toString());
    }

    /**
     * Cancels currently running application on the given UUID. Dialplan
     * execution proceeds to the next application. Optionally clears all
     * unprocessed events (queued applications) on the channel.
     * 
     * @param all
     *            clear all unprocessed events (queued applications) on the
     *            channel, otherwise just the current application.
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_break
     * @throws ExecuteException
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
     * @param target
     *            endpoint
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_bridge
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
     * @param key
     *            channel variable name
     * @param value
     *            channel variable value
     * @param local
     *            to only export to the B leg false, otherwise true
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_bridge_export
     * @throws ExecuteException
     */
    public void bridgeExport(String key, String value, boolean local)
            throws ExecuteException {
        StringBuilder sb = new StringBuilder();
        if(!local)
            sb.append("nolocal:");
        sb.append(key);
        sb.append("=");
        sb.append(value);
        sendExeMesg("bridge_export",sb.toString());
    }

    /**
     * Send a text message to a IM client.
     * 
     * @param proto
     *            ex: sip
     * @param from
     *            ex: 1000@127.0.0.1
     * @param to
     *            ex: 1001@127.0.0.1
     * @param message
     *            ex: Hello chat from freeswitch
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_chat
     * @throws ExecuteException
     */
    public void chat(String proto, String from, String to, String message)
            throws ExecuteException {
        StringBuilder sb = new StringBuilder(proto);
        sb.append("|").append(from);
        sb.append("|").append(to);
        sb.append("|").append(message);
        sendExeMesg("chat", sb.toString());
    }

    /**
     * cng plc is just an app that says to perform plc on any lost packets and
     * execute on originate. It is like execute on answer, etc. but only for
     * outbound calls during originate.
     * 
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_cng_plc
     * @throws ExecuteException
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
     * @param endpoint
     *            SIP URI to contact (with or without "sip:")
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_deflect
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
     * @param ms
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_delay_echo
     */
    public void delayEcho(long ms) throws ExecuteException {
        sendExeMesg("delay_echo", ms + "");
    }

    /**
     * Implements speech recognition.
     * 
     * @param args
     *            <mod_name> <gram_name> <gram_path> [<addr>] grammar
     *            <gram_name> [<path>] grammaron <gram_name> grammaroff
     *            <gram_name> grammarsalloff nogrammar <gram_name> param <name>
     *            <value> pause resume start_input_timers stop
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_detect_speech
     */
    public void detectSpeech(String args) throws ExecuteException {
        sendExeMesg("detect_speech", args);
    }

    /**
     * Displace file. Plays a file or stream to a channel.
     * 
     * @param path
     *            any sound format FreeSWITCH supports, wav, local_steam, shout
     *            etc.
     * @param optionalFlags
     *            flags to stream, null if none
     * @param optionalTimeLimitMillis
     *            optional time limit, 0 for none
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_displace_session
     */
    public void displaceSession(String path, String optionalFlags,
            long optionalTimeLimitMillis) throws ExecuteException {
        StringBuilder sb = new StringBuilder(path);
        if(nn(optionalFlags))
            sb.append(" ").append(optionalFlags);
        if(optionalTimeLimitMillis > 0 )
            sb.append(" +").append(optionalTimeLimitMillis);
        
        sendExeMesg("displace_session",sb.toString());
    }

    /**
     * Provides the ability to spy on a channel. It often referred to as call
     * barge. For persistent spying on a user see Mod_spy.
     * 
     * DTMF signals during eavesdrop, 2 to speak with the uuid, 1 to speak with
     * the other half, 3 to engage a three way, 0 to restore eavesdrop, * to
     * next channel.
     * 
     * @param uuid
     *            uuid of the call or 'all' for all
     * @param optionalGroupId
     *            if specified, eavesdrop only works with channels that have an
     *            "eavesdrop_group" channel variable set to the same name.
     * @param optionalFailedWav
     *            ex: /sounds/failed.wav
     * @param optionalNewChannelWav
     *            ex: /sounds/new_chan_announce.wav
     * @param optionalIdleWav
     *            ex: /sounds/idle.wav
     * @param enableDTMF
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_eavesdrop
     */
    public void eavesdrop(String uuid, String optionalGroupId,
            String optionalFailedWav, String optionalNewChannelWav,
            String optionalIdleWav, boolean enableDTMF) throws ExecuteException {

        if (nn(optionalGroupId))
            set("eavesdrop_require_group", optionalGroupId);
        if (nn(optionalFailedWav))
            set("eavesdrop_indicate_failed", optionalFailedWav);
        if (nn(optionalNewChannelWav))
            set("eavesdrop_indicate_new", optionalNewChannelWav);
        if (nn(optionalIdleWav))
            set("eavesdrop_indicate_idle", optionalIdleWav);
        set("eavesdrop_enable_dtmf", String.valueOf(enableDTMF));

        sendExeMesg("eavesdrop", uuid);
    }

    /**
     * Places the calling channel in loopback mode. It simply returns everything
     * sent, including voice, DTMF, etc. Consider it an "echo test". It is
     * generally useful for checking delay in a call path.
     * 
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_echo
     */
    public void echo() throws ExecuteException {
        sendExeMesg("echo");
    }

    /**
     * This application is used to play a file endlessly and the playing cannot
     * be stopped externally.
     * 
     * @param file
     *            to play
     * @throws ExecuteException
     */
    public void endlessPlayback(String file) throws ExecuteException {
        sendExeMesg("endless_playback", file);
    }

    /**
     * Eval can be used to execute an internal API or simply log some text to
     * the console.
     * 
     * @param string
     *            to eval
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_eval
     */
    public void eval(String string) throws ExecuteException {
        sendExeMesg("eval", string);
    }

    /**
     * Event application can be used to fire aribtrary events.
     * 
     * @param event
     *            to send ex:
     *            Event-Subclass=myevent::notify,Event-Name=CUSTOM,key1
     *            =value1,key2=value2
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_event
     */
    public void event(String event) throws ExecuteException {
        sendExeMesg("event", event);
    }

    /**
     * execute an extension from within another extension with this dialplan
     * application.
     * 
     * 
     * @param extension
     * @param optionalDialplan
     * @param optionalContext
     *            (will only be set if optionalDialplan is not null)
     * @throws ExecuteException
     * @see http
     *      ://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_execute_extension
     */
    public void executeExtension(String extension, String optionalDialplan,
            String optionalContext) throws ExecuteException {
        StringBuilder sb = new StringBuilder(extension);
        if(nn(optionalDialplan)) {
            sb.append(" ").append(optionalDialplan);
            if(nn(optionalContext))
                sb.append(" ").append(optionalContext);
        }
        sendExeMesg("execute_extension", sb.toString());
    }

    /**
     * Exports a channel variable from the A leg to the B leg. Variables and
     * their values will be replicated in any new channels created from the one
     * export was called.
     * 
     * @param key
     *            channel variable name
     * @param value
     *            channel variable value
     * @param local
     *            to only export to the B leg false, otherwise true
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_export
     * @throws ExecuteException
     */
    public void export(String key, String value, boolean local)
            throws ExecuteException {
        StringBuilder sb = new StringBuilder();
        if(!local)
            sb.append("nolocal:");
        sb.append(key);
        sb.append("=");
        sb.append(value);
        sendExeMesg("export", sb.toString());
    }

    /**
     * When a fax is detected, the call will be routed to the ext in the context
     * 
     * @param context
     * @param ext
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_fax_detect
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
     * @see PlayAndGetDigits
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_flush_dtmf
     */
    public void flushDTMF() throws ExecuteException {
        sendExeMesg("flush_dtmf");
    }

    /**
     * Generate TGML tones.
     * 
     * @param tone
     *            ex: Generate a 500ms beep at 800Hz, tone = "%(500,0,800)"
     * @param optionalLoops
     *            set to a non zero nu,ber, -1 for infinate loop
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_gentones
     * @see http://wiki.freeswitch.org/wiki/TGML
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
     * @param action
     *            (insert|delete|call )
     * @param groupName
     *            ex: :01@example.com
     * @param url
     *            ex: sofia/gateway/provider/0123456789
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_group
     */
    public void group(String action, String groupName, String url)
            throws ExecuteException {
        sendExeMesg("group", action + ":" + groupName + ":" + url);
    }

    /**
     * Hangs up a channel, with an optional reason supplied.
     * 
     * @param optionalReason
     *            if not null the hangup reason, ex: USER_BUSY
     * @throws ExecuteException
     * @http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_hangup
     */
    public void hangup(String optionalReason) throws ExecuteException {
        sendExeMesg("hangup", optionalReason);
    }

    /**
     * Dumps channel information to console.
     * 
     * @param optionalLevel
     *            if not null the level to log. Ex: notice Ex:
     *            bridge_pre_execute_bleg_app=info
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_info
     */
    public void info(String optionalLevel) throws ExecuteException {
        sendExeMesg("info", optionalLevel);
    }

    /**
     * Allows one channel to bridge itself to the a or b leg of another call.
     * The remaining leg of the original call gets hungup
     * 
     * @param uuid
     * @param bleg
     *            intercept the b leg of the call
     * @throws ExecuteException
     */
    public void intercept(String uuid, boolean bleg) throws ExecuteException {
        sendExeMesg("intercept", (bleg ? "-bleg " : "") + uuid);
    }

    /**
     * Logs a string of text to the console
     * 
     * @param optionalLevel
     *            ex: DEBUG, INFO
     * @param message
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_log
     * @see http://wiki.freeswitch.org/wiki/Mod_logfile
     */
    public void log(String optionalLevel, String message)
            throws ExecuteException {
        sendExeMesg("log", (nn(optionalLevel) ? optionalLevel + " " : "")
                + message);
    }

    /**
     * Creates a directory. Also creates parent directories by default(When they
     * don't exist).
     * 
     * @param path
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_mkdir
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
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_park
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
     * @param macroName
     *            ex: spell, timespec, saydate
     * @param data
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_phrase
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
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_pickup
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
     * @param optionalParams
     * @param grammer
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_play_and_detect_speech
     */
    public String playAndDetectSpeech(String file, String engine,
            String optionalParams, String grammer) throws ExecuteException {
        StringBuilder sb = new StringBuilder(file);
        sb.append(" detect:");
        sb.append(engine);
        sb.append(" {");
        sb.append((nn(optionalParams) ? optionalParams : ""));
        sb.append("}");
        sb.append(grammer);
        CommandResponse resp = sendExeMesg("play_and_detect_speech",sb.toString());
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
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_play_and_get_digits
     */
    public String playAndGetDigits(int min, int max, int tries, int timeout,
            char terminator, String file, String invalidFile, String regexp,
            int digitTimeout) throws ExecuteException {

        String id = UUID.randomUUID().toString();

        StringBuilder sb = new StringBuilder(min);
        sb.append(" ").append(max);
        sb.append(" ").append(tries);
        sb.append(" ").append(timeout);
        sb.append(" ").append(terminator);
        sb.append(" ").append(file);
        sb.append(" ").append(invalidFile);
        sb.append(" ").append(id);
        sb.append(" ").append(regexp);
        sb.append(" ").append(digitTimeout);

        CommandResponse resp = sendExeMesg("play_and_get_digits", sb.toString());

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
     * @param optionalData
     *            ex: var1=val1,var2=val2 adds specific vars that will be sent
     *            in PLAYBACK_START and PLAYBACK_STOP events
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_playback
     */
    public void playback(String file, String optionalData)
            throws ExecuteException {
        StringBuilder sb = new StringBuilder(file);
        if(nn(optionalData)) {
            sb.append(" {");
            sb.append(optionalData);
            sb.append("}");
        }
        sendExeMesg("playback",sb.toString());
    }
    
    /**
     * equivalent to a SIP status code 183 with SDP. (This is the same as cmd
     * Progress in Asterisk.) It establishes media (early media) but does not
     * answer. You can use this for example to send an in-band error message to
     * the caller before disconnecting them (pre_answer, playback, reject with a
     * cause code of xxxx).
     * 
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_pre_answer
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
     * @param in
     *            true if in, false if out
     * @param rpid
     *            ex: dnd, unavailable
     * @param message
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_presence
     */
    public void presence(String user, boolean in, String rpid, String message)
            throws ExecuteException {
        StringBuilder sb = new StringBuilder(in ? "in" : "out");
        sb.append("|").append(user);
        sb.append("|").append(rpid);
        sb.append("|").append(message);
        
        sendExeMesg("presence", sb.toString());
    }

    /**
     * Set caller privacy on calls.
     * 
     * @param type
     *            ex: no, yes, name, full, member
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_privacy
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
     * @param optionalDurationsMillis ignored if <=0
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_queue_dtmf
     */
    public void queueDTMF(String digits, int optionalDurationsMillis)
            throws ExecuteException {
        sendExeMesg("dtmf_digits", digits
                + (optionalDurationsMillis > 0 ? "@" + optionalDurationsMillis
                        : ""));
    }

    /**
     * Read DTMF (touch-tone) digits.
     * 
     * @param min
     *            Minimum number of digits to fetch.
     * @param max
     *            Maximum number of digits to fetch.
     * @param soundFile
     *            Sound file to play before digits are fetched.
     * @param timeout
     *            Number of milliseconds to wait on each digit
     * @param terminators
     *            Digits used to end input if less than <min> digits have been
     *            pressed. (Typically '#')
     * @return read string or null
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_read
     */
    public String read(int min, int max, String soundFile, long timeout,
            String terminators) throws ExecuteException {

        String id = UUID.randomUUID().toString();

        StringBuilder sb = new StringBuilder(min);
        sb.append(" ").append(max);
        sb.append(" ").append(soundFile);
        sb.append(" ").append(id);
        sb.append(" ").append(timeout);
        sb.append(" ").append(terminators);

        CommandResponse resp = sendExeMesg("read", sb.toString());

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
     * Record is used for recording messages, like in a voicemail system. This
     * application will record a file to file
     * 
     * @param file
     * @param optionalTimeLimitSeconds
     *            the maximum duration of the recording.
     * @param optionalSilenceThreshold
     *            is the energy level.
     * @param optionalSilenceHits
     *            how many positive hits on being below that thresh you can
     *            tolerate to stop default hits are sample rate * 3 / the number
     *            of samples per frame so the default, if missing, is 3.
     * @param wateResources
     *            By default record doesn't send RTP packets. This is generally
     *            acceptable, but for longer recordings or depending on the RTP
     *            timer of your gateway, your channel may hangup with cause
     *            MEDIA_TIMEOUT. Setting this variable will 'waste' bandwidth by
     *            sending RTP even during recording. The value can be
     *            true/false/<desired silence factor>. By default the silence
     *            factor is 1400 if true
     * @param append
     *            append or overwite if file exists
     * @param optionalRecordTile
     *            store in the file header meta data (provided the file format
     *            supports meta headers).
     * @param optionalRecordCopyright
     *            store in the file header meta data (provided the file format
     *            supports meta headers).
     * @param optionalRecordSoftware
     *            store in the file header meta data (provided the file format
     *            supports meta headers).
     * @param optionalRecordArtist
     *            store in the file header meta data (provided the file format
     *            supports meta headers).
     * @param optionalRecordComment
     *            store in the file header meta data (provided the file format
     *            supports meta headers).
     * @param optionalRecordDate
     *            store in the file header meta data (provided the file format
     *            supports meta headers).
     * @param optionalRecordRate
     *            the sample rate of the recording.
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_record
     */
    public void record(String file, int optionalTimeLimitSeconds,
            int optionalSilenceThreshold, int optionalSilenceHits,
            boolean wateResources, boolean append, String optionalRecordTile,
            String optionalRecordCopyright, String optionalRecordSoftware,
            String optionalRecordArtist, String optionalRecordComment,
            String optionalRecordDate, int optionalRecordRate)
            throws ExecuteException {
        record("record", file, optionalTimeLimitSeconds,
                optionalSilenceThreshold, optionalSilenceHits, wateResources,
                append, optionalRecordTile, optionalRecordCopyright,
                optionalRecordSoftware, optionalRecordArtist,
                optionalRecordComment, optionalRecordDate, optionalRecordRate);
    }
    
    /**
     * Records an entire phone call or session.
     * 
     * @param file
     * @param optionalTimeLimitSeconds
     *            the maximum duration of the recording.
     * @param optionalSilenceThreshold
     *            is the energy level.
     * @param optionalSilenceHits
     *            how many positive hits on being below that thresh you can
     *            tolerate to stop default hits are sample rate * 3 / the number
     *            of samples per frame so the default, if missing, is 3.
     * @param wateResources
     *            By default record doesn't send RTP packets. This is generally
     *            acceptable, but for longer recordings or depending on the RTP
     *            timer of your gateway, your channel may hangup with cause
     *            MEDIA_TIMEOUT. Setting this variable will 'waste' bandwidth by
     *            sending RTP even during recording. The value can be
     *            true/false/<desired silence factor>. By default the silence
     *            factor is 1400 if true
     * @param append
     *            append or overwite if file exists
     * @param optionalRecordTile
     *            store in the file header meta data (provided the file format
     *            supports meta headers).
     * @param optionalRecordCopyright
     *            store in the file header meta data (provided the file format
     *            supports meta headers).
     * @param optionalRecordSoftware
     *            store in the file header meta data (provided the file format
     *            supports meta headers).
     * @param optionalRecordArtist
     *            store in the file header meta data (provided the file format
     *            supports meta headers).
     * @param optionalRecordComment
     *            store in the file header meta data (provided the file format
     *            supports meta headers).
     * @param optionalRecordDate
     *            store in the file header meta data (provided the file format
     *            supports meta headers).
     * @param optionalRecordRate
     *            the sample rate of the recording.
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_record_session
     */
    public void recordSession(String file, int optionalTimeLimitSeconds,
            int optionalSilenceThreshold, int optionalSilenceHits,
            boolean wateResources, boolean append, String optionalRecordTile,
            String optionalRecordCopyright, String optionalRecordSoftware,
            String optionalRecordArtist, String optionalRecordComment,
            String optionalRecordDate, int optionalRecordRate)
            throws ExecuteException {
        record("record_session", file, optionalTimeLimitSeconds,
                optionalSilenceThreshold, optionalSilenceHits, wateResources,
                append, optionalRecordTile, optionalRecordCopyright,
                optionalRecordSoftware, optionalRecordArtist,
                optionalRecordComment, optionalRecordDate, optionalRecordRate);
    }

    private void record(String action, String file, int optionalTimeLimitSeconds,
            int optionalSilenceThreshold, int optionalSilenceHits,
            boolean wateResources, boolean append, String optionalRecordTile,
            String optionalRecordCopyright, String optionalRecordSoftware,
            String optionalRecordArtist, String optionalRecordComment,
            String optionalRecordDate, int optionalRecordRate)
            throws ExecuteException {

        if (nn(optionalRecordTile))
            set("RECORD_TITLE", optionalRecordTile);
        if (nn(optionalRecordCopyright))
            set("RECORD_COPYRIGHT", optionalRecordCopyright);
        if (nn(optionalRecordSoftware))
            set("RECORD_SOFTWARE", optionalRecordSoftware);
        if (nn(optionalRecordArtist))
            set("RECORD_ARTIST", optionalRecordArtist);
        if (nn(optionalRecordComment))
            set("RECORD_COMMENT", optionalRecordComment);
        if (nn(optionalRecordDate))
            set("RECORD_DATE", optionalRecordDate);
        if (optionalRecordRate > 0)
            set("record_sample_rate", String.valueOf(optionalRecordRate));

        set("RECORD_APPEND", String.valueOf(append));
        set("record_waste_resources", String.valueOf(wateResources));

        StringBuilder sb = new StringBuilder(file);
        if (optionalTimeLimitSeconds > 0) {
            sb.append(" ").append(optionalTimeLimitSeconds);
            if (optionalSilenceThreshold > 0) {
                sb.append(" ").append(optionalSilenceThreshold);
                if (optionalSilenceHits > 0) {
                    sb.append(" ").append(optionalSilenceHits);
                }
            }
        }

        sendExeMesg(action, sb.toString());
    }
    
    /**
     * Can redirect a channel to another endpoint, you must take care to not
     * redirect incompatible channels, as that wont have the desired effect. Ie
     * if you redirect to a SIP url it should be a SIP channel. By providing a
     * single SIP URI FreeSWITCH will issue a 302 "Moved Temporarily":
     * 
     * @param endpoint
     *            ex:"sip:foo@bar.com " or "sip:foo@bar.com,sip:foo@end.com"
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_redirect
     */
    public void redirect(String endpoint) throws ExecuteException {
        sendExeMesg("redirect", endpoint);
    }

    /**
     * Send SIP session respond code to the SIP device.
     * 
     * @param code
     *            ex: "407" or "480 Try again later"
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_respond
     */
    public void respond(String code) throws ExecuteException {
        sendExeMesg("respond", code);
    }
    
    /**
     * This causes an 180 Ringing to be sent to the originator.
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_ring_ready
     */
    public void ringReady() throws ExecuteException {
        sendExeMesg("ring_ready");
    }
    
    /**
     * The say application will use the pre-recorded sound files to read or say
     * various things like dates, times, digits, etc. The say application can
     * read digits and numbers as well as dollar amounts, date/time values and
     * IP addresses. It can also spell out alpha-numeric text, including
     * punctuation marks. There's a transcript of the pre-recorded files in the
     * sources under docs/phrase/phrase_en.xml
     * 
     * @param moduleName
     *            Module name is usually the channel language, e.g. "en" or "es"
     * @param sayType
     *            Say type is one of the following NUMBER ITEMS PERSONS MESSAGES
     *            CURRENCY TIME_MEASUREMENT CURRENT_DATE CURRENT_TIME
     *            CURRENT_DATE_TIME TELEPHONE_NUMBER TELEPHONE_EXTENSION URL
     *            IP_ADDRESS EMAIL_ADDRESS POSTAL_ADDRESS ACCOUNT_NUMBER
     *            NAME_SPELLED NAME_PHONETIC SHORT_DATE_TIME
     * @param sayMethod
     *            Say method is one of the following N/A PRONOUNCED ITERATED
     *            COUNTED
     * @param optionalGender
     *            Say gender is one of the following (For languages with
     *            gender-specific grammar, like French and German) FEMININE
     *            MASCULINE NEUTER
     * 
     * @param text
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_say
     */
    public void say(String moduleName, String sayType, String sayMethod,
            String optionalGender, String text) throws ExecuteException {

        StringBuilder sb = new StringBuilder(moduleName);
        sb.append(" ").append(sayType);
        sb.append(" ").append(sayMethod);
        if (nn(optionalGender))
            sb.append(" ").append(optionalGender);
        sb.append(" ").append(text);

        sendExeMesg("say", sb.toString());
    }
    
    /**
     * Schedule future broadcast.
     * 
     * @param seconds
     *            the epoc time in the future, or the number of seconds in the
     *            future
     * @param interval
     *            is the param seconds an epoc time or interval
     * @param path
     *            ex: /tmp/howdy.wav
     * @param leg
     *            can be aleg,bleg,both
     * @throws ExecuteException
     */
    public void schedBroadcast(long seconds, boolean interval, String path,
            String leg) throws ExecuteException {
        StringBuilder sb = new StringBuilder();
        if (interval)
            sb.append('+');
        sb.append(seconds);
        sb.append(" ").append(path);
        sb.append(" ").append(leg);
        sendExeMesg("sched_broadcast", sb.toString());
    }
    
    /**
     * The sched_hangup application allows you to schedule a hangup action for a
     * call, basically to limit call duration.
     * 
     * @param seconds
     *            the epoc time in the future, or the number of seconds in the
     *            future
     * @param interval
     *            is the param seconds an epoc time or interval
     * @param optionalCause
     *            ex:allotted_timeout
     * @throws ExecuteException
     */
    public void schedHangup(long seconds, boolean interval, String optionalCause)
            throws ExecuteException {
        StringBuilder sb = new StringBuilder();
        if (interval)
            sb.append('+');
        sb.append(seconds);
        if (nn(optionalCause))
            sb.append(" ").append(optionalCause);
        sendExeMesg("sched_hangup", sb.toString());
    }

    /**
     * Schedule a transfer in the future.
     * 
     * @param seconds
     *            the epoc time in the future, or the number of seconds in the
     *            future
     * @param interval
     *            is the param seconds an epoc time or interval
     * @param extension
     * @param optionalDialPlan
     * @param optionalContext
     * @throws ExecuteException
     * @see http://wiki.freeswitch.org/wiki/Misc._Dialplan_Tools_sched_transfer
     */
    public void schedTransfer(long seconds, boolean interval, String extension,
            String optionalDialPlan, String optionalContext)
            throws ExecuteException {
        StringBuilder sb = new StringBuilder();
        if (interval)
            sb.append('+');
        sb.append(seconds);
        sb.append(" ").append(extension);
        if (nn(optionalDialPlan)) {
            sb.append(" ").append(optionalDialPlan);
            if (nn(optionalContext)) {
                sb.append(" ").append(optionalContext);
            }
        }
        sendExeMesg("sched_transfer", sb.toString());
    }

    /**
     * Send DTMF digits from the session using the method(s) configured on the
     * endpoint in use. Use the character w for a .5 second delay and the
     * character W for a 1 second delay.
     * 
     * @param digits
     * @param optionalDurationMillis
     * @throws ExecuteException
     */
    public void sendDTMF(String digits, int optionalToneDurationMillis)
            throws ExecuteException {
        StringBuilder sb = new StringBuilder(digits);
        if (nn(optionalToneDurationMillis))
            sb.append('@').append(optionalToneDurationMillis);

        sendExeMesg("send_dtmf", sb.toString());
    }
    
    /**
     * Set a channel variable for the channel calling the application.
     * 
     * @param key
     *            channel_variable name
     * @param value
     *            channel_variable value
     * @throws ExecuteException
     */
    public void set(String key, String value) throws ExecuteException {
        sendExeMesg("set", key + "=" + value);
    }

    private CommandResponse sendExeMesg(String app) throws ExecuteException {
        return sendExeMesg(app, null);
    }

    private CommandResponse sendExeMesg(String app, String args)
            throws ExecuteException {
        SendMsg msg = new SendMsg();
        msg.addCallCommand("execute");
        msg.addExecuteAppName(app);
        if (nn(args))
            msg.addExecuteAppArg(args);
        CommandResponse resp = api.sendMessage(msg);
        if (!resp.isOk())
            throw new ExecuteException(resp.getReplyText());
        else
            return resp;
    }
    
    private boolean nn(Object obj) {return obj != null;}

}
