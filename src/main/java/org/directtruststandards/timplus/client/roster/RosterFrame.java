package org.directtruststandards.timplus.client.roster;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.InetAddress;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;


import org.apache.commons.lang3.StringUtils;
import org.directtruststandards.timplus.client.chat.SingleChatManager;
import org.directtruststandards.timplus.client.config.Configuration;
import org.directtruststandards.timplus.client.config.ConfigurationManager;
import org.directtruststandards.timplus.client.filetransport.IncomingFileTransferManager;
import org.directtruststandards.timplus.client.roster.AddContactDialog.AddContactStatus;
import org.directtruststandards.timplus.client.roster.RosterItem.Presense;
import org.directtruststandards.timplus.client.roster.RosterItem.Subscription;
import org.directtruststandards.timplus.client.vcard.VCardManager;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException.ConnectionException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.StreamErrorException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.roster.Roster.SubscriptionMode;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration.Builder;
import org.jxmpp.jid.Jid;
import org.springframework.stereotype.Component;

@Component
public class RosterFrame extends JFrame
{
	
	private static final long serialVersionUID = -5862072428442358408L;

	protected JTable contactsList;

	protected JLabel connected;
	
	protected JLabel disconnected;
	
	protected JLabel connectStatusLabel;
	
	protected ExecutorService connectionExecutor;
	
	protected AbstractXMPPConnection con;
	
	protected Roster roster;
	
	protected JPopupMenu contactPopup;
	
	public RosterFrame()
	{
		super("TIM+ Client");
		setDefaultLookAndFeelDecorated(true);
		setSize(300, 400);
		
		Point pt = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
		
		this.setLocation(pt.x - (150), pt.y - (200));			
		
	    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
	    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    
	    try
	    {
	    	initUI();
	    }
	    catch (Exception e) { e.printStackTrace();}
	    
	    connectionExecutor = Executors.newSingleThreadExecutor();
	}
	
	protected void initUI() throws Exception
	{
		this.getContentPane().setLayout(new BorderLayout());
		
		/*
		 * Contacts Label
		 */
		final JPanel rosterLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		final JLabel rosterLabel = new JLabel("Contacts:"); 
		rosterLabelPanel.add(rosterLabel);
		
		this.getContentPane().add(rosterLabelPanel, BorderLayout.NORTH);
		
		/*
		 * Contacts List
		 */		
		contactsList = new JTable(new RosterTableModel(Collections.emptyList()));

		
		contactsList.setTableHeader(null);
		contactsList.setRowHeight(30);
		contactsList.setDefaultRenderer(RosterItem.class, new RosterItemRenderer());
		//contactsList.getTableHeader().setResizingAllowed(false);
		contactsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		final JScrollPane scrollPane = new JScrollPane(contactsList);
		contactsList.setFillsViewportHeight(true);
		
		this.getContentPane().add(scrollPane);
		
		/*
		 * Status 
		 */
		final JPanel statusPanel = new JPanel(new BorderLayout());
		
		final JPanel connectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		URL imageURL = this.getClass().getResource("/images/connected.png");
		BufferedImage image = ImageIO.read(imageURL);
		ImageIcon icon = new ImageIcon(new ImageIcon(image).getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH));
		connected = new JLabel(icon);
		
