/*
 *    Copyright (C) 2008 Igor Kriznar
 *    
 *    This file is part of GTD-Free.
 *    
 *    GTD-Free is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *    
 *    GTD-Free is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *    
 *    You should have received a copy of the GNU General Public License
 *    along with GTD-Free.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.gtdfree.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.ComboBoxEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.plaf.ComboBoxUI;

import org.gtdfree.ApplicationHelper;
import org.gtdfree.model.Folder;
import org.gtdfree.model.FolderEvent;
import org.gtdfree.model.GTDModel;
import org.gtdfree.model.GTDModelAdapter;
import org.gtdfree.model.Project;
import org.gtdfree.model.Folder.FolderType;

/**
 * @author ikesan
 *
 */
public class ProjectsCombo extends JComboBox {
	
	class ProjectEditor extends JTextField implements ComboBoxEditor {
		
		private static final long serialVersionUID = 1L;
		Object item;
		
		public ProjectEditor() {
			addKeyListener(new KeyAdapter() {
			
				@Override
				public void keyTyped(KeyEvent e) {
					if (e.getKeyChar()==KeyEvent.VK_ESCAPE) {
						setItem(item);
					}
				}
			
			});
			setMargin(new Insets(0,0,0,0));
		}

		/* (non-Javadoc)
		 * @see javax.swing.ComboBoxEditor#getEditorComponent()
		 */
		public Component getEditorComponent() {
			return this;
		}

		/* (non-Javadoc)
		 * @see javax.swing.ComboBoxEditor#getItem()
		 */
		public Object getItem() {
			if (item instanceof Project && getText().length()>0) {
				if (!getText().equals(((Project)item).getName())) {
					((Project)item).rename(getText());
				}
			} else if (getText().length()>0) {
				item= getText();
			}
			
			return item;
		}

		/* (non-Javadoc)
		 * @see javax.swing.ComboBoxEditor#selectAll()
		 */
		@Override
		public void selectAll() {
			select(0, getText().length()-1);
		}

		/* (non-Javadoc)
		 * @see javax.swing.ComboBoxEditor#setItem(java.lang.Object)
		 */
		public void setItem(Object anObject) {
			item= anObject;
			if (item instanceof Project) {
				setText(((Project)item).getName());
			} else {
				setText(item!=null && item!=NONE ? item.toString() : ApplicationHelper.EMPTY_STRING);
			}
		}
		
	}
	
	private static final long serialVersionUID = 1L;
	//private static final String NEW= "<New>";
	private static final String NONE= "<None>";
	
	public static final String SELECTED_PROJECT_PROPERTY_NAME = "selectedProject";
	
	private GTDModel gtdModel;
	private DefaultComboBoxModel comboModel;
	private Project selectedProject;
	private boolean reloading=false;
		
	public ProjectsCombo() {
		initialize();
	}
	
	@Override
	public void setUI(ComboBoxUI ui) {
		super.setUI(ui);
	}

	private void initialize() {
		
		setFont(getFont().deriveFont(Font.ITALIC));
		
		comboModel= new DefaultComboBoxModel();
		
		comboModel.addListDataListener(new ListDataListener() {
		
			public void intervalRemoved(ListDataEvent e) {
				//
		
			}
		
			public void intervalAdded(ListDataEvent e) {
				//
		
			}
		
			public void contentsChanged(ListDataEvent e) {
				if (comboModel.getSelectedItem() instanceof Project) {
					setSelectedProject((Project)comboModel.getSelectedItem());
				} else {
					if (comboModel.getSelectedItem()==NONE || comboModel.getSelectedItem()==null) {
						setSelectedProject(null);
					} else {
						Project p= (Project)gtdModel.createFolder(comboModel.getSelectedItem().toString(), FolderType.PROJECT);
						reload();
						setSelectedProject(p);
					}
				}
			}
		
		});
		
		setModel(comboModel);
		
		setRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;
			@Override
			public Component getListCellRendererComponent(JList list, Object value,
					int index, boolean isSelected, boolean cellHasFocus) {
				if (value instanceof Project) {
					value= ((Project)value).getName();
				};
				return super.getListCellRendererComponent(list, value, index,
						isSelected, cellHasFocus);
			}
		
		});
		
		setEditor(new ProjectEditor());
		
		setEditable(true);
	}
	
	@Override
	public void addNotify() {
		super.addNotify();
		firePropertyChange("preferedSize", null, getPreferredSize());
	}
	
	private void reload() {
		reloading=true;
		
		Project selected= getSelectedProject();
		
		comboModel.removeAllElements();
		
		comboModel.addElement(NONE);
		
		if (gtdModel==null) {
			return;
		}
		
		//comboModel.addElement(NEW);
		
		Project[] p= gtdModel.projects();
		
		Arrays.sort(p, new Comparator<Project>() {
		
			public int compare(Project o1, Project o2) {
				return o1.getName().compareTo(o2.getName());
			}
		
		});

		boolean contains=false;
		for (int i = 0; i < p.length; i++) {
			contains= contains || p[i].equals(selected); 
			if (!p[i].isClosed()) {
				comboModel.addElement(p[i]);
			}
		}
		
		if (contains) {
			setSelectedProject(selected);
		}
		
		reloading=false;

		if (!contains) {
			setSelectedProject(null);
		}
	}

	/**
	 * @return the gtdModel
	 */
	public GTDModel getGTDModel() {
		return gtdModel;
	}

	/**
	 * @param m the gtdModel to set
	 */
	public void setGTDModel(GTDModel m) {
		this.gtdModel = m;
		reload();
		m.addGTDModelListener(new GTDModelAdapter() {
			@Override
			public void folderRemoved(Folder folder) {
				reload();
			}
		
			@Override
			public void folderModified(FolderEvent folder) {
				reload();
			}
		
			@Override
			public void folderAdded(Folder folder) {
				reload();
			}
		
		});
	}

	/**
	 * @return the selectedProject
	 */
	public Project getSelectedProject() {
		return selectedProject;
	}

	/**
	 * @param selectedProject the selectedProject to set
	 */
	public void setSelectedProject(Project selectedProject) {
		if (selectedProject == this.selectedProject) {
			return;
		}
		Project old= this.selectedProject;
		this.selectedProject = selectedProject;
		
		if (selectedProject!=comboModel.getSelectedItem()) {
			if (selectedProject==null) {
				comboModel.setSelectedItem(comboModel.getElementAt(0));
			} else {
				comboModel.setSelectedItem(selectedProject);
			}
		}
		
		if (!reloading) {
			firePropertyChange(SELECTED_PROJECT_PROPERTY_NAME, old, selectedProject);
		}
	}
	
	public int getPreferredWidth() {
		Dimension d= getUI().getPreferredSize(this);
		return d.width-d.height+getInsets().bottom+getInsets().top+4;
	}
	
	@Override
	public Dimension getPreferredSize() {
		if (isPreferredSizeSet()) {
			return super.getPreferredSize();
		}

		Dimension d= getUI().getPreferredSize(this);
		return new Dimension(d.width+d.height/2,d.height);
		
	}
	
	@Override
	public void setOpaque(boolean isOpaque) {
		super.setOpaque(isOpaque);
		Component[] c= getComponents();
		if (c!=null) {
			for (int i = 0; i < c.length; i++) {
				if (c[i] instanceof JComponent) {
					((JComponent)c[i]).setOpaque(isOpaque);
				}
			}
		}
	}

}
