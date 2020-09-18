package org.directtruststandards.timplus.client.connection;

import java.net.InetAddress;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.directtruststandards.timplus.client.config.Configuration;
import org.directtruststandards.timplus.client.config.ConfigurationManager;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.XMPPConnection;

import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration.Builder;


public class ConnectionManager
{
	static protected ConnectionManager INSTANCE;
	
	protected AbstractXMPPConnection con;
	
	protected final Collection<org.directtruststandards.timplus.client.connection.ConnectionListener> connectionListeners;
	
	protected ExecutorService connectionMonitorExecutor;
	
	protected BlockingQueue<ConRequest> connectQueue;
	
	public static synchronized ConnectionManager getInstance()
	{
		if (INSTANCE == null)
			INSTANCE = new ConnectionManager();
		
		return INSTANCE;
	}
	
	private ConnectionManager()
	{
		con = null;
		
		this.connectionListeners = new ArrayList<>();
		
		this.connectionMonitorExecutor = Executors.newSingleThreadExecutor();
		
		this.connectQueue = new LinkedBlockingQueue<>();
		
		connectionMonitorExecutor.execute(new ConnectionOperator());
	}
	
	public void addConnectionListener(org.directtruststandards.timplus.client.connection.ConnectionListener listener)
	{
		if (!connectionListeners.contains(listener))
			connectionListeners.add(listener);
	}
	
	public void removerConnectionListener(org.directtruststandards.timplus.client.connection.ConnectionListener listener)
	{
		connectionListeners.remove(listener);
	}
	
	public void connect()
	{
		connectQueue.offer(ConRequest.CONNECT);
		
		synchronized(connectQueue)
		{
			connectQueue.notify();
		}
	}
	
	public void disconnect()
	{
		if (con != null && con.isConnected())
			connectQueue.offer(ConRequest.DISCONNECT);
		
		synchronized(connectQueue)
		{
			connectQueue.notify();
		}
	}
	
	protected synchronized AbstractXMPPConnection doConnect()
	{
		if (!ConfigurationManager.getInstance().isCompleteConfiguration())
		{
			ConfigurationManager.getInstance().doConfigure(null);
		}
		
		for (org.directtruststandards.timplus.client.connection.ConnectionListener listener : connectionListeners)
		{
			try
			{
				listener.onConnecting();
			}
			catch (Exception e) {}
		}
		

		final Configuration config = ConfigurationManager.getInstance().getConfiguration();
		
		try
		{
			final Builder conBuilder = XMPPTCPConnectionConfiguration.builder().setUsernameAndPassword(config.getUsername(), config.getPassword())
					.setXmppDomain(config.getDomain())
					.setCompressionEnabled(true);
	        
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() 
	        {
	            public java.security.cert.X509Certificate[] getAcceptedIssuers() 
	            {
	                return null;
	            }
	            
	            public void checkClientTrusted(X509Certificate[] certs, String authType) 
	            {
	            }
	            
	            public void checkServerTrusted(X509Certificate[] certs, String authType) 
	            {
	            }
	        }};
	        
	        final SSLContext sc = SSLContext.getInstance("TLSv1.2");
	        sc.init(null, trustAllCerts, new java.security.SecureRandom());
	        
	        conBuilder.setCustomSSLContext(sc);
	        
			if (!StringUtils.isEmpty(config.getServer()))
				conBuilder.setHostAddress(InetAddress.getByName(config.getServer()));
	        
			XMPPTCPConnectionConfiguration xmppConfig = conBuilder.build();
			
			con = new XMPPTCPConnection(xmppConfig);
			
			con.addConnectionListener(new ConnectionListener()
			{
				@Override
				public void connected(XMPPConnection connection)
				{
					
				}

				@Override
				public void authenticated(XMPPConnection connection, boolean resumed)
				{
					
				}

				@Override
				public void connectionClosed()
				{
					
				}

				@Override
				public void connectionClosedOnError(Exception e)
				{
					/*
					 * for now just close and reconnect
					 */
					try
					{
						if (con != null && con.isConnected())
							con.disconnect();
					}
					catch (Exception conExp)
					{
						/* no op */
					}

					System.out.println("Connection was closed.  Reconnecting");
					try
					{
						connect();
					}
					catch(Exception conExp)
					{
						
					}
		
				}
				
				
			});
			
			con.connect();
			
			con.login();
			
			for (org.directtruststandards.timplus.client.connection.ConnectionListener listener : connectionListeners)
			{
				try
				{
					listener.onConnected(con);
				}
				catch (Exception e2) {}
			}
		}
		catch (Exception e)
		{
			for (org.directtruststandards.timplus.client.connection.ConnectionListener listener : connectionListeners)
			{
				try
				{
					listener.onDisconnectedWithError(e);
				}
				catch (Exception e2) {}
			}	
		}
		return con;
			
	}
	
	public AbstractXMPPConnection getConnection()
	{
		return con;
	}
	
	protected class ConnectionOperator implements Runnable
	{
		@Override
		public void run()
		{
			while (true)
			{
				ConRequest request = null;
				try 
				{
					request = connectQueue.poll(10, TimeUnit.SECONDS);
				}
				catch (Exception e) {}
				if (request == ConRequest.CONNECT)
				{
					AbstractXMPPConnection con = doConnect();				
					
					while (con == null || !con.isConnected())
					{
						// wait and try again after 10 seconds
						synchronized (connectQueue)
						{
							try
							{
								connectQueue.wait(10000);
							}
							catch (Exception e) {}
						}
						
						// check if there is another connection request
						if (connectQueue.peek() != null)
							break;
						
						con = doConnect();
					}
				}
				else if (request == ConRequest.DISCONNECT)
				{
					if (con != null && con.isConnected())
						con.disconnect();
				}
			}
		}
	}
	
	protected enum ConRequest
	{
		NONE,
		
		CONNECT,
		
		DISCONNECT
	}
}
