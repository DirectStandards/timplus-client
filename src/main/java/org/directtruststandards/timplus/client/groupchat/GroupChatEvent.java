package org.directtruststandards.timplus.client.groupchat;

import org.jivesoftware.smackx.muc.MultiUserChat;

public class GroupChatEvent
{
	public enum Event
	{
		ROOM_ENTER,
		
		ROOM_EXIT,
		
		PARTICIPANT_ENTER,
		
		PARTICIPANT_EXIT
	}
	
	protected MultiUserChat room;
	
	protected Event event;
	
	public GroupChatEvent()
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

	public Event getEvent()
	{
		return event;
	}

	public void setEvent(Event event)
	{
		this.event = event;
	}
	
	
}
