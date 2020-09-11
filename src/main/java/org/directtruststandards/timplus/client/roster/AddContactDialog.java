package org.directtruststandards.timplus.client.roster;

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
import javax.swing.JTextField;

import org.apache.commons.lang3.StringUtils;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

public class AddContactDialog extends JDialog
{
	private static final long serialVersionUID = 5838777481362641933L;

	protected JTextField endpointText;
	
	protected JTextField aliasText;
	
	protected AddContactStatus addStatus;
	
	public AddContactDialog(Window w)
	{
		super(w, "Add Contact");
		
		addStatus = AddContactStatus.CANCEL;
		
		this.setModal(true);
		this.setResizable(false);
		
		setSize(320, 140);
		
		Point pt = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
		
		this.setLocation(pt.x - (350), pt.y - (200));	
		
		initUI();
	}
	
	protected void initUI()
	{
		getContentPane().setLayout(new BorderLayout());
		
		final JPanel contactPanel = new JPanel();
		contactPanel.setLayout(new GridLayout(2, 1));
		
		/*
		 * Endpoint
		 */
		final JPanel endpointPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		final JLabel endpointLabel = new JLabel("TIM+ Address:");
		endpointLabel.setPreferredSize(new Dimension(100, endpointLabel.getPreferredSize().getSize().height));
		endpointText = new JTextField("");

		endpointText.setPreferredSize(new Dimension(210, endpointText.getPreferredSize().getSize().height));
		
		endpointPanel.add(endpointLabel);
		endpointPanel.add(endpointText);
		contactPanel.add(endpointPanel);
		
		/*
		 * Alias
		 */
		final JPanel aliasPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		final JLabel aliasLabel = new JLabel("Alias:");
		aliasLabel.setPreferredSize(new Dimension(100, aliasLabel.getPreferredSize().getSize().height));
		aliasText = new JTextField("");

		aliasText.setPreferredSize(new Dimension(210, aliasText.getPreferredSize().getSize().height));
		
		aliasPanel.add(aliasLabel);
		aliasPanel.add(aliasText);
		contactPanel.add(aliasPanel);
		
		getContentPane().add(contactPanel, BorderLayout.NORTH);
		
		/*
		 * Buttons
		 */
		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		
		final JButton add = new JButton("Add");
		final JButton cancel = new JButton("Cancel");
		
		buttonPanel.add(cancel);
		buttonPanel.add(add);
		
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
		
		add.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				addPerformed();
			}
		});
	}
	
	protected void addPerformed()
	{
		if (StringUtils.isEmpty(endpointText.getText().trim()))
		{
			JOptionPane.showMessageDialog(this,"TIM+ address must have a value.", 
		 		    "Invalid Address", JOptionPane.ERROR_MESSAGE );
			return;
		}
		
		try
		{
			JidCreate.bareFrom(endpointText.getText().trim());
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this,"TIM+ address is not a valid TIM+ address.", 
		 		    "Invalid Address", JOptionPane.ERROR_MESSAGE );
			return;
		}
		
		addStatus = AddContactStatus.ADD;
		setVisible(false);	
	}
	
	public AddContactStatus getAddActionStatus()
	{
		return addStatus;
	}
	
	public enum AddContactStatus
	{
		ADD, 
		
		CANCEL;
	}
	
	public Jid getContactJid()
	{
		try
		{
			return JidCreate.bareFrom(endpointText.getText().trim());
		}
		catch (Exception e)
		{
			return null;
		}
		
	}
	
	public String getAlias()
	{
		return !StringUtils.isEmpty(aliasText.getText().trim()) ? aliasText.getText().trim() : endpointText.getText().trim();
	}
}
