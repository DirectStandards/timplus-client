package org.directtruststandards.timplus.client.roster;

import org.jxmpp.jid.Jid;

public class RosterItem
{
	protected Presense presence;
	protected Presense presenceStatusText;
	protected Jid rosterJID;
	protected Subscription sub;
	
	public RosterItem()
	{
		
	}
	
	public Presense getPresence()
	{
		return presence;
	}

	public void setPresence(Presense presence)
	{
		this.presence = presence;
	}

	public Presense getPresenceStatusText()
	{
		return presenceStatusText;
	}

	public void setPresenceStatusText(Presense presenceStatusText)
	{
		this.presenceStatusText = presenceStatusText;
	}

	public Jid getRosterJID()
	{
		return rosterJID;
	}

	public void setRosterJID(Jid rosterJID)
	{
		this.rosterJID = rosterJID;
	}

	public Subscription getSub()
	{
		return sub;
	}

	public void setSub(Subscription sub)
	{
		this.sub = sub;
	}



	public enum Presense
	{
		AVAILABLE,
		
		UNAVAILABLE,
		
		NOT_AUTHORIZED,
		
		AWAY,
		
		DND,
		
		BLOCKED
	}
	
	public enum Subscription
	{
		NONE,
		
		REQUESTED,
		
		APPROVED,
		
		DENIED
	}
}	
