package org.directtruststandards.timplus.client.groupchat;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.directtruststandards.timplus.client.config.PreferencesManager;
import org.directtruststandards.timplus.client.roster.GroupChatItem;
import org.directtruststandards.timplus.client.util.DocumentUtils;
import org.directtruststandards.timplus.client.util.WrapEditorKit;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PresenceListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.xhtmlim.packet.XHTMLExtension;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class GroupChatDialog extends JDialog
{

	private static final long serialVersionUID = 247130058897979136L;

	protected static final String XHTML_IM_HEADER = "<body xmlns='http://www.w3.org/1999/xhtml'>";
	
	protected static final String XHTML_IM_HEADER_ALT = "<body xmlns=\"http://www.w3.org/1999/xhtml\">";
			
	protected AbstractXMPPConnection con;
	
	protected MultiUserChat room;
	
	private static final String TEXT_SUBMIT = "text-submit";
	
	private static final String INSERT_BREAK = "insert-break";
	
	protected JTextPane createText;
	
	protected JTable participantList;
	
	protected WebView webChatView;
	
	public GroupChatDialog(AbstractXMPPConnection con, MultiUserChat room, boolean initialRoomCreation)
	{
		super((Frame)null, "Group Chat: " + room.getRoom().getLocalpart());
		
		this.con = con;
		
		this.room = room;
		
		setSize(500, 430);
		
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		Point pt = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
		
		this.setLocation(pt.x - (100), pt.y - (50));	
		
		initUI();
		
		resetChat(con, initialRoomCreation);
		
		refreshMemberList();
	}
	
	protected void initUI()
	{
		getContentPane().setLayout(new BorderLayout(10, 10));
		
		/*
		 * Chat text
		 */
		
		final JFXPanel jfxPane = new JFXPanel();
		
		Platform.setImplicitExit(false);
		Platform.runLater(() -> 
		{
			try				
			{
				webChatView = new WebView();
				webChatView.getEngine().loadContent( "<html><body style=\"font-size:90%;\"></body></html>");
				
				StackPane root = new StackPane();
				root.getChildren().add(webChatView);
				
				jfxPane.setScene( new Scene( root ) );
			}
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		});
		
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
		
		//final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, textScrollPane, participantPanel);
		final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, jfxPane, participantPanel);
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
	
	public void resetChat(AbstractXMPPConnection con, boolean initialRoomCreation)
	{		
		this.con = con;
		
		room = MultiUserChatManager.getInstanceFor(con).getMultiUserChat(room.getRoom());
		
		final String nickString = PreferencesManager.getInstance().getPreferences().getGroupChatNickName();
		
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
		
		/*
		 * If this is result of an initial room creation, we already joined the room
		 * at the time that we created and unlocked the room.  Trying to rejoin at this point
		 * could cause us to leave and try to rejoin which will cause the room to be destroyed
		 * because we left the room and were the only occupants.  An empty room automatically
		 * get destroyed.
		 */
		
		if (!initialRoomCreation)
		{
			try
			{
				final Resourcepart nickname = (StringUtils.isEmpty(nickString)) ? Resourcepart.from(con.getUser().getLocalpart().toString()) :
				Resourcepart.from(nickString);
			
				room.join(nickname);
			}
			catch (Exception e)
			{
				JOptionPane.showMessageDialog(null, "An error occured joining the group chat room.", 
			 		    "Group chat failure", JOptionPane.ERROR_MESSAGE );
				
				setVisible(false);
				return;
			}
		}
	}
	
	public void onIncomingMessage(final Message msg)
	{
		final AtomicBoolean isFromMe = new AtomicBoolean(false);
		
		if (msg.getFrom().getResourceOrNull() == null)
			return;
		
		// check to see if this is a delayed message
	    final DelayInformation delay = (DelayInformation)msg.getExtension(DelayInformation.NAMESPACE);
		
	    /*
	     * If the message is from ourself, but has a delayed time, then we will
	     * go ahead and put the message in the conversation panel and tag it as history
	     */
		if (room.getNickname().equals(msg.getFrom().getResourceOrNull()))
		{
			if (delay == null)
				return;
			else
				isFromMe.set(true);
		}
		
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
		if (isFromMe.get())
			builder.append("Me");
		else
			builder.append(msg.getFrom().getResourceOrNull());		
		
        while (webChatView == null)
        {
        	try
        	{
        		TimeUnit.SECONDS.sleep(1);
        	}
        	catch (Exception e) {}
        }
		final WebEngine eng = webChatView.getEngine();

        Document checkDoc = eng.getDocument();
        while (checkDoc == null)
        {
        	try
        	{
        		TimeUnit.SECONDS.sleep(1);
        	}
        	catch (Exception e) {}
        }
		
		/*
		 * Do this on the platform queue 
		 */
		Platform.runLater(() -> 
		{
			
			final WebEngine engine = webChatView.getEngine();

	        Document webDoc = engine.getDocument();
	        Element body = (Element) webDoc.getElementsByTagName("body").item(0);
			
	        Element msgHeader = webDoc.createElement("i");
	        if (isFromMe.get())
	        	msgHeader.setAttribute("style", "color:blue");
	        else
	        	msgHeader.setAttribute("style", "color:red");
	        msgHeader.setTextContent(builder.toString());
	        
	        body.appendChild(msgHeader);
	        
	        Element br = webDoc.createElement("br");
	        body.appendChild(br);
	        
			// check if there is alternative text
			final XHTMLExtension htmlBody = (XHTMLExtension)msg.getExtension(XHTMLExtension.NAMESPACE);
			if (htmlBody != null)
			{
				for (CharSequence seq : htmlBody.getBodies())
				{
					final StringBuilder sb = new StringBuilder(seq.length());
					sb.append(seq);
					final String seqText = sb.toString();
					
					// this is probably too heavy using a document parser, but at least it's robust and reliable
			        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			        try
			        {
				        final DocumentBuilder docBuilder = factory.newDocumentBuilder();
				        final Document incomingDoc = docBuilder.parse(new InputSource(new StringReader(seqText)));
				        Element msgBodyEle = (Element)incomingDoc.getFirstChild();
				        
				        // check if the next element is a <p>.... if so, let eat it and move on
				        if (msgBodyEle.getFirstChild() instanceof Element && ((Element)msgBodyEle.getFirstChild()).getTagName().compareToIgnoreCase("p") == 0)
				        	msgBodyEle = (Element)msgBodyEle.getFirstChild();
				        
				        Element msgText = DocumentUtils.deepCopyElement(msgBodyEle, webDoc, "msg");
				        body.appendChild(msgText);

			        }
			        catch (Exception e)
			        {
			        	e.printStackTrace();
			        }
				}
			}
			else
			{
		        Element msgText = webDoc.createElement("msg");
		        msgText.setTextContent(msg.getBody());
		        body.appendChild(msgText);
			}
	        
	        Element br2 = webDoc.createElement("br");
	        body.appendChild(br2);
	        
	        webChatView.getEngine().executeScript("window.scrollTo(0, document.body.scrollHeight);");
	        
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
				
				Platform.runLater(() -> 
				{
					final String pattern = "HH:mm:ss";
					final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
					final String date = simpleDateFormat.format(new Date());

					final StringBuilder builder = new StringBuilder("(").append(date).append(") ");
					builder.append("Me");
					
					
					final WebEngine engine = webChatView.getEngine();
			        Document doc = engine.getDocument();
			        Element body = (Element) doc.getElementsByTagName("body").item(0);
					
			        Element msgHeader = doc.createElement("i");
			        msgHeader.setAttribute("style", "color:blue");
			        msgHeader.setTextContent(builder.toString());
			        
			        body.appendChild(msgHeader);
			        
			        Element br = doc.createElement("br");
			        body.appendChild(br);
			        
			        Element msg = doc.createElement("msg");
			        msg.setTextContent(text);
			        body.appendChild(msg);
			        
			        Element br2 = doc.createElement("br");
			        body.appendChild(br2);
			     
			        webChatView.getEngine().executeScript("window.scrollTo(0, document.body.scrollHeight);");
				});
				
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
			Platform.runLater(() -> 
			{				
				final WebEngine engine = webChatView.getEngine();
		        Document doc = engine.getDocument();
		        
		        if (doc != null)
		        {
			        Element body = (Element) doc.getElementsByTagName("body").item(0);
					
			        Element msgHeader = doc.createElement("i");
			        msgHeader.setAttribute("style", "color:red");
			        msgHeader.setTextContent(builder.toString());
			        
			        body.appendChild(msgHeader);
			        
			        Element br = doc.createElement("br");
			        body.appendChild(br);
			        
			        Element msg = doc.createElement("msg");
			        msg.setAttribute("style", "color:grey");
			        msg.setTextContent(actionText);
			        body.appendChild(msg);
			        
			        Element br2 = doc.createElement("br");
			        body.appendChild(br2);
			        
			        webChatView.getEngine().executeScript("window.scrollTo(0, document.body.scrollHeight);");
		        }
			});		
		}
	}
	
	protected void inviteContact()
	{
		final GroupChatInviteDialog diag = new GroupChatInviteDialog(this, con);
		
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