		imageURL = this.getClass().getResource("/images/disconnected.png");
		image = ImageIO.read(imageURL);
		icon = new ImageIcon(new ImageIcon(image).getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH));
		disconnected = new JLabel(icon);
		

		connectStatusLabel = new JLabel("Connecting...");
		
		
		connected.setVisible(false);
		connectionPanel.add(connected);
		connectionPanel.add(disconnected);
		connectionPanel.add(connectStatusLabel);
		
		
		statusPanel.add(connectionPanel, BorderLayout.NORTH);
		
		this.getContentPane().add(statusPanel, BorderLayout.SOUTH);
		
		/*
		 * Menu bar
		 */
		final JMenuBar menuBar = new JMenuBar();
		
		
		final JMenu contactsMenu = new JMenu("Contacts");
		final JMenuItem addContact = new JMenuItem("Add contact");
		addContact.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				addContact();
			}	
		});
		contactsMenu.add(addContact);
		
		final JMenu accountMenu = new JMenu("Account");
		final JMenuItem configAccount = new JMenuItem("Configure/Modify");
		configAccount.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				modifyAccount();
			}	
		});
		accountMenu.add(configAccount);
		
		menuBar.add(contactsMenu);
		menuBar.add(accountMenu);
		
		this.setJMenuBar(menuBar);
		
		/*
		 * Contact popup menu
		 */
		contactPopup = new JPopupMenu();
		
		final JMenuItem imItem = new JMenuItem("Chat");
		imItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				imContact();
				
			}	
		});
		contactPopup.add(imItem);		
		
		final JMenuItem deleteItem = new JMenuItem("Delete Contact");
		deleteItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				deleteContact();
				
			}	
		});
		contactPopup.add(deleteItem);
		
		final JMenuItem subRequest = new JMenuItem("Send Subscription Request");
		subRequest.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				sendSubscriptionRequest();
				
			}	
		});
		contactPopup.add(subRequest);
		
		final JMenuItem viewVCard = new JMenuItem("View vCard");
		viewVCard.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				viewVCard();
			}	
		});
		contactPopup.add(viewVCard);
		
		/*
		 * Actions
		 */
		contactsList.addMouseListener(new MouseAdapter()
		{
			public void mousePressed(MouseEvent e)
			{
				contactListMousePress(e);
			}
		});
		
	}
	
	@Override
	public void setVisible(boolean visible)
	{
		if (!ConfigurationManager.getInstance().isCompleteConfiguration())
		{
			configure();
			
			if (!ConfigurationManager.getInstance().isCompleteConfiguration())
			{
				JOptionPane.showMessageDialog(this,"Configuration is incomplete.  The TIM+ Client will now exit.", 
			 		    "Incomplete Configuration", JOptionPane.WARNING_MESSAGE );
				/*
				 * hard exit
				 */
				System.exit(-1);
				return;
			}
		}
		
		
		super.setVisible(visible);
		
		connectionExecutor.execute(new Runnable()
		{
			@Override
			public void run()
			{
				connect();
			}
		});
	}
	
	protected void configure()
	{
		ConfigurationManager.getInstance().doConfigure(this);
	}
	
	protected void connect()
	{
		final Configuration config = ConfigurationManager.getInstance().getConfiguration();
		
		try
		{
			final Builder conBuilder = XMPPTCPConnectionConfiguration.builder().setUsernameAndPassword(config.getUsername(), config.getPassword())
					.setXmppDomain(config.getDomain())
					.setCompressionEnabled(true);
	        
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() 
	        {
	            public java.security.cert.X509Certificate[] getAcceptedIssuers() 
	            {
	                return null;
	            }
	            
	            public void checkClientTrusted(X509Certificate[] certs, String authType) 
	            {
	            }
	            
	            public void checkServerTrusted(X509Certificate[] certs, String authType) 
	            {
	            }
	        }};
	        
	        final SSLContext sc = SSLContext.getInstance("TLSv1.2");
	        sc.init(null, trustAllCerts, new java.security.SecureRandom());
	        
	        conBuilder.setCustomSSLContext(sc);
	        
			if (!StringUtils.isEmpty(config.getServer()))
				conBuilder.setHostAddress(InetAddress.getByName(config.getServer()));
	        
			XMPPTCPConnectionConfiguration xmppConfig = conBuilder.build();
			
			con = new XMPPTCPConnection(xmppConfig);
			con.addConnectionListener(new ConnectionListener()
			{
				@Override
				public void connected(XMPPConnection connection)
				{
					
				}

				@Override
				public void authenticated(XMPPConnection connection, boolean resumed)
				{
					
				}

				@Override
				public void connectionClosed()
				{
					
				}

				@Override
				public void connectionClosedOnError(Exception e)
				{
					/*
					 * for now just close and reconnect
					 */
					try
					{
						if (con != null && con.isConnected())
							con.disconnect();
					}
					catch (Exception conExp)
					{
						/* no op */
					}

					System.out.println("Connection was closed.  Reconnecting");
					try
					{
						connect();
					}
					catch(Exception conExp)
					{
						
					}
		
				}
				
				
			});
			
			con.connect();
			
			con.login();
			
			connectStatusLabel.setText("Connected");
			connected.setVisible(true);
			disconnected.setVisible(false);
			
			/*
			 * Load the roster 
			 */
			roster = Roster.getInstanceFor(con);
			roster.setSubscriptionMode(SubscriptionMode.accept_all);
			
	    	loadRoster();
			
	    	roster.addRosterListener(new RosterListener() 
	    	{
	    		public void entriesAdded(Collection<Jid> addresses) {}
	    		public void entriesDeleted(Collection<Jid> addresses) {}
	    		public void entriesUpdated(Collection<Jid> addresses) {}
	    		public void presenceChanged(Presence presence) 
	    		{
	    			contactPresenseUpdated(presence);
	    			
	    		}
	    	});
	    	
			Presence pres = new Presence(Presence.Type.available);
			pres.setStatus("Available");
			con.sendStanza(pres);
			
			// init the chat manager
			SingleChatManager.getInstance(con).setConnection(con);
			
			// init the incoming file transfer manager
			IncomingFileTransferManager.getInstance(con, this).setConnection(con);
			
		}
		catch (Exception e)
		{
			final StringBuilder status = new StringBuilder("Connection failed: ");
			if (e instanceof SASLErrorException)
			{
				final SASLErrorException error = (SASLErrorException)e;
				status.append(error.getSASLFailure().getSASLErrorString());
			}
			else if (e instanceof ConnectionException)
			{
				status.append("network error");
			}
			else if (e instanceof StreamErrorException)
			{
				final StreamErrorException error = (StreamErrorException)e;
				status.append(error.getStreamError().getCondition().toString());
			}
			else
				status.append("unknown error");
			
			connectStatusLabel.setText(status.toString());
			connected.setVisible(false);
			disconnected.setVisible(true);
			
		}
		
	}
	
	protected void modifyAccount()
	{
		final Configuration oldConfig = ConfigurationManager.getInstance().getConfiguration();
		
		configure();
		
		final Configuration newConfig = ConfigurationManager.getInstance().getConfiguration();
	}
	
	protected void addContact()
	{
		final AddContactDialog addContact = new AddContactDialog(this);
		addContact.setVisible(true);
		
		if (addContact.getAddActionStatus() == AddContactStatus.ADD)
		{
			/*
			 * Add a new contact
			 */
			final Jid contactJid = addContact.getContactJid();
			final String alias = addContact.getAlias();
			
			try
			{
				roster.createEntry(contactJid.asBareJid(), alias, null);
			}
			catch (Exception e)
			{
				JOptionPane.showMessageDialog(this,"An error occured trying to add the contact.", 
			 		    "Contact Error", JOptionPane.ERROR_MESSAGE );
				return;
			}
			
			final RosterItem item = new RosterItem();
			item.setPresence(Presense.NOT_AUTHORIZED);
			item.setRosterJID(contactJid);
			item.setSub(Subscription.REQUESTED);
			
			RosterTableModel model = (RosterTableModel)contactsList.getModel();
			model.addRow(item);
		}
	}
	
	protected void deleteContact()
	{
		int idx = contactsList.getSelectedRow();
		if (idx >= 0)
		{
			final RosterItem item = (RosterItem)contactsList.getModel().getValueAt(idx, 0);
			
			int selection = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete the contact " + item.getRosterJID().toString() + "?",
					"Delete Contact", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			
			if (selection == JOptionPane.NO_OPTION)
				return;	
			
			
			for (RosterEntry entry : roster.getEntries())
			{
				if (entry.getJid().toString().compareToIgnoreCase(item.getRosterJID().toString()) == 0)
				{
					try
					{
						roster.removeEntry(entry);
					}
					catch (Exception e)
					{
						JOptionPane.showMessageDialog(this,"An error occured trying to delete the contact.", 
					 		    "Contact Error", JOptionPane.ERROR_MESSAGE );
					}
					
					((RosterTableModel)contactsList.getModel()).removeRow(idx);
					
					break;
				}
			}
		}
	}
	
	protected void imContact()
	{
		int idx = contactsList.getSelectedRow();
		if (idx >= 0)
		{
			final RosterItem item = (RosterItem)contactsList.getModel().getValueAt(idx, 0);
		
			SingleChatManager.getInstance(con).createChat(item.getRosterJID());
		}
	}
	
	protected void sendSubscriptionRequest()
	{
		int idx = contactsList.getSelectedRow();
		if (idx >= 0)
		{
			final RosterItem item = (RosterItem)contactsList.getModel().getValueAt(idx, 0);
		
			Presence pres = new Presence(Presence.Type.subscribe);
			pres.setTo(item.rosterJID.asBareJid());
			
			try
			{
				con.sendStanza(pres);
			}
			catch (Exception e)
			{
				JOptionPane.showMessageDialog(this,"An error occured sending the presence subscription request.", 
			 		    "Subscription Request", JOptionPane.ERROR_MESSAGE );
			}
		}
	}
	
	protected void viewVCard()
	{
		int idx = contactsList.getSelectedRow();
		if (idx >= 0)
		{
			final RosterItem item = (RosterItem)contactsList.getModel().getValueAt(idx, 0);
			VCardManager.getInstance(con).showVCard(item.getRosterJID());
		}
	}
	
	protected void loadRoster()
	{
		final List<RosterItem> rosterItems = new ArrayList<>();
		for (RosterEntry entry : roster.getEntries())
		{
			final RosterItem item = new RosterItem();
			
			item.setRosterJID(entry.getJid());
			final Subscription sub = entry.canSeeHisPresence() ? Subscription.APPROVED : (entry.isSubscriptionPending() ? Subscription.REQUESTED : Subscription.DENIED);
			
			if (sub == Subscription.APPROVED)
				item.setPresence(Presense.UNAVAILABLE);
			else
				item.setPresence(Presense.NOT_AUTHORIZED);
			
			item.setSub(sub);
			
			rosterItems.add(item);
		}
		
		contactsList.setModel(new RosterTableModel(rosterItems));
		contactsList.updateUI();
	}
	
	protected void contactPresenseUpdated(Presence pres)
	{
		final Jid from = pres.getFrom();
		
		for (int i = 0; i < contactsList.getModel().getRowCount(); ++i)
		{
			final RosterItem contact = (RosterItem)contactsList.getModel().getValueAt(i, 0);
			if (from.asBareJid().equals(contact.getRosterJID().asBareJid()))
			{
				final RosterItem rosterItem = (RosterItem)contactsList.getModel().getValueAt(i, 0);
				
				if (pres.getType() == Presence.Type.available)
				{
					rosterItem.setPresence(RosterItem.Presense.AVAILABLE);
					contactsList.getModel().setValueAt(rosterItem, i, 0);
				}
				else if (pres.getType() == Presence.Type.unavailable)
				{
					rosterItem.setPresence(RosterItem.Presense.UNAVAILABLE);
					contactsList.getModel().setValueAt(rosterItem, i, 0);
					// double check the presense in case this user is logged in
					// via more than 1 client (might have multiple resources)
					final Presence presense = roster.getPresence(contact.getRosterJID().asBareJid());
					if (presense != null && presense.getType() == Presence.Type.available)
					{
						rosterItem.setPresence(RosterItem.Presense.AVAILABLE);
						contactsList.getModel().setValueAt(rosterItem, i, 0);
					}	
				}
				// need to check the roster and see if the approval status changed
				for (RosterEntry entry : roster.getEntries())
				{
					if (entry.getJid().equals(contact.getRosterJID().asBareJid()))
					{
						final Subscription sub = entry.canSeeHisPresence() ? Subscription.APPROVED : entry.isSubscriptionPending() ? Subscription.REQUESTED : Subscription.DENIED;
						rosterItem.setSub(sub);
						
						if (sub != Subscription.APPROVED)
							rosterItem.setPresence(Presense.NOT_AUTHORIZED);
						
						contactsList.getModel().setValueAt(rosterItem, i, 0);
						
						break;
					}
				}
					
				break;
			}
		}
	}
	
	protected void contactListMousePress(MouseEvent e)
	{
	    Point pt = e.getPoint();
	    int clickedRow = contactsList.rowAtPoint(pt);
		
		if (e.getButton() == MouseEvent.BUTTON3)
		{

		    if (clickedRow >= 0)
		    {
		    	contactsList.getSelectionModel().setSelectionInterval(clickedRow, clickedRow);
		    	contactPopup.show(e.getComponent(), pt.x, pt.y);
		    }
		}
		else if (e.getButton() == MouseEvent.BUTTON1)
		{
			if (e.getClickCount() == 2 && clickedRow >= 0)
			{
				imContact();
			}
		}
	}
}
