package org.directtruststandards.timplus.client.notifications;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.amp.AMPDeliverCondition;
import org.jivesoftware.smackx.amp.packet.AMPExtension;
import org.jivesoftware.smackx.amp.packet.AMPExtension.Rule;
import org.jxmpp.jid.Jid;

public class AMPNotificationManager extends Manager
{
    private static final Map<XMPPConnection, AMPNotificationManager> INSTANCES = new WeakHashMap<>();
	
    private final Set<IncomingAMPMessageListener> incomingAMPListeners = new CopyOnWriteArraySet<>();
    
    protected static ExecutorService notificateExecutor;
    
    public static synchronized AMPNotificationManager getInstanceFor(XMPPConnection connection) 
    {
    	notificateExecutor = Executors.newSingleThreadExecutor();	
    	
    	AMPNotificationManager chatManager = INSTANCES.get(connection);
        if (chatManager == null) 
        {
            chatManager = new AMPNotificationManager(connection);
            INSTANCES.put(connection, chatManager);
        }
        return chatManager;
    }
    
    private AMPNotificationManager(final XMPPConnection connection) 
    {
        super(connection);
        
        connection.addSyncStanzaListener(new StanzaListener() 
        {
            @Override
            public void processStanza(Stanza stanza) 
            {
            	final Message message = (Message)stanza;
            	
                final Jid from = message.getFrom();

                final AMPExtension ampExt = (AMPExtension)message.getExtension(AMPExtension.NAMESPACE);
                
                AMPMessageNotification.MessageStatus status = null;
                
                for (Rule rule : ampExt.getRules())
                {
                	if (rule.getCondition() != null && rule.getCondition().getName() == AMPDeliverCondition.NAME)
                	{
                		final AMPDeliverCondition cond = AMPDeliverCondition.class.cast(rule.getCondition());
                		
                		if (cond.getValue().compareToIgnoreCase(AMPDeliverCondition.Value.direct.name()) == 0)
                		{
                			status = AMPMessageNotification.MessageStatus.DELIVERED;
                		}
                		else if (cond.getValue().compareToIgnoreCase(AMPDeliverCondition.Value.stored.name()) == 0)
                		{
                			status = AMPMessageNotification.MessageStatus.STORED_OFFLINE;
                		}
                	}
                }
                
                final AMPMessageNotification notif = new AMPMessageNotification(message.getStanzaId(), message.getTo().asBareJid(), 
                		from.asBareJid(), status);
                
                notificateExecutor.execute(new Runnable() 
                {
                	public void run()
                	{
		                for (IncomingAMPMessageListener listener : incomingAMPListeners) 
		                {
		                    listener.newIncomingAMPMessage(from.asEntityBareJidOrThrow(), message, notif);
		                }
                	}
                });
            }
        }, new AMPFilter());
    }
    
    public boolean addIncomingAMPListener(IncomingAMPMessageListener listener) 
    {
        return incomingAMPListeners.add(listener);
    }

    public boolean removeIncomingAMPListener(IncomingAMPMessageListener listener) 
    {
        return incomingAMPListeners.remove(listener);
    }
}
