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
import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jxmpp.jid.Jid;

public class OutgoingFileTransferDialog extends JDialog
{
	private static final long serialVersionUID = 6886742055697461812L;

	protected final File transFile;
	
	protected final Jid recipient;
	
	protected JLabel statusLabel;
	
	protected JProgressBar progressBar;
	
	protected JLabel bytesLabel;
	
	protected long fileSize;
	
	protected final OutgoingFileTransport outTransport;
	
	protected AtomicBoolean complete;
	
	protected AtomicBoolean aborted;
	
	protected JButton cancel;
	
	protected JButton close;
	
	public OutgoingFileTransferDialog(File transFile, Jid recipient, AbstractXMPPConnection con)
	{
		super((Frame)null, "File Transfer: " + transFile.getName());
		
		this.transFile = transFile;
		
		this.recipient = recipient;
		
		this.complete = new AtomicBoolean(false);
		
		this.aborted = new AtomicBoolean(false);
		
		try
		{
			final BasicFileAttributes attr = Files.readAttributes(transFile.toPath(), BasicFileAttributes.class);
	    
			this.fileSize = attr.size();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		outTransport = new OutgoingFileTransport(con);
		
		setSize(300, 150);
		
		this.setResizable(false);
		
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		final Point pt = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
		
		this.setLocation(pt.x - 200, pt.y - (50));	
		
		initUI();
	}
	
	protected void initUI()
	{
		getContentPane().setLayout(new BorderLayout(10, 10));
		
		/*
		 * Status label
		 */
		final JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		statusLabel = new JLabel("Negotiating transfer");
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
		bytesLabel = new JLabel("0 of " + fileSize);
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
	
	public void sendFile()
	{
		
		outTransport.addFileTransferDataListener(new SendFileDataListener(fileSize));
		outTransport.addFileTransferStatusListener(new SendFileStatusListener());
		try
		{
			outTransport.sendFile(recipient.asEntityFullJidIfPossible(), transFile, "");
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this,"Unknown error creating file transfer request.", 
		 		    "File Transfer", JOptionPane.ERROR_MESSAGE);
			
			cancelTransport();
		}
	}
	
	protected void cancelTransport()
	{
		// don't abort twice if already done
		if (aborted.get())
			return;
		
		aborted.set(true);
		outTransport.cancelFileTransfer();
		
        java.awt.EventQueue.invokeLater(new Runnable() 
        {
            @Override
            public void run() 
            {            	
            	statusLabel.setText("Aborted");
            }
        });
		
		setVisible(false);
		dispose();
		
	}
	
	protected class SendFileDataListener implements FileTransferDataListener
	{
		
		public SendFileDataListener(long totalBytesToSend) 
		{
		}

		@Override
		public int dataTransfered(long transferedSoFar)
		{
			double ratio = (double)transferedSoFar/(double)fileSize;
			
			int percent =  (int) (ratio * 100);

			if (transferedSoFar == fileSize)
			{
				complete.set(true);
				close.setVisible(true);
				cancel.setVisible(false);
			}
				
	        java.awt.EventQueue.invokeLater(new Runnable() 
	        {
	            @Override
	            public void run() 
	            {
	            	progressBar.setValue(percent);
	            	bytesLabel.setText(transferedSoFar + " of " + fileSize);
	            	
	            	if (complete.get())
	            		statusLabel.setText("Transfer Complete");
	            		
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
				case SESSION_TERIMINATE:
					statusStr = "Session terminated";
					cancel.setVisible(false);
					close.setVisible(true);
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
					statusStr = "Recipient proxy server error";
					break;		
				case SESSION_UNKNOWN:
					statusStr = "Unknown error";
					break;	
				case INITIATOR_CANDIDATE_USED:
					statusStr = "Sender selected proxy server";
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
