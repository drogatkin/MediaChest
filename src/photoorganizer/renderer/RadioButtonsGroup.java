/* PhotoOrganizer - $RCSfile: RadioButtonsGroup.java,v $                                                  
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
 *  Visit http://mediachest.sourceforge.net to get the latest information        
 *  about Rogatkin's products.                                                 
 *  $Id: RadioButtonsGroup.java,v 1.5 2012/10/18 06:58:59 cvs Exp $                      
 */

package photoorganizer.renderer;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class RadioButtonsGroup extends ButtonGroup /* implements ChangeListener */{
	Hashtable sel_map;

	ChangeListener changeListener;

	RadioButtonsGroup() {
		this(null);
	}

	RadioButtonsGroup(ChangeListener changeListener) {
		sel_map = new Hashtable();
		this.changeListener = changeListener;
	}

	public void add(AbstractButton b, int index) {
		sel_map.put(b, new Integer(index));
		super.add(b);
		if (changeListener != null)
			b.addChangeListener(changeListener);
	}

	public int getSelectedIndex() {
		Enumeration els = getElements();
		AbstractButton rb;
		while (els.hasMoreElements()) {
			if ((rb = (AbstractButton) els.nextElement()).isSelected())
				return ((Integer) sel_map.get(rb)).intValue();
		}
		return -1;
	}

	public void setSelectedIndex(int index) {
		Enumeration els = getElements();
		AbstractButton rb;
		while (els.hasMoreElements()) {
			rb = (AbstractButton) els.nextElement();
			rb.setSelected(((Integer) sel_map.get(rb)).intValue() == index);
		}
	}

	public AbstractButton get(int index) {
		Enumeration els = getElements();
		AbstractButton rb;
		while (els.hasMoreElements()) {
			rb = (AbstractButton) els.nextElement();
			if (((Integer) sel_map.get(rb)).intValue() == index)
				return rb;
		}
		return null;
	}

	public void stateChanged(ChangeEvent e) {
	}
}
