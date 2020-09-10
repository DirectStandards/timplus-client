package org.directtruststandards.timplus.client.filetransport;

import java.awt.Frame;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransfer;

public class IncomingFileTransferManager
{
	static protected IncomingFileTransferManager INSTANCE;
	
	protected AbstractXMPPConnection con;
	
	protected JingleManager jingleManager;
	
	protected final Frame parentFrame;
	
	public static synchronized IncomingFileTransferManager getInstance(AbstractXMPPConnection con, Frame parentFrame)
	{
		if (INSTANCE == null)
			INSTANCE = new IncomingFileTransferManager(con, parentFrame);
		
		return INSTANCE;
	}
	
	public IncomingFileTransferManager(AbstractXMPPConnection con, Frame parentFrame)
	{
		this.con = con;
		
		this.parentFrame = parentFrame;
		
		resetJingleManager();
	}
	
	public void setConnection(AbstractXMPPConnection con)
	{
		if (this.con != con)
		{
			this.con = con;
			resetJingleManager();
		}
	}
	
	public void resetJingleManager()
	{
		jingleManager = JingleManager.getInstanceFor(con);
		
		jingleManager.registerDescriptionHandler(JingleFileTransfer.NAMESPACE_V5, new IncomingFileTransport(con, parentFrame));
	}
}
