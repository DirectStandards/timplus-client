package org.directtruststandards.timplus.client.chat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.text.EditorKit;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.apache.commons.lang3.StringUtils;
import org.directtruststandards.timplus.client.filetransport.OutgoingFileTransferDialog;
import org.directtruststandards.timplus.client.notifications.AMPMessageNotification;
import org.directtruststandards.timplus.client.util.WrapEditorKit;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.xhtmlim.packet.XHTMLExtension;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;

public class ChatDialog extends JDialog
{
	private static final long serialVersionUID = 8460553115637319919L;

	private static final String TEXT_SUBMIT = "text-submit";
	
	private static final String INSERT_BREAK = "insert-break";
	
	protected ExecutorService typingChatExecutor;
	
	protected BlockingQueue<ChatState> activityQueue;
	
	protected AbstractXMPPConnection con;
	
	protected Jid contactJid;
	
	protected JTextPane chatText;
	
	protected JTextPane createText;
	
	protected JScrollPane textScrollPane;
	
	protected Map<String, AtomicInteger> unackedMessages; 
	
	protected ChatState activeChatState;
	
	protected AtomicBoolean runChatStateThread;
	
	protected JLabel activityLabel;
	
	public ChatDialog(Jid contactJid, AbstractXMPPConnection con)
	{
		super((Frame)null, contactJid.toString());
		
		this.contactJid = contactJid;

		this.con = con;
		
		this.unackedMessages = new HashMap<>();
		
		this.typingChatExecutor = Executors.newSingleThreadExecutor();
		
		this.activeChatState = ChatState.active;
		
		this.runChatStateThread = new AtomicBoolean(true);
		
		this.activityQueue = new LinkedBlockingQueue<>();
		
		setSize(400, 400);
		
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		Point pt = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
		
		this.setLocation(pt.x - (100), pt.y - (50));	
		
		initUI();
		
		typingChatExecutor.execute(new TypingActivity());
	}
	
	public void resetChat(AbstractXMPPConnection con)
	{
		this.con = con;
	}
	
	public void initUI()
	{
		getContentPane().setLayout(new BorderLayout());
		
		/*
		 * Chat text
		 */
		chatText = new JTextPane();
		chatText.setEditable(false);
		chatText.setEditorKit(new HTMLEditorKit());
		chatText.setDocument(new HTMLDocument());
		
		textScrollPane = new JScrollPane(chatText);
		textScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		activityLabel = new JLabel(" ");
		final Font displayFont = new Font("Helvetica", Font.ITALIC | Font.PLAIN, 12);
		activityLabel.setFont(displayFont);
		activityLabel.setForeground(Color.GRAY);
		
		
		final JPanel textPanel = new JPanel(new BorderLayout());
		textPanel.add(textScrollPane, BorderLayout.CENTER);
		textPanel.add(activityLabel, BorderLayout.SOUTH);
		
		getContentPane().add(textPanel, BorderLayout.CENTER);
		
		
		
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
		 * Actions
		 */
		chatText.setDropTarget(new DropTarget() 
		{
			private static final long serialVersionUID = 7559255704248380014L;

			public synchronized void drop(DropTargetDropEvent evt) 
		    {
				sendFile(evt);
		    }
		});
		
		createText.setDropTarget(new DropTarget() 
		{
			private static final long serialVersionUID = 7559255704248380014L;

			public synchronized void drop(DropTargetDropEvent evt) 
		    {
				sendFile(evt);
		    }
		});
		
		
		createText.addKeyListener(new KeyAdapter()
		{

			
			@Override
			public void keyTyped(KeyEvent e)
			{
				activityQueue.offer(ChatState.composing);
				//super.keyTyped(e);
			}
			
			
		});
		
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
		
		addWindowListener(new  WindowAdapter()
		{
			@Override
			public void windowClosed(WindowEvent e)
			{
				runChatStateThread.set(false);
			}
			
		});
	}
	
	public void onIncomingChatState(final ChatState state)
	{
		EventQueue.invokeLater(() ->
		{
			if (state == ChatState.composing)
				activityLabel.setText(contactJid.getLocalpartOrNull() + " is typing");
			else
				activityLabel.setText(" ");
		});

	}
	
