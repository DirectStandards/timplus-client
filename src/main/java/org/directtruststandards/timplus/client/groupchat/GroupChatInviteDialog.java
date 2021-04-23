package org.directtruststandards.timplus.client.groupchat;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.util.StringUtils;
import org.jxmpp.jid.impl.JidCreate;

public class GroupChatInviteDialog extends JDialog
{
	private static final long serialVersionUID = 7628501375257987712L;

	protected JComboBox<String> inviteeText;
	
	protected JTextArea messgaeText;
	
	protected AbstractXMPPConnection con;
	
	protected boolean invite;
	
	public GroupChatInviteDialog(Window parent, AbstractXMPPConnection con)
	{
		super(parent, "Invite to conversation");
		
		setSize(500, 150);
		
		setResizable(false);
		
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		setModal(true);
		
		Point pt = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
		
		this.setLocation(pt.x - (250), pt.y - (100));	
		
		this.con = con;
		
		initUI();
		
		invite = false;
	}
	
	protected void initUI()
	{
		getContentPane().setLayout(new BorderLayout(10, 10));
		
		/*
		 * Invitee
		 */
		final JPanel infoPanel = new JPanel(new GridLayout(4,1));
		
		
		final Roster roster = Roster.getInstanceFor(con);
		final ArrayList<String> contacts = new ArrayList<>();
		for (RosterEntry entry : roster.getEntries())
			contacts.add(entry.getJid().asBareJid().toString());
		
		Collections.sort(contacts);
		
		final JLabel inviteeLable = new JLabel("Invitee TIM+ Address");
		inviteeText = new JComboBox<>(contacts.toArray(new String[contacts.size()]));
		inviteeText.setEditable(true);
		inviteeText.setSelectedIndex(-1);
		
		infoPanel.add(inviteeLable);
		infoPanel.add(inviteeText);
		
		/*
		 * Invitation message
		 */
		final JLabel messgaeLable = new JLabel("Message (optional)");
		messgaeText = new JTextArea();
		
		infoPanel.add(messgaeLable);
		infoPanel.add(messgaeText);
		
		getContentPane().add(infoPanel, BorderLayout.CENTER);
		
		/*
		 * Buttons
		 */
		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		
		final JButton invite = new JButton("Invite");
		final JButton cancel = new JButton("Cancel");
		
		buttonPanel.add(cancel);
		
		buttonPanel.add(invite);
		
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		
		/*
		 * Actions 
		 */
		cancel.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				setVisible(false);
			}
			
		});
		
		invite.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				validateInvitation();
			}
			
		});
		
	}
	
	public boolean isInvited()
	{
		return this.invite;
	}
	
	public String getInvitee()
	{
		return inviteeText.getEditor().getItem().toString().trim();
	}
	
	public String getMessage()
	{
		return messgaeText.getText().trim();
	}
	
	protected void validateInvitation()
	{
		if (StringUtils.isEmpty(getInvitee()))
		{
			JOptionPane.showMessageDialog(this,"Invitee cannot be empty.", 
		 		    "Group Chat", JOptionPane.WARNING_MESSAGE);
			
			return;
		}
		
		try
		{
			JidCreate.entityBareFrom(getInvitee());
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this,"Invalid TIM+ address.", 
		 		    "Group Chat", JOptionPane.WARNING_MESSAGE);
			
			return;
		}
		
		invite = true;
		setVisible(false);
	}
}
