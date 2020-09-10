package org.directtruststandards.timplus.client.roster;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.directtruststandards.timplus.client.roster.RosterItem.Presense;
import org.directtruststandards.timplus.client.roster.RosterItem.Subscription;
import org.jxmpp.jid.Jid;

public class RosterTableModel extends AbstractTableModel
{
	private static final long serialVersionUID = -6387854970042572629L;

	protected List<RosterItem> rosterData;
 	
	public RosterTableModel(List<RosterItem> rosterData)
	{

		this.rosterData = rosterData;
	}
	
	@Override
    public int getRowCount() 
    {
        return rosterData.size();
    }
	
	@Override
    public int getColumnCount() 
    {
        return 3;
    }
    
	@Override
    public Object getValueAt(int row, int colum)
    {
		final RosterItem item = rosterData.get(row);
		
		switch (colum)
		{
			case 0:
			{
				return item.getPresence();
			}
			case 1:
			{
				return item.getRosterJID();
			}
			case 2:
			{
				return item.getSub();
			}
			default:
			{
				return item.getRosterJID();
			}
		}
    }
	
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
		final RosterItem item = rosterData.get(rowIndex);
		
		switch (columnIndex)
		{
			case 0:
				item.setPresence((Presense)aValue);
				break;
			case 1:
				item.setRosterJID((Jid)aValue);
				break;
			case 2:
				item.setSub((Subscription)aValue);
				break;
		}
		
		this.fireTableCellUpdated(rowIndex, columnIndex);
    }
	
	public void addRow(RosterItem item)
    {
		rosterData.add(item);
		
		this.fireTableRowsInserted(rosterData.size() - 1, rosterData.size() - 1);
    }
	
	public void removeRow(int idx)
    {
		rosterData.remove(idx);
		
		this.fireTableRowsDeleted(idx, idx);
    }	
}
