package org.directtruststandards.timplus.client.groupchat;

import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.InvitationListener;
import org.jivesoftware.smackx.muc.MucEnterConfiguration;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.MultiUserChat.MucCreateConfigFormHandle;
import org.jivesoftware.smackx.muc.packet.MUCUser.Invite;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;

public class GroupChatManager
{
	static protected GroupChatManager INSTANCE;
	
	protected AbstractXMPPConnection con;
	
	protected Map<Jid, GroupChatDialog> activeChats;
	
	protected MultiUserChatManager roomManager;
	
	protected DomainBareJid defaultGroupChatDomain;
	
	protected Collection<GroupChatEventListener> groupChatEventListeners; 
	
	public static synchronized GroupChatManager getInstance(AbstractXMPPConnection con)
	{
		if (INSTANCE == null)
			INSTANCE = new GroupChatManager(con);
		
		return INSTANCE;
	}
	
	public GroupChatManager(AbstractXMPPConnection con)
	{
		this.con = con;
		
		activeChats = new HashMap<>();
		
		groupChatEventListeners = new ArrayList<>();
		
		resetChatManagerConnection();
	}
	
	public AbstractXMPPConnection getConnection()
	{
		return this.con;
	}
	
	public void registerGroupChatEventListener(GroupChatEventListener listener)
	{
		if (!groupChatEventListeners.contains(listener))
			groupChatEventListeners.add(listener);
	}
	
	public void setConnection(AbstractXMPPConnection con)
	{
		if (this.con != con)
		{
			this.con = con;
			resetChatManagerConnection();
		}
	}
	
	protected void resetChatManagerConnection()
	{
		roomManager = MultiUserChatManager.getInstanceFor(con);
		try
		{
			final List<DomainBareJid> chatDomains = roomManager.getMucServiceDomains();
			if (!chatDomains.isEmpty())
			{
				/*
				 * TIM+ service providers should define a single group chat subdomain
				 */
				defaultGroupChatDomain = chatDomains.get(0);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		roomManager.addInvitationListener(new InvitationListener()
		{

			@Override
			public void invitationReceived(XMPPConnection conn, MultiUserChat room, EntityJid inviter,
					String reason, String password, Message message, Invite invitation)
			{
				GroupChatManager.this.invitationReceived(room, inviter);	
			}
		});
        
        for (GroupChatDialog chatDialog : activeChats.values())
        	chatDialog.resetChat(con);
        
	}
	
	public MultiUserChat createGroupChat()
	{
		MultiUserChat retVal = null;
		
		final String roomName = UUID.randomUUID().toString() + "@" + defaultGroupChatDomain.toString();

		try
		{
			retVal = roomManager.getMultiUserChat(JidCreate.entityBareFrom(roomName));
			
			joinRoom(retVal);
			
			return retVal;
			
		}
		catch (Exception e)
		{			
			return null;
		}
	}
	
	protected void joinRoom(MultiUserChat room) throws Exception
	{
		final Resourcepart nickname = Resourcepart.from(con.getUser().getLocalpart().toString());
		
		final MucEnterConfiguration mucConfig = room.getEnterConfigurationBuilder(nickname).build();
		
		final MucCreateConfigFormHandle createConfig = room.createOrJoin(mucConfig);
		
		if (createConfig == null)
		{
			JOptionPane.showMessageDialog(null, "An error occured creating the group chat room.", 
		 		    "Group chat failure", JOptionPane.ERROR_MESSAGE );
			throw new RuntimeException();
		}
		createConfig.makeInstant();
		
		createChatDialog(room);
	}
	
	public void reEnterGroupChat(EntityBareJid jid)
	{
		final MultiUserChat room  = roomManager.getMultiUserChat(jid);
		
		final GroupChatDialog diag = activeChats.get(jid);
		if (diag != null)
		{
			EventQueue.invokeLater(() ->
			{
				diag.toFront();
				diag.repaint();
			});
			return;
		}

		try
		{
			final Resourcepart nickname = Resourcepart.from(con.getUser().getLocalpart().toString());
			
			room.join(nickname);
			
			createChatDialog(room);
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(null, "An error occured re-joining the group chat room.", 
		 		    "Group chat failure", JOptionPane.ERROR_MESSAGE );
			throw new RuntimeException();
		}
	}
	
	protected void invitationReceived(MultiUserChat room, EntityJid inviter)
	{
		if (room.isJoined())
		{
			final GroupChatDialog diag = activeChats.get(room.getRoom());   
			EventQueue.invokeLater(() ->
			{
				diag.toFront();
				diag.repaint();
			});
			return;
		}
		try
		{
			
			int selection = JOptionPane.showConfirmDialog(null, inviter.asEntityBareJidString() + " has invited you to a group chat.\r\nDo you wish to join?",
					"Chat Room", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			
			if (selection == JOptionPane.NO_OPTION)
				return;	
			
			final Resourcepart nickname = Resourcepart.from(con.getUser().getLocalpart().toString());
			
			room.join(nickname);
			
			
			createChatDialog(room);
			
		}
		catch (Exception e)
		{
			// no-op
		}
	}
	
	protected GroupChatDialog createChatDialog(MultiUserChat room)
	{
		GroupChatDialog chat = activeChats.get(room.getRoom());
		if (chat != null)
		{
			final GroupChatDialog theChat = chat;
	        EventQueue.invokeLater(() ->
	        {
            	theChat.toFront();
            	theChat.repaint();
	        });
	        
	        return theChat;
		}
		else
		{
			final GroupChatDialog newChat = new GroupChatDialog(con, room);
			newChat.addWindowListener(new  WindowAdapter()
			{
				@Override
				public void windowClosed(WindowEvent e)
				{
					try
					{
						room.leave();
					}
					catch (Exception e2) {}
					activeChats.remove(room.getRoom());
					newChat.dispose();
					
					final GroupChatEvent event = new GroupChatEvent();
					event.setRoom(room);
					event.setEvent(GroupChatEvent.Event.ROOM_EXIT);
					
					fireGroupChatEvent(event);
				}
				
			});
			newChat.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			newChat.setVisible(true);
			activeChats.put(room.getRoom(), newChat);
			
			final GroupChatEvent event = new GroupChatEvent();
			event.setRoom(room);
			event.setEvent(GroupChatEvent.Event.ROOM_ENTER);
			
			fireGroupChatEvent(event);
			
			return newChat;
		}
	}
	
	protected void fireGroupChatEvent(GroupChatEvent event)
	{
		for (GroupChatEventListener listener : groupChatEventListeners)
		{
			try
			{
				listener.onGroupChatEvent(event);
			}
			catch (Exception e) {}
		}
	}
	
}
