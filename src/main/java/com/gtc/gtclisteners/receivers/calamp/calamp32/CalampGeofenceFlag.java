package com.gtc.gtclisteners.receivers.calamp.calamp32;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

//import org.apache.log4j.Logger;

/**
 * This class is a timed flag used to indicate if a GPS unit is in the process receiving a
 * geofence update.  An {@link AtomicBoolean} acts as the flag.  A task timer is started every
 * time the flag is set <code>true</code>.  When any message is sent, the timer is cancelled.
 * In this way, the time is reset when the flag is set <code>true</code>, and the timer is
 * cancelled when the flag was set <code>false</code>.
 */
public class CalampGeofenceFlag {

	//protected final Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Indicates that unit is in the process of a geofence OTA.  The flag is <code>true</code> if
	 * the unit is currently receiving a geofence OTA.
	 */
	private AtomicBoolean flag;

	/**
	 * Timer used to determine when to set the flag to <code>false</code>.
	 */
	private Timer timer;

	public CalampGeofenceFlag() {
		this.flag = new AtomicBoolean();
	}

	public CalampGeofenceFlag(boolean flag) {
		this();
		this.flag.set(flag);
	}

	/**
	 * Sets the flag and cancels the timer.  If the specified flag is <code>true</code>
	 * the time is restarted.
	 *
	 * @param flag boolean value to be set
	 */
	public void setFlag(boolean flag) {
		this.flag.set(flag);
		if(timer != null) {
			timer.cancel();
		}
		if(this.flag.get()) {
			timer = new Timer("flag timer");
			FlagTask task = new FlagTask(this.flag);
			timer.schedule(task, 10000);
		}
	}

	public boolean getFlag() {
		return flag.get();
	}

	/**
	 * When this task is fired, it sets the specified boolean to false.  This is used to ensure
	 * that an OTA flag won't remain in the 'true' state indefinitely.  This can occur when a
	 * message is sent to the unit, but there is no response.  In that case, the receiver
	 * wait indefinitely (with the flag in the 'true' state) for a response from the unit.
	 */
	private class FlagTask extends TimerTask {

		private AtomicBoolean flag;

		public FlagTask(AtomicBoolean flag) {
			this.flag = flag;
		}

		@Override
		public void run() {
			//logger.info("OTA timer expired - releasing flag");
			flag.set(false);
		}
	}
}
