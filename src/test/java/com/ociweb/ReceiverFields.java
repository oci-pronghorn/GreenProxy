package com.ociweb;

public class ReceiverFields {

	public int callTimeTail = 0;
	public int countDownReceived;
	public long[] receivedTimes;
	
	public ReceiverFields(int cpt) {
		countDownReceived = cpt;
		receivedTimes = new long[cpt];
	}
	
	
}
