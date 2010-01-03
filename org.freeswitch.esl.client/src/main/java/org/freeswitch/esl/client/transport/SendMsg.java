package org.freeswitch.esl.client.transport;

import java.util.ArrayList;
import java.util.List;

public class SendMsg
{
    private final List<String> msgLines = new ArrayList<String>();
    
    public SendMsg()
    {
        msgLines.add( "sendmsg" );
    }
    
    public SendMsg( String uuid )
    {
        msgLines.add( "sendmsg " + uuid );
    }
    
    public void addCallCommand( String command )
    {
        msgLines.add( "call-command: " + command );
    }
    
    public void addExecuteAppName( String appName )
    {
        msgLines.add( "execute-app-name: " + appName );
    }
    
    public void addExecuteAppArg( String arg )
    {
        msgLines.add( "execute-app-arg: " + arg );
    }
    
    public void addLoops( int count )
    {
        msgLines.add( "loops: " + count );
    }

    public void addHangupCause( String cause )
    {
        msgLines.add( "hangup-cause: " + cause );
    }

    public void addNomediaUuid( String info )
    {
        msgLines.add( "nomedia-uuid: " + info );
    }
    
    public void addEventLock()
    {
        msgLines.add( "event-lock: true" );
    }
    
    public void addGenericLine( String name, String value )
    {
        msgLines.add( name + ": " + value );
    }
    
    public List<String> getMsgLines()
    {
        return msgLines;
    }
}
