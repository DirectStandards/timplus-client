package org.directtruststandards.timplus.client.filetransport;

import java.awt.EventQueue;
import java.awt.Frame;
import java.io.File;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.directtruststandards.timplus.client.packets.CredRequest;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.StanzaCollector;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.EmptyResultIQ;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smackx.bytestreams.BytestreamListener;
import org.jivesoftware.smackx.bytestreams.BytestreamRequest;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamSession;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream.StreamHost;
import org.jivesoftware.smackx.filetransfer.AuthSocks5Client;
import org.jivesoftware.smackx.jingle.JingleHandler;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSessionHandler;
import org.jivesoftware.smackx.jingle.JingleUtil;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportCandidate;
import org.jivesoftware.smackx.jingle.element.JingleReason;
import org.jivesoftware.smackx.jingle.element.JingleContent.Creator;
import org.jivesoftware.smackx.jingle.transports.jingle_ibb.element.JingleIBBTransport;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements.JingleS5BTransport;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements.JingleS5BTransportCandidate;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements.JingleS5BTransportInfo;

/**
 * This class handles incoming file transfer requests.
 * @author Greg Meyer
 * @since 1.0
 *
 */
public class IncomingFileTransport implements JingleHandler
{	
	protected AbstractXMPPConnection con;
	
	protected final JingleManager jingleManager;
	
	protected static ExecutorService acceptFileExecutor;
	
	protected static ExecutorService transferFileExecutor;
	
	protected final Map<String, FileTransferDataListener> fileTransferDataListeners;
	
	protected final Map<String, FileTransferStatusListener> fileTransferStatusListeners;
	
	protected final Frame parentFrame;
	
	protected JingleUtil util;
	
	static
	{
		acceptFileExecutor = Executors.newSingleThreadExecutor();	
		
		transferFileExecutor = Executors.newSingleThreadExecutor();	
	}
	
	public IncomingFileTransport(AbstractXMPPConnection con, Frame parentFrame)
	{
		this.con = con;
		
		this.parentFrame = parentFrame;
		
		this.jingleManager = JingleManager.getInstanceFor(con);
		
		this.util = new JingleUtil(con);
		
		this.fileTransferDataListeners = new HashMap<>();
		
		this.fileTransferStatusListeners = new HashMap<>();
	}
	
	public void addFileTransferDataListener(String sessionId, FileTransferDataListener listener)
	{
		fileTransferDataListeners.put(sessionId, listener);
	}
	
	public void removeFileTransferDataListener(String sessionId)
	{
		fileTransferDataListeners.remove(sessionId);
	}
	
	public void addFileTransferStatusistener(String sessionId, FileTransferStatusListener listener)
	{
		fileTransferStatusListeners.put(sessionId, listener);
	}
	
	public void removeFileTransferStatusistener(String sessionId)
	{
		fileTransferStatusListeners.remove(sessionId);
	}
	
