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
	
	public IncomingFileTransferDialog(String transFile, IncomingFileTransport inTransport)
	{
		super((Frame)null, "Incoming File Transfer: " + transFile);
		
		this.complete = new AtomicBoolean(false);
		
		this.aborted = new AtomicBoolean(false);
		
		this.transFile = transFile;
		
		this.inTransport = inTransport;
		
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
		//bytesLabel = new JLabel("0 of " + fileSize);
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
		//outTransport.cancelFileTransfer();
		
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
	
}
