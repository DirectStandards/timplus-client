package org.directtruststandards.timplus.client.groupchat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.apache.commons.lang3.StringUtils;
import org.directtruststandards.timplus.client.roster.GroupChatItem;
import org.directtruststandards.timplus.client.util.WrapEditorKit;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PresenceListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.impl.JidCreate;

public class GroupChatDialog extends JDialog
{

	private static final long serialVersionUID = 247130058897979136L;

	protected AbstractXMPPConnection con;
	
	protected MultiUserChat room;
	
	private static final String TEXT_SUBMIT = "text-submit";
	
	private static final String INSERT_BREAK = "insert-break";
	
	protected JTextPane chatText;
	
	protected JTextPane createText;
	
	protected JScrollPane textScrollPane;
	
	protected JTable participantList;
	
	public GroupChatDialog(AbstractXMPPConnection con, MultiUserChat room)
	{
		super((Frame)null, "Group Chat: " + room.getRoom().getLocalpart());
		
		this.con = con;
		
		this.room = room;
		
		setSize(500, 430);
		
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		Point pt = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
		
		this.setLocation(pt.x - (100), pt.y - (50));	
		
		initUI();
		
		resetChat(con);
		
		refreshMemberList();
	}
	
	protected void initUI()
	{
		getContentPane().setLayout(new BorderLayout(10, 10));
		
		/*
		 * Chat text
		 */
		chatText = new JTextPane();
		chatText.setEditable(false);
		
		textScrollPane = new JScrollPane(chatText);
		textScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		/*
		 * Participant list
		 */
		final JPanel participantPanel = new JPanel(new BorderLayout());
		
		final JLabel participantLabel = new JLabel("Room Participants");
		participantPanel.add(participantLabel, BorderLayout.NORTH);
		
		participantList = new JTable(new GroupChatMemberTableModel(new ArrayList<>()));
		
		participantList.setTableHeader(null);
		participantList.setRowHeight(30);
		participantList.setDefaultRenderer(GroupChatItem.class, new GroupChatMemberItemRenderer());
		participantList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		final JScrollPane participantScrollPane = new JScrollPane(participantList);
		participantList.setFillsViewportHeight(true);
		
		participantPanel.add(participantScrollPane, BorderLayout.CENTER);
		
		/*
		 * Split pane for chat and member list
		 */
		
		final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, textScrollPane, participantPanel);
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerLocation(350);
		
		getContentPane().add(splitPane, BorderLayout.CENTER);

		/*
		 * Text creation
		 */
		final JPanel textCreationPanel = new JPanel(new GridBagLayout());
		
		textCreationPanel.setSize(new Dimension(textCreationPanel.getPreferredSize().width, 300));
		
		createText = new JTextPane();
		createText.setEditorKit(new WrapEditorKit());
		createText.setSize(new Dimension(createText.getPreferredSize().width, 300));
	
		
		final JScrollPane createtextScrollPane = new JScrollPane(createText);
		createtextScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		createtextScrollPane.setSize(new Dimension(createtextScrollPane.getPreferredSize().width, 300));
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.ipady = 50;
		c.weightx = 1;
		textCreationPanel.add(createtextScrollPane, c);
		
		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		final JButton sendButton = new JButton("Send");
		
		buttonPanel.add(sendButton);
		
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 1;
		textCreationPanel.add(buttonPanel, c);
		
		getContentPane().add(textCreationPanel, BorderLayout.SOUTH);
		
		
		/*
		 * Menu bar
		 */
		final JMenuBar menuBar = new JMenuBar();
		
