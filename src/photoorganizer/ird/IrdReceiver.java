/* MediaChest - $RCSfile: IrdReceiver.java,v $                                                  
 * Copyright (C) 2001 Dmitriy Rogatkin.  All rights reserved.                  
 * Redistribution and use in source and binary forms, with or without          
 * modification, are permitted provided that the following conditions          
 * are met:                                                                    
 * 1. Redistributions of source code must retain the above copyright           
 *    notice, this list of conditions and the following disclaimer.            
 * 2. Redistributions in binary form must reproduce the above copyright        
 *    notice, this list of conditions and the following disclaimer in the      
 *    documentation and/or other materials provided with the distribution.     
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND     
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE      
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR 
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL     
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT         
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY  
 *  OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF     
 *  SUCH DAMAGE.                                                               
 *                                                                             
 *  Visit http://drogatkin.openestate.net to get the latest information        
 *  about Rogatkin's products.                                                 
 *  $Id: IrdReceiver.java,v 1.4 2012/10/18 06:58:59 cvs Exp $                      
 */

package photoorganizer.ird;

import java.awt.Component;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TooManyListenersException;

import javax.comm.CommPortIdentifier;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;
import javax.comm.SerialPortEvent;
import javax.comm.SerialPortEventListener;
import javax.comm.UnsupportedCommOperationException;

import org.aldan3.util.DataConv;
import org.aldan3.util.IniPrefs;

import photoorganizer.IrdControllable;
import photoorganizer.PhotoOrganizer;
import photoorganizer.renderer.RemoteOptionsTab;

// TODO: implements some generic remote control interface
// and create particular instance at startup
public class IrdReceiver extends Component implements SerialPortEventListener {
	public static final int PORT_TIMEOUT = 2000; // millis

	public void init() {
		Enumeration portList = CommPortIdentifier.getPortIdentifiers();
		CommPortIdentifier portId;
		while (portList.hasMoreElements()) {
			portId = (CommPortIdentifier) portList.nextElement();
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				System.err.println("Checking port " + portId.getName());
				if ((irdPort = isIrdConnected(portId)) != null) {
					System.err.println(portId.getName() + " connected to irman.");
					break;
				}
				portId = null;
			}
		}
		if (irdPort != null) {
			try {
				irdPort.addEventListener(this);
			} catch (TooManyListenersException e) {
			}
			try {
				irdStream = irdPort.getInputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
			irdPort.notifyOnDataAvailable(true);
		}
		controllableList = new ArrayList();
		registerList = new ArrayList();
	}

	public void standBy(IniPrefs ser) {
		componentsMap = RemoteOptionsTab.loadComponentsMap(ser, this);
	}

	protected SerialPort isIrdConnected(CommPortIdentifier portId) {
		SerialPort serialPort;
		boolean success = false;
		try {
			serialPort = (SerialPort) portId.open(PhotoOrganizer.PROGRAMNAME, PORT_TIMEOUT);
		} catch (PortInUseException e) {
			e.printStackTrace();
			return null;
		}
		InputStream inputStream = null;
		OutputStream outputStream = null;
		try {
			try {
				inputStream = serialPort.getInputStream();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
			/*
			 * try { serialPort.addEventListener(this); } catch
			 * (TooManyListenersException e) {}
			 * 
			 * serialPort.notifyOnDataAvailable(true);
			 */
			try {
				outputStream = serialPort.getOutputStream();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}

			try {
				serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
						SerialPort.PARITY_NONE);
			} catch (UnsupportedCommOperationException e) {
				return null;
			}
			/*
			 * serialPort.setDTR(false); serialPort.setRTS(false); try {
			 * Thread.sleep(510); } catch (InterruptedException e) {}
			 */
			serialPort.setDTR(true);
			serialPort.setRTS(true);
			// microcontroller startup time
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
			}
			// read garbage
			byte[] readBuffer = new byte[6];

			try {
				int numBytes = 0;
				while (inputStream.available() > 0) {
					numBytes = inputStream.read(readBuffer);
				}
				System.err.print(new String(readBuffer, 0, numBytes));
			} catch (IOException e) {
				return null;
			}
			// activate
			try {
				outputStream.write('I');
				try {
					Thread.sleep(510);
				} catch (InterruptedException e) {
				}
				outputStream.write('R');
			} catch (IOException e) {
				return null;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
			try {
				int numBytes = 0;
				while (inputStream.available() > 0) {
					numBytes = inputStream.read(readBuffer);
				}
				if ("OK".equals(new String(readBuffer, 0, numBytes))) {
					success = true;
					return serialPort;
				}
			} catch (IOException e) {
				return null;
			}
		} finally {
			if (!success) {
				try {
					inputStream.close();
				} catch (IOException e) {
				}
				try {
					outputStream.close();
				} catch (IOException e) {
				}
				serialPort.close();
			}
		}
		return null;
	}

