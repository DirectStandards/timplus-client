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

public class ModifyContactAliasDialog extends JDialog
{
	private static final long serialVersionUID = 1L;

	protected JTextField aliasText;
	
	protected ModifyAliasStatus modifyStatus;
	
	protected final String originalAlias;
	
	public ModifyContactAliasDialog(Window w, String alias)
	{
		super(w, "Modify Alias");
		
		modifyStatus = ModifyAliasStatus.MODIFY;
		
		originalAlias = alias;
		
		this.setModal(true);
		this.setResizable(false);
		
		setSize(320, 130);
		
		Point pt = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
		
		this.setLocation(pt.x - (350), pt.y - (110));	
		
		initUI();
	}
	
	protected void initUI()
	{
		getContentPane().setLayout(new BorderLayout());
		
		final JPanel contactPanel = new JPanel();
		contactPanel.setLayout(new GridLayout(2, 1));
		
		/*
		 * Alias
		 */
		final JPanel aliasPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		final JLabel aliasLabel = new JLabel("Alias:");
		aliasLabel.setPreferredSize(new Dimension(50, aliasLabel.getPreferredSize().getSize().height));
		aliasText = new JTextField(originalAlias);

		aliasText.setPreferredSize(new Dimension(240, aliasText.getPreferredSize().getSize().height));
		
		aliasPanel.add(aliasLabel);
		aliasPanel.add(aliasText);
		contactPanel.add(aliasPanel);
		
		getContentPane().add(contactPanel, BorderLayout.CENTER);
		
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
				savePerformed();
			}
		});
	}	
	
	protected void savePerformed()
	{
		if (StringUtils.isEmpty(aliasText.getText().trim()))
		{
			JOptionPane.showMessageDialog(this,"TIM+ alias must have a value.", 
		 		    "Invalid Alias", JOptionPane.ERROR_MESSAGE );
			return;
		}
		
		modifyStatus = ModifyAliasStatus.MODIFY;
		setVisible(false);	
	}	
	
	public ModifyAliasStatus getModifyAliasStatus()
	{
		return modifyStatus;
	}
	
	public enum ModifyAliasStatus
	{
		MODIFY, 
		
		CANCEL;
	}
	
	public String getAlias()
	{
		return aliasText.getText().trim();
	}	
}
