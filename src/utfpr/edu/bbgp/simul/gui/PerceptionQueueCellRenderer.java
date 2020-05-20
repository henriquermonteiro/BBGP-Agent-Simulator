/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.simul.gui;

import java.awt.Color;
import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

/**
 *
 * @author henri
 */
public class PerceptionQueueCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component c = super.getListCellRendererComponent(list, value, index, false, cellHasFocus);
        if (list.getModel() instanceof PerceptionQueueListModel) {
            PerceptionQueueListModel model = (PerceptionQueueListModel) list.getModel();
            if (model.isActiveElementAt(index)) {
                setForeground(Color.BLACK);
            } else {
                setForeground(Color.lightGray);
            }
        }
        return c;
    }

}
