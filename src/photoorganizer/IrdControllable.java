/* PhotoOrganizer - $RCSfile: IrdControllable.java,v $                                              
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
 *  $Id: IrdControllable.java,v 1.2 2012/10/18 06:59:01 cvs Exp $            
 */                                                                             
package photoorganizer;

import java.util.Iterator;
// TODO: rename to RemoteControllable
public interface IrdControllable { 
	public String getName();
	// TODO: reconsider usage Iterator, because List can be more convenient
	public Iterator	getKeyMnemonics();
	public boolean doAction(String keyCode);
	public void bringOnTop();
}
