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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.jivesoftware.smack.util.StringUtils;

public class ConfigurationDialog extends JDialog
{
	private static final long serialVersionUID = 7450563115889570565L;

	protected final org.apache.commons.configuration2.Configuration config;
	
	protected JTextField domainText;
	protected JTextField usernameText;
	protected JPasswordField passwordText;
	protected JTextField severText;
	
	public ConfigurationDialog(Window parent, org.apache.commons.configuration2.Configuration config)
	{
		super(parent);
		
		this.config = config;
		
		this.setTitle("TIM+ Client Configuration");
		this.setModal(true);
		this.setResizable(false);
		
		setSize(300, 250);
		
		Point pt = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
		
		this.setLocation(pt.x - (150), pt.y - (100));	
		
		initUI();
	}
	
	protected void initUI() 
	{ 
		
		getContentPane().setLayout(new BorderLayout());
	
		final JPanel serverConfigPanel = new JPanel();
		serverConfigPanel.setLayout(new GridLayout(4, 1));
		
		/*
		 * Domain
		 */
		final JPanel domainPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		final JLabel domainLabel = new JLabel("TIM+ Domain:");
		domainLabel.setPreferredSize(new Dimension(90, domainLabel.getPreferredSize().getSize().height));
		domainText = new JTextField("");
		if (config.getProperty("timplus.im.domain") != null)
			domainText.setText(config.getProperty("timplus.im.domain").toString());
		
		domainText.setPreferredSize(new Dimension(200, domainText.getPreferredSize().getSize().height));
		
		domainPanel.add(domainLabel);
		domainPanel.add(domainText);
		serverConfigPanel.add(domainPanel);
		
		/*
		 * Username
		 */
		final JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		final JLabel userLabel = new JLabel("Username:");
		userLabel.setPreferredSize(new Dimension(90, userLabel.getPreferredSize().getSize().height));
		usernameText = new JTextField("");
		if (config.getProperty("timplus.im.username") != null)
			usernameText.setText(config.getProperty("timplus.im.username").toString());
		
		usernameText.setPreferredSize(new Dimension(200, usernameText.getPreferredSize().getSize().height));
		
		userPanel.add(userLabel);
		userPanel.add(usernameText);
		serverConfigPanel.add(userPanel);
		
		/*
		 * Password
		 */
		final JPanel passwordPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		final JLabel passwordLabel = new JLabel("Password:");
		passwordLabel.setPreferredSize(new Dimension(90, passwordLabel.getPreferredSize().getSize().height));
		passwordText = new JPasswordField("");
		passwordText.setPreferredSize(new Dimension(200, passwordText.getPreferredSize().getSize().height));
		if (config.getProperty("timplus.im.password") != null)
			passwordText.setText(config.getProperty("timplus.im.password").toString());
		
		
		passwordPanel.add(passwordLabel);
		passwordPanel.add(passwordText);
		serverConfigPanel.add(passwordPanel);
		
		/*
		 * Server
		 */
		final JPanel serverPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		final JLabel serverLabel = new JLabel("Server:");
		serverLabel.setPreferredSize(new Dimension(90, serverLabel.getPreferredSize().getSize().height));
		severText = new JTextField("");
		if (config.getProperty("timplus.im.server") != null)
			severText.setText(config.getProperty("timplus.im.server").toString());
		
		severText.setPreferredSize(new Dimension(200, severText.getPreferredSize().getSize().height));
		
		serverPanel.add(serverLabel);
		serverPanel.add(severText);
		serverConfigPanel.add(serverPanel);		
		
		getContentPane().add(serverConfigPanel, BorderLayout.CENTER);
		
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
				saveConfig();
			}
	
		});
	}
	
	@SuppressWarnings("deprecation")
	protected void saveConfig()
	{
		if (StringUtils.isEmpty(domainText.getText().trim()))
		{
			JOptionPane.showMessageDialog(this,"TIM+ domain must have a value.", 
		 		    "Invalid Domain", JOptionPane.ERROR_MESSAGE );
			return;
		}
		
		if (StringUtils.isEmpty(usernameText.getText().trim()))
		{
			JOptionPane.showMessageDialog(this,"Username must have a value.", 
		 		    "Invalid Username", JOptionPane.ERROR_MESSAGE );
			return;
		}
		
		if (StringUtils.isEmpty(passwordText.getText().trim()))
		{
			JOptionPane.showMessageDialog(this,"Password must have a value.", 
		 		    "Invalid Password", JOptionPane.ERROR_MESSAGE );
			return;
		}
		
		config.setProperty("timplus.im.domain", domainText.getText().trim());
		config.setProperty("timplus.im.username", usernameText.getText().trim());
		config.setProperty("timplus.im.password", passwordText.getText().trim());
		config.setProperty("timplus.im.server", severText.getText().trim());

		setVisible(false);
	}
}
