package org.directtruststandards.timplus.client.roster;

import org.jivesoftware.smackx.muc.MultiUserChat;

public class GroupChatItem
{
	protected MultiUserChat room; 
	
	public GroupChatItem()
	{
		
	}

	public MultiUserChat getRoom()
	{
		return room;
	}

	public void setRoom(MultiUserChat room)
	{
		this.room = room;
	}
	
	
}
