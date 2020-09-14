package org.directtruststandards.timplus.client.vcard;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.apache.commons.lang3.StringUtils;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jxmpp.jid.Jid;

public class VCardDialog extends JDialog
{
	private static final long serialVersionUID = -216635417778695460L;

	protected final Jid contact;
	
	protected final VCard vCard;
	
	protected JTextPane infoText;
	
	public VCardDialog(Jid contact, VCard vCard)
	{
		super((Frame)null, "vCard Information");
		
		this.contact = contact;
		
		this.vCard = vCard;
		
		setSize(400, 600);
		
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		Point pt = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
		
		this.setLocation(pt.x - (100), pt.y - (300));	
		
		initUI();
		
		populateVCard();
	}
	
	protected void initUI()
	{
		getContentPane().setLayout(new BorderLayout());
		
		/*
		 * Top label
		 */
		final JLabel contactLabel = new JLabel("Info for " + contact.asBareJid().toString());
		final JPanel contactPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		
		contactPanel.add(contactLabel);
		getContentPane().add(contactPanel, BorderLayout.NORTH);
	
		/*
		 * Info text pane
		 */
		infoText = new JTextPane();
		infoText.setEditable(false);
		
		final JScrollPane textScrollPane = new JScrollPane(infoText);
		textScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		getContentPane().add(textScrollPane, BorderLayout.CENTER);
		
		/*
		 * Button panel
		 */
		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		final JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				setVisible(false);
				dispose();
			}
			
		});
		
		buttonPanel.add(closeButton);
		
		
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
	}
	
	protected void populateVCard()
	{
		final StyledDocument doc = infoText.getStyledDocument();
		
		addVCardDetail(doc, "First Name: ", vCard.getFirstName());
		
		addVCardDetail(doc, "Last Name: ", vCard.getLastName());
		
		addVCardDetail(doc, "Middle Name: ", vCard.getMiddleName());
		
		addVCardDetail(doc, "Prefix: ", vCard.getPrefix());
		
		addVCardDetail(doc, "Suffix: ", vCard.getSuffix());
		
		addVCardDetail(doc, "Organization: ", vCard.getOrganization());
		
		addVCardDetail(doc, "Organization Unit: ", vCard.getOrganizationUnit());
		
		addVCardDetail(doc, "Title: ", vCard.getField("TITLE"));
		
		addVCardDetail(doc, "Role: ", vCard.getField("ROLE"));
		
		addVCardDetail(doc, "Work Street: ", vCard.getAddressFieldWork("STREET"));
		
		addVCardDetail(doc, "Work Street 2: ", vCard.getAddressFieldWork("EXTADR"));
		
		addVCardDetail(doc, "Work City: ", vCard.getAddressFieldWork("LOCALITY"));
		
		addVCardDetail(doc, "Work State: ", vCard.getAddressFieldWork("REGION"));
		
		addVCardDetail(doc, "Work Country: ", vCard.getAddressFieldWork("CTRY"));
		
		addVCardDetail(doc, "Work Zipcode: ", vCard.getAddressFieldWork("PCODE"));
		
		addVCardDetail(doc, "Work Phone: ", vCard.getPhoneWork("VOICE"));
		
		addVCardDetail(doc, "Work Cell: ", vCard.getPhoneWork("CELL"));
		
		addVCardDetail(doc, "Work Email: ", vCard.getEmailWork());
	}
	
	protected void addVCardDetail(StyledDocument doc, String header, String detail)
	{
		final SimpleAttributeSet headerAttr = new SimpleAttributeSet();
		StyleConstants.setBold(headerAttr, true);
		
		final SimpleAttributeSet detailAttr = new SimpleAttributeSet();
		StyleConstants.setItalic(detailAttr, true);
		
		try
		{
			doc.insertString(doc.getLength(), header, headerAttr);
			if (!StringUtils.isEmpty(vCard.getFirstName()))
			{
				doc.insertString(doc.getLength(), detail, detailAttr);
			}
			doc.insertString(doc.getLength(), "\r\n\r\n", null);
		}
		catch (Exception e)
		{
			
		}
	}
}