	/**
	 * Handles the session init request.
	 */
	@Override
	public IQ handleJingleRequest(Jingle jingle)
	{
		// check if this is a session initiate
		if (jingle.getAction() == JingleAction.session_initiate)
		{	
			// by spec, we return an empty IQ result
			final EmptyResultIQ result = new EmptyResultIQ();
			result.setFrom(jingle.getTo());
			result.setTo(jingle.getFrom());
			result.setType(Type.result);
			result.setStanzaId(jingle.getStanzaId());
			
			acceptFileExecutor.execute(new Runnable() 
			{
				public void run()
				{
					final FileTransferSession ftSession = new FileTransferSession();
					ftSession.con = con;
					TargetSessionManager sessionManager = new TargetSessionManager(ftSession);
					
					FileTransferStatusListener statusListener = null;
					
					try
					{
						
						final StandardExtensionElement fileTransfer = (StandardExtensionElement)jingle.getContents().get(0).getDescription().getJingleContentDescriptionChildren().get(0);
						
						for (StandardExtensionElement element : fileTransfer.getElements())
						{
							if (element.getElementName().contains("name"))
							{
								ftSession.fileName = element.getText();
							}
							else if (element.getElementName().contains("size"))
							{
								ftSession.fileSize = Long.parseLong(element.getText());
							}
						}
						
						System.out.println("Contact " + jingle.getFrom().asBareJid().toString() + " requesting to send file.");
		    			int selection = JOptionPane.showConfirmDialog(parentFrame, jingle.getFrom().asBareJid().toString() + " is requesting to send \r\nthe file " 
		    					+ ftSession.fileName + ".  Do want to accept this file?",
		    					"File Transfer", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
						
		    			if (selection == JOptionPane.NO_OPTION)
		    			{
		    				final Jingle sessionTerminate = util.createSessionTerminateCancel(ftSession.initiatorJID, ftSession.streamId);

		    				con.sendIqRequestAsync(sessionTerminate);
		    				return;
		    			}
		    			
		    			/*
		    			 * Select the location to save the file
		    			 */
		    			final AtomicInteger appr = new AtomicInteger(0);
		    			EventQueue.invokeLater(new Runnable() 
		    			{
		    				public void run() 
		    				{
				    			JFileChooser chooser = new JFileChooser();
				    			chooser.setSelectedFile(new File(ftSession.fileName));
				    			int approve = chooser.showSaveDialog(parentFrame);
				    			
				    			if (approve == JFileChooser.APPROVE_OPTION)
				    				ftSession.fileName = chooser.getSelectedFile().getAbsolutePath();
				    			
				    			appr.set(approve);
				    			synchronized(appr)
				    			{
				    				appr.notify();
				    			}
		    				}
		    			});

		    			synchronized(appr)
		    			{
		    				appr.wait();
		    			}
		    			
		    			if (appr.get() != JFileChooser.APPROVE_OPTION)
		    			{
		    				final Jingle sessionTerminate = util.createSessionTerminateCancel(ftSession.initiatorJID, ftSession.streamId);

		    				con.sendIqRequestAsync(sessionTerminate);
		    				return;
		    			}
		    			
		    			
		    			
						System.out.println("Accepted the incoming file transfer session to location " + ftSession.fileName);	
						
						ftSession.streamId = jingle.getSid();
						final File file = new File(ftSession.fileName);
						
						IncomingFileTransferDialog statusDialog = new IncomingFileTransferDialog(file.getName(), IncomingFileTransport.this, ftSession);
						statusDialog.setVisible(true);
						
						final Jingle sessionAccept = util.createSessionAccept(jingle.getFrom().asFullJidIfPossible(), jingle.getSid(), JingleContent.Creator.initiator, jingle.getContents().get(0).getName(), 
								JingleContent.Senders.initiator, jingle.getContents().get(0).getDescription(), jingle.getContents().get(0).getTransport());
						
						jingleManager.registerJingleSessionHandler(jingle.getFrom().asFullJidIfPossible(), jingle.getSid(), sessionManager);
						
						final StanzaCollector fileOfferCollector = con.createStanzaCollectorAndSend(sessionAccept);
						
						statusListener = fileTransferStatusListeners.get(jingle.getSid());
						
						// make sure we get an ack
						IQ result = fileOfferCollector.nextResultOrThrow();
						
						if (result != null && result instanceof EmptyResultIQ)
						{
							System.out.println("Session accept was acknowleged.");	
							if (statusListener != null)
								statusListener.statusUpdated(FileTransferState.SESSION_ACCEPT_ACK);
							
							// Set class member variables and kick off the session
							// management thread
							ftSession.fileTransferTargetJID = jingle.getTo().asEntityFullJidIfPossible();
							ftSession.initiatorJID = jingle.getFrom().asEntityFullJidIfPossible();
						
							
							final JingleS5BTransport transport = (JingleS5BTransport)jingle.getContents().get(0).getTransport();
							
							sessionManager.setSocks5Info(transport.getCandidates(), transport.getDestinationAddress());
							
							acceptFileExecutor.execute(sessionManager);
						}
						else
						{
							System.out.println("The initiator did not acknowledge the session accept. Aborting session");	
							final Jingle sessionTerminate = util.createSessionTerminateCancel(ftSession.initiatorJID, ftSession.streamId);

							con.sendIqRequestAsync(sessionTerminate);
							
							if (statusListener != null)
								statusListener.statusUpdated(FileTransferState.SESSION_TERIMINATE);
							jingleManager.unregisterJingleSessionHandler(jingle.getFrom().asFullJidIfPossible(), jingle.getSid(), sessionManager);
						}
					}
					catch (Exception e)
					{
						System.out.println("Error accepting the session.");
						
						jingleManager.unregisterJingleSessionHandler(jingle.getFrom().asFullJidIfPossible(), jingle.getSid(), sessionManager);
						
						if (statusListener != null)
							statusListener.statusUpdated(FileTransferState.SESSION_TERIMINATE);
						
						final Jingle sessionTerm = util.createSessionTerminate(jingle.getFrom().asFullJidIfPossible(), jingle.getSid(), JingleReason.Timeout);
						con.sendIqRequestAsync(sessionTerm);
						
						jingleManager.unregisterJingleSessionHandler(jingle.getFrom().asFullJidIfPossible(), jingle.getSid(), sessionManager);
					}
				}
			});
			
			return result;
		}
		return null;
		
	}

	public void cancelFileTransfer(FileTransferSession ftSession)
	{
		if (ftSession == null)
			return; 
		
		final Jingle sessionTerminate = util.createSessionTerminateFailedTransport(ftSession.initiatorJID, ftSession.streamId);

		con.sendIqRequestAsync(sessionTerminate);
		
		jingleManager.unregisterJingleSessionHandler(ftSession.fileTransferTargetJID, ftSession.streamId, ftSession.sessionHandler);
		
		if (ftSession.sessionHandler != null && ftSession.sessionHandler instanceof TargetSessionManager)
		{
			final TargetSessionManager handler = (TargetSessionManager)ftSession.sessionHandler;
			handler.ibbManager.removeIncomingBytestreamListener(ftSession.initiatorJID);
		}
	}
	
	protected class TargetSessionManager implements Runnable, JingleSessionHandler, BytestreamListener
	{
		protected InBandBytestreamManager ibbManager;
		
		protected List<JingleContentTransportCandidate> candidates;
		
		protected String dstAddressHashString;
		
		protected final FileTransferSession ftSession;
		
		public TargetSessionManager(FileTransferSession ftSession)
		{
			this.ftSession = ftSession;
			
			ftSession.sessionHandler = this;
			
			this.ibbManager = InBandBytestreamManager.getByteStreamManager(ftSession.con);
		}
		
		public void setSocks5Info(List<JingleContentTransportCandidate> candidates, String dstAddressHashString)
		{
			this.candidates = candidates;
			this.dstAddressHashString = dstAddressHashString;
		}
		
		
		@Override
		public IQ handleJingleSessionRequest(Jingle jingle)
		{
			final FileTransferStatusListener statusListener = fileTransferStatusListeners.get(jingle.getSid());
			
			if (jingle.getAction() == JingleAction.transport_info)
			{
				switch (TransportInfoType.getTransportInfoType(jingle.getContents().get(0)))
				{
					case CANDIDATE_USED:
					{
						final JingleS5BTransportInfo.CandidateUsed candidateUsed = 
								(JingleS5BTransportInfo.CandidateUsed)jingle.getContents().get(0).getTransport().getInfo();
						
						if (statusListener != null)
							statusListener.statusUpdated(FileTransferState.INITIATOR_CANDIDATE_USED);
						
						System.out.println("The initiator selected a proxy server with candidate ID " + candidateUsed.getCandidateId());
						
						// by spec, we return an empty IQ result
						final EmptyResultIQ result = new EmptyResultIQ();
						result.setFrom(jingle.getTo());
						result.setTo(jingle.getFrom());
						result.setType(Type.result);
						result.setStanzaId(jingle.getStanzaId());
						
						return result;
					}
					case CANDIDATE_ACTIVATED:
					{
						// start reading
						System.out.println("The initiator acitvated the proxy server.  Startup file transfer receive operation.");
					
						if (statusListener != null)
							statusListener.statusUpdated(FileTransferState.TRANSPORT_ACTIVATED);
						
						FileTransferDataListener listener = fileTransferDataListeners.get(ftSession.streamId);
						
						transferFileExecutor.execute(new Socks5ReadManager(ftSession, listener));
						
						break;
					}
					case CANDIATE_ERROR:
					{
						System.out.println("The initiator reported a candidate error.  Aborting SOCKS5 transfer.");
						
						if (statusListener != null)
							statusListener.statusUpdated(FileTransferState.INITIATOR_CANDIDATE_USED_ERROR);
						
						break;
					}	
					case PROXY_ERROR:
					{
						System.out.println("The initiator reported a proxy error.  Aborting SOCKS5 transfer.");
						
						if (statusListener != null)
							statusListener.statusUpdated(FileTransferState.TRANSPORT_PROXY_ERROR);
						
						break;
					}	
					case UNKNOWN:
						 default:
					{
						if (statusListener != null)
							statusListener.statusUpdated(FileTransferState.SESSION_UNKNOWN);
						
						break;
					}
				}
			}
			else if (jingle.getAction() == JingleAction.transport_replace)
			{
				System.out.println("The initiator has requested to replace the transport with In-Band bytestreams.");
				System.out.println("Acknowledging the transport-replace request.");
				
				if (statusListener != null)
					statusListener.statusUpdated(FileTransferState.TRANSPORT_REPLACE);
				
				// by spec, we return an empty IQ result
				final EmptyResultIQ result = new EmptyResultIQ();
				result.setFrom(jingle.getTo());
				result.setTo(jingle.getFrom());
				result.setType(Type.result);
				result.setStanzaId(jingle.getStanzaId());
				
				executeIBBFallBack();
				
				return result;	
			}
			else if (jingle.getAction() == JingleAction.session_terminate)
			{
				System.out.println("The initiator has terminated the session.");
				
				if (statusListener != null)
					statusListener.statusUpdated(FileTransferState.SESSION_TERIMINATE);
				
				ibbManager.addIncomingBytestreamListener(TargetSessionManager.this, ftSession.initiatorJID);
				jingleManager.unregisterJingleSessionHandler(ftSession.initiatorJID, ftSession.streamId, this);
				
				// by spec, we return an empty IQ result
				final EmptyResultIQ result = new EmptyResultIQ();
				result.setFrom(jingle.getTo());
				result.setTo(jingle.getFrom());
				result.setType(Type.result);
				result.setStanzaId(jingle.getStanzaId());
				
				return result;			
			}
			return null;
		}		
		
		@Override
		public void run()
		{
			final List<JingleContentTransportCandidate> sortedCandidates = new ArrayList<>(candidates);
			
			// sort by priority
			Collections.sort(sortedCandidates, new Comparator<JingleContentTransportCandidate>() 
			{
				  @Override
				  public int compare(JingleContentTransportCandidate c1, JingleContentTransportCandidate c2) 
				  {
					  if (c1 instanceof JingleS5BTransportCandidate && c2 instanceof JingleS5BTransportCandidate)
						  return ((JingleS5BTransportCandidate)c1).getPriority() > ((JingleS5BTransportCandidate)c1).getPriority() ? 1 : 0;
						  
					  return 0;
				  }
			});
			
			for (JingleContentTransportCandidate candidate : sortedCandidates)
			{
				final JingleS5BTransportCandidate s5can = (JingleS5BTransportCandidate)candidate;
				
				final StreamHost host = s5can.getStreamHost();
				
				// connect to the stream host
				final AuthSocks5Client sock5Client = new AuthSocks5Client(host, dstAddressHashString, con, ftSession.streamId, ftSession.fileTransferTargetJID);
				
				try
				{
					// get a one time user name password
					final CredRequest request = new CredRequest();
					request.setTo(host.getJID());
					request.setType(IQ.Type.get);

					final CredRequest creds = (CredRequest)con.createStanzaCollectorAndSend(request).nextResultOrThrow();
					
					// creation of the socket connects and authenticates
					final Socket socket = sock5Client.getSocket(10000, creds.getSubject(), creds.getSecret(), creds.getProxyServerCA());
					
					ftSession.selectedCandidateId = s5can.getCandidateId();
					
					// send the candidate used message
					final Jingle cadidateUsed = createTransportInfoMessage(new JingleS5BTransportInfo.CandidateUsed(ftSession.selectedCandidateId), ftSession);
					
					// make sure our peer acks the cadidate used message
					final IQ result = con.createStanzaCollectorAndSend(cadidateUsed).nextResultOrThrow();
					
					if (result != null && result instanceof EmptyResultIQ)
					{
						// now sit and wait for the sender to send a candidate used message
						// and activation message
						ftSession.proxySocket = socket;
						
					}
					
					break;
				}
				catch (Exception e)
				{
					System.out.println("Failed to select and use proxy cadidate.  Will try next candidate.");
				}
				
				if (ftSession.selectedCandidateId == null)
				{
					System.out.println("Could not connect to any proxy candidates.  Aborting Socks 5 transport");
					
					final Jingle candidateError = createTransportInfoMessage(JingleS5BTransportInfo.CandidateError.INSTANCE, ftSession);
					con.sendIqRequestAsync(candidateError);
				}
			}
		}
		
		@Override
		public void incomingBytestreamRequest(BytestreamRequest request)
		{
			try
			{
				System.out.println("Received In-band open command.  Accepting open command and kicking of file transport thread.");
				
				final InBandBytestreamSession session = (InBandBytestreamSession)request.accept();
				
				session.setCloseBothStreamsEnabled(true);
				
				ftSession.ibbInputStream = session.getInputStream();
				
				transferFileExecutor.execute(new IBBReadManager(ftSession));
				
			}
			catch (Exception e)
			{
				System.out.println("Error processing In-band file transfer open request.  Terminating session.");
				
				final Jingle sessionTerminate = util.createSessionTerminateFailedTransport(ftSession.initiatorJID, ftSession.streamId);

				con.sendIqRequestAsync(sessionTerminate);
				
				jingleManager.unregisterJingleSessionHandler(ftSession.fileTransferTargetJID, ftSession.streamId, TargetSessionManager.this);
				ibbManager.removeIncomingBytestreamListener(ftSession.initiatorJID);
			}
		}
		
		protected void executeIBBFallBack()
		{
			final FileTransferStatusListener statusListener = fileTransferStatusListeners.get(ftSession.streamId);
			
			acceptFileExecutor.execute(new Runnable()
			{
				@Override
				public void run()
				{
					ibbManager.addIncomingBytestreamListener(TargetSessionManager.this, ftSession.initiatorJID);
					
					System.out.println("Accepting the transport change.");
					
					final Jingle transportAccept = util.createTransportAccept(ftSession.initiatorJID, ftSession.initiatorJID, ftSession.streamId, JingleContent.Creator.initiator, "file-offer",
							new JingleIBBTransport((short)4096, ftSession.streamId));
					
					try
					{
						IQ result = con.createStanzaCollectorAndSend(transportAccept).nextResultOrThrow();
						if (result != null && result instanceof EmptyResultIQ)
						{						
							if (statusListener != null)
								statusListener.statusUpdated(FileTransferState.TRANSPORT_ACTIVATED);
							
							System.out.println("Received acknowledgement of transport accept.  Waiting for a In-Band file stream open message");
						}
						
					}
					catch (NoResponseException | XMPPErrorException |
			                InterruptedException | NotConnectedException e)
					{
						System.out.println("Did not get an acknowlegment for the transport accept.  Terminating session.");
						
						final Jingle sessionTerminate = util.createSessionTerminateFailedTransport(ftSession.initiatorJID, ftSession.streamId);

						con.sendIqRequestAsync(sessionTerminate);
						
						if (statusListener != null)
							statusListener.statusUpdated(FileTransferState.SESSION_TERIMINATE);
						
						jingleManager.unregisterJingleSessionHandler(ftSession.fileTransferTargetJID, ftSession.streamId, TargetSessionManager.this);
						ibbManager.removeIncomingBytestreamListener(ftSession.initiatorJID);
						
					}
				}
			});
		}
	}
	
	protected Jingle createTransportInfoMessage(JingleS5BTransportInfo info, FileTransferSession session)
	{
		Jingle.Builder jb = Jingle.getBuilder();
		jb.setAction(JingleAction.transport_info)
		  .setInitiator(session.initiatorJID)
		  .setSessionId(session.streamId);
		
		final JingleS5BTransport transport = 
				JingleS5BTransport.getBuilder().setStreamId(session.streamId).setTransportInfo(info).build();
		
		JingleContent.Builder cb = JingleContent.getBuilder();
		cb.setCreator(Creator.initiator)
		  .setName("file-offer")
		  .setTransport(transport);
		
		final Jingle jingle = jb.addJingleContent(cb.build()).build();
		jingle.setFrom(con.getUser());
		jingle.setTo(session.initiatorJID);
		
		return jingle;
	}
}
