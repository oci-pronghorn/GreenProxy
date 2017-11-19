package com.ociweb;

import com.ociweb.gl.api.GreenRuntime;

public class RunServer {

	public static void main(String[] args) {
		GreenRuntime.run(new TestServer(false, 8082, true));
	}

}
