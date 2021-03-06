package org.directtruststandards.timplus.client.chat;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JDialog;

import org.directtruststandards.timplus.client.notifications.AMPMessageNotification;
import org.directtruststandards.timplus.client.notifications.AMPNotificationManager;
import org.directtruststandards.timplus.client.notifications.IncomingAMPMessageListener;
import org.directtruststandards.timplus.client.roster.RosterItem;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.ChatStateListener;
import org.jivesoftware.smackx.chatstates.ChatStateManager;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;

public class SingleChatManager
{
	static protected SingleChatManager INSTANCE;
	
	protected AbstractXMPPConnection con;
	
	protected Map<Jid, ChatDialog> activeChats;
	
	protected ChatManager chatManager;
	
	protected ChatStateManager chatStateManager;
	
	public static synchronized SingleChatManager getInstance(AbstractXMPPConnection con)
	{
		if (INSTANCE == null)
			INSTANCE = new SingleChatManager(con);
		
		return INSTANCE;
	}
	
	public SingleChatManager(AbstractXMPPConnection con)
	{
		this.con = con;
		
		activeChats = new HashMap<>();
		
		resetChatManagerConnection();
	}
	
	public AbstractXMPPConnection getConnection()
	{
		return this.con;
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
		chatManager = ChatManager.getInstanceFor(con);
		
        chatManager.addIncomingListener(new IncomingChatMessageListener() 
        {
     	   public void newIncomingMessage(EntityBareJid from, Message message, Chat chat) 
     	   {
     		   processIncomingMessage(from, message, chat);
     	   }
        });
        
        for (ChatDialog chatDialog : activeChats.values())
        	chatDialog.resetChat(con);
        
        AMPNotificationManager.getInstanceFor(con).addIncomingAMPListener(new IncomingAMPMessageListener()
        {

			@Override
			public void newIncomingAMPMessage(EntityBareJid from, Message message, AMPMessageNotification notif)
			{
				processIncomingAMPMessage(from, message, notif);
			}
        	
        });
        
        con.addAsyncStanzaListener(new StanzaListener()
		{

			@Override
			public void processStanza(Stanza packet)
					throws NotConnectedException, InterruptedException, NotLoggedInException 
			{
				processIncomingErrorMessage(packet.getFrom().asEntityBareJidIfPossible(), (Message)packet);
				
			}
	
		}, MessageTypeFilter.ERROR);
        
        chatStateManager = ChatStateManager.getInstance(con);
        chatStateManager.addChatStateListener( new ChatStateListener()
        {
			@Override
			public void stateChanged(Chat chat, ChatState state, Message message)
			{
				processIncomingChatState(chat, state, message);
			}	
        });
	}
	
	public ChatDialog createChat(final RosterItem rosterItem)
	{
		ChatDialog chat = activeChats.get(rosterItem.getRosterJID().asBareJid());
		if (chat != null)
		{
			final ChatDialog theChat = chat;
	        java.awt.EventQueue.invokeLater(new Runnable() 
	        {
	            @Override
	            public void run() 
	            {
	            	theChat.toFront();
	            	theChat.repaint();
	            }
	        });
	        
	        return theChat;
		}
		else
		{
			final ChatDialog newChat = new ChatDialog(rosterItem, con);
			newChat.addWindowListener(new  WindowAdapter()
			{
				@Override
				public void windowClosed(WindowEvent e)
				{
					activeChats.remove(rosterItem.getRosterJID().asBareJid());
					newChat.dispose();
				}
				
			});
			newChat.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			newChat.setVisible(true);
			activeChats.put(rosterItem.getRosterJID().asBareJid(), newChat);
			
			return newChat;
		}
	}
	
	protected void processIncomingMessage(EntityBareJid from, Message message, Chat chat)
	{
		// create a chat window
		 
		final RosterItem item = new RosterItem();
		item.setRosterJID(from.asBareJid());
		
		// look through the roster and see if there an entry with this same JID
		// populate the alias if we can find an entry in the roster
		for (RosterEntry entry : Roster.getInstanceFor(con).getEntries())
		{
			if (entry.getJid().equals(from.asBareJid()))
			{
				item.setAlias(entry.getName());
				break;
			}
		}
	
		final ChatDialog chatDialog = createChat(item);

		chatDialog.onIncomingMessage(message);
	}
	
	protected void processIncomingChatState(Chat chat, ChatState state, Message message)
	{
		// don't auto popup the window because chat state
		// messages can be sent before the first message body is sent
		final ChatDialog chatDialog = activeChats.get(chat.getXmppAddressOfChatPartner().asBareJid());
		
		if (chatDialog != null)
			chatDialog.onIncomingChatState(state);
	}
	
	protected void processIncomingAMPMessage(EntityBareJid from, Message message, AMPMessageNotification notif)
	{
		// only deliver to active chats
		final ChatDialog chat = activeChats.get(from.asBareJid());

		if (chat != null)
			chat.onIncomingAMPMessage(notif);
	}
	
	protected void processIncomingErrorMessage(EntityBareJid from, Message message)
	{
		if (from != null)
		{
			final ChatDialog chat = activeChats.get(from.asBareJid());
			
			if (chat != null)
				chat.onIncomingErrorMessage(message);
		}
	}
	
}
