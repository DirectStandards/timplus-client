package org.directtruststandards.timplus.client.groupchat;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.directtruststandards.timplus.client.roster.GroupChatItem;

@SuppressWarnings("serial")
public class GroupChatMemberTableModel extends AbstractTableModel
{
	protected List<GroupChatMemberItem> participantData;
	
	public GroupChatMemberTableModel(List<GroupChatMemberItem> participantData)
	{

		this.participantData = participantData;
	}
	
	@Override
	public int getRowCount()
	{
		return participantData.size();
	}

	@Override
	public int getColumnCount()
	{
		return 1;
	}

	@Override
    public Object getValueAt(int row, int colum)
    {
		return participantData.get(row);
    }

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
		final GroupChatMemberItem item = participantData.get(rowIndex);
		
		final GroupChatMemberItem upItem = (GroupChatMemberItem)aValue;
		
		item.setMember(upItem.getMember());
		
		this.fireTableCellUpdated(rowIndex, columnIndex);
    }
	
	@Override
    public Class<?> getColumnClass(int columnIndex) 
    {
		return GroupChatItem.class;
    }
	
	public void addRow(GroupChatMemberItem item)
    {
		participantData.add(item);
		
		this.fireTableRowsInserted(participantData.size() - 1, participantData.size() - 1);
    }
	
	public void removeRow(int idx)
    {
		participantData.remove(idx);
		
		this.fireTableRowsDeleted(idx, idx);
    }	
}