	public void onIncomingMessage(Message msg)
	{
		final StyledDocument doc = chatText.getStyledDocument();
		final EditorKit editKit = chatText.getEditorKit();
		// check to see if this is a delayed message
	    final DelayInformation delay = (DelayInformation)msg.getExtension(DelayInformation.NAMESPACE);
		
		final String pattern = "HH:mm:ss";
		final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		
		String date = "";
		if (delay == null)
			date = simpleDateFormat.format(new Date());
		else
		{
			date = simpleDateFormat.format(delay.getStamp()) + " Offline";
		}
		
		final StringBuilder builder = new StringBuilder("(").append(date).append(") ");
		
		
		/*
		 * Do this on the event queue so the 
		 * scroll pane auto scrolls to the bottom
		 */
        java.awt.EventQueue.invokeLater(new Runnable() 
        {
            @Override
            public void run() 
            {
            	synchronized(unackedMessages)
            	{
	            	
	    			builder.append(msg.getFrom().asBareJid().getLocalpartOrNull());
	    			
	    			
	    			builder.append("\r\n");
	    			
	    			try
	    			{
		    			final SimpleAttributeSet red = new SimpleAttributeSet();
		    			StyleConstants.setForeground(red, Color.red);
		    			StyleConstants.setItalic(red, true);
		    			
		    			doc.insertString(doc.getLength(), builder.toString(), red);
		    			if (!(editKit instanceof HTMLEditorKit && doc instanceof HTMLDocument))
		    				doc.insertString(doc.getLength(), msg.getBody() + "\r\n", null);
		    			else
		    			{
		    				// check if there is alternative text
		    				final XHTMLExtension htmlBody = (XHTMLExtension)msg.getExtension(XHTMLExtension.NAMESPACE);
		    				if (htmlBody != null)
		    				{
		    					final HTMLEditorKit htmlKit = (HTMLEditorKit)editKit;
		    					for (CharSequence seq : htmlBody.getBodies())
		    					{
		    						htmlKit.insertHTML((HTMLDocument)doc, doc.getLength(), seq.toString(), 0, 0, null);
		    					}
		    				}
		    				else
		    					doc.insertString(doc.getLength(), msg.getBody() + "\r\n", null);
		    			}
	    			}
	    			catch (Exception e) {}
            	}
            }
        });


	}
	
