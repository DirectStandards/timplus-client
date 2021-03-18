package org.directtruststandards.timplus.client.roster;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.apache.commons.lang3.StringUtils;


public class RosterItemRenderer implements TableCellRenderer
{
	static ImageIcon ONLINE;
	
	static ImageIcon AWAY;
	
	static ImageIcon OFFLINE;
	
	static ImageIcon DND;
	
	static ImageIcon UNAUTHORIZED;
	
	static ImageIcon BLOCKED;	
	
	protected JLabel displayLabel;
	
	protected JLabel presLabel;
	
	protected JPanel renderPanel;
	
	static
	{
		try
		{
			URL imageURL = RosterItemRenderer.class.getResource("/images/connected.png");
			BufferedImage image = ImageIO.read(imageURL);
			ONLINE = new ImageIcon(new ImageIcon(image).getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
			
			imageURL = RosterItemRenderer.class.getResource("/images/away.png");
			image = ImageIO.read(imageURL);
			AWAY = new ImageIcon(new ImageIcon(image).getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
			
			imageURL = RosterItemRenderer.class.getResource("/images/offline.png");
			image = ImageIO.read(imageURL);
			OFFLINE = new ImageIcon(new ImageIcon(image).getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
			
			imageURL = RosterItemRenderer.class.getResource("/images/dnd.png");
			image = ImageIO.read(imageURL);
			DND = new ImageIcon(new ImageIcon(image).getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
			
			imageURL = RosterItemRenderer.class.getResource("/images/unauthorized.png");
			image = ImageIO.read(imageURL);
			UNAUTHORIZED = new ImageIcon(new ImageIcon(image).getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
			
			imageURL = RosterItemRenderer.class.getResource("/images/blocked.png");
			image = ImageIO.read(imageURL);
			BLOCKED = new ImageIcon(new ImageIcon(image).getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
		}
		catch (IOException e)
		{
			ONLINE = null;
			AWAY = null;
			OFFLINE = null;
			UNAUTHORIZED = null;
			DND = null;
			BLOCKED = null;
		}
	}
	
	public RosterItemRenderer()
	{
		presLabel = new JLabel();
		displayLabel = new JLabel();
		
		final Font defaultLabelFont = displayLabel.getFont();
		
		final Font displayFont = new Font("", Font.ITALIC | Font.PLAIN, defaultLabelFont.getSize());
		displayLabel.setFont(displayFont);
		
		
		renderPanel = new JPanel(new BorderLayout());
		
		renderPanel.add(presLabel, BorderLayout.WEST);
		renderPanel.add(displayLabel, BorderLayout.CENTER);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column)
	{
		if (value == null)
		{
			presLabel.setIcon(null);
			displayLabel.setText("");
		}
		else
		{
			final RosterItem item = (RosterItem)value;
			
			switch(item.getPresence())
			{
				case AVAILABLE:
					presLabel.setIcon(ONLINE);
					break;
				case UNAVAILABLE:
					presLabel.setIcon(OFFLINE);
					break;
				case AWAY:
					presLabel.setIcon(AWAY);
					break;	
				case DND:
					presLabel.setIcon(DND);
					break;							
				case NOT_AUTHORIZED:
					presLabel.setIcon(UNAUTHORIZED);
					break;	
				case BLOCKED:
					presLabel.setIcon(BLOCKED);
					break;						
				default:
					presLabel.setIcon(OFFLINE);
			}
			
			if (!StringUtils.isEmpty(item.getAlias()))
				displayLabel.setText(item.getAlias());
			else
				displayLabel.setText(item.getRosterJID().asBareJid().toString());
		}
		
		return renderPanel;
	}
}
