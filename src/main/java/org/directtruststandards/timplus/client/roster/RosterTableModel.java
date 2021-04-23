package org.directtruststandards.timplus.client.roster;

import java.util.List;

import javax.swing.table.AbstractTableModel;

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
        return 1;
    }
    
	@Override
    public Object getValueAt(int row, int colum)
    {
		return rosterData.get(row);
    }
	
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
		final RosterItem item = rosterData.get(rowIndex);
		
		final RosterItem upItem = (RosterItem)aValue;
		
		item.setPresence(upItem.getPresence());
		item.setRosterJID(upItem.getRosterJID());
		item.setPresenceStatusText(upItem.getPresenceStatusText());
		item.setSub(upItem.getSub());
		
		this.fireTableCellUpdated(rowIndex, columnIndex);
    }
	
	@Override
    public Class<?> getColumnClass(int columnIndex) 
    {
		return RosterItem.class;
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
