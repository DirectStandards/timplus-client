package org.directtruststandards.timplus.client.roster;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.directtruststandards.timplus.client.chat.SingleChatManager;
import org.directtruststandards.timplus.client.config.Configuration;
import org.directtruststandards.timplus.client.config.ConfigurationManager;
import org.directtruststandards.timplus.client.connection.ConnectionListener;
import org.directtruststandards.timplus.client.connection.ConnectionManager;
import org.directtruststandards.timplus.client.filetransport.IncomingFileTransferManager;
import org.directtruststandards.timplus.client.groupchat.GroupChatEvent;
import org.directtruststandards.timplus.client.groupchat.GroupChatEventListener;
import org.directtruststandards.timplus.client.groupchat.GroupChatManager;
import org.directtruststandards.timplus.client.roster.AddContactDialog.AddContactStatus;
import org.directtruststandards.timplus.client.roster.RosterItem.Presense;
import org.directtruststandards.timplus.client.roster.RosterItem.Subscription;
import org.directtruststandards.timplus.client.vcard.VCardManager;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException.ConnectionException;
import org.jivesoftware.smack.XMPPException.StreamErrorException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.roster.Roster.SubscriptionMode;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;

public class RosterFrame extends JFrame implements ConnectionListener
{
	
	private static final long serialVersionUID = -5862072428442358408L;

	protected JTable contactsList;

	protected JTable groupChatList;
	
	protected JLabel connected;
	
	protected JLabel connecting;
	
	protected JLabel disconnected;
	
	protected JLabel connectStatusLabel;
	
	protected JMenu contactsMenu;
	
	protected JMenu groupChatMenu;
	
	protected JComboBox<RosterStatusShow> showDropDown;
	
	protected ExecutorService connectionExecutor;
	
	protected AbstractXMPPConnection con;
	
	protected Roster roster;
	
	protected JPopupMenu contactPopup;
	
	protected GroupChatListener groupChatListener;
	
	protected ExecutorService reconnectExecutorService;
	
	public RosterFrame()
	{
		super("TIM+ Client");
		
		groupChatListener = new GroupChatListener();
		
		setDefaultLookAndFeelDecorated(true);
		setSize(400, 500);
		
		Point pt = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
		
		this.setLocation(pt.x - (200), pt.y - (250));			
		
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
		 * 
		 */
		final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		
		final JPanel rosterPanel = new JPanel(new BorderLayout()); 
		
		
		/*
		 * Contacts Label
		 */
		final JPanel rosterLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		final JLabel rosterLabel = new JLabel("Contacts:"); 
		rosterLabelPanel.add(rosterLabel);
		
		rosterPanel.add(rosterLabelPanel, BorderLayout.NORTH);
		
		/*
		 * Contacts List
		 */		
		contactsList = new JTable(new RosterTableModel(new ArrayList<>()));

		
		contactsList.setTableHeader(null);
		contactsList.setRowHeight(30);
		contactsList.setDefaultRenderer(RosterItem.class, new RosterItemRenderer());
		contactsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		final JScrollPane scrollPane = new JScrollPane(contactsList);
		contactsList.setFillsViewportHeight(true);
		
		rosterPanel.add(scrollPane, BorderLayout.CENTER);
		
		/*
		 * Status 
		 */
		final JPanel statusPanel = new JPanel(new GridLayout(2,1));
		
		// connection
		final JPanel connectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		URL imageURL = this.getClass().getResource("/images/connected.png");
		BufferedImage image = ImageIO.read(imageURL);
		ImageIcon icon = new ImageIcon(new ImageIcon(image).getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH));
		connected = new JLabel(icon);
		
