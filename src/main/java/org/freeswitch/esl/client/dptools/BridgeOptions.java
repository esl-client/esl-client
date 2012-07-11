package org.freeswitch.esl.client.dptools;

import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: rusekd
 * Date: 7/10/12
 * Time: 12:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class BridgeOptions {

	private TimeUnit timeoutUnit = TimeUnit.MILLISECONDS;
	private int timeoutLength;

	private boolean bypassMedia = false;

	private String effectiveCallerIdName = "";
	private String effectiveCallerIdNumber = "";

	private String ringBack;

	public TimeUnit getTimeoutUnit() {
		return timeoutUnit;
	}

	public void setTimeoutUnit(TimeUnit timeoutUnit) {
		this.timeoutUnit = timeoutUnit;
	}

	public int getTimeoutLength() {
		return timeoutLength;
	}

	public void setTimeoutLength(int timeoutLength) {
		this.timeoutLength = timeoutLength;
	}

	public boolean isBypassMedia() {
		return bypassMedia;
	}

	public void setBypassMedia(boolean bypassMedia) {
		this.bypassMedia = bypassMedia;
	}

	public String getEffectiveCallerIdName() {
		return effectiveCallerIdName;
	}

	public void setEffectiveCallerIdName(String effectiveCallerIdName) {
		this.effectiveCallerIdName = effectiveCallerIdName;
	}

	public String getEffectiveCallerIdNumber() {
		return effectiveCallerIdNumber;
	}

	public void setEffectiveCallerIdNumber(String effectiveCallerIdNumber) {
		this.effectiveCallerIdNumber = effectiveCallerIdNumber;
	}

	public String getRingBack() {
		return ringBack;
	}

	public void setRingBack(String ringBack) {
		this.ringBack = ringBack;
	}
}
