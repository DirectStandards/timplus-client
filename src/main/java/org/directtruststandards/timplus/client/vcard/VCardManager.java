package org.directtruststandards.timplus.client.vcard;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jxmpp.jid.Jid;

public class VCardManager 
{
	static protected VCardManager INSTANCE;
	
	protected AbstractXMPPConnection con;
	
	protected org.jivesoftware.smackx.vcardtemp.VCardManager cardManager;
	
	protected Map<Jid, VCardDialog> activeVCards;
	
	public static synchronized VCardManager getInstance(AbstractXMPPConnection con)
	{
		if (INSTANCE == null)
			INSTANCE = new VCardManager(con);
		
		return INSTANCE;
	}
	
	public VCardManager(AbstractXMPPConnection con)
	{
		this.con = con;
		
		this.activeVCards = new HashMap<>();
		
		resetVCardManagerConnection();
	}
	
	public void setConnection(AbstractXMPPConnection con)
	{
		if (this.con != con)
		{
			this.con = con;
			resetVCardManagerConnection();
		}
	}
	
	protected void resetVCardManagerConnection()
	{
		cardManager = org.jivesoftware.smackx.vcardtemp.VCardManager.getInstanceFor(con);
	}
	
	public void showVCard(Jid contact)
	{
		try
		{
			final VCard vCard = cardManager.loadVCard(contact.asEntityBareJidIfPossible());
			
			if (vCard == null)
			{
				JOptionPane.showMessageDialog(null,"This contact does have a vCard", 
			 		    "vCard Information", JOptionPane.WARNING_MESSAGE );
				
				return;
			}
			
			showVCardDialog(contact, vCard);
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(null,"An error occured obtaining the vCard", 
		 		    "vCard Information", JOptionPane.ERROR_MESSAGE );
		}
	}
	
	protected void showVCardDialog(Jid contact, VCard vCard)
	{
		VCardDialog dialog = activeVCards.get(contact.asBareJid());
		if (dialog != null)
		{
			final VCardDialog theDialog = dialog;
	        java.awt.EventQueue.invokeLater(new Runnable() 
	        {
	            @Override
	            public void run() 
	            {
	            	theDialog.toFront();
	            	theDialog.repaint();
	            }
	        });
		}
		else
		{
			final VCardDialog newDialog = new VCardDialog(contact, vCard);
			newDialog.addWindowListener(new  WindowAdapter()
			{
				@Override
				public void windowClosed(WindowEvent e)
				{
					activeVCards.remove(contact.asBareJid());
					newDialog.dispose();
				}
				
			});
			newDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			newDialog.setVisible(true);
			activeVCards.put(contact.asBareJid(), newDialog);
		}
	}
}