		imageURL = this.getClass().getResource("/images/disconnected.png");
		image = ImageIO.read(imageURL);
		icon = new ImageIcon(new ImageIcon(image).getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH));
		disconnected = new JLabel(icon);
		
		imageURL = this.getClass().getResource("/images/connecting.png");
		image = ImageIO.read(imageURL);
		icon = new ImageIcon(new ImageIcon(image).getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH));
		connecting = new JLabel(icon);
		

		connectStatusLabel = new JLabel("Connecting...");
		
		
		// my status
		final RosterStatusShow[] rosterShowItems = {RosterStatusShow.AVAILABLE, RosterStatusShow.AWAY, RosterStatusShow.DND, RosterStatusShow.PRIVATE};
		showDropDown = new JComboBox<RosterStatusShow>(rosterShowItems);
		showDropDown.setRenderer(new RosterShowStatusRenderer());
		
		
		
		connected.setVisible(false);
		disconnected.setVisible(false);
		connecting.setVisible(true);
		showDropDown.setVisible(false);
		connectionPanel.add(connected);
		connectionPanel.add(connecting);
		connectionPanel.add(disconnected);
		connectionPanel.add(connectStatusLabel);
		
		
		statusPanel.add(connectionPanel);
		statusPanel.add(showDropDown);
		
		this.getContentPane().add(statusPanel, BorderLayout.SOUTH);
		
		tabbedPane.addTab("Roster", rosterPanel);
		
		
		this.getContentPane().add(tabbedPane);
		
		/*
		 * Group chat tab
		 */
		final JPanel groupChatPanel = new JPanel(new BorderLayout());
		
		/*
		 * Group chat label 
		 *
		 */
		final JPanel groupChagLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		final JLabel groupChagLabel= new JLabel("Active Group Chats Rooms:"); 
		groupChagLabelPanel.add(groupChagLabel);
		
		groupChatPanel.add(groupChagLabelPanel, BorderLayout.NORTH);
		
		
		/*
		 * Group Chat List
		 */		
		groupChatList = new JTable(new GroupChatTableModel(new ArrayList<>()));

		
		groupChatList.setTableHeader(null);
		groupChatList.setRowHeight(30);
		groupChatList.setDefaultRenderer(GroupChatItem.class, new GroupChatItemRenderer());
		groupChatList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		final JScrollPane groupChatScrollPane = new JScrollPane(groupChatList);
		groupChatList.setFillsViewportHeight(true);
		
		groupChatPanel.add(groupChatScrollPane);
		
		
		
		tabbedPane.addTab("Group Chats", groupChatPanel);
		
		
		
		/*
		 * Menu bar
		 */
		final JMenuBar menuBar = new JMenuBar();
		
		contactsMenu = new JMenu("Contacts");
		final JMenuItem addContact = new JMenuItem("Add Contact...");
		addContact.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				addContact();
			}	
		});
		contactsMenu.add(addContact);
		contactsMenu.setEnabled(false);
		
		groupChatMenu = new JMenu("Group Chat");
		final JMenuItem newChatRoom = new JMenuItem("New Chat Room");
		newChatRoom.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				newGroupChat();
			}	
		});
		groupChatMenu.add(newChatRoom);
		groupChatMenu.setEnabled(false);
		
		
		final JMenu accountMenu = new JMenu("Account");
		final JMenuItem configAccount = new JMenuItem("Configure/Modify...");
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
		menuBar.add(groupChatMenu);
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
		
		groupChatList.addMouseListener(new MouseAdapter()
		{
			public void mousePressed(MouseEvent e)
			{
				groupChatListMousePress(e);
			}
		});
		
		showDropDown.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				onSelectShow();
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
		        EventQueue.invokeLater(() -> 
		        {
					JOptionPane.showMessageDialog(this,"Configuration is incomplete.  The TIM+ Client will now exit.", 
				 		    "Incomplete Configuration", JOptionPane.WARNING_MESSAGE );
					/*
					 * hard exit
					 */
					System.exit(-1);
					return;
		        });
			}
		}
		
		
		super.setVisible(visible);
		
		final ConnectionManager conManager = ConnectionManager.getInstance();
		conManager.addConnectionListener(this);
		conManager.connect();
		
	}
	
	protected void configure()
	{
		ConfigurationManager.getInstance().doConfigure(this);
	}
	
	@Override
	public void onConnected(AbstractXMPPConnection con)
	{
		this.con = con;
		
		EventQueue.invokeLater(() ->
		{
			connectStatusLabel.setText("Connected");
			connected.setVisible(true);
			disconnected.setVisible(false);
			connecting.setVisible(false);
			contactsMenu.setEnabled(true);
			groupChatMenu.setEnabled(true);
			showDropDown.setVisible(true);
		});
		
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
		try 
		{
			con.sendStanza(pres);
		}
		catch (Exception e) {}
		
		// init the chat manager
		SingleChatManager.getInstance(con).setConnection(con);
		
		// init the group chat manager
		GroupChatManager.getInstance(con).setConnection(con);
		GroupChatManager.getInstance(con).registerGroupChatEventListener(groupChatListener);
		
		// init the incoming file transfer manager
		IncomingFileTransferManager.getInstance(con, this).setConnection(con);
				
	}
	
	@Override
	public void onConnecting()
	{
		EventQueue.invokeLater(() ->
		{
			connectStatusLabel.setText("Connecting");
			connected.setVisible(false);
			disconnected.setVisible(false);
			connecting.setVisible(true);
			showDropDown.setVisible(false);
		});
	}
	
	@Override
	public void onDisconnectedWithError(Exception e)
	{
		contactsList.setModel(new GroupChatTableModel(new ArrayList<>()));
		
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
		
		EventQueue.invokeLater(() ->
		{
			connectStatusLabel.setText(status.toString());
			connected.setVisible(false);
			disconnected.setVisible(true);
			connecting.setVisible(false);
			contactsMenu.setEnabled(false);
			groupChatMenu.setEnabled(false);
			showDropDown.setVisible(false);
		});
	}
	
	@Override
	public void onDiconneected()
	{
		contactsList.setModel(new GroupChatTableModel(new ArrayList<>()));
		EventQueue.invokeLater(() ->
		{
			connectStatusLabel.setText("Disconnected");
			connected.setVisible(false);
			disconnected.setVisible(true);
			connecting.setVisible(false);
			contactsMenu.setEnabled(false);
			groupChatMenu.setEnabled(false);
			showDropDown.setVisible(false);
		});
	}
	
	protected void modifyAccount()
	{
		final Configuration oldConfig = ConfigurationManager.getInstance().getConfiguration();
		
		configure();
		
		final Configuration newConfig = ConfigurationManager.getInstance().getConfiguration();
		
		if (oldConfig.getDomain().trim().compareToIgnoreCase(newConfig.getDomain().trim()) != 0 || oldConfig.getUsername().trim().compareToIgnoreCase(newConfig.getUsername().trim()) != 0 ||
				oldConfig.getPassword().trim().compareToIgnoreCase(newConfig.getPassword().trim()) != 0 || oldConfig.getServer().trim().compareToIgnoreCase(newConfig.getServer().trim()) != 0)
		{
			ConnectionManager.getInstance().disconnect();
			
			ConnectionManager.getInstance().connect();
		}
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
	
	protected void newGroupChat()
	{
		final GroupChatManager manager = GroupChatManager.getInstance(con);
		
		manager.createGroupChat();
	}
	
	protected void onSelectShow()
	{	
		final RosterStatusShow statusShow =  (RosterStatusShow)showDropDown.getSelectedItem();
		if (statusShow != null && con != null)
		{
			Presence pres = null;
			switch (statusShow)
			{
				case AVAILABLE:
					pres = new Presence(Presence.Type.available);
					break;
				case AWAY:
					pres = new Presence(Presence.Type.available);
					pres.setMode(Presence.Mode.away);
					break;
				case DND:
					pres = new Presence(Presence.Type.available);
					pres.setMode(Presence.Mode.dnd);					
					break;
				case PRIVATE:
					pres = new Presence(Presence.Type.unavailable);	
					break;					
			}
			
			try
			{
				con.sendStanza(pres);
			}
			catch (Exception e)
			{
			}
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
				
				if (pres.getType() == null || pres.getType() == Presence.Type.available)
				{
					switch (pres.getMode())
					{
						case available:
						case chat:
							rosterItem.setPresence(RosterItem.Presense.AVAILABLE);
							break;
						case away:
						case xa:
							rosterItem.setPresence(RosterItem.Presense.AWAY);
							break;
						case dnd:
							rosterItem.setPresence(RosterItem.Presense.DND);
							break;		
						default:
							rosterItem.setPresence(RosterItem.Presense.AVAILABLE);
							break;
					}
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
	
	protected void groupChatListMousePress(MouseEvent e)
	{
	    Point pt = e.getPoint();
	    int clickedRow = contactsList.rowAtPoint(pt);
		
		if (e.getButton() == MouseEvent.BUTTON3)
		{

		}
		else if (e.getButton() == MouseEvent.BUTTON1)
		{
			if (e.getClickCount() == 2 && clickedRow >= 0)
			{
				final GroupChatTableModel model = (GroupChatTableModel)this.groupChatList.getModel();
				final GroupChatItem item = (GroupChatItem)model.getValueAt(clickedRow, 0);
				
				// check to see if we are still a member
				try 
				{
					final RoomInfo roomInfo = MultiUserChatManager.getInstanceFor(con).getRoomInfo(item.getRoom().getRoom());
					if (roomInfo == null)
					{
						JOptionPane.showMessageDialog(this,"This group chat room no longer exists.", 
					 		    "Group Chat", JOptionPane.WARNING_MESSAGE );
						model.removeRow(clickedRow);
						
						return;
					}
				}
				catch (Exception e2)
				{
					JOptionPane.showMessageDialog(this,"This group chat room no longer exists.", 
				 		    "Group Chat", JOptionPane.WARNING_MESSAGE );
					// most likely caused by an "item-not-found" error
					model.removeRow(clickedRow);
					
					return;
				}
				
				rejoinRoom(item.getRoom().getRoom());
			}
		}
	}
	
	protected class GroupChatListener implements GroupChatEventListener
	{
		public GroupChatListener()
		{
			
		}

		@Override
		public void onGroupChatEvent(GroupChatEvent event)
		{
			final GroupChatTableModel model = (GroupChatTableModel)groupChatList.getModel();
			
			switch(event.getEvent())
			{
				case ROOM_EXIT:
				{
					for (int i = 0; i < model.getRowCount(); ++i)
					{
						final GroupChatItem item = (GroupChatItem)model.getValueAt(i, 1);
						if (item.getRoom().getRoom().equals(event.getRoom().getRoom()) )
						{
							// check to see if we are still a member
							try 
							{
								final RoomInfo roomInfo = MultiUserChatManager.getInstanceFor(con).getRoomInfo(event.getRoom().getRoom());
								if (roomInfo == null)
									model.removeRow(i);
							}
							catch (Exception e)
							{
								// most likely caused by an "item-not-found" error
								model.removeRow(i);
							}
							
							break;
						}
					}	
					break;
				}
				case ROOM_ENTER:
				{
					// make sure it the room doesn't already exist in the list
					for (int i = 0; i < model.getRowCount(); ++i)
					{
						final GroupChatItem item = (GroupChatItem)model.getValueAt(i, 1);
						if (item.getRoom().getRoom().equals(event.getRoom().getRoom()) )
							return;
					}
					
					final GroupChatItem item = new GroupChatItem();
					item.setRoom(event.getRoom());

					model.addRow(item);
					
					break;
				}
				

				default:
					break;
			}
		}	
	}
	
	protected void rejoinRoom(EntityBareJid roomJid)
	{
		GroupChatManager.getInstance(con).reEnterGroupChat(roomJid);
	}
}
