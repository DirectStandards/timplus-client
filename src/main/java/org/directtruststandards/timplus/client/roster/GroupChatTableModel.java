package org.directtruststandards.timplus.client.roster;


import java.util.List;

import javax.swing.table.AbstractTableModel;

public class GroupChatTableModel extends AbstractTableModel
{
	private static final long serialVersionUID = 7136689580112955307L;

	protected List<GroupChatItem> groupChatData;
 	
	public GroupChatTableModel(List<GroupChatItem> groupChatData)
	{

		this.groupChatData = groupChatData;
	}
	
	@Override
	public int getRowCount()
	{
		return groupChatData.size();
	}

	@Override
	public int getColumnCount()
	{
		return 1;
	}

	@Override
    public Object getValueAt(int row, int colum)
    {
		return groupChatData.get(row);
    }

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
		final GroupChatItem item = groupChatData.get(rowIndex);
		
		final GroupChatItem upItem = (GroupChatItem)aValue;
		
		item.setRoom(upItem.getRoom());
		
		this.fireTableCellUpdated(rowIndex, columnIndex);
    }
	
	@Override
    public Class<?> getColumnClass(int columnIndex) 
    {
		return GroupChatItem.class;
    }
	
	public void addRow(GroupChatItem item)
    {
		groupChatData.add(item);
		
		this.fireTableRowsInserted(groupChatData.size() - 1, groupChatData.size() - 1);
    }
	
	public void removeRow(int idx)
    {
		groupChatData.remove(idx);
		
		this.fireTableRowsDeleted(idx, idx);
    }
}
