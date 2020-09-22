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
		
		System.out.println("Starting up connection manager thread.");
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
		
		System.out.println("Getting connection configuration.");
		final Configuration config = ConfigurationManager.getInstance().getConfiguration();
		System.out.println("\tDomain: " + config.getDomain());
		System.out.println("\tUsername: " + config.getUsername());
		if (StringUtils.isEmpty(config.getServer()))
			System.out.println("\tServer: Looking up from DNS SRV");
		else
			System.out.println("\tServer: " + config.getServer());
		
		try
		{
			System.out.println("Creating configuration builder.");
			final Builder conBuilder = XMPPTCPConnectionConfiguration.builder();
			System.out.println("Setting username and password.");
			conBuilder.setUsernameAndPassword(config.getUsername(), config.getPassword());
			System.out.println("Setting domain.");
			conBuilder.setXmppDomain(config.getDomain());
			System.out.println("Setting compression enabled.");
			conBuilder.setCompressionEnabled(true);
	        
			System.out.println("Creating trust manager.");
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
	        
			System.out.println("Creating TLS context.");
	        final SSLContext sc = SSLContext.getInstance("TLSv1.2");
	        sc.init(null, trustAllCerts, new java.security.SecureRandom());
	        
	        conBuilder.setCustomSSLContext(sc);
	        
	        System.out.println("Checking resolution of server name (if set).");
			if (!StringUtils.isEmpty(config.getServer()))
				conBuilder.setHostAddress(InetAddress.getByName(config.getServer()));
	        
			System.out.println("Building the connection object.");
			XMPPTCPConnectionConfiguration xmppConfig = conBuilder.build();
			
			con = new XMPPTCPConnection(xmppConfig);
			
			System.out.println("Adding connection listeners");
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
			
			System.out.println("Making conenction to server.");
			con.connect();
			
			System.out.println("Connection successful.  Attempting to log in.");
			con.login();
			
			for (org.directtruststandards.timplus.client.connection.ConnectionListener listener : connectionListeners)
			{
				try
				{
					listener.onConnected(con);
				}
				catch (Exception e2) {}
			}
			
			System.out.println("Successfully connected and authenticated to server.");
		}
		catch (Exception e)
		{
			System.out.println("Connection or authentication failed: " + e.getMessage());
			
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
			System.out.println("Connection manager thread is running.");
			while (true)
			{
				ConRequest request = null;
				try 
				{
					System.out.println("Looking for connection work in connection queue.");
					request = connectQueue.poll(10, TimeUnit.SECONDS);
				}
				catch (Exception e) {}
				if (request == ConRequest.CONNECT)
				{
					System.out.println("Attempting to connect...");
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
						
						System.out.println("Attempting to connect...");
						con = doConnect();
					}
				}
				else if (request == ConRequest.DISCONNECT)
				{
					System.out.println("Disconnecting.");
					if (con != null && con.isConnected())
						con.disconnect();
				}
				else
				{
					System.out.println("No connection work found.");
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
