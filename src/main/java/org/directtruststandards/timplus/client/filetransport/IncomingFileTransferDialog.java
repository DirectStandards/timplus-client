package org.directtruststandards.timplus.client.filetransport;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;


public class IncomingFileTransferDialog extends JDialog
{
	private static final long serialVersionUID = 7292870839941655324L;

	protected AtomicBoolean complete;
	
	protected AtomicBoolean aborted;
	
	protected JButton cancel;
	
	protected JButton close;
	
	protected JLabel statusLabel;
	
	protected JProgressBar progressBar;
	
	protected JLabel bytesLabel;
	
	protected final String transFile;
	
	protected final IncomingFileTransport inTransport;
	
	protected final FileTransferSession ftSession;
	
	public IncomingFileTransferDialog(String transFile, IncomingFileTransport inTransport, FileTransferSession ftSession)
	{
		super((Frame)null, "Incoming File Transfer: " + transFile);
		
		this.complete = new AtomicBoolean(false);
		
		this.aborted = new AtomicBoolean(false);
		
		this.transFile = transFile;
		
		this.inTransport = inTransport;
		
		this.ftSession = ftSession;
		
		setSize(300, 150);
		
		this.setResizable(false);
		
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		final Point pt = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
		
		this.setLocation(pt.x - 200, pt.y - (50));	
		
		inTransport.addFileTransferDataListener(ftSession.streamId, new ReceiveFileDataListener());
		
		inTransport.addFileTransferStatusistener(ftSession.streamId, new SendFileStatusListener());
		
		initUI();
	}
	
	protected void initUI()
	{
		getContentPane().setLayout(new BorderLayout(10, 10));
		
		/*
		 * Status label
		 */
		final JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		statusLabel = new JLabel("Session accpeted");
		statusPanel.add(statusLabel);
		
		getContentPane().add(statusPanel, BorderLayout.NORTH);
		
		/*
		 * Progress bar
		 */
		final JPanel progressPanel = new JPanel(new FlowLayout());
		progressBar = new JProgressBar(0, 100); 
		progressBar.setValue(0);
		progressBar.setStringPainted(true);
		progressBar.setPreferredSize(new Dimension(280, 30));
		progressPanel.add(progressBar);
		
		
		getContentPane().add(progressPanel, BorderLayout.CENTER);
		
		/*
		 * Transfer and cancel
		 */
		final JPanel transPanel = new JPanel(new BorderLayout());
		
		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				cancelTransport();
			}
		});
		buttonPanel.add(cancel);
		
		close = new JButton("Close");
		close.setVisible(false);
		close.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				setVisible(false);
				dispose();
			}
		});
		
		buttonPanel.add(close);
		
		transPanel.add(buttonPanel, BorderLayout.EAST);
		
		final JPanel transferedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		bytesLabel = new JLabel("0 of " + ftSession.fileSize);
		transferedPanel.add(bytesLabel);
		transPanel.add(transferedPanel, BorderLayout.WEST);
		
		getContentPane().add(transPanel, BorderLayout.SOUTH);
		
		/*
		 * Window close action
		 */
		this.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosed(WindowEvent e)
			{
				if (!complete.get())
					cancelTransport();
			}
		});
	}
	
	protected void cancelTransport()
	{
		// don't abort twice if already done
		if (aborted.get())
			return;
		
		aborted.set(true);
		inTransport.cancelFileTransfer(ftSession);
		
        java.awt.EventQueue.invokeLater(new Runnable() 
        {
            @Override
            public void run() 
            {            	
            	statusLabel.setText("Aborted");
            }
        });
		
        inTransport.removeFileTransferDataListener(ftSession.selectedCandidateId);
        inTransport.removeFileTransferStatusistener(ftSession.selectedCandidateId);
        
		setVisible(false);
		dispose();
		
	}
	
	protected class ReceiveFileDataListener implements FileTransferDataListener
	{
		
		public ReceiveFileDataListener() 
		{
		}

		@Override
		public int dataTransfered(long transferedSoFar)
		{
			double ratio = (double)transferedSoFar/(double)ftSession.fileSize;
			
			int percent =  (int) (ratio * 100);

			if (transferedSoFar == ftSession.fileSize)
			{
				complete.set(true);
				close.setVisible(true);
				cancel.setVisible(false);
        		inTransport.removeFileTransferDataListener(ftSession.selectedCandidateId);
        		inTransport.removeFileTransferStatusistener(ftSession.selectedCandidateId);
			}
				
	        java.awt.EventQueue.invokeLater(new Runnable() 
	        {
	            @Override
	            public void run() 
	            {
	            	progressBar.setValue(percent);
	            	bytesLabel.setText(transferedSoFar + " of " + ftSession.fileSize);
	            	
	            	if (complete.get())
	            	{
	            		statusLabel.setText("Transfer Complete");
	            	}
	            		
	            }
	        });
			
			return (aborted.get()) ? 1 : 0;
		}
	}
	
	protected class SendFileStatusListener implements  FileTransferStatusListener
	{
		public SendFileStatusListener()
		{
			
		}

		@Override
		public void statusUpdated(FileTransferState status)
		{
			if (complete.get() || aborted.get())
				return;
			
			String statusStr = "";
			
			switch (status)
			{
				case SESSION_INITIATE_ACK:
					statusStr = "Waiting for recipient to accept transfer";
					break;
				case SESSION_ACCEPT:
					statusStr = "Session accepted";
					break;
				case SESSION_ACCEPT_ACK:
					statusStr = "Session accept acknowledged";
					break;
				case SESSION_TERIMINATE:
					statusStr = "Session terminated";
					cancel.setVisible(false);
					close.setVisible(true);
					inTransport.removeFileTransferDataListener(ftSession.selectedCandidateId);
					inTransport.removeFileTransferStatusistener(ftSession.selectedCandidateId);
					break;
				case TRANSPORT_ACTIVATED:
					statusStr = "Transfer activated";
					break;
				case RESPONDER_CANDIDATE_USED:
					statusStr = "Recipient selected proxy server";
					break;
				case RESPONDER_CANDIDATE_ERROR:
					statusStr = "Recipient proxy selection error";
					break;
				case TRANSPORT_PROXY_ERROR:
					statusStr = "Sender proxy server error";
					break;		
				case SESSION_UNKNOWN:
					statusStr = "Unknown error";
					break;	
				case INITIATOR_CANDIDATE_USED:
					statusStr = "Sender selected proxy server";
					break;
				case INITIATOR_CANDIDATE_USED_ERROR:
					statusStr = "Sender proxy selection error";
					break;					
				case TRANSPORT_REPLACE:
					statusStr = "Falling back to IBB transport";
					break;	
				default:
					statusStr = "Unknow status";
					break;
				
			}
			
			final String updateStr = statusStr;
	        java.awt.EventQueue.invokeLater(new Runnable() 
	        {
	            @Override
	            public void run() 
	            {
	            	statusLabel.setText(updateStr);		
	            }
	        });
			
		}
	}
}
