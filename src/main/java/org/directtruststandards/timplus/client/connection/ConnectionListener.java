package org.directtruststandards.timplus.client.connection;

import org.jivesoftware.smack.AbstractXMPPConnection;

public interface ConnectionListener
{
	public void onConnected(AbstractXMPPConnection con);
	
	public void onConnecting();
	
	public void onDisconnectedWithError(Exception e);
	
	public void onDiconneected();
}