		final JMenu conversationMenu = new JMenu("Conversation");
		final JMenuItem inviteContact = new JMenuItem("Invite...");
		inviteContact.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				inviteContact();
			}	
		});
		conversationMenu.add(inviteContact);
		
		menuBar.add(conversationMenu);
		
		this.setJMenuBar(menuBar);
		
		/*
		 * Actions
		 */
		
	    final InputMap input = createText.getInputMap();
	    KeyStroke enter = KeyStroke.getKeyStroke("ENTER");
	    KeyStroke shiftEnter = KeyStroke.getKeyStroke("shift ENTER");
	    input.put(shiftEnter, INSERT_BREAK);
	    input.put(enter, TEXT_SUBMIT);
	    
	    final ActionMap actions = createText.getActionMap();

	    actions.put(TEXT_SUBMIT, new AbstractAction() 
	    {
			private static final long serialVersionUID = -3128070169467821818L;

			@Override
	        public void actionPerformed(ActionEvent e) 
	        {
	        	sendMessage();
	        }
	    });
	    
		sendButton.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				sendMessage();		
			}
			
		});
	}
	
	public void resetChat(AbstractXMPPConnection con)
	{
		this.con = con;
		
		room = MultiUserChatManager.getInstanceFor(con).getMultiUserChat(room.getRoom());
		
		room.addMessageListener(new MessageListener() 
		{
			@Override
			public void processMessage(Message message)
			{
				onIncomingMessage(message);
			}	
		});
		
		room.addParticipantListener(new PresenceListener()
		{

			@Override
			public void processPresence(Presence presence)
			{
				refreshMemberList();
				
				presenceUpdated(presence);
			}
			
		});
	}
	
	public void onIncomingMessage(Message msg)
	{
		if (msg.getFrom().getResourceOrNull() == null)
			return;
		
		if (room.getNickname().equals(msg.getFrom().getResourceOrNull()))
			return;
		
		final StyledDocument doc = chatText.getStyledDocument();
		
		// check to see if this is a delayed message
	    final DelayInformation delay = (DelayInformation)msg.getExtension(DelayInformation.NAMESPACE);
		
		final String pattern = "HH:mm:ss";
		final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		
		String date = "";
		if (delay == null)
			date = simpleDateFormat.format(new Date());
		else
		{
			date = simpleDateFormat.format(delay.getStamp()) + " History";
		}
		
		final StringBuilder builder = new StringBuilder("(").append(date).append(") ");
		
		
		/*
		 * Do this on the event queue so the 
		 * scroll pane auto scrolls to the bottom
		 */
        EventQueue.invokeLater(() ->
        {
            {

    			builder.append(msg.getFrom().getResourceOrNull().toString());
    			
    			
    			builder.append("\r\n");
    			
    			try
    			{
	    			final SimpleAttributeSet red = new SimpleAttributeSet();
	    			StyleConstants.setForeground(red, Color.red);
	    			StyleConstants.setItalic(red, true);
	    			
	    			doc.insertString(doc.getLength(), builder.toString(), red);
	    			doc.insertString(doc.getLength(), msg.getBody() + "\r\n", null);
    			}
    			catch (Exception e) {}
            }
        });


	}
	
	protected void sendMessage()
	{
		if (!StringUtils.isEmpty(createText.getText().trim()))
		{			
			try
			{
				final String text = createText.getText().trim();
								
				room.sendMessage(text);
				
				final StyledDocument doc = chatText.getStyledDocument();
				
				final String pattern = "HH:mm:ss";
				final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
				final String date = simpleDateFormat.format(new Date());

				final StringBuilder builder = new StringBuilder("(").append(date).append(") ");
				builder.append("Me");
				
    			builder.append("\r\n");
				
				final SimpleAttributeSet blue = new SimpleAttributeSet();
				StyleConstants.setForeground(blue, Color.blue);
				StyleConstants.setItalic(blue, true);
				doc.insertString(doc.getLength(), builder.toString(), blue);
				doc.insertString(doc.getLength(), text + "\r\n", null);
				
				createText.setText("");
			}
			catch (Exception e)
			{
				JOptionPane.showMessageDialog(this,"The message failed to send.", 
			 		    "Message failure", JOptionPane.ERROR_MESSAGE );
			}
			
		}
	}
	
	protected void refreshMemberList()
	{
		final List<GroupChatMemberItem> participants = new ArrayList<>();
		try
		{
			for (EntityFullJid part : room.getOccupants())
			{
				final GroupChatMemberItem item = new GroupChatMemberItem();
				item.setMember(part);
				participants.add(item);
			}
			
			this.participantList.setModel(new GroupChatMemberTableModel(participants));
		}
		catch (Exception e)
		{
			
		}
	}
	
	protected void presenceUpdated(Presence presence)
	{
		final StyledDocument doc = chatText.getStyledDocument();
		
		final String pattern = "HH:mm:ss";
		final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		
		final String date = simpleDateFormat.format(new Date());
		final StringBuilder builder = new StringBuilder("(").append(date).append(") ");
		
		String action = "";
		
		switch (presence.getType())
		{
			case available:
			{

				action = presence.getFrom().getResourceOrNull() + " entered the room\r\n";
				break;
			}
			case unavailable:
			{
				action = presence.getFrom().getResourceOrNull() + " left the room\r\n";
				
				break;
			}
			default:
				break;
		}
		
		final String actionText = action;
		
		if (!StringUtils.isEmpty(actionText))
		{
			EventQueue.invokeLater(() ->
			{
    			final SimpleAttributeSet red = new SimpleAttributeSet();
    			StyleConstants.setForeground(red, Color.red);
    			StyleConstants.setItalic(red, true);
    			
				final SimpleAttributeSet notifFont = new SimpleAttributeSet();
				StyleConstants.setForeground(notifFont, Color.gray);
				StyleConstants.setItalic(notifFont, true);
    			
				try
				{
					doc.insertString(doc.getLength(), builder.toString(), red);
					doc.insertString(doc.getLength(), actionText, notifFont);
				}
				catch (Exception e) {}
			});
		}
	}
	
	protected void inviteContact()
	{
		final GroupChatInviteDialog diag = new GroupChatInviteDialog(this);
		
		diag.setVisible(true);
		
		if (diag.invite)
		{
			try
			{
				room.invite(JidCreate.entityBareFrom(diag.getInvitee()), diag.getMessage());
			}
			catch (Exception e)
			{
				JOptionPane.showMessageDialog(this,"An error occured sending the invite.", 
			 		    "Group Chat", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
}