	public void serialEvent(SerialPortEvent event) {
		switch (event.getEventType()) {
		case SerialPortEvent.BI:
		case SerialPortEvent.OE:
		case SerialPortEvent.FE:
		case SerialPortEvent.PE:
		case SerialPortEvent.CD:
		case SerialPortEvent.CTS:
		case SerialPortEvent.DSR:
		case SerialPortEvent.RI:
		case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
			break;
		case SerialPortEvent.DATA_AVAILABLE:
			byte[] readBuffer = new byte[20];

			try {
				if (irdStream.available() > 0) {
					int numBytes = irdStream.read(readBuffer);
					if (learnMonitor != null)
						synchronized (learnMonitor) {
							learnMonitor[0] = DataConv.bytesToHex(readBuffer, 0, numBytes);
							learnMonitor.notify();
						}
					else {
						// put command to buffer
						// get map and do action
						if (componentsMap != null)
							synchronized (controllableList) {
								ListIterator li = controllableList.listIterator(controllableList.size());
								String cmd = DataConv.bytesToHex(readBuffer, 0, numBytes);
								System.err.println("Looking for command " + cmd + " in list of "
										+ controllableList.size());
								while (li.hasPrevious()) {
									IrdControllable c = (IrdControllable) li.previous(); // ((WeakReference)li.previous()).get();
									// get map for
									Map cm = (Map) componentsMap.get(c.getName());
									if (cm != null) {
										String keyCode = (String) cm.get(cmd);
										if (keyCode != null) {
											System.err.println("Found command " + keyCode + " for code " + cmd);
											if (c.doAction(keyCode))
												break;
										}
									} else
										System.err.println("No map has been defined for " + c);
								}
							}
					}
				}
			} catch (IOException e) {
			}
			break;
		}
	}

	public void setOnTop(IrdControllable controllable) {
		System.err.println("Add on top " + controllable.getName());
		// TODO: reimplement using new WeakReference(controllable)
		synchronized (controllableList) {
			int pos = controllableList.lastIndexOf(controllable);
			if (pos < 0)
				controllableList.add(controllable);
			else {
				// swap
				controllableList.add(controllableList.remove(pos));
			}
		}
	}

	public void register(IrdControllable controllable) {
		synchronized (registerList) {
			registerList.add(controllable);
		}
	}

	public List getRegisteredList() {
		return registerList;
	}

	public Map getComponentsMap() {
		return componentsMap;
	}

	public SerialPort getPort() {
		return irdPort;
	}

	public String learn(Object[] monitor) {
		learnMonitor = monitor;
		synchronized (monitor) {
			try {
				monitor.wait();
			} catch (InterruptedException ie) {
				monitor[0] = null;
			}
			learnMonitor = null;
		}
		return (String) monitor[0];
	}

	protected SerialPort irdPort;

	protected InputStream irdStream;

	protected List controllableList, registerList;

	protected Object[] learnMonitor;

	protected Map componentsMap;
}
