/*
 * Copyright (c) 2014 to present, doctainer gmbh
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.maushake;

import java.io.IOException;

import org.maushake.bootstrap.BootstrapConfig;
import org.maushake.bootstrap.ConnectionController;
import org.maushake.bootstrap.OtpBootstrapContainer;
import org.maushake.bootstrap.OtpConnectionController;

import com.ericsson.otp.erlang.OtpNode;

public class HowTo {

	public static void main(String[] args) throws IOException {
	 final String[] reworkedArgs = args.length == 0 ? new String[]{"abc@xyz"} : args;
	 for(int i=0, n=reworkedArgs.length; i<n; i++) {
		 bootstrapOnBefalfOfNode(new OtpNode(reworkedArgs[i]));
	 }
	}
	
	public static void bootstrapOnBefalfOfNode(OtpNode otpNode) {
		
		// stick with the defaults 
		BootstrapConfig bootstrapConfig = new BootstrapConfig();
		 
		// use OtpConnectionController as is
		// OtpConnectionController conCtrl = OtpConnectionController.create(otpNode, bootstrapConfig);
		
		final OtpBootstrapContainer otpBootstrapContainer = new OtpBootstrapContainer(bootstrapConfig);
		
		otpBootstrapContainer.addNode(otpNode);
		
		}
}
