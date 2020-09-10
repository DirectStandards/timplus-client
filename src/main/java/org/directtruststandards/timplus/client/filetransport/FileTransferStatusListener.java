package org.directtruststandards.timplus.client.filetransport;

public interface FileTransferStatusListener
{
	/**
	 * Call back when the status of the transfer is updated.
	 * @param status The new status of the transfer session.
	 */
	public void statusUpdated(FileTransferState status);
}
