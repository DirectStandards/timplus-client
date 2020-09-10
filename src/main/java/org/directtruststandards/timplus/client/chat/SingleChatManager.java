package org.directtruststandards.timplus.client.chat;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JDialog;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;

public class SingleChatManager
{
	static protected SingleChatManager INSTANCE;
	
	protected AbstractXMPPConnection con;
	
	protected Map<Jid, ChatDialog> activeChats;
	
	protected ChatManager chatManager;
	
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
	}
	
	public ChatDialog createChat(final Jid contactJid)
	{
		ChatDialog chat = activeChats.get(contactJid.asBareJid());
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
			final ChatDialog newChat = new ChatDialog(contactJid.asBareJid(), con);
			newChat.addWindowListener(new  WindowAdapter()
			{
				@Override
				public void windowClosed(WindowEvent e)
				{
					activeChats.remove(contactJid.asBareJid());
					newChat.dispose();
				}
				
			});
			newChat.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			newChat.setVisible(true);
			activeChats.put(contactJid.asBareJid(), newChat);
			
			return newChat;
		}
	}
	
	protected void processIncomingMessage(EntityBareJid from, Message message, Chat chat)
	{
		// create a chat window
		final ChatDialog chatDialog = createChat(from.asBareJid());

		chatDialog.onIncomingMessage(message);
	}
}
