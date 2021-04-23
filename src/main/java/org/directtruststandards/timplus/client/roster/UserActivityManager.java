package org.directtruststandards.timplus.client.roster;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Timer;

import org.jivesoftware.smack.packet.Presence;

public class UserActivityManager implements ActionListener, AWTEventListener
{
	static protected UserActivityManager INSTANCE;
	
	public final static long KEY_EVENTS = AWTEvent.KEY_EVENT_MASK;

	public final static long MOUSE_EVENTS =
		AWTEvent.MOUSE_MOTION_EVENT_MASK + AWTEvent.MOUSE_EVENT_MASK;

	public final static long USER_EVENTS = KEY_EVENTS + MOUSE_EVENTS;

	final protected Collection<UserActivityListener> activityListeners;
	
	protected int interval;
	
	protected long eventMask;
	
	protected Timer timer = new Timer(0, this);

	protected Presence.Mode mode;
	
	protected AtomicBoolean running;
	
	public static synchronized UserActivityManager getInstance()
	{
		if (INSTANCE == null)
			INSTANCE = new UserActivityManager();
		
		return INSTANCE;
	}
	
	/*
	 *  Use a default inactivity interval of 10 minute and listen for
	 *  USER_EVENTS
	 */
	private UserActivityManager()
	{
		this(10);
	}

	/*
	 *	Specify the inactivity interval and listen for USER_EVENTS
	 */
	public UserActivityManager(int interval)
	{
		this(interval, USER_EVENTS);
	}

	/*
	 *  Specify the inactivity interval and the events to listen for
	 */
	public UserActivityManager(int minutes, long eventMask)
	{
		setInterval( minutes );
		setEventMask( eventMask );
		
		this.mode = Presence.Mode.available;
		this.activityListeners = new ArrayList<>();
		this.running = new AtomicBoolean(false);
	}

	/*
	 *  Adds an activity listener
	 */
	public void addUserActivityListener(UserActivityListener listener)
	{
		activityListeners.add(listener);
	}

	/*
	 *  Removes an activity listener
	 */
	public void removeUserActivityListener(UserActivityListener listener)
	{
		activityListeners.remove(listener);
	}
	
	/*
	 *  The interval before the Action is invoked specified in minutes
	 */
	public void setInterval(int minutes)
	{
		this.interval = (minutes * 60000);
		timer.setInitialDelay(interval);
		
	}

	/*
	 *	A mask specifying the events to be passed to the AWTEventListener
	 */
	public void setEventMask(long eventMask)
	{
		this.eventMask = eventMask;
	}

	/*
	 *  Start listening for events.
	 */
	public void start()
	{
		if (!running.get())
		{
			timer.setInitialDelay(interval);
			timer.setRepeats(false);
			timer.start();
			Toolkit.getDefaultToolkit().addAWTEventListener(this, eventMask);
		}
		
		running.set(true);
	}

	/*
	 *  Stop listening for events
	 */
	public void stop()
	{
		running.set(true);
		Toolkit.getDefaultToolkit().removeAWTEventListener(this);
		timer.stop();
	}

	//  Implement ActionListener for the Timer
	@Override
	public void actionPerformed(ActionEvent e)
	{
		mode = Presence.Mode.away;
		
		fireModeChange(mode);
	}

	//  Implement AWTEventListener
	@Override
	public void eventDispatched(AWTEvent e)
	{
		if (running.get())
		{
			timer.restart();
			if (mode != Presence.Mode.available)
			{
				mode = Presence.Mode.available;
				fireModeChange(mode);
			}
		}
	}
	
	protected void fireModeChange(Presence.Mode mode)
	{
		for (UserActivityListener listener : activityListeners)
		{
			try
			{
				listener.onUserActivityStateChange(mode);
			}
			catch (Exception e) {}
		}
	}
}
