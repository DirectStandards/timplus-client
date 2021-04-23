package org.directtruststandards.timplus.client.roster;

import org.jivesoftware.smack.packet.Presence;

public interface UserActivityListener
{
	public void onUserActivityStateChange(Presence.Mode mode);
}
