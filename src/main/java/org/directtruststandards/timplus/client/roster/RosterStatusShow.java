package org.directtruststandards.timplus.client.roster;

public enum RosterStatusShow
{
	AVAILABLE("Available"),
	
	AWAY("Away"),
	
	DND("Do Not Disturb"),
	
	PRIVATE("Private/Appear Offline");
	
	protected final String display;
	
	private  RosterStatusShow(String display)
	{
		this.display = display;
	}
	
	public String getDisplay()
	{
		return this.display;
	}
}