	protected void sendMessage()
	{
		if (!StringUtils.isEmpty(createText.getText().trim()))
		{
			final ChatManager mgr  = ChatManager.getInstanceFor(con);
			final Chat chat = mgr.chatWith(contactJid.asEntityBareJidIfPossible());
			
			try
			{
				final String text = createText.getText().trim();
				
		        Message stanza = new Message();
		        stanza.setBody(text);
		        stanza.setType(Message.Type.chat);
		        stanza.addExtension(new ChatStateExtension(ChatState.active));
				
				chat.send(stanza);	
				
				final StyledDocument doc = chatText.getStyledDocument();
				
				final String pattern = "HH:mm:ss";
				final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
				final String date = simpleDateFormat.format(new Date());

				final StringBuilder builder = new StringBuilder("(").append(date).append(") ");
				builder.append("Me");
				
    			int notificationInsertLoc = doc.getLength() + builder.toString().length();
    			unackedMessages.put(stanza.getStanzaId(), new AtomicInteger(notificationInsertLoc));
				
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
	
	public void onIncomingAMPMessage(AMPMessageNotification notif)
	{
		synchronized (unackedMessages)
		{
			final AtomicInteger notifLoc = unackedMessages.remove(notif.getStanzaId());
			if (notifLoc != null)
			{
				final StringBuilder builder = new StringBuilder("  ");
				switch (notif.getMessageStatus())
				{
					case DELIVERED:
						builder.append("delivered");
						break;
					case STORED_OFFLINE:
						builder.append("stored offline");
						break;
					case ERROR:
						builder.append("error");
						break;
				}
				
				final SimpleAttributeSet notifFont = new SimpleAttributeSet();
				StyleConstants.setForeground(notifFont, Color.gray);
				StyleConstants.setItalic(notifFont, true);
				StyleConstants.setAlignment(notifFont, StyleConstants.ALIGN_RIGHT);
				
		        java.awt.EventQueue.invokeLater(new Runnable() 
		        {
		            @Override
		            public void run() 
		            {
		            	synchronized(unackedMessages)
		            	{
		            	
			            	final StyledDocument doc = chatText.getStyledDocument();
			            	try
			            	{
			            		doc.insertString(notifLoc.get(), builder.toString(), notifFont);
			            		
				            	// shift all exiting un-acked message locations
				            	for (Entry<String, AtomicInteger> entry : unackedMessages.entrySet())
				            	{
				            		if (entry.getKey() != notif.getStanzaId() && entry.getValue().get() > notifLoc.get())
				            		{
				            			int newLoc = entry.getValue().get() + builder.toString().length();
				            			entry.getValue().set(newLoc);
				            		}
				            	}
			            	}
			            	catch (Exception e) {};
		            	}
		            }
		        });
				
			}
		}
	}
	
	protected void sendFile(DropTargetDropEvent evt)
	{
        try {
            evt.acceptDrop(DnDConstants.ACTION_LINK);
            
            @SuppressWarnings("unchecked")
			final List<File> droppedFiles = (List<File>)evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
            
            if (droppedFiles != null && droppedFiles.size() > 0)
            {
            	if (droppedFiles.size() > 1)
            	{
					JOptionPane.showMessageDialog(this,"This TIM+ client only allows one file at a time to be dropped.", 
				 		    "File Transfer", JOptionPane.WARNING_MESSAGE);
					
					return;
            	}
            	
            	// make sure the recipient is online
            	final Roster roster = Roster.getInstanceFor(con);
            	
        		final Presence presense = roster.getPresence(contactJid.asBareJid());
        		final Jid resourceJid = presense.getFrom();
        		if (resourceJid == null || !(resourceJid instanceof EntityFullJid))
        		{
					JOptionPane.showMessageDialog(this,"No online resources found for " + contactJid.asBareJid().toString() + ".\r\nCannot transfer file.", 
				 		    "File Transfer", JOptionPane.WARNING_MESSAGE);
        			System.out.println("No online resources found for " + contactJid.asBareJid().toString() + ".  ");
        			return;
        		}
            	
            	final File transFile = droppedFiles.get(0);
            	
    			int selection = JOptionPane.showConfirmDialog(this, "Do you wish to send the file " + transFile.getName() +  " \r\nto recipient " + this.contactJid.asBareJid().toString() + "?",
    					"File Transfer", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
    			
    			if (selection == JOptionPane.NO_OPTION)
    				return;	
    			
    			final OutgoingFileTransferDialog transDiag =  new OutgoingFileTransferDialog(transFile, resourceJid, con);
    			transDiag.setVisible(true);
    			transDiag.sendFile();
            }
 
        } 
        catch (Exception ex) 
        {
			JOptionPane.showMessageDialog(this,"Unknown error creating file transfer request.", 
		 		    "File Transfer", JOptionPane.ERROR_MESSAGE);
        }
	}
	
	protected class TypingActivity implements Runnable
	{
		@Override
		public void run()
		{
			while (runChatStateThread.get())
			{
		        try
		        {
					ChatState state = (activeChatState == ChatState.composing) ? activityQueue.poll(2, TimeUnit.SECONDS) : activityQueue.take();
					if (state == ChatState.composing)
					{
						// if we are already a composing state, don't send
						if (activeChatState == ChatState.composing)
							continue;
						
						final ChatManager mgr  = ChatManager.getInstanceFor(con);
						final Chat chat = mgr.chatWith(contactJid.asEntityBareJidIfPossible());
						
						activeChatState = ChatState.composing;
						
						// send the message for typing
				        Message stanza = new Message();
				        stanza.setType(Message.Type.chat);
				        stanza.addExtension(new ChatStateExtension(ChatState.composing));
				        chat.send(stanza);	
	
					}
					else if (state == null)
					{
						if (activeChatState == ChatState.active)
							continue;
						
						final ChatManager mgr  = ChatManager.getInstanceFor(con);
						final Chat chat = mgr.chatWith(contactJid.asEntityBareJidIfPossible());
						
						activeChatState = ChatState.active;
						
						// send the message for active
				        Message stanza = new Message();
				        stanza.setType(Message.Type.chat);
				        stanza.addExtension(new ChatStateExtension(ChatState.active));
				        chat.send(stanza);	
					}
		        }
		        catch (Exception e)
		        {
		        	e.printStackTrace();
		        }
			}
		}
	}
}
