package com.ociweb;

public class SenderFields {

	public int callTimeHead = 0;
	public int countDownSent;
	public long[] sentTimes;
	public SenderFields(int cpt) {
		countDownSent = cpt;
		sentTimes = new long[cpt];
	}
	
	
}
