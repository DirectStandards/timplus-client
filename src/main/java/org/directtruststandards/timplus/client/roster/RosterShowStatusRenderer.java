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
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

public class RosterShowStatusRenderer implements ListCellRenderer<RosterStatusShow>
{

	static ImageIcon ONLINE;
	
	static ImageIcon AWAY;
	
	static ImageIcon OFFLINE;
	
	static ImageIcon DND;
	
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
		}
		catch (IOException e)
		{
			ONLINE = null;
			AWAY = null;
			OFFLINE = null;
			DND = null;
		}
	}
	
	public RosterShowStatusRenderer()
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
	public Component getListCellRendererComponent(JList<? extends RosterStatusShow> list, RosterStatusShow value,
			int index, boolean isSelected, boolean cellHasFocus)
	{
		if (value == null)
		{
			presLabel.setIcon(null);
			displayLabel.setText("");
		}
		else
		{			
			switch(value)
			{
				case AVAILABLE:
					presLabel.setIcon(ONLINE);
					break;
				case AWAY:
					presLabel.setIcon(AWAY);
					break;	
				case DND:
					presLabel.setIcon(DND);
					break;							
				case PRIVATE:
					presLabel.setIcon(OFFLINE);
					break;					
			}
			
			displayLabel.setText(value.getDisplay());
		}
		
		return renderPanel;
	}
}
