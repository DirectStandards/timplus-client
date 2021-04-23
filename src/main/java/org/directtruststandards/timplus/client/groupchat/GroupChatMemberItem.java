package org.directtruststandards.timplus.client.groupchat;

import org.jxmpp.jid.Jid;

public class GroupChatMemberItem
{
	protected Jid member;
	
	public GroupChatMemberItem()
	{
		
	}

	public Jid getMember()
	{
		return member;
	}

	public void setMember(Jid member)
	{
		this.member = member;
	}
	
	
}
