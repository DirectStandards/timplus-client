package org.directtruststandards.timplus.client.config;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class PreferencesDialog extends JDialog
{
	private static final long serialVersionUID = 8961135848096696222L;

	protected final org.apache.commons.configuration2.Configuration config;
	
	protected JTextField groupChatNickNameText;
	
	public PreferencesDialog(Window parent, org.apache.commons.configuration2.Configuration config)
	{
		super(parent);
		
		this.config = config;
		
		this.setTitle("TIM+ Client Preferences");
		this.setModal(true);
		this.setResizable(false);
		
		setSize(400, 250);
		
		Point pt = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
		
		this.setLocation(pt.x - (200), pt.y - (100));	
		
		initUI();
	}
	
	protected void initUI() 
	{ 
		
		getContentPane().setLayout(new BorderLayout());
	
		final JPanel preferencesConfigPanel = new JPanel();
		preferencesConfigPanel.setLayout(new GridLayout(4, 1));
		
		/*
		 * Group Chat Nick Name
		 */
		final JPanel nickNamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		final JLabel nickNameLabel = new JLabel("Group Chat Nickname:");
		nickNameLabel.setPreferredSize(new Dimension(150, nickNameLabel.getPreferredSize().getSize().height));
		groupChatNickNameText = new JTextField("");
		if (config.getProperty("timplus.preferences.groupchat.defaultNickName") != null)
			groupChatNickNameText.setText(config.getProperty("timplus.preferences.groupchat.defaultNickName").toString());
		
		groupChatNickNameText.setPreferredSize(new Dimension(230, groupChatNickNameText.getPreferredSize().getSize().height));
		
		nickNamePanel.add(nickNameLabel);
		nickNamePanel.add(groupChatNickNameText);
		preferencesConfigPanel.add(nickNamePanel);
		
		getContentPane().add(preferencesConfigPanel, BorderLayout.CENTER);
		
		/*
		 * Buttons
		 */
		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		
		final JButton save = new JButton("Save");
		final JButton cancel = new JButton("Cancel");
		
		buttonPanel.add(cancel);
		buttonPanel.add(save);
		
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
		
		save.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				savePreferences();
			}
	
		});
	}	
	
	protected void savePreferences()
	{		
		config.setProperty("timplus.preferences.groupchat.defaultNickName", groupChatNickNameText.getText().trim());

		setVisible(false);
	}	
}
