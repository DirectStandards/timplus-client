package org.directtruststandards.timplus.client.roster;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class GroupChatItemRenderer implements TableCellRenderer
{
	protected JLabel displayLabel;
	
	protected JPanel renderPanel;
	
	public GroupChatItemRenderer()
	{
		displayLabel = new JLabel();
		
		final Font defaultLabelFont = displayLabel.getFont();
		
		final Font displayFont = new Font("", Font.ITALIC | Font.PLAIN, defaultLabelFont.getSize());
		displayLabel.setFont(displayFont);
		
		
		renderPanel = new JPanel(new BorderLayout());

		renderPanel.add(displayLabel, BorderLayout.CENTER);
	}
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column)
	{
		if (value == null)
		{
			displayLabel.setText("");
		}
		else
		{
			final GroupChatItem item = (GroupChatItem)value;
			
			
			displayLabel.setText(item.getRoom().getRoom().toString());
		}
		
		return renderPanel;
	}
}
